package org.icatproject.ids;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.icatproject.ids.exceptions.BadRequestException;
import org.icatproject.ids.exceptions.DataNotOnlineException;
import org.icatproject.ids.exceptions.InsufficientPrivilegesException;
import org.icatproject.ids.exceptions.InternalException;
import org.icatproject.ids.exceptions.NotFoundException;
import org.icatproject.ids.exceptions.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
@Stateless
public class IdsService {

	private final static Logger logger = LoggerFactory.getLogger(IdsService.class);

	@EJB
	private IdsBean idsBean;

	private Pattern rangeRe;

	/**
	 * Return list of id values of data files included in the preparedId
	 * returned by a call to prepareData or by the investigationIds, datasetIds
	 * and datafileIds specified along with a sessionId if the preparedId is not
	 * set.
	 * 
	 * @summary getDatafileIds
	 * @param preparedId
	 *            A valid preparedId returned by a call to prepareData
	 * @param sessionId
	 *            A sessionId returned by a call to the icat server.
	 * @param investigationIds
	 *            A comma separated list of investigation id values.
	 * @param datasetIds
	 *            A comma separated list of data set id values.
	 * @param datafileIds
	 *            A comma separated list of datafile id values.
	 * 
	 * @return a list of id values
	 * 
	 * @throws NotImplementedException
	 * @throws BadRequestException
	 * @throws InternalException
	 * @throws NotFoundException
	 * @throws InsufficientPrivilegesException
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("getDatafileIds")
	@Produces(MediaType.APPLICATION_JSON)
	public String getDatafileIds(@QueryParam("preparedId") String preparedId,
			@QueryParam("sessionId") String sessionId, @QueryParam("investigationIds") String investigationIds,
			@QueryParam("datasetIds") String datasetIds, @QueryParam("datafileIds") String datafileIds)
			throws NotImplementedException, BadRequestException, InternalException, NotFoundException,
			InsufficientPrivilegesException {
		if (preparedId != null) {
			return idsBean.getDatafileIds(preparedId);
		} else {
			return idsBean.getDatafileIds(sessionId, investigationIds, datasetIds, datafileIds);
		}
	}

	/**
	 * Return a hard link to a data file.
	 * 
	 * This is only useful in those cases where the user has direct access to
	 * the file system where the IDS is storing data. Only read access to the
	 * file is granted.
	 * 
	 * @summary getLink
	 * 
	 * @param sessionId
	 *            A valid ICAT session ID
	 * @param datafileId
	 *            the id of a data file
	 * @param username
	 *            the name of the user who will will be granted access to the
	 *            linked file.
	 * 
	 * @return the path of the created link.
	 * 
	 * @throws BadRequestException
	 * @throws InsufficientPrivilegesException
	 * @throws NotImplementedException
	 * @throws InternalException
	 * @throws NotFoundException
	 * @throws DataNotOnlineException
	 * 
	 * @statuscode 200 To indicate success
	 * 
	 */
	@POST
	@Path("getLink")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	public String getLink(@FormParam("sessionId") String sessionId, @FormParam("datafileId") long datafileId,
			@FormParam("username") String username) throws BadRequestException, InsufficientPrivilegesException,
			NotImplementedException, InternalException, NotFoundException, DataNotOnlineException {
		return idsBean.getLink(sessionId, datafileId, username);
	}

	/**
	 * Archive data specified by the investigationIds, datasetIds and
	 * datafileIds specified along with a sessionId. If two level storage is not
	 * in use this has no effect.
	 * 
	 * @summary archive
	 * 
	 * @param sessionId
	 *            A sessionId returned by a call to the icat server.
	 * @param investigationIds
	 *            If present, a comma separated list of investigation id values
	 * @param datasetIds
	 *            If present, a comma separated list of data set id values or
	 *            null
	 * @param datafileIds
	 *            If present, a comma separated list of datafile id values.
	 * 
	 * @throws BadRequestException
	 * @throws InsufficientPrivilegesException
	 * @throws NotImplementedException
	 * @throws InternalException
	 * @throws NotFoundException
	 * 
	 * @statuscode 200 To indicate success
	 */
	@POST
	@Path("archive")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void archive(@FormParam("sessionId") String sessionId,
			@FormParam("investigationIds") String investigationIds, @FormParam("datasetIds") String datasetIds,
			@FormParam("datafileIds") String datafileIds) throws BadRequestException, InsufficientPrivilegesException,
			NotImplementedException, InternalException, NotFoundException {
		idsBean.archive(sessionId, investigationIds, datasetIds, datafileIds);
	}

	/**
	 * Archive data specified by the investigationIds, datasetIds and
	 * datafileIds specified along with a sessionId.
	 * 
	 * @summary delete
	 * 
	 * @param sessionId
	 *            A sessionId returned by a call to the icat server.
	 * @param investigationIds
	 *            If present, a comma separated list of investigation id values
	 * @param datasetIds
	 *            If present, a comma separated list of data set id values or
	 *            null
	 * @param datafileIds
	 *            If present, a comma separated list of datafile id values.
	 * 
	 * @throws NotImplementedException
	 * @throws BadRequestException
	 * @throws InsufficientPrivilegesException
	 * @throws NotFoundException
	 * @throws InternalException
	 * @throws DataNotOnlineException
	 * 
	 * @statuscode 200 To indicate success
	 */
	@DELETE
	@Path("delete")
	public void delete(@QueryParam("sessionId") String sessionId,
			@QueryParam("investigationIds") String investigationIds, @QueryParam("datasetIds") String datasetIds,
			@QueryParam("datafileIds") String datafileIds) throws NotImplementedException, BadRequestException,
			InsufficientPrivilegesException, NotFoundException, InternalException, DataNotOnlineException {
		idsBean.delete(sessionId, investigationIds, datasetIds, datafileIds);
	}

	/**
	 * An ids server can be configured to be read only. This returns the
	 * readOnly status of the server.
	 * 
	 * @summary isReadOnly
	 * 
	 * @return true if readonly, else false
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("isReadOnly")
	@Produces(MediaType.TEXT_PLAIN)
	public boolean isReadOnly() {
		return idsBean.isReadOnly();
	}

	/**
	 * An ids server can be configured to support one or two levels of data
	 * storage. This returns the twoLevel status of the server.
	 * 
	 * @summary isTwoLevel
	 * 
	 * @return true if twoLevel, else false
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("isTwoLevel")
	@Produces(MediaType.TEXT_PLAIN)
	public boolean isTwoLevel() {
		return idsBean.isTwoLevel();
	}

	/**
	 * Return the version of the server
	 * 
	 * @summary getApiVersion
	 * 
	 * @return the version of the ids server
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("getApiVersion")
	@Produces(MediaType.TEXT_PLAIN)
	public String getApiVersion() {
		return Constants.API_VERSION;
	}

	/**
	 * Return the url of the icat.server that this ids.server has been
	 * configured to use. This is the icat.server from which a sessionId must be
	 * obtained.
	 * 
	 * @return the url of the icat server
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("getIcatUrl")
	@Produces(MediaType.TEXT_PLAIN)
	public String getIcatUrl() {
		return idsBean.getIcatUrl();
	}

	/**
	 * Return the total size of all the data files specified by the
	 * investigationIds, datasetIds and datafileIds along with a sessionId.
	 * 
	 * @summary getSize
	 * 
	 * @param sessionId
	 *            A sessionId returned by a call to the icat server.
	 * @param investigationIds
	 *            If present, a comma separated list of investigation id values
	 * @param datasetIds
	 *            If present, a comma separated list of data set id values or
	 *            null
	 * @param datafileIds
	 *            If present, a comma separated list of data file id values.
	 * 
	 * @return the size in bytes
	 * 
	 * @throws BadRequestException
	 * @throws NotFoundException
	 * @throws InsufficientPrivilegesException
	 * @throws InternalException
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("getSize")
	@Produces(MediaType.TEXT_PLAIN)
	public long getSize(@QueryParam("sessionId") String sessionId,
			@QueryParam("investigationIds") String investigationIds, @QueryParam("datasetIds") String datasetIds,
			@QueryParam("datafileIds") String datafileIds) throws BadRequestException, NotFoundException,
			InsufficientPrivilegesException, InternalException {
		return idsBean.getSize(sessionId, investigationIds, datasetIds, datafileIds);
	}

	/**
	 * Return data files included in the preparedId returned by a call to
	 * prepareData or by the investigationIds, datasetIds and datafileIds, any
	 * of which may be omitted, along with a sessionId if the preparedId is not
	 * set. If preparedId is set the compress and zip arguments are not used.
	 * 
	 * @summary getData
	 * @param preparedId
	 *            A valid preparedId returned by a call to prepareData
	 * @param sessionId
	 *            A sessionId returned by a call to the icat server.
	 * @param investigationIds
	 *            A comma separated list of investigation id values.
	 * @param datasetIds
	 *            A comma separated list of data set id values.
	 * @param datafileIds
	 *            A comma separated list of data file id values.
	 * @param compress
	 *            If true use default compression otherwise no compression. This
	 *            only applies if preparedId is not set and if the results are
	 *            being zipped.
	 * @param zip
	 *            If true the data should be zipped. If multiple files are
	 *            requested (or could be because a datasetId or investigationId
	 *            has been specified) the data are zipped regardless of the
	 *            specification of this flag.
	 * @param outname
	 *            The file name to put in the returned header
	 *            "ContentDisposition". If it does not end in .zip but it is a
	 *            zip file then a ".zip" will be appended.
	 * @param range
	 *            A range header which must match "bytes=(\\d+)-" to specify an
	 *            offset i.e. to skip a number of bytes.
	 * 
	 * @return a stream of json data.
	 * 
	 * @throws BadRequestException
	 * @throws NotFoundException
	 * @throws InternalException
	 * @throws InsufficientPrivilegesException
	 * @throws NotImplementedException
	 * @throws DataNotOnlineException
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("getData")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getData(@QueryParam("preparedId") String preparedId, @QueryParam("sessionId") String sessionId,
			@QueryParam("investigationIds") String investigationIds, @QueryParam("datasetIds") String datasetIds,
			@QueryParam("datafileIds") String datafileIds, @QueryParam("compress") boolean compress,
			@QueryParam("zip") boolean zip, @QueryParam("outname") String outname, @HeaderParam("Range") String range)
			throws BadRequestException, NotFoundException, InternalException, InsufficientPrivilegesException,
			NotImplementedException, DataNotOnlineException {
		Response response = null;

		long offset = 0;
		if (range != null) {

			Matcher m = rangeRe.matcher(range);
			if (!m.matches()) {
				throw new BadRequestException("The range must match " + rangeRe.pattern());
			}
			offset = Long.parseLong(m.group(1));
			logger.debug("Range " + range + " -> offset " + offset);
		}

		if (preparedId != null) {
			response = idsBean.getData(preparedId, outname, offset);
		} else {
			response = idsBean.getData(sessionId, investigationIds, datasetIds, datafileIds, compress, zip, outname,
					offset);
		}
		return response;
	}

	/**
	 * Return the archive status of the data files specified by the
	 * investigationIds, datasetIds and datafileIds along with a sessionId.
	 * 
	 * @summary getStatus
	 * 
	 * @param sessionId
	 *            A sessionId returned by a call to the icat server. If the
	 *            sessionId is omitted or null the ids reader account will be
	 *            used which has read access to all data.
	 * @param investigationIds
	 *            If present, a comma separated list of investigation id values
	 * @param datasetIds
	 *            If present, a comma separated list of data set id values or
	 *            null
	 * @param datafileIds
	 *            If present, a comma separated list of data file id values.
	 * 
	 * @return a string with "ONLINE" if all data are online, "RESTORING" if one
	 *         or more files are in the process of being restored but none are
	 *         archived and no restoration has been requested or "ARCHIVED" if
	 *         one or more files are archived and and no restoration has been
	 *         requested.
	 * 
	 * @throws BadRequestException
	 * @throws NotFoundException
	 * @throws InsufficientPrivilegesException
	 * @throws InternalException
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("getStatus")
	@Produces(MediaType.TEXT_PLAIN)
	public String getStatus(@QueryParam("sessionId") String sessionId,
			@QueryParam("investigationIds") String investigationIds, @QueryParam("datasetIds") String datasetIds,
			@QueryParam("datafileIds") String datafileIds) throws BadRequestException, NotFoundException,
			InsufficientPrivilegesException, InternalException {
		return idsBean.getStatus(sessionId, investigationIds, datasetIds, datafileIds);
	}

	/**
	 * Returns true if all the data files are ready to be downloaded. As a side
	 * effect, if any data files are archived and no restoration has been
	 * requested then a restoration of those data files will be launched.
	 * 
	 * @summary isPrepared
	 * 
	 * @param preparedId
	 *            A valid preparedId returned by a call to prepareData
	 * 
	 * @return true if all the data files are ready to be downloaded else false.
	 * 
	 * @throws BadRequestException
	 * @throws NotFoundException
	 * @throws InternalException
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("isPrepared")
	@Produces(MediaType.TEXT_PLAIN)
	public boolean isPrepared(@QueryParam("preparedId") String preparedId) throws BadRequestException,
			NotFoundException, InternalException {
		return idsBean.isPrepared(preparedId);
	}

	/**
	 * Obtain detailed information about what the ids is doing. You need to be
	 * privileged to use this call.
	 * 
	 * @summary getServiceStatus
	 * 
	 * @param sessionId
	 *            A valid ICAT session ID of a user in the IDS rootUserNames
	 *            set.
	 * 
	 * @return a json string.
	 * 
	 * @throws InternalException
	 * @throws InsufficientPrivilegesException
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("getServiceStatus")
	@Produces(MediaType.APPLICATION_JSON)
	public String getServiceStatus(@QueryParam("sessionId") String sessionId) throws InternalException,
			InsufficientPrivilegesException {
		return idsBean.getServiceStatus(sessionId);
	}

	@PostConstruct
	private void init() {
		logger.info("creating IdsService");
		rangeRe = Pattern.compile("bytes=(\\d+)-");
		logger.info("created IdsService");
	}

	@PreDestroy
	private void exit() {
		logger.info("destroyed IdsService");
	}

	/**
	 * Should return "IdsOK"
	 * 
	 * @summary ping
	 * 
	 * @return "IdsOK"
	 * 
	 * @statuscode 200 To indicate success
	 */
	@GET
	@Path("ping")
	@Produces(MediaType.TEXT_PLAIN)
	public String ping() {
		logger.debug("ping request received");
		return "IdsOK";
	}

