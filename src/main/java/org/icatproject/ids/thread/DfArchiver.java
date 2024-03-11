package org.icatproject.ids.thread;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.icatproject.ids.LockManager.Lock;
import org.icatproject.ids.models.DataFileInfo;
import org.icatproject.ids.PropertyHandler;
import org.icatproject.ids.plugin.MainStorageInterface;
import org.icatproject.ids.v3.FiniteStateMachine.FiniteStateMachine;

/*
 * Removes datafiles from the fast storage (doesn't write them to archive storage)
 */
public class DfArchiver implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(DfArchiver.class);

    private MainStorageInterface mainStorageInterface;
    private FiniteStateMachine fsm;
    private List<DataFileInfo> dfInfos;
    private Path markerDir;
    private Collection<Lock> locks;

    public DfArchiver(List<DataFileInfo> dfInfos, PropertyHandler propertyHandler, FiniteStateMachine fsm, Collection<Lock> locks) {
        this.dfInfos = dfInfos;
        this.fsm = fsm;
        this.locks = locks;
        mainStorageInterface = propertyHandler.getMainStorage();
        markerDir = propertyHandler.getCacheDir().resolve("marker");
    }

    @Override
    public void run() {
        try {
            for (DataFileInfo dfInfo : dfInfos) {
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
        } finally {
            for (Lock l : locks) {
                l.release();
            }
        }
    }
}
