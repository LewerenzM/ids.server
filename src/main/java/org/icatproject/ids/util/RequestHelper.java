package org.icatproject.ids.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.icatproject.Datafile;
import org.icatproject.Dataset;
import org.icatproject.IcatException_Exception;
import org.icatproject.ids.entity.IdsDataEntity;
import org.icatproject.ids.entity.IdsDatafileEntity;
import org.icatproject.ids.entity.IdsDatasetEntity;
import org.icatproject.ids.entity.IdsRequestEntity;
import org.icatproject.ids.entity.IdsWriteTimesEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class RequestHelper {
	private final static String DEFAULT_COMPRESS = "false";
	private final static String DEFAULT_ZIP = "false";
	private final static Logger logger = LoggerFactory.getLogger(RequestHelper.class);
	private PropertyHandler properties = PropertyHandler.getInstance();

	@PersistenceContext(unitName = "IDS-PU")
	private EntityManager em;

	@EJB
	private Icat icatServiceFacade;

	public IdsRequestEntity createPrepareRequest(String sessionId, String compress, String zip)
			throws IcatException_Exception {
		return createRequest(sessionId, compress, zip, RequestedState.PREPARE_REQUESTED);
	}

	public IdsRequestEntity createArchiveRequest(String sessionId) throws IcatException_Exception {
		return createRequest(sessionId, DEFAULT_COMPRESS, DEFAULT_ZIP, RequestedState.ARCHIVE_REQUESTED);
	}

	public IdsRequestEntity createRestoreRequest(String sessionId) throws IcatException_Exception {
		return createRequest(sessionId, DEFAULT_COMPRESS, DEFAULT_ZIP, RequestedState.RESTORE_REQUESTED);
	}
	
	public IdsRequestEntity createWriteRequest(String sessionId) throws IcatException_Exception {
		return createRequest(sessionId, DEFAULT_COMPRESS, DEFAULT_ZIP, RequestedState.WRITE_REQUESTED);
	}

	private IdsRequestEntity createRequest(String sessionId, String compress, String zip, RequestedState requestedState)
			throws IcatException_Exception {
		Calendar expireDate = Calendar.getInstance();
		expireDate.add(Calendar.DATE, properties.getRequestExpireTimeDays());

		String username = icatServiceFacade.getUserName(sessionId);

		IdsRequestEntity requestEntity = new IdsRequestEntity();
		requestEntity.setSessionId(sessionId);
		requestEntity.setUserId(username);
		requestEntity.setPreparedId(UUID.randomUUID().toString());
		requestEntity.setStatus(StatusInfo.SUBMITTED);
		requestEntity.setCompress(Boolean.parseBoolean(compress));
		requestEntity.setSubmittedTime(new Date());
		requestEntity.setExpireTime(expireDate.getTime());
		requestEntity.setRequestedState(requestedState);

		try {
			em.persist(requestEntity);
		} catch (Exception e) {
			logger.error("Couldn't persist " + requestEntity + ", exception: " + e.getMessage());
			throw new RuntimeException(e);
		}

		return requestEntity;
	}

	public void addDatasets(String sessionId, IdsRequestEntity requestEntity, String datasetIds)
			throws NumberFormatException, IcatException_Exception {
		List<String> datasetIdList = Arrays.asList(datasetIds.split("\\s*,\\s*"));

		for (String id : datasetIdList) {
			Dataset ds = icatServiceFacade.getDatasetWithDatafilesForDatasetId(sessionId, Long.parseLong(id));
			addDataset(sessionId, requestEntity, ds);
		}
	}

	public void addDataset(String sessionId, IdsRequestEntity requestEntity, Dataset dataset) {
		IdsDatasetEntity newDataset = new IdsDatasetEntity();
		newDataset.setIcatDatasetId(dataset.getId());
		newDataset.setIcatDataset(dataset);
		newDataset.setRequest(requestEntity);
		newDataset.setStatus(StatusInfo.SUBMITTED);
		em.persist(newDataset);
		requestEntity.getDatasets().add(newDataset);
	}

	public void addDatafiles(String sessionId, IdsRequestEntity requestEntity, String datafileIds)
			throws NumberFormatException, IcatException_Exception {
		List<String> datafileIdList = Arrays.asList(datafileIds.split("\\s*,\\s*"));

		for (String id : datafileIdList) {
			Datafile df = icatServiceFacade.getDatafileWithDatasetForDatafileId(sessionId, Long.parseLong(id));
			addDatafile(sessionId, requestEntity, df);
		}
	}

	public void addDatafile(String sessionId, IdsRequestEntity requestEntity, Datafile datafile) {
		IdsDatafileEntity newDatafile = new IdsDatafileEntity();
		newDatafile.setIcatDatafileId(datafile.getId());
		newDatafile.setIcatDatafile(datafile);
		newDatafile.setRequest(requestEntity);
		newDatafile.setStatus(StatusInfo.SUBMITTED);
		em.persist(newDatafile);
		requestEntity.getDatafiles().add(newDatafile);
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
		logger.info(String.format("Will check status of %s data entities (%s DS, %s DF)", 
				request.getDataEntities().size(), request.getDatasets().size(), request.getDatafiles().size()));
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
		Iterator<IdsRequestEntity> it = requests.iterator();
		while (it.hasNext()) {
			IdsRequestEntity request = it.next();
			try {
				for (IdsDatafileEntity df : request.getDatafiles()) {
					df.setIcatDatafile(icatServiceFacade.getDatafileWithDatasetForDatafileId(request.getSessionId(),
							df.getIcatDatafileId()));
				}
				for (IdsDatasetEntity ds : request.getDatasets()) {
					ds.setIcatDataset(icatServiceFacade.getDatasetWithDatafilesForDatasetId(request.getSessionId(),
							ds.getIcatDatasetId()));
				}
			} catch (IcatException_Exception e) {
				logger.warn("Couldn't resume processing " + request);
				setRequestStatus(request, StatusInfo.INCOMPLETE);
				it.remove();
			}
		}
		logger.info(requests.size() + " unfinished requests ready to be resumed");
		return requests;
	}

	/*
	 * This method has to be synchronized manually on deferredOpsQueue. This
	 * caveat applies also to other methods operating on IdsWriteTimeEntity. It
	 * doesn't synchronize by default in order to avoid deadlocks and improve
	 * flexibility.
	 */
	public void setWriteTime(Dataset ds, Long time) {
		logger.info(String.format("Setting write time %s for dataset %s\n", time, ds.getId()));
		IdsWriteTimesEntity oldWriteTime = em.find(IdsWriteTimesEntity.class, ds.getId());
		if (oldWriteTime == null) {
			IdsWriteTimesEntity newWriteTime = new IdsWriteTimesEntity(ds.getId(), time);
			em.persist(newWriteTime);
		} else {
			logger.info(String.format("Updating oldWriteTime from %s to %s", oldWriteTime.getWriteTime(), time));
			oldWriteTime.setWriteTime(time);
			em.persist(oldWriteTime);
		}

		Map<Dataset, Long> writeTimes = RequestQueues.getInstance().getWriteTimes();
		writeTimes.put(ds, time);
	}

	public void removeWriteTime(Dataset ds) {
		logger.info("Removing write time for dataset " + ds.getId());
		IdsWriteTimesEntity oldWriteTime = em.find(IdsWriteTimesEntity.class, ds.getId());
		if (oldWriteTime == null) {
			throw new IllegalArgumentException(String.format(
					"Write time for Dataset %s doesn't exist in the database and cannot be removed", ds.getId()));
		}
		em.remove(oldWriteTime);
		Map<Dataset, Long> writeTimes = RequestQueues.getInstance().getWriteTimes();
		writeTimes.remove(ds);
	}

}
