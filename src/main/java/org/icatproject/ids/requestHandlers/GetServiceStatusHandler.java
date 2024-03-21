package org.icatproject.ids.requestHandlers;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Set;

import org.icatproject.IcatExceptionType;
import org.icatproject.IcatException_Exception;
import org.icatproject.ids.enums.CallType;
import org.icatproject.ids.enums.OperationIdTypes;
import org.icatproject.ids.enums.RequestIdNames;
import org.icatproject.ids.enums.RequestType;
import org.icatproject.ids.exceptions.BadRequestException;
import org.icatproject.ids.exceptions.DataNotOnlineException;
import org.icatproject.ids.exceptions.InsufficientPrivilegesException;
import org.icatproject.ids.exceptions.InternalException;
import org.icatproject.ids.exceptions.NotFoundException;
import org.icatproject.ids.exceptions.NotImplementedException;
import org.icatproject.ids.helpers.ValueContainer;
import org.icatproject.ids.services.ServiceProvider;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;

public class GetServiceStatusHandler extends RequestHandlerBase {

    private Set<String> rootUserNames;

    public  GetServiceStatusHandler() {
        super(OperationIdTypes.SESSIONID, RequestType.GETSERVICESTATUS);
    }

    public void init() throws InternalException {
        logger.info("Initializing GetServiceStatusHandler...");
        super.init();        
        rootUserNames = ServiceProvider.getInstance().getPropertyHandler().getRootUserNames();
        logger.info("GetDataHandler GetServiceStatusHandler");
    }

    @Override
    public ValueContainer handle(HashMap<String, ValueContainer> parameters)
            throws BadRequestException, InternalException, InsufficientPrivilegesException, NotFoundException,
            DataNotOnlineException, NotImplementedException {
        
        long start = System.currentTimeMillis();

        String sessionId = parameters.get(RequestIdNames.sessionId).getString();
        var serviceProvider = ServiceProvider.getInstance();

        // Log and validate
        logger.info("New webservice request: getServiceStatus");

        try {
            String uname = serviceProvider.getIcat().getUserName(sessionId);
            if (!rootUserNames.contains(uname)) {
                throw new InsufficientPrivilegesException(uname + " is not included in the ids rootUserNames set.");
            }
        } catch (IcatException_Exception e) {
            IcatExceptionType type = e.getFaultInfo().getType();
            if (type == IcatExceptionType.SESSION) {
                throw new InsufficientPrivilegesException(e.getClass() + " " + e.getMessage());
            }
            throw new InternalException(e.getClass() + " " + e.getMessage());
        }

        if (serviceProvider.getLogSet().contains(CallType.INFO)) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (JsonGenerator gen = Json.createGenerator(baos).writeStartObject()) {
                    gen.write("userName", serviceProvider.getIcat().getUserName(sessionId));
                    gen.writeEnd();
                }
                String body = baos.toString();
                serviceProvider.getTransmitter().processMessage("getServiceStatus", parameters.get("ip").getString(), body, start);
            } catch (IcatException_Exception e) {
                logger.error("Failed to prepare jms message " + e.getClass() + " " + e.getMessage());
            }
        }

        return new ValueContainer(serviceProvider.getFsm().getServiceStatus());
    }

}