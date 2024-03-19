package org.icatproject.ids.requestHandlers.getDataHandlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.icatproject.IcatException_Exception;
import org.icatproject.ids.dataSelection.DataSelectionBase;
import org.icatproject.ids.enums.CallType;
import org.icatproject.ids.enums.PreparedDataStatus;
import org.icatproject.ids.enums.RequestType;
import org.icatproject.ids.exceptions.BadRequestException;
import org.icatproject.ids.exceptions.DataNotOnlineException;
import org.icatproject.ids.exceptions.IdsException;
import org.icatproject.ids.exceptions.InsufficientPrivilegesException;
import org.icatproject.ids.exceptions.InternalException;
import org.icatproject.ids.exceptions.NotFoundException;
import org.icatproject.ids.exceptions.NotImplementedException;
import org.icatproject.ids.helpers.SO;
import org.icatproject.ids.helpers.ValueContainer;
import org.icatproject.ids.models.DataInfoBase;
import org.icatproject.ids.models.Prepared;
import org.icatproject.ids.plugin.AlreadyLockedException;
import org.icatproject.ids.requestHandlers.RequestHandlerBase;
import org.icatproject.ids.services.ServiceProvider;
import org.icatproject.ids.services.LockManager.Lock;
import org.icatproject.ids.services.LockManager.LockType;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;

public class GetDataHandler extends RequestHandlerBase {

    private Pattern rangeRe;
    private static AtomicLong atomicLong = new AtomicLong();


    public GetDataHandler() {
        super(PreparedDataStatus.NOMATTER, RequestType.GETDATA );
    }


    public void init() throws InternalException {
        logger.info("Initializing GetDataHandler...");
        super.init();        
        this.rangeRe = Pattern.compile("bytes=(\\d+)-");
        logger.info("GetDataHandler initialized");
    }


    @Override
    public ValueContainer handle(HashMap<String, ValueContainer> parameters) throws BadRequestException, NotFoundException, InternalException, InsufficientPrivilegesException, DataNotOnlineException, NotImplementedException  {
        Response response = null;

        long offset = 0;
        var range = parameters.get("range");
        if ( range != null && range.getString() != null) {
            var rangeValue = range.getString();

            Matcher m = rangeRe.matcher(rangeValue);
            if (!m.matches()) {
                throw new BadRequestException("The range must match " + rangeRe.pattern());
            }
            offset = Long.parseLong(m.group(1));
            logger.debug("Range " + rangeValue + " -> offset " + offset);
        }

        var preparedId = parameters.get("preparedId");
        if (preparedId.getString() != null) {
            response = this.getData(preparedId.getString(), 
                                    parameters.getOrDefault("outname", ValueContainer.getInvalid()).getString(), 
                                    offset, 
                                    parameters.getOrDefault("request", ValueContainer.getInvalid()).getRequest().getRemoteAddr());
        } else {
            response = this.getData(parameters.getOrDefault("sessionId", ValueContainer.getInvalid()).getString(), 
                                    parameters.getOrDefault("investigationIds", ValueContainer.getInvalid()).getString(), 
                                    parameters.getOrDefault("datasetIds", ValueContainer.getInvalid()).getString(), 
                                    parameters.getOrDefault("datafileIds", ValueContainer.getInvalid()).getString(), 
                                    parameters.getOrDefault("compress", ValueContainer.getInvalid()).getBool(), 
                                    parameters.getOrDefault("zip", ValueContainer.getInvalid()).getBool(), 
                                    parameters.getOrDefault("outname", ValueContainer.getInvalid()).getString(),
                                    offset, 
                                    parameters.getOrDefault("request", ValueContainer.getInvalid()).getRequest().getRemoteAddr());
        }

        return new ValueContainer(response);
    }


    private Response getData(String preparedId, String outname, final long offset, String ip) throws BadRequestException,
            NotFoundException, InternalException, InsufficientPrivilegesException, DataNotOnlineException {

        long time = System.currentTimeMillis();

        // Log and validate
        logger.info("New webservice request: getData preparedId = '" + preparedId + "' outname = '" + outname
                + "' offset = " + offset);

        validateUUID("preparedId", preparedId);

        // Do it
        Prepared prepared;
        try (InputStream stream = Files.newInputStream(preparedDir.resolve(preparedId))) {
            prepared = unpack(stream);
        } catch (NoSuchFileException e) {
            throw new NotFoundException("The preparedId " + preparedId + " is not known");
        } catch (IOException e) {
            throw new InternalException(e.getClass() + " " + e.getMessage());
        }

        DataSelectionBase dataSelection = this.getDataSelection(prepared.dsInfos, prepared.dfInfos, prepared.emptyDatasets, prepared.fileLength);
        final boolean zip = prepared.zip;
        final boolean compress = prepared.compress;
        final Map<Long, DataInfoBase> dfInfos = prepared.dfInfos;
        final Map<Long, DataInfoBase> dsInfos = prepared.dsInfos;
        var length = zip ? OptionalLong.empty() : dataSelection.getFileLength();

        Lock lock = null;
        try {
            var serviceProvider = ServiceProvider.getInstance();
            lock = serviceProvider.getLockManager().lock(dsInfos.values(), LockType.SHARED);

            if (twoLevel) {
                dataSelection.checkOnline();
            }
            checkDatafilesPresent(dfInfos.values());

            /* Construct the name to include in the headers */
            String name;
            if (outname == null) {
                if (zip) {
                    name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".zip";
                } else {
                    name = dfInfos.values().iterator().next().getName();
                }
            } else {
                if (zip) {
                    String ext = outname.substring(outname.lastIndexOf(".") + 1, outname.length());
                    if ("zip".equals(ext)) {
                        name = outname;
                    } else {
                        name = outname + ".zip";
                    }
                } else {
                    name = outname;
                }
            }

            Long transferId = null;
            if (serviceProvider.getPropertyHandler().getLogSet().contains(CallType.READ)) {
                transferId = atomicLong.getAndIncrement();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (JsonGenerator gen = Json.createGenerator(baos).writeStartObject()) {
                    gen.write("transferId", transferId);
                    gen.write("preparedId", preparedId);
                    gen.writeEnd();
                }
                serviceProvider.getTransmitter().processMessage("getDataStart", ip, baos.toString(), time);
            }

            var response = Response.status(offset == 0 ? HttpURLConnection.HTTP_OK : HttpURLConnection.HTTP_PARTIAL)
                    .entity(new SO(dsInfos, dfInfos, offset, zip, compress, lock, transferId, ip, time, serviceProvider))
                    .header("Content-Disposition", "attachment; filename=\"" + name + "\"").header("Accept-Ranges", "bytes");
            length.stream()
                    .map(l -> Math.max(0L, l - offset))
                    .forEach(l -> response.header(CONTENT_LENGTH, l));
                
            return response.build();

        } catch (AlreadyLockedException e) {
            logger.debug("Could not acquire lock, getData failed");
            throw new DataNotOnlineException("Data is busy");
        } catch (IOException e) {
            // if (lock != null) {
            //     lock.release();
            // }
            logger.error("I/O error " + e.getMessage());
            throw new InternalException(e.getClass() + " " + e.getMessage());
        } catch (IdsException e) {
            lock.release();
            throw e;
        }
    }


