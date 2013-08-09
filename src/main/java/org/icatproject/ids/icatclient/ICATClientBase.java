package org.icatproject.ids.icatclient;

import java.util.ArrayList;

import org.icatproject.Dataset;
import org.icatproject.ids.entity.DatafileEntity;
import org.icatproject.ids.icatclient.exceptions.ICATClientException;


public interface ICATClientBase {
	public abstract String getUserId(String sessionId) throws ICATClientException;
	public abstract ArrayList<String> getDatafilePaths(String sessionId, ArrayList<Long> datafileIds) throws ICATClientException;
	public abstract ArrayList<DatafileEntity> getDatafilesInDataset(String sessionId, Long datasetId) throws ICATClientException;
	
	public Dataset getDatasetForDatasetId(String sessionId, Long datasetId) throws ICATClientException;
	public Dataset getDatasetForDatafileId(String sessionId, Long datafileId) throws ICATClientException;
}