package org.icatproject.ids.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.icatproject.ids.storage.StorageInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Load the properties specified in the properties file ids.properties.
 */
public class PropertyHandler {

    private static final Logger logger = LoggerFactory.getLogger(PropertyHandler.class);
    private static PropertyHandler instance = null;

    private int numberOfDaysToExpire;
    private String icatURL;
    private long writeDelaySeconds;
    private long processQueueIntervalSeconds;
    private int numberOfDaysToKeepFilesInCache;
    private Class<StorageInterface> fastStorageInterfaceImplementation;
    private Class<StorageInterface> slowStorageInterfaceImplementation;

    @SuppressWarnings("unchecked")
	private PropertyHandler() {
        File f = new File("ids.properties");
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(f));
            logger.info("Property file " + f + " loaded");
        } catch (Exception e) {
            String msg = "Problem with " + f.getAbsolutePath() + ": " + e.getMessage();
            logger.error(msg);
            throw new IllegalStateException(msg);
        }

        // do some very basic error checking on the config options
        icatURL = props.getProperty("ICAT_URL");
        try {
            final URLConnection connection = new URL(icatURL).openConnection();
            connection.connect();
        } catch (MalformedURLException e) {
            String msg = "Invalid property ICAT_URL (" + icatURL + "). Check URL format";
            logger.error(msg);
            throw new IllegalStateException(msg);
        } catch (IOException e) {
            String msg = "Unable to contact URL supplied for ICAT_URL (" + icatURL + ")";
            logger.error(msg);
            throw new IllegalStateException(msg);
        }

        numberOfDaysToExpire = Integer.parseInt(props.getProperty("NUMBER_OF_DAYS_TO_EXPIRE"));
        if (numberOfDaysToExpire < 1) {
            String msg = "Invalid property NUMBER_OF_DAYS_TO_EXPIRE ("
                    + props.getProperty("NUMBER_OF_DAYS_TO_EXPIRE")
                    + "). Must be an integer greater than 0.";
            logger.error(msg);
            throw new IllegalStateException(msg);
        }

        numberOfDaysToKeepFilesInCache = Integer.parseInt(props
                .getProperty("NUMBER_OF_DAYS_TO_KEEP_FILES_IN_CACHE"));
        if (numberOfDaysToKeepFilesInCache < 1) {
            String msg = "Invalid property NUMBER_OF_DAYS_TO_KEEP_FILES_IN_CACHE ("
                    + props.getProperty("NUMBER_OF_DAYS_TO_KEEP_FILES_IN_CACHE")
                    + ") Must be an integer greater than 0.";
            logger.error(msg);
            throw new IllegalStateException(msg);
        }
        
        writeDelaySeconds = Long.parseLong(props.getProperty("WRITE_DELAY_SECONDS"));
        if (writeDelaySeconds < 1) {
            String msg = "Invalid property WRITE_DELAY_SECONDS ("
                    + props.getProperty("WRITE_DELAY_SECONDS")
                    + "). Must be an integer greater than 0.";
            logger.error(msg);
            throw new IllegalStateException(msg);
        }
        
        processQueueIntervalSeconds = Long.parseLong(props.getProperty("PROCESS_QUEUE_INTERVAL_SECONDS"));
        if (processQueueIntervalSeconds < 1) {
            String msg = "Invalid property PROCESS_QUEUE_INTERVAL_SECONDS ("
                    + props.getProperty("PROCESS_QUEUE_INTERVAL_SECONDS")
                    + "). Must be an integer greater than 0.";
            logger.error(msg);
            throw new IllegalStateException(msg);
        }
        
        String fastStorageInterfaceImplementationName = props.getProperty("FAST_STORAGE_INTERFACE_IMPLEMENTATION");
        if (fastStorageInterfaceImplementationName == null) {
        	String msg = "Property FAST_STORAGE_INTERFACE_IMPLEMENTATION must be set.";
        	logger.error(msg);
        	throw new IllegalStateException(msg);
        }
        try {
        	fastStorageInterfaceImplementation = (Class<StorageInterface>) Class.forName(fastStorageInterfaceImplementationName);
        } catch (Exception e) {
        	String msg = "Could not get class implementing StorageInterface from " + fastStorageInterfaceImplementationName;
        	logger.error(msg);
        	throw new IllegalStateException(msg);
        }
        
        String slowStorageInterfaceImplementationName = props.getProperty("SLOW_STORAGE_INTERFACE_IMPLEMENTATION");
        if (slowStorageInterfaceImplementationName == null) {
        	String msg = "Property SLOW_STORAGE_INTERFACE_IMPLEMENTATION must be set.";
        	logger.error(msg);
        	throw new IllegalStateException(msg);
        }
        try {
        	slowStorageInterfaceImplementation = (Class<StorageInterface>) Class.forName(slowStorageInterfaceImplementationName);
        } catch (Exception e) {
        	String msg = "Could not get class implementing StorageInterface from " + slowStorageInterfaceImplementationName;
        	logger.error(msg);
        	throw new IllegalStateException(msg);
        }
    }

    public static PropertyHandler getInstance() {
        if (instance == null) {
            instance = new PropertyHandler();
        }
        return instance;
    }

    public String getIcatURL() {
        return icatURL;
    }
    
    public long getWriteDelaySeconds() {
    	return writeDelaySeconds;
    }
    
    public long getProcessQueueIntervalSeconds() {
    	return processQueueIntervalSeconds;
    }

    public int getNumberOfDaysToExpire() {
        return numberOfDaysToExpire;
    }

    public int getNumberOfDaysToKeepFilesInCache() {
        return numberOfDaysToKeepFilesInCache;
    }

	public Class<StorageInterface> getFastStorageInterfaceImplementation() {
		return fastStorageInterfaceImplementation;
	}
	
	public Class<StorageInterface> getSlowStorageInterfaceImplementation() {
		return slowStorageInterfaceImplementation;
	}
}