    private Response getData(String sessionId, String investigationIds, String datasetIds, String datafileIds,
                            final boolean compress, boolean zip, String outname, final long offset, String ip)
            throws BadRequestException, InternalException, InsufficientPrivilegesException, NotFoundException,
            DataNotOnlineException, NotImplementedException {

        long start = System.currentTimeMillis();

        // Log and validate
        logger.info(String.format("New webservice request: getData investigationIds=%s, datasetIds=%s, datafileIds=%s",
                investigationIds, datasetIds, datafileIds));

        validateUUID("sessionId", sessionId);

        var serviceProvider = ServiceProvider.getInstance();

        final DataSelectionBase dataSelection = this.getDataSelection(sessionId, investigationIds, datasetIds, datafileIds);

        // Do it
        Map<Long, DataInfoBase> dsInfos = dataSelection.getDsInfo();
        Map<Long, DataInfoBase> dfInfos = dataSelection.getDfInfo();
        var length = zip ? OptionalLong.empty() : dataSelection.getFileLength();

        Lock lock = null;
        try {
            lock = serviceProvider.getLockManager().lock(dsInfos.values(), LockType.SHARED);

            if (twoLevel) {
                dataSelection.checkOnline();
            }
            checkDatafilesPresent(dfInfos.values());

            final boolean finalZip = zip ? true : dataSelection.mustZip();

            /* Construct the name to include in the headers */
            String name;
            if (outname == null) {
                if (finalZip) {
                    name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".zip";
                } else {
                    name = dataSelection.getDfInfo().values().iterator().next().getName();
                }
            } else {
                if (finalZip) {
                    String ext = outname.substring(outname.lastIndexOf(".") + 1, outname.length());
                    if ("zip".equals(ext)) {
                        name = outname;
                    } else {
                        name = outname + ".zip";
                    }
                } else {
                    name = outname;
                }
            }

            Long transferId = null;
            if (serviceProvider.getPropertyHandler().getLogSet().contains(CallType.READ)) {
                try {
                    transferId = atomicLong.getAndIncrement();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (JsonGenerator gen = Json.createGenerator(baos).writeStartObject()) {
                        gen.write("transferId", transferId);
                        gen.write("userName", serviceProvider.getIcat().getUserName(sessionId));
                        addIds(gen, investigationIds, datasetIds, datafileIds);
                        gen.writeEnd();
                    }
                    serviceProvider.getTransmitter().processMessage("getDataStart", ip, baos.toString(), start);
                } catch (IcatException_Exception e) {
                    logger.error("Failed to prepare jms message " + e.getClass() + " " + e.getMessage());
                }
            }

            var response =  Response.status(offset == 0 ? HttpURLConnection.HTTP_OK : HttpURLConnection.HTTP_PARTIAL)
                    .entity(new SO(dataSelection.getDsInfo(), dataSelection.getDfInfo(), offset, finalZip, compress, lock,
                            transferId, ip, start, serviceProvider))
                    .header("Content-Disposition", "attachment; filename=\"" + name + "\"").header("Accept-Ranges", "bytes");
            length.stream()
                    .map(l -> Math.max(0L, l - offset))
                    .forEach(l -> response.header(CONTENT_LENGTH, l));
            
            return response.build();

        } catch (AlreadyLockedException e) {
            logger.debug("Could not acquire lock, getData failed");
            throw new DataNotOnlineException("Data is busy");
        } catch (IOException e) {
            // if (lock != null) {
            //     lock.release();
            // }
            logger.error("I/O error " + e.getMessage());
            throw new InternalException(e.getClass() + " " + e.getMessage());
        } catch (IdsException e) {
            lock.release();
            throw e;
        }
    }

    private void checkDatafilesPresent(Collection<DataInfoBase> dfInfos)
            throws NotFoundException, InternalException {

        var serviceProvider = ServiceProvider.getInstance();
        /* Check that datafiles have not been deleted before locking */
        int n = 0;
        StringBuffer sb = new StringBuffer("SELECT COUNT(df) from Datafile df WHERE (df.id in (");
        for (DataInfoBase dfInfo : dfInfos) {
            if (n != 0) {
                sb.append(',');
            }
            sb.append(dfInfo.getId());
            if (++n == serviceProvider.getPropertyHandler().getMaxIdsInQuery()) {
                try {
                    if (((Long) serviceProvider.getIcatReader().search(sb.append("))").toString()).get(0)).intValue() != n) {
                        throw new NotFoundException("One of the data files requested has been deleted");
                    }
                    n = 0;
                    sb = new StringBuffer("SELECT COUNT(df) from Datafile df WHERE (df.id in (");
                } catch (IcatException_Exception e) {
                    throw new InternalException(e.getFaultInfo().getType() + " " + e.getMessage());
                }
            }
        }
        if (n != 0) {
            try {
                if (((Long) serviceProvider.getIcatReader().search(sb.append("))").toString()).get(0)).intValue() != n) {
                    throw new NotFoundException("One of the datafiles requested has been deleted");
                }
            } catch (IcatException_Exception e) {
                throw new InternalException(e.getFaultInfo().getType() + " " + e.getMessage());
            }
        }

    }

}