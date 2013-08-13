package org.icatproject.ids2.ported;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.icatproject.ids.icatclient.ICATClientBase;
import org.icatproject.ids.icatclient.ICATClientFactory;
import org.icatproject.ids.icatclient.exceptions.ICATClientException;
import org.icatproject.ids.util.PropertyHandler;
import org.icatproject.ids.util.StatusInfo;
import org.icatproject.ids2.ported.entity.Ids2DataEntity;
import org.icatproject.ids2.ported.entity.Ids2DatafileEntity;
import org.icatproject.ids2.ported.entity.Ids2DatasetEntity;
import org.icatproject.ids2.ported.entity.RequestEntity;

@Stateless
public class RequestHelper {
	private final static String DEFAULT_COMPRESS = "false";
	private final static String DEFAULT_ZIP = "false";
	private final static Logger logger = Logger.getLogger(RequestHelper.class.getName());
	private PropertyHandler properties = PropertyHandler.getInstance();
	private ICATClientBase icatClient;

	@PersistenceContext(unitName = "IDS-PU")
	private EntityManager em;
	
	@PostConstruct
	public void postConstruct() throws MalformedURLException, ICATClientException {
		icatClient = ICATClientFactory.getInstance().createICATInterface();
	}
	
	public RequestEntity createRestoreRequest(String sessionId, String compress, String zip) 
			throws ICATClientException, MalformedURLException {
		return createRequest(sessionId, compress, zip, RequestedState.RESTORE_REQUESTED);
	}
	
	public RequestEntity createArchiveRequest(String sessionId) throws MalformedURLException, ICATClientException {
		return createRequest(sessionId, DEFAULT_COMPRESS, DEFAULT_ZIP, RequestedState.ARCHIVE_REQUESTED);
	}

	public RequestEntity createRequest(String sessionId, String compress, String zip, RequestedState requestedState)
			throws ICATClientException, MalformedURLException {
		ICATClientBase client = ICATClientFactory.getInstance().createICATInterface();
		Calendar expireDate = Calendar.getInstance();
		expireDate.add(Calendar.DATE, properties.getNumberOfDaysToExpire());

		String username = client.getUserId(sessionId);

		RequestEntity requestEntity = new RequestEntity();
		requestEntity.setSessionId(sessionId);
		requestEntity.setUserId(username);
		requestEntity.setPreparedId(UUID.randomUUID().toString());
		requestEntity.setStatus(StatusInfo.SUBMITTED);
		requestEntity.setCompress(Boolean.parseBoolean(compress));
		requestEntity.setSubmittedTime(new Date());
		requestEntity.setExpireTime(expireDate.getTime());
		requestEntity.setRequestedState(requestedState);

		em.persist(requestEntity);
		em.flush();

		return requestEntity;
	}
	
	public void addDatasets(String sessionId, RequestEntity requestEntity, String datasetIds) throws Exception {
		 List<String> datasetIdList = Arrays.asList(datasetIds.split("\\s*,\\s*"));
	        List<Ids2DatasetEntity> newDatasetList = new ArrayList<Ids2DatasetEntity>();

	        for (String id : datasetIdList) {
	            Ids2DatasetEntity newDataset = new Ids2DatasetEntity();
	            newDataset.setIcatDatasetId(Long.parseLong(id));
	            newDataset.setIcatDataset(icatClient.getDatasetForDatasetId(sessionId, Long.parseLong(id)));
	            newDataset.setRequest(requestEntity);
	            newDataset.setStatus(StatusInfo.SUBMITTED);         
	            newDatasetList.add(newDataset);
	            em.persist(newDataset);
	        }
	        
	        requestEntity.setDatasets(newDatasetList);
	        em.merge(requestEntity);
	        em.flush();
    }
	
	public void addDatafiles(String sessionId, RequestEntity requestEntity, String datafileIds) throws Exception {
		List<String> datafileIdList = Arrays.asList(datafileIds.split("\\s*,\\s*"));
		List<Ids2DatafileEntity> newDatafileList = new ArrayList<Ids2DatafileEntity>();
		
		for (String id : datafileIdList) {
            Ids2DatafileEntity newDatafile = new Ids2DatafileEntity();
            newDatafile.setIcatDatafileId(Long.parseLong(id));
            newDatafile.setIcatDatafile(icatClient.getDatafileWithDatasetForDatafileId(sessionId, Long.parseLong(id)));
            newDatafile.setRequest(requestEntity);
            newDatafile.setStatus(StatusInfo.SUBMITTED);         
            newDatafileList.add(newDatafile);
            em.persist(newDatafile);
        }
        
        requestEntity.setDatafiles(newDatafileList);
        em.merge(requestEntity);
        em.flush();
	}
	
	public void setDataEntityStatus(Ids2DataEntity de, StatusInfo status) {
		de = em.merge(de);
		de.setStatus(status);
		setRequestCompletedIfEverythingDone(de);
		em.merge(de);
	}
	
	private void setRequestCompletedIfEverythingDone(Ids2DataEntity dataEntity) {
		Set<StatusInfo> finalStatuses = new HashSet<StatusInfo>();
		finalStatuses.add(StatusInfo.COMPLETED);
		finalStatuses.add(StatusInfo.INCOMPLETE);
		
		RequestEntity request = dataEntity.getRequest();
		StatusInfo resultingRequestStatus = StatusInfo.COMPLETED; // assuming that everything went OK
		logger.info("Will check status of " + request.getDataEntities().size() + " data entities");
		
		for (Ids2DataEntity de : request.getDataEntities()) {
			if (!finalStatuses.contains(de.getStatus())) {
				return;
			}
			if (de.getStatus() != StatusInfo.COMPLETED) {
				resultingRequestStatus = StatusInfo.INCOMPLETE;
			}
		}
		logger.info("all tasks in request " + request + " finished");
		request.setStatus(resultingRequestStatus);
	}
	
	public RequestEntity getRequestByPreparedId(String preparedId) {
		Query q = em.createQuery("SELECT d FROM RequestEntity d WHERE d.preparedId = :preparedId").setParameter("preparedId", preparedId);
        return (RequestEntity) q.getSingleResult();
	}

}