	/**
	 * Prepare data files for subsequent download. For single level storage the
	 * only benefit of this call is that the returned preparedId may be shared
	 * with others to provide access to the data. The data files are specified
	 * by the investigationIds, datasetIds and datafileIds, any of which may be
	 * omitted, along with a sessionId.
	 * 
	 * @summary prepareData
	 * 
	 * @param sessionId
	 *            A sessionId returned by a call to the icat server.
	 * @param investigationIds
	 *            A comma separated list of investigation id values.
	 * @param datasetIds
	 *            A comma separated list of data set id values.
	 * @param datafileIds
	 *            A comma separated list of datafile id values.
	 * @param compress
	 *            If true use default compression otherwise no compression. This
	 *            only applies if preparedId is not set and if the results are
	 *            being zipped.
	 * @param zip
	 *            If true the data should be zipped. If multiple files are
	 *            requested (or could be because a datasetId or investigationId
	 *            has been specified) the data are zipped regardless of the
	 *            specification of this flag.
	 * 
	 * @return a string with the preparedId
	 * 
	 * @throws NotImplementedException
	 * @throws BadRequestException
	 * @throws InsufficientPrivilegesException
	 * @throws NotFoundException
	 * @throws InternalException
	 * 
	 * @statuscode 200 To indicate success
	 */
	@POST
	@Path("prepareData")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	public String prepareData(@FormParam("sessionId") String sessionId,
			@FormParam("investigationIds") String investigationIds, @FormParam("datasetIds") String datasetIds,
			@FormParam("datafileIds") String datafileIds, @FormParam("compress") boolean compress,
			@FormParam("zip") boolean zip) throws NotImplementedException, BadRequestException,
			InsufficientPrivilegesException, NotFoundException, InternalException {
		return idsBean.prepareData(sessionId, investigationIds, datasetIds, datafileIds, compress, zip);
	}

	/**
	 * Stores a data file
	 * 
	 * @summary put
	 * 
	 * @param body
	 *            The contents of the file to be stored
	 * @param sessionId
	 *            A sessionId returned by a call to the icat server.
	 * @param name
	 *            A name to assign to the data file
	 * @param datafileFormatId
	 *            The id of the data file format to associate with the data file
	 * @param datasetId
	 *            The id of the data set to which the data file should be
	 *            associated.
	 * @param description
	 *            An optional description to associate with the data file
	 * @param doi
	 *            An optional description to associate with the data file
	 * @param datafileCreateTime
	 *            An optional datafileCreateTime to associate with the data file
	 * @param datafileModTime
	 *            An optional datafileModTime to associate with the data file
	 * 
	 * @return a json object with attributes of "id", "checksum", "location" and
	 *         "size";
	 * 
	 * @throws BadRequestException
	 * @throws NotFoundException
	 * @throws InternalException
	 * @throws InsufficientPrivilegesException
	 * @throws NotImplementedException
	 * @throws DataNotOnlineException
	 * 
	 * @statuscode 201 When object successfully created
	 */
	@PUT
	@Path("put")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(InputStream body, @QueryParam("sessionId") String sessionId, @QueryParam("name") String name,
			@QueryParam("datafileFormatId") long datafileFormatId, @QueryParam("datasetId") long datasetId,
			@QueryParam("description") String description, @QueryParam("doi") String doi,
			@QueryParam("datafileCreateTime") Long datafileCreateTime,
			@QueryParam("datafileModTime") Long datafileModTime) throws BadRequestException, NotFoundException,
			InternalException, InsufficientPrivilegesException, NotImplementedException, DataNotOnlineException {
		return idsBean.put(body, sessionId, name, datafileFormatId, datasetId, description, doi, datafileCreateTime,
				datafileModTime, false, false);
	}

	/**
	 * This is an alternative to using PUT on the put resource. All the same
	 * arguments appear as form fields. In addition there are two boolean fields
	 * wrap and padding which should be set to true as a CORS work around. These
	 * two fields will be removed shortly as they are only required by the old
	 * (GWT based) topcat.
	 * 
	 * @summary putAsPost
	 * 
	 * @param request
	 * 
	 * @return a json object with attributes of "id", "checksum", "location" and
	 *         "size";
	 * 
	 * @throws BadRequestException
	 * @throws NotFoundException
	 * @throws InternalException
	 * @throws InsufficientPrivilegesException
	 * @throws NotImplementedException
	 * @throws DataNotOnlineException
	 * 
	 * @statuscode 201 When object successfully created
	 */
	@POST
	@Path("put")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response putAsPost(@Context HttpServletRequest request) throws BadRequestException, NotFoundException,
			InternalException, InsufficientPrivilegesException, NotImplementedException, DataNotOnlineException {
		if (!ServletFileUpload.isMultipartContent(request)) {
			throw new BadRequestException("Multipart content expected");
		}
		try {
			ServletFileUpload upload = new ServletFileUpload();
			String sessionId = null;
			String name = null;
			long datafileFormatId = 0;
			long datasetId = 0;
			String description = null;
			String doi = null;
			Long datafileCreateTime = null;
			Long datafileModTime = null;
			Response result = null;
			boolean wrap = false;
			boolean padding = false;

			// Parse the request
			FileItemIterator iter = upload.getItemIterator(request);
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				String fieldName = item.getFieldName();
				InputStream stream = item.openStream();
				if (item.isFormField()) {
					String value = Streams.asString(stream);
					if (fieldName.equals("sessionId")) {
						sessionId = value;
					} else if (fieldName.equals("name")) {
						name = value;
					} else if (fieldName.equals("datafileFormatId")) {
						datafileFormatId = Long.parseLong(value);
					} else if (fieldName.equals("datasetId")) {
						datasetId = Long.parseLong(value);
					} else if (fieldName.equals("description")) {
						description = value;
					} else if (fieldName.equals("doi")) {
						doi = value;
					} else if (fieldName.equals("datafileCreateTime")) {
						datafileCreateTime = Long.parseLong(value);
					} else if (fieldName.equals("datafileModTime")) {
						datafileModTime = Long.parseLong(value);
					} else if (fieldName.equals("wrap")) {
						wrap = (value != null && value.toUpperCase().equals("TRUE"));
					} else if (fieldName.equals("padding")) {
						padding = (value != null && value.toUpperCase().equals("TRUE"));
					} else {
						throw new BadRequestException("Form field " + fieldName + "is not recognised");
					}
				} else {
					if (name == null) {
						name = item.getName();
					}
					result = idsBean.put(stream, sessionId, name, datafileFormatId, datasetId, description, doi,
							datafileCreateTime, datafileModTime, wrap, padding);
				}
			}
			return result;
		} catch (IOException | FileUploadException e) {
			throw new InternalException(e.getClass() + " " + e.getMessage());
		}
	}

	/**
	 * Restore data specified by the investigationIds, datasetIds and
	 * datafileIds specified along with a sessionId. If two level storage is not
	 * in use this has no effect.
	 * 
	 * @summary restore
	 * 
	 * @param sessionId
	 *            A sessionId returned by a call to the icat server.
	 * @param investigationIds
	 *            If present, a comma separated list of investigation id values
	 * @param datasetIds
	 *            If present, a comma separated list of data set id values or
	 *            null
	 * @param datafileIds
	 *            If present, a comma separated list of datafile id values.
	 *
	 * @throws NotImplementedException
	 * @throws BadRequestException
	 * @throws InsufficientPrivilegesException
	 * @throws InternalException
	 * @throws NotFoundException
	 * 
	 * @statuscode 200 To indicate success
	 */
	@POST
	@Path("restore")
	@Consumes("application/x-www-form-urlencoded")
	public void restore(@FormParam("sessionId") String sessionId,
			@FormParam("investigationIds") String investigationIds, @FormParam("datasetIds") String datasetIds,
			@FormParam("datafileIds") String datafileIds) throws NotImplementedException, BadRequestException,
			InsufficientPrivilegesException, InternalException, NotFoundException {
		idsBean.restore(sessionId, investigationIds, datasetIds, datafileIds);
	}

}