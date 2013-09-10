package org.icatproject.ids2.ported.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

import org.icatproject.Datafile;
import org.icatproject.Dataset;
import org.icatproject.ids.util.StatusInfo;
import org.icatproject.ids2.ported.RequestedState;

@SuppressWarnings("serial")
@Entity
@Table(name = "IDS2_REQUEST")
@XmlRootElement
public class RequestEntity implements Serializable {

	@Id
	@GeneratedValue
	private Long id;

	@Enumerated(EnumType.STRING)
	private StatusInfo status;

	@Temporal(TemporalType.TIMESTAMP)
	private Date submittedTime;

	@Temporal(TemporalType.TIMESTAMP)
	private Date expireTime;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "request")
	private List<Ids2DatafileEntity> datafiles;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "request")
	private List<Ids2DatasetEntity> datasets;

	@Enumerated(EnumType.STRING)
	private RequestedState requestedState;

	private String preparedId;
	private String sessionId;
	private String userId;

	@Column(name = "IDS_COMPRESS")
	private boolean compress;

	public RequestEntity() {
	}

	public RequestEntity(Long id) {
		this.id = id;
	}

	public RequestEntity(Long id, String preparedId, String userId, Date submittedTime, Date expireTime,
			RequestedState type) {
		this.id = id;
		this.preparedId = preparedId;
		this.userId = userId;
		this.submittedTime = submittedTime;
		this.expireTime = expireTime;
		this.requestedState = type;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public StatusInfo getStatus() {
		return status;
	}

	public void setStatus(StatusInfo status) {
		this.status = status;
	}

	public Date getSubmittedTime() {
		return submittedTime;
	}

	public void setSubmittedTime(Date submittedTime) {
		this.submittedTime = submittedTime;
	}

	public Date getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Date expireTime) {
		this.expireTime = expireTime;
	}

	public List<Ids2DatafileEntity> getDatafiles() {
		return datafiles;
	}

	public void setDatafiles(List<Ids2DatafileEntity> datafiles) {
		this.datafiles = datafiles;
	}

	public List<Ids2DatasetEntity> getDatasets() {
		return datasets;
	}

	public void setDatasets(List<Ids2DatasetEntity> datasets) {
		this.datasets = datasets;
	}

	public RequestedState getRequestedState() {
		return requestedState;
	}

	public void setRequestedState(RequestedState requestedState) {
		this.requestedState = requestedState;
	}

	public String getPreparedId() {
		return preparedId;
	}

	public void setPreparedId(String preparedId) {
		this.preparedId = preparedId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public boolean isCompress() {
		return compress;
	}

	public void setCompress(boolean compress) {
		this.compress = compress;
	}

	@Override
	public String toString() {
		return String.format("RequestEntity id=%s, requestedState=%s", id, requestedState);
	}

	public List<Ids2DataEntity> getDataEntities() {
		List<Ids2DataEntity> res = new ArrayList<Ids2DataEntity>();
		res.addAll(datafiles);
		res.addAll(datasets);
		return res;
	}

	/**
	 * Returns all ICAT Datafiles that were requested directly (Datafiles from
	 * requested Datasets don't count)
	 */
	public Set<Datafile> getIcatDatafiles() {
		Set<Datafile> datafiles = new HashSet<Datafile>();
		for (Ids2DatafileEntity df : this.getDatafiles()) {
			datafiles.addAll(df.getIcatDatafiles()); // will only add one DF
		}
		return datafiles;
	}

	/**
	 * Returns all ICAT Datasets that were requested directly (Datasets
	 * operation on which has been caused by a requested Datafile don't count)
	 */
	public Set<Dataset> getIcatDatasets() {
		Set<Dataset> datasets = new HashSet<Dataset>();
		for (Ids2DatasetEntity ds : this.getDatasets()) {
			datasets.add(ds.getIcatDataset());
		}
		return datasets;
	}

}