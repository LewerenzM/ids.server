package org.icatproject.ids.thread;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.icatproject.ids.FiniteStateMachine;
import org.icatproject.ids.PropertyHandler;
import org.icatproject.ids.plugin.DfInfo;
import org.icatproject.ids.plugin.MainStorageInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Removes datafiles from the fast storage (doesn't write them to archive storage)
 */
public class DfArchiver implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(DfArchiver.class);

	private MainStorageInterface mainStorageInterface;
	private FiniteStateMachine fsm;
	private List<DfInfo> dfInfos;
	private Path markerDir;

	public DfArchiver(List<DfInfo> dfInfos, PropertyHandler propertyHandler, FiniteStateMachine fsm) {
		this.dfInfos = dfInfos;
		this.fsm = fsm;
		mainStorageInterface = propertyHandler.getMainStorage();
		markerDir = propertyHandler.getCacheDir().resolve("marker");
	}

	@Override
	public void run() {
		for (DfInfo dfInfo : dfInfos) {
			try {
				if (Files.exists(markerDir.resolve(Long.toString(dfInfo.getDfId())))) {
					logger.error("Archive of " + dfInfo
							+ " not carried out because a write to secondary storage operation failed previously");
				} else {
					String dfLocation = dfInfo.getDfLocation();
					mainStorageInterface.delete(dfLocation, dfInfo.getCreateId(), dfInfo.getModId());
					logger.debug("Archive of " + dfInfo + " completed");
				}
			} catch (Exception e) {
				logger.error("Archive of " + dfInfo + " failed due to " + e.getClass() + " " + e.getMessage());
			} finally {
				fsm.removeFromChanging(dfInfo);
			}
		}
	}
}
