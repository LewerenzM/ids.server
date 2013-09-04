package org.icatproject.ids2.ported.thread;

import java.util.Map;
import java.util.Set;

import org.icatproject.Dataset;
import org.icatproject.ids.storage.StorageFactory;
import org.icatproject.ids.storage.StorageInterface;
import org.icatproject.ids.util.StatusInfo;
import org.icatproject.ids2.ported.RequestHelper;
import org.icatproject.ids2.ported.RequestQueues;
import org.icatproject.ids2.ported.RequestedState;
import org.icatproject.ids2.ported.entity.Ids2DataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Writer implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(ProcessQueue.class);

	private Ids2DataEntity de;
	private RequestQueues requestQueues;
	private RequestHelper requestHelper;
	
	public Writer(Ids2DataEntity de, RequestHelper requestHelper) {
		this.de = de;
		this.requestQueues = RequestQueues.getInstance();
		this.requestHelper = requestHelper;
	}
	
	@Override
	public void run() {
//		logger.info("starting writer");
//		StorageInterface storageInterface = StorageFactory.getInstance().createStorageInterface();
//		StatusInfo resultingStatus = storageInterface.writeToArchive(de.getIcatDataset());
//		Map<Ids2DataEntity, RequestedState> deferredOpsQueue = requestQueues.getDeferredOpsQueue();
//		Set<Dataset> changing = requestQueues.getChanging();
//		synchronized (deferredOpsQueue) {
//			logger.info(String.format("Changing status of %s to %s", de, resultingStatus));
//			requestHelper.setDataEntityStatus(de, resultingStatus);
//			changing.remove(de.getIcatDataset());
//		}
	}
}
