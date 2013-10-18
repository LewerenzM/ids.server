package org.icatproject.ids.webservice.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Capture any {@link org.icatproject.ids.webservice.exceptions.IdsException WebServiceException}
 * thrown from {@link org.icatproject.ids.webservice.IdsService WebService} and generate the
 * appropriate response code and message.
 */
@Provider
public class IdsExceptionMapper implements ExceptionMapper<IdsException> {

	@Override
	public Response toResponse(IdsException e) {
		ObjectMapper om = new ObjectMapper();
		ObjectNode error = om.createObjectNode();
		error.put("code", e.getCode().name());
		error.put("message", e.getShortMessage());
		try {
			return Response.status(e.getHttpStatusCode()).entity(om.writeValueAsString(error))
					.build();
		} catch (JsonProcessingException e1) {
			return Response.status(e.getHttpStatusCode()).entity(e.getMessage()).build();
		}

	}
}