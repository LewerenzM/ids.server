package org.icatproject.ids.thread;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.icatproject.Dataset;
import org.icatproject.ids.entity.IdsDataEntity;
import org.icatproject.ids.entity.IdsDatasetEntity;
import org.icatproject.ids.storage.StorageFactory;
import org.icatproject.ids.storage.StorageInterface;
import org.icatproject.ids.util.RequestHelper;
import org.icatproject.ids.util.RequestQueues;
import org.icatproject.ids.util.RequestedState;
import org.icatproject.ids.util.StatusInfo;
import org.icatproject.ids.util.ZipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Copies datasets across storages (fast to slow)
 */
public class Writer implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(Writer.class);

	private IdsDataEntity de;
	private RequestQueues requestQueues;
	private RequestHelper requestHelper;

	public Writer(IdsDataEntity de, RequestHelper requestHelper) {
		this.de = de;
		this.requestQueues = RequestQueues.getInstance();
		this.requestHelper = requestHelper;
	}

	@Override
	public void run() {
		logger.info("starting Writer");
		Map<IdsDataEntity, RequestedState> deferredOpsQueue = requestQueues.getDeferredOpsQueue();
		Set<Dataset> changing = requestQueues.getChanging();
		StorageInterface slowStorageInterface = StorageFactory.getInstance()
				.createSlowStorageInterface();
		StorageInterface fastStorageInterface = StorageFactory.getInstance()
				.createFastStorageInterface();

		StatusInfo resultingStatus = StatusInfo.COMPLETED; // assuming that everything will go OK
		Dataset ds = null;
		try {
			if (de instanceof IdsDatasetEntity
					&& !fastStorageInterface.datasetExists(de.getLocation())) {
				if (slowStorageInterface != null) {
					slowStorageInterface.deleteDataset(de.getLocation());
				}
				return;
			}
			ds = de.getIcatDataset();
			InputStream zipIs = ZipHelper.zipDataset(ds, false, fastStorageInterface);
			fastStorageInterface.putDataset(ds.getLocation(), zipIs);

			if (slowStorageInterface == null) {
				logger.error("Writer can't perform because there's no slow storage");
				resultingStatus = StatusInfo.ERROR;
				return;
			}
			if (fastStorageInterface.datasetExists(ds.getLocation())) {
				InputStream is = fastStorageInterface.getDataset(ds.getLocation());
				slowStorageInterface.putDataset(ds.getLocation(), is);
			}
			logger.info("Write of  " + ds.getLocation() + " succesful");
		} catch (Exception e) {
			logger.error("Write of " + ds.getLocation() + " failed due to " + e.getMessage());
			resultingStatus = StatusInfo.INCOMPLETE;
		} finally {
			synchronized (deferredOpsQueue) {
				logger.info(String.format("Changing status of %s to %s", de, resultingStatus));
				requestHelper.setDataEntityStatus(de, resultingStatus);
				changing.remove(de.getIcatDataset());
			}
		}
	}

}
