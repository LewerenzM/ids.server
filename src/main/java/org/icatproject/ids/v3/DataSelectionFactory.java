package org.icatproject.ids.v3;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.icatproject.Datafile;
import org.icatproject.Dataset;
import org.icatproject.ICAT;
import org.icatproject.IcatExceptionType;
import org.icatproject.IcatException_Exception;
import org.icatproject.icat.client.IcatException;
import org.icatproject.icat.client.Session;
import org.icatproject.ids.IcatReader;
import org.icatproject.ids.IdsBean;
import org.icatproject.ids.PropertyHandler;
import org.icatproject.ids.StorageUnit;
import org.icatproject.ids.exceptions.BadRequestException;
import org.icatproject.ids.exceptions.InsufficientPrivilegesException;
import org.icatproject.ids.exceptions.InternalException;
import org.icatproject.ids.exceptions.NotFoundException;
import org.icatproject.ids.v3.models.DataFileInfo;
import org.icatproject.ids.v3.models.DataSetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;



public class DataSelectionFactory {

    private final static Logger logger = LoggerFactory.getLogger(DataSelectionFactory.class);

    private static DataSelectionFactory instance = null;

    private PropertyHandler propertyHandler;
    private ICAT icat;
    private IcatReader icatReader;
    private org.icatproject.icat.client.ICAT restIcat;
    private int maxEntities;

    public enum Returns {
        DATASETS, DATASETS_AND_DATAFILES, DATAFILES
    }

    public static DataSelectionFactory getInstance() throws InternalException {
        if (instance == null) {
            instance = new DataSelectionFactory();
        }
        return instance;
    }

    public static DataSelectionV3Base get(String userSessionId,
                            String investigationIds, String datasetIds, String datafileIds, Returns returns) throws InternalException, BadRequestException, NotFoundException, InsufficientPrivilegesException {

        return DataSelectionFactory.getInstance().getSelection(userSessionId, investigationIds, datasetIds, datafileIds, returns);
    }


    
    public static DataSelectionV3Base get(Map<Long, DataSetInfo> dsInfos, Set<DataFileInfo> dfInfos, Set<Long> emptyDatasets) throws InternalException {
        List<Long> dsids = new ArrayList<Long>(dsInfos.keySet());
        List<Long> dfids = new ArrayList<Long>();
        for(DataFileInfo dfInfo: dfInfos) {
            dfids.add(dfInfo.getId());
        }
        return DataSelectionFactory.getInstance().createSelection(dsInfos, dfInfos, emptyDatasets, new ArrayList<Long>(), dsids, dfids);
    }

    private DataSelectionFactory() throws InternalException
    {
        logger.info("### Constructing...");
        this.propertyHandler = ServiceProvider.getInstance().getPropertyHandler();
        this.icat = propertyHandler.getIcatService();
        this.icatReader = ServiceProvider.getInstance().getIcatReader();
        this.restIcat = propertyHandler.getRestIcat();
        this.maxEntities = propertyHandler.getMaxEntities();
        logger.info("### Constructing finished");
    }

    private DataSelectionV3Base createSelection(Map<Long, DataSetInfo> dsInfos, Set<DataFileInfo> dfInfos, Set<Long> emptyDatasets, List<Long> invids2, List<Long> dsids, List<Long> dfids) throws InternalException {

        StorageUnit storageUnit = this.propertyHandler.getStorageUnit();

        if(storageUnit == null )
            return new DataSelectionForSingleLevelStorage(dsInfos, dfInfos, emptyDatasets, invids2, dsids, dfids);

        else if (storageUnit == StorageUnit.DATAFILE)
            return new DataSelectionForStorageUnitDatafile(dsInfos, dfInfos, emptyDatasets, invids2, dsids, dfids);

        else if(storageUnit == StorageUnit.DATASET)
            return new DataSelectionForStorageUnitDataset(dsInfos, dfInfos, emptyDatasets, invids2, dsids, dfids);

        else throw new InternalException("StorageUnit " + storageUnit + " unknown. Maybe you forgot to handle a new StorageUnit here?");

    }

    private DataSelectionV3Base getSelection( String userSessionId,
                         String investigationIds, String datasetIds, String datafileIds, Returns returns) throws InternalException, BadRequestException, NotFoundException, InsufficientPrivilegesException {
        
        List<Long> dfids = getValidIds("datafileIds", datafileIds);
        List<Long> dsids = getValidIds("datasetIds", datasetIds);
        List<Long> invids = getValidIds("investigationIds", investigationIds);
        boolean dfWanted = returns == Returns.DATASETS_AND_DATAFILES || returns == Returns.DATAFILES;
        boolean dsWanted = returns == Returns.DATASETS_AND_DATAFILES || returns == Returns.DATASETS;

        Session userRestSession = restIcat.getSession(userSessionId);
        // by default use the user's REST ICAT session
        Session restSessionToUse = userRestSession;
        
        try {
            logger.debug("useReaderForPerformance = {}", propertyHandler.getUseReaderForPerformance());
            if (propertyHandler.getUseReaderForPerformance()) {
                // if this is set, use a REST session for the reader account where possible
                // to improve performance due to the final database queries being simpler
                restSessionToUse = restIcat.getSession(this.icatReader.getSessionId());
            }
        } catch (IcatException_Exception e) {
            throw new InternalException(e.getClass() + " " + e.getMessage());
        }

        logger.debug("dfids: {} dsids: {} invids: {}", dfids, dsids, invids);

        return prepareFromIds(dfWanted, dsWanted, dfids, dsids, invids, userSessionId, restSessionToUse, userRestSession);
    }

    

    private DataSelectionV3Base prepareFromIds(boolean dfWanted, boolean dsWanted, List<Long> dfids, List<Long> dsids, List<Long> invids, String userSessionId, Session restSessionToUse, Session userRestSession)
            throws NotFoundException, InsufficientPrivilegesException, InternalException, BadRequestException {
        var dsInfos = new HashMap<Long, DataSetInfo>();
        var emptyDatasets = new HashSet<Long>();
        var dfInfos = new HashSet<DataFileInfo>();
        if (dfWanted) {
            dfInfos = new HashSet<>();
        }

        try {

            for (Long dfid : dfids) {
                List<Object> dss = icat.search(userSessionId,
                        "SELECT ds FROM Dataset ds JOIN ds.datafiles df WHERE df.id = " + dfid
                                + " AND df.location IS NOT NULL INCLUDE ds.investigation.facility");
                if (dss.size() == 1) {
                    Dataset ds = (Dataset) dss.get(0);
                    long dsid = ds.getId();
                    dsInfos.put(dsid, new DataSetInfo(ds));
                    if (dfWanted) {
                        Datafile df = (Datafile) icat.get(userSessionId, "Datafile", dfid);
                        String location = IdsBean.getLocation(dfid, df.getLocation());
                        dfInfos.add(
                                new DataFileInfo(dfid, df.getName(), location, df.getCreateId(), df.getModId(), dsid));
                    }
                } else {
                    // Next line may reveal a permissions problem
                    icat.get(userSessionId, "Datafile", dfid);
                    throw new NotFoundException("Datafile " + dfid);
                }
            }

            for (Long dsid : dsids) {
                Dataset ds = (Dataset) icat.get(userSessionId, "Dataset ds INCLUDE ds.investigation.facility", dsid);
                dsInfos.put(dsid, new DataSetInfo(ds));
                // dataset access for the user has been checked so the REST session for the
                // reader account can be used if the IDS setting to allow this is enabled
                String query = "SELECT min(df.id), max(df.id), count(df.id) FROM Datafile df WHERE df.dataset.id = "
                        + dsid + " AND df.location IS NOT NULL";
                JsonArray result = Json.createReader(new ByteArrayInputStream(restSessionToUse.search(query).getBytes()))
                        .readArray().getJsonArray(0);
                if (result.getJsonNumber(2).longValueExact() == 0) { // Count 0
                    emptyDatasets.add(dsid);
                } else if (dfWanted) {
                    manyDfs(dfInfos, dsid, restSessionToUse, result);
                }
            }

            for (Long invid : invids) {
                String query = "SELECT min(ds.id), max(ds.id), count(ds.id) FROM Dataset ds WHERE ds.investigation.id = "
                        + invid;
                JsonArray result = Json.createReader(new ByteArrayInputStream(userRestSession.search(query).getBytes()))
                        .readArray().getJsonArray(0);
                manyDss(dsInfos, emptyDatasets, dfInfos, invid, dfWanted, userRestSession, restSessionToUse, result);

            }

        } catch (IcatException_Exception e) {
            IcatExceptionType type = e.getFaultInfo().getType();
            if (type == IcatExceptionType.INSUFFICIENT_PRIVILEGES || type == IcatExceptionType.SESSION) {
                throw new InsufficientPrivilegesException(e.getMessage());
            } else if (type == IcatExceptionType.NO_SUCH_OBJECT_FOUND) {
                throw new NotFoundException(e.getMessage());
            } else {
                throw new InternalException(e.getClass() + " " + e.getMessage());
            }

        } catch (IcatException e) {
            org.icatproject.icat.client.IcatException.IcatExceptionType type = e.getType();
            if (type == org.icatproject.icat.client.IcatException.IcatExceptionType.INSUFFICIENT_PRIVILEGES
                    || type == org.icatproject.icat.client.IcatException.IcatExceptionType.SESSION) {
                throw new InsufficientPrivilegesException(e.getMessage());
            } else if (type == org.icatproject.icat.client.IcatException.IcatExceptionType.NO_SUCH_OBJECT_FOUND) {
                throw new NotFoundException(e.getMessage());
            } else {
                throw new InternalException(e.getClass() + " " + e.getMessage());
            }
        }
        /*
         * TODO: don't calculate what is not needed - however this ensures that
         * the flag is respected
         */
        if (!dsWanted) {
            dsInfos = null;
            emptyDatasets = null;
        }

        return this.createSelection(dsInfos, dfInfos, emptyDatasets, invids, dsids, dfids);
    }

    /**
     * Checks to see if the investigation, dataset or datafile id list is a
     * valid comma separated list of longs. No spaces or leading 0's. Also
     * accepts null.
     */
    public static List<Long> getValidIds(String thing, String idList) throws BadRequestException {

        List<Long> result;
        if (idList == null) {
            result = Collections.emptyList();
        } else {
            String[] ids = idList.split("\\s*,\\s*");
            result = new ArrayList<>(ids.length);
            for (String id : ids) {
                try {
                    result.add(Long.parseLong(id));
                } catch (NumberFormatException e) {
                    throw new BadRequestException("The " + thing + " parameter '" + idList + "' is not a valid "
                            + "string representation of a comma separated list of longs");
                }
            }
        }
        return result;
    }

    private void manyDfs(HashSet<DataFileInfo> dfInfos, long dsid, Session restSessionToUse, JsonArray result)
            throws IcatException, InsufficientPrivilegesException, InternalException {
        // dataset access for the user has been checked so the REST session for the
        // reader account can be used if the IDS setting to allow this is enabled
        long min = result.getJsonNumber(0).longValueExact();
        long max = result.getJsonNumber(1).longValueExact();
        long count = result.getJsonNumber(2).longValueExact();
        logger.debug("manyDfs min: {} max: {} count: {}", min, max, count);
        if (count != 0) {
            if (count <= maxEntities) {
                String query = "SELECT df.id, df.name, df.location, df.createId, df.modId FROM Datafile df WHERE df.dataset.id = "
                        + dsid + " AND df.location IS NOT NULL AND df.id BETWEEN " + min + " AND " + max;
                result = Json.createReader(new ByteArrayInputStream(restSessionToUse.search(query).getBytes())).readArray();
                for (JsonValue tupV : result) {
                    JsonArray tup = (JsonArray) tupV;
                    long dfid = tup.getJsonNumber(0).longValueExact();
                    String location = IdsBean.getLocation(dfid, tup.getString(2, null));
                    dfInfos.add(
                            new DataFileInfo(dfid, tup.getString(1), location, tup.getString(3), tup.getString(4), dsid));
                }
            } else {
                long half = (min + max) / 2;
                String query = "SELECT min(df.id), max(df.id), count(df.id) FROM Datafile df WHERE df.dataset.id = "
                        + dsid + " AND df.location IS NOT NULL AND df.id BETWEEN " + min + " AND " + half;
                result = Json.createReader(new ByteArrayInputStream(restSessionToUse.search(query).getBytes())).readArray()
                        .getJsonArray(0);
                manyDfs(dfInfos, dsid, restSessionToUse, result);
                query = "SELECT min(df.id), max(df.id), count(df.id) FROM Datafile df WHERE df.dataset.id = " + dsid
                        + " AND df.location IS NOT NULL AND df.id BETWEEN " + (half + 1) + " AND " + max;
                result = Json.createReader(new ByteArrayInputStream(restSessionToUse.search(query).getBytes())).readArray()
                        .getJsonArray(0);
                manyDfs(dfInfos, dsid, restSessionToUse, result);
            }
        }
    }

    private void manyDss(HashMap<Long, DataSetInfo> dsInfos, HashSet<Long> emptyDatasets, HashSet<DataFileInfo> dfInfos, Long invid, boolean dfWanted, Session userRestSession, Session restSessionToUseForDfs, JsonArray result)
            throws IcatException, InsufficientPrivilegesException, InternalException {
        long min = result.getJsonNumber(0).longValueExact();
        long max = result.getJsonNumber(1).longValueExact();
        long count = result.getJsonNumber(2).longValueExact();
        logger.debug("manyDss min: {} max: {} count: {}", min, max, count);
        if (count != 0) {
            if (count <= maxEntities) {
                String query = "SELECT inv.name, inv.visitId, inv.facility.id,  inv.facility.name FROM Investigation inv WHERE inv.id = "
                        + invid;
                result = Json.createReader(new ByteArrayInputStream(userRestSession.search(query).getBytes())).readArray();
                if (result.size() == 0) {
                    return;
                }
                result = result.getJsonArray(0);
                String invName = result.getString(0);
                String visitId = result.getString(1);
                long facilityId = result.getJsonNumber(2).longValueExact();
                String facilityName = result.getString(3);

                query = "SELECT ds.id, ds.name, ds.location FROM Dataset ds WHERE ds.investigation.id = " + invid
                        + " AND ds.id BETWEEN " + min + " AND " + max;
                result = Json.createReader(new ByteArrayInputStream(userRestSession.search(query).getBytes())).readArray();
                for (JsonValue tupV : result) {
                    JsonArray tup = (JsonArray) tupV;
                    long dsid = tup.getJsonNumber(0).longValueExact();
                    dsInfos.put(dsid, new DataSetInfo(dsid, tup.getString(1), tup.getString(2, null), invid, invName,
                            visitId, facilityId, facilityName));

                    query = "SELECT min(df.id), max(df.id), count(df.id) FROM Datafile df WHERE df.dataset.id = "
                            + dsid + " AND df.location IS NOT NULL";
                    result = Json.createReader(new ByteArrayInputStream(userRestSession.search(query).getBytes()))
                            .readArray().getJsonArray(0);
                    if (result.getJsonNumber(2).longValueExact() == 0) {
                        emptyDatasets.add(dsid);
                    } else if (dfWanted) {
                        manyDfs(dfInfos, dsid, restSessionToUseForDfs, result);
                    }

                }
            } else {
                long half = (min + max) / 2;
                String query = "SELECT min(ds.id), max(ds.id), count(ds.id) FROM Dataset ds WHERE ds.investigation.id = "
                        + invid + " AND ds.id BETWEEN " + min + " AND " + half;
                result = Json.createReader(new ByteArrayInputStream(userRestSession.search(query).getBytes())).readArray();
                manyDss(dsInfos, emptyDatasets, dfInfos, invid, dfWanted, userRestSession, restSessionToUseForDfs, result);
                query = "SELECT min(ds.id), max(ds.id), count(ds.id) FROM Dataset ds WHERE ds.investigation.id = "
                        + invid + " AND ds.id BETWEEN " + half + 1 + " AND " + max;
                result = Json.createReader(new ByteArrayInputStream(userRestSession.search(query).getBytes())).readArray()
                        .getJsonArray(0);
                manyDss(dsInfos, emptyDatasets, dfInfos, invid, dfWanted, userRestSession, restSessionToUseForDfs, result);
            }
        }

    }

}