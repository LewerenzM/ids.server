package org.icatproject.ids.util;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.icatproject.ids.entity.IdsDataEntity;
import org.icatproject.ids.entity.IdsDatafileEntity;
import org.icatproject.ids.entity.IdsDatasetEntity;
import org.icatproject.ids.entity.IdsRequestEntity;
import org.icatproject.ids.icatclient.ICATClientBase;
import org.icatproject.ids.icatclient.ICATClientFactory;
import org.icatproject.ids.icatclient.exceptions.ICATClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class RequestHelper {
	private final static String DEFAULT_COMPRESS = "false";
	private final static String DEFAULT_ZIP = "false";
	private final static Logger logger = LoggerFactory.getLogger(RequestHelper.class);
	private PropertyHandler properties = PropertyHandler.getInstance();
	private ICATClientBase icatClient;

	@PersistenceContext(unitName = "IDS-PU")
	private EntityManager em;

	@PostConstruct
	public void postConstruct() throws MalformedURLException, ICATClientException {
		icatClient = ICATClientFactory.getInstance().createICATInterface();
	}

	public IdsRequestEntity createPrepareRequest(String sessionId, String compress, String zip)
			throws ICATClientException, MalformedURLException {
		return createRequest(sessionId, compress, zip, RequestedState.PREPARE_REQUESTED);
	}

	public IdsRequestEntity createArchiveRequest(String sessionId) throws MalformedURLException, ICATClientException {
		return createRequest(sessionId, DEFAULT_COMPRESS, DEFAULT_ZIP, RequestedState.ARCHIVE_REQUESTED);
	}

	public IdsRequestEntity createRestoreRequest(String sessionId) throws ICATClientException, MalformedURLException {
		return createRequest(sessionId, DEFAULT_COMPRESS, DEFAULT_ZIP, RequestedState.RESTORE_REQUESTED);
	}

	public IdsRequestEntity createRequest(String sessionId, String compress, String zip, RequestedState requestedState)
			throws ICATClientException, MalformedURLException {
		ICATClientBase client = ICATClientFactory.getInstance().createICATInterface();
		Calendar expireDate = Calendar.getInstance();
		expireDate.add(Calendar.DATE, properties.getRequestExpireTimeDays());

		String username = client.getUserId(sessionId);

		IdsRequestEntity requestEntity = new IdsRequestEntity();
		requestEntity.setSessionId(sessionId);
		requestEntity.setUserId(username);
		requestEntity.setPreparedId(UUID.randomUUID().toString());
		requestEntity.setStatus(StatusInfo.SUBMITTED);
		requestEntity.setCompress(Boolean.parseBoolean(compress));
		requestEntity.setSubmittedTime(new Date());
		requestEntity.setExpireTime(expireDate.getTime());
		requestEntity.setRequestedState(requestedState);

		try{
			em.persist(requestEntity);
		} catch (Exception e) {
			logger.error("Couldn't persist " + requestEntity + ", exception: " + e.getMessage());
			throw new RuntimeException(e);
		}

		return requestEntity;
	}

	public void addDatasets(String sessionId, IdsRequestEntity requestEntity, String datasetIds) throws Exception {
		List<String> datasetIdList = Arrays.asList(datasetIds.split("\\s*,\\s*"));
		List<IdsDatasetEntity> newDatasetList = new ArrayList<IdsDatasetEntity>();

		for (String id : datasetIdList) {
			IdsDatasetEntity newDataset = new IdsDatasetEntity();
			newDataset.setIcatDatasetId(Long.parseLong(id));
			newDataset.setIcatDataset(icatClient.getDatasetWithDatafilesForDatasetId(sessionId, Long.parseLong(id)));
			newDataset.setRequest(requestEntity);
			newDataset.setStatus(StatusInfo.SUBMITTED);
			newDatasetList.add(newDataset);
			em.persist(newDataset);
		}

		requestEntity.setDatasets(newDatasetList);
		em.merge(requestEntity);
	}

	public void addDatafiles(String sessionId, IdsRequestEntity requestEntity, String datafileIds) throws Exception {
		List<String> datafileIdList = Arrays.asList(datafileIds.split("\\s*,\\s*"));
		List<IdsDatafileEntity> newDatafileList = new ArrayList<IdsDatafileEntity>();

		for (String id : datafileIdList) {
			IdsDatafileEntity newDatafile = new IdsDatafileEntity();
			newDatafile.setIcatDatafileId(Long.parseLong(id));
			newDatafile.setIcatDatafile(icatClient.getDatafileWithDatasetForDatafileId(sessionId, Long.parseLong(id)));
			newDatafile.setRequest(requestEntity);
			newDatafile.setStatus(StatusInfo.SUBMITTED);
			newDatafileList.add(newDatafile);
			em.persist(newDatafile);
		}

		requestEntity.setDatafiles(newDatafileList);
		em.merge(requestEntity);
	}

	public void setDataEntityStatus(IdsDataEntity de, StatusInfo status) {
		logger.info("Changing status of " + de + " to " + status);
		de.setStatus(status);
		setRequestCompletedIfEverythingDone(de.getRequest());
		em.merge(de);
	}

	private void setRequestCompletedIfEverythingDone(IdsRequestEntity request) {
		Set<StatusInfo> finalStatuses = new HashSet<StatusInfo>();
		finalStatuses.add(StatusInfo.COMPLETED);
		finalStatuses.add(StatusInfo.INCOMPLETE);

		// assuming that everything went OK
		StatusInfo resultingRequestStatus = StatusInfo.COMPLETED; 
		logger.info("Will check status of " + request.getDataEntities().size() + " data entities");
		for (IdsDataEntity de : request.getDataEntities()) {
			logger.info("Status of " + de + " is " + de.getStatus());
			if (!finalStatuses.contains(de.getStatus())) {
				return;
			}
			if (de.getStatus() != StatusInfo.COMPLETED) {
				resultingRequestStatus = StatusInfo.INCOMPLETE;
				break;
			}
		}
		logger.info("all tasks in request " + request + " finished");
		setRequestStatus(request, resultingRequestStatus);
	}

	public void setRequestStatus(IdsRequestEntity request, StatusInfo status) {
		logger.info("Changing status of " + request + " to " + status);
		request.setStatus(status);
		em.merge(request);
	}

	public IdsRequestEntity getRequestByPreparedId(String preparedId) {
		Query q = em.createQuery("SELECT d FROM IdsRequestEntity d WHERE d.preparedId = :preparedId").setParameter(
				"preparedId", preparedId);
		return (IdsRequestEntity) q.getSingleResult();
	}
	
	public List<IdsRequestEntity> getUnfinishedRequests() {
		Query q = em.createQuery("SELECT r FROM IdsRequestEntity r "
				+ "WHERE r.status = org.icatproject.ids.util.StatusInfo.SUBMITTED "
				+ "OR r.status = org.icatproject.ids.util.StatusInfo.RETRIVING");
		@SuppressWarnings("unchecked")
		List<IdsRequestEntity> requests = (List<IdsRequestEntity>) q.getResultList();
		logger.info("Found " + requests.size() + " unfinished requests");
		return requests;
	}

}
