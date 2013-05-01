package org.icatproject.ids.icatclient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.icatproject.ids.entity.DatafileEntity;
import org.icatproject.ids.icatclient.ICATClientBase;
import org.icatproject.ids.icatclient.exceptions.ICATBadParameterException;
import org.icatproject.ids.icatclient.exceptions.ICATClientException;
import org.icatproject.ids.icatclient.exceptions.ICATInsufficientPrivilegesException;
import org.icatproject.ids.icatclient.exceptions.ICATInternalException;
import org.icatproject.ids.icatclient.exceptions.ICATNoSuchObjectException;
import org.icatproject.ids.icatclient.exceptions.ICATObjectAlreadyExistsException;
import org.icatproject.ids.icatclient.exceptions.ICATSessionException;
import org.icatproject.ids.icatclient.exceptions.ICATValidationException;
import org.icatproject.ids.icatclient.icat42.Datafile;
import org.icatproject.ids.icatclient.icat42.ICAT;
import org.icatproject.ids.icatclient.icat42.ICATService;
import org.icatproject.ids.icatclient.icat42.IcatException_Exception;
import org.icatproject.ids.util.StatusInfo;


/*
 * TODO: move out code that references DatafileEntity ie. make better
 * separation
 */
public class ICATClient42 implements ICATClientBase {

    private ICAT service;

    public ICATClient42(String url) throws MalformedURLException {
        service = new ICATService(new URL(url), new QName("http://icatproject.org", "ICATService"))
                .getICATPort();
    }

    @Override
    public String getUserId(String sessionId) throws ICATClientException {
        String retval = null;
        try {
            retval = service.getUserName(sessionId);
        } catch (IcatException_Exception e) {
            convertToICATClientException(e);
        }
        return retval;
    }

    @Override
    public ArrayList<String> getDatafilePaths(String sessionId, ArrayList<Long> datafileIds)
            throws ICATClientException {
        ArrayList<String> results = new ArrayList<String>();
        List<Object> datafileLocations = null;

        try {
            datafileLocations = service.search(sessionId, "Datafile.location [id IN ("
                    + datafileIds.toString().replace('[', ' ').replace(']', ' ') + ")]");
        } catch (IcatException_Exception e) {
            convertToICATClientException(e);
        }

        // if the number of locations returned does not match number
        // of datafileIds then one or more of the ids were not found
        if (datafileIds.size() != datafileLocations.size()) {
            throw new ICATNoSuchObjectException();
        }

        for (Object location : datafileLocations) {
            results.add((String) location);
        }

        return results;
    }

    @Override
    public ArrayList<DatafileEntity> getDatafilesInDataset(String sessionId, Long datasetId)
            throws ICATClientException {
        ArrayList<DatafileEntity> results = new ArrayList<DatafileEntity>();
        List<Object> datafileList = null;

        try {
            datafileList = service.search(sessionId, "Datafile [dataset.id = " + datasetId + "]");
        } catch (IcatException_Exception e) {
            convertToICATClientException(e);
        }
        
        // if no datafiles are returned, check to see if dataset actually exists
        if (datafileList.size() == 0) {
            try {
                List<Object> datasets = service.search(sessionId, "Dataset [id = " + datasetId + "]");
                if (datasets.size() == 0) {
                    throw new ICATNoSuchObjectException();
                }
            } catch (IcatException_Exception e) {
                convertToICATClientException(e);
            }
        }

        for (Object icatDatafile : datafileList) {
            DatafileEntity datafile = new DatafileEntity();
            datafile.setDatafileId(((Datafile) icatDatafile).getId());
            datafile.setName(((Datafile) icatDatafile).getLocation());
            datafile.setStatus(StatusInfo.SUBMITTED.name());
            results.add(datafile);
        }

        return results;
    }
    
    public String getICATVersion() throws ICATClientException {
        String version = null;
        try {
            version = service.getApiVersion();
        } catch (IcatException_Exception e) {
            convertToICATClientException(e);
        }
        return version;
    }

    // TODO: add proper logging
    private void convertToICATClientException(IcatException_Exception e) throws ICATClientException {
        switch (e.getFaultInfo().getType()) {
            case BAD_PARAMETER:
                throw new ICATBadParameterException();
            case INSUFFICIENT_PRIVILEGES:
                throw new ICATInsufficientPrivilegesException();
            case INTERNAL:
                throw new ICATInternalException();
            case NO_SUCH_OBJECT_FOUND:
                throw new ICATNoSuchObjectException();
            case OBJECT_ALREADY_EXISTS:
                throw new ICATObjectAlreadyExistsException();
            case SESSION:
                throw new ICATSessionException();
            case VALIDATION:
                throw new ICATValidationException();
            default:
                break;
        }
    }
}
