package org.icatproject.ids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.icatproject.Datafile;
import org.icatproject.Dataset;
import org.icatproject.ICAT;
import org.icatproject.IcatExceptionType;
import org.icatproject.IcatException_Exception;
import org.icatproject.ids.exceptions.BadRequestException;
import org.icatproject.ids.exceptions.InsufficientPrivilegesException;
import org.icatproject.ids.exceptions.InternalException;
import org.icatproject.ids.exceptions.NotFoundException;
import org.icatproject.ids.plugin.DsInfo;

public class DataSelection {

	/**
	 * Checks to see if the investigation, dataset or datafile id list is a valid comma separated
	 * list of longs. No spaces or leading 0's. Also accepts null.
	 */
	public static List<Long> getValidIds(String thing, String idList) throws BadRequestException {

		List<Long> result;
		if (idList == null) {
			result = Collections.emptyList();
		} else {
			String[] ids = idList.split("\\s*,\\s*");
			result = new ArrayList<>(ids.length);
			for (String id : ids) {
				try {
					result.add(Long.parseLong(id));
				} catch (NumberFormatException e) {
					throw new BadRequestException("The " + thing + " parameter '" + idList
							+ "' is not a valid "
							+ "string representation of a comma separated list of longs");
				}
			}
		}
		return result;
	}

	private Map<Long, DsInfo> dsInfos;
	private ICAT icat;
	private String sessionId;
	private boolean dfWanted;
	private List<Long> dfids;
	private List<Long> dsids;
	private List<Long> invids;
	private Set<DfInfoImpl> dfInfos;
	private Set<Long> emptyDatasets;

	public enum Returns {
		DATASETS, DATASETS_AND_DATAFILES
	}

	public DataSelection(ICAT icat, String sessionId, String investigationIds, String datasetIds,
			String datafileIds, Returns returns) throws BadRequestException, NotFoundException,
			InsufficientPrivilegesException, InternalException {

		this.icat = icat;
		this.sessionId = sessionId;
		dfids = getValidIds("datafileIds", datafileIds);
		dsids = getValidIds("datasetIds", datasetIds);
		invids = getValidIds("investigationIds", investigationIds);
		dfWanted = returns == Returns.DATASETS_AND_DATAFILES;

		resolveDatasetIds();

	}

	public Map<Long, DsInfo> getDsInfo() {
		return dsInfos;
	}

	private void resolveDatasetIds() throws NotFoundException, InsufficientPrivilegesException,
			InternalException, BadRequestException {
		dsInfos = new HashMap<>();
		if (dfWanted) {
			dfInfos = new HashSet<>();
			emptyDatasets = new HashSet<>();
		}

		try {
			for (Long dfid : dfids) {
				List<Object> dss = icat.search(sessionId,
						"SELECT ds FROM Dataset ds JOIN ds.datafiles df WHERE df.id = " + dfid
								+ " INCLUDE ds.investigation.facility");
				if (dss.size() == 1) {
					Dataset ds = (Dataset) dss.get(0);
					long dsid = ds.getId();
					dsInfos.put(dsid, new DsInfoImpl(ds));
					if (dfWanted) {
						Datafile df = (Datafile) icat.get(sessionId, "Datafile", dfid);
						dfInfos.add(new DfInfoImpl(df.getId(), df.getName(), df.getLocation(), df
								.getCreateId(), df.getModId(), dsid));
					}
				} else {
					icat.get(sessionId, "Datafile", dfid); // May reveal a permissions problem
					throw new NotFoundException("Datafile " + dfid);
				}
			}
			for (Long dsid : dsids) {
				Dataset ds;
				boolean empty = true;
				if (dfWanted) {
					ds = (Dataset) icat.get(sessionId,
							"Dataset ds INCLUDE ds.datafiles, ds.investigation.facility", dsid);
					for (Datafile df : ds.getDatafiles()) {
						dfInfos.add(new DfInfoImpl(df.getId(), df.getName(), df.getLocation(), df
								.getCreateId(), df.getModId(), dsid));
						empty = false;
					}
				} else {
					ds = (Dataset) icat.get(sessionId,
							"Dataset ds INCLUDE ds.investigation.facility", dsid);
				}
				dsInfos.put(dsid, new DsInfoImpl(ds));
				if (dfWanted && empty) {
					emptyDatasets.add(dsid);
				}
			}

			for (Long invid : invids) {
				List<Object> dss;

				if (dfWanted) {
					dss = icat.search(sessionId,
							"SELECT ds FROM Dataset ds WHERE ds.investigation.id = " + invid
									+ " INCLUDE ds.datafiles, ds.investigation.facility");
				} else {
					dss = icat.search(sessionId,
							"SELECT ds FROM Dataset ds WHERE ds.investigation.id = " + invid
									+ " INCLUDE ds.investigation.facility");
				}
				if (dss.size() >= 1) {
					for (Object o : dss) {
						Dataset ds = (Dataset) o;
						boolean empty = true;
						long dsid = ds.getId();
						if (dfWanted) {
							for (Datafile df : ds.getDatafiles()) {
								dfInfos.add(new DfInfoImpl(df.getId(), df.getName(), df
										.getLocation(), df.getCreateId(), df.getModId(), dsid));
							}
						}
						dsInfos.put(dsid, new DsInfoImpl(ds));
						if (dfWanted && empty) {
							emptyDatasets.add(dsid);
						}
					}
				} else {
					icat.get(sessionId, "Investigation", invid); // May reveal a permissions problem
					throw new NotFoundException("Investigation " + invid);
				}
			}

		} catch (IcatException_Exception e) {
			IcatExceptionType type = e.getFaultInfo().getType();
			if (type == IcatExceptionType.INSUFFICIENT_PRIVILEGES
					|| type == IcatExceptionType.SESSION) {
				throw new InsufficientPrivilegesException(e.getMessage());
			} else if (type == IcatExceptionType.NO_SUCH_OBJECT_FOUND) {
				throw new NotFoundException(e.getMessage());
			} else {
				throw new InternalException(e.getClass() + " " + e.getMessage());
			}

		}

		if (dsInfos.isEmpty()) {
			throw new BadRequestException("No investigation, dataset nor datafile specified");
		}
	}

	public Set<DfInfoImpl> getDfInfo() {
		return dfInfos;
	}

	public boolean mustZip() {
		return dfids.size() > 1L || !dsids.isEmpty() || !invids.isEmpty();
	}

	public boolean isSingleDataset() {
		return dfids.isEmpty() && dsids.size() == 1 && invids.isEmpty();
	}

	public Set<Long> getEmptyDatasets() {
		return emptyDatasets;
	}

}
