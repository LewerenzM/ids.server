package org.icatproject.ids.requestHandlers;

import java.util.HashMap;

import org.icatproject.ids.enums.CallType;
import org.icatproject.ids.enums.PreparedDataStatus;
import org.icatproject.ids.enums.RequestType;
import org.icatproject.ids.exceptions.BadRequestException;
import org.icatproject.ids.exceptions.DataNotOnlineException;
import org.icatproject.ids.exceptions.InsufficientPrivilegesException;
import org.icatproject.ids.exceptions.InternalException;
import org.icatproject.ids.exceptions.NotFoundException;
import org.icatproject.ids.exceptions.NotImplementedException;
import org.icatproject.ids.helpers.ValueContainer;
import org.icatproject.ids.services.ServiceProvider;

public class IsTwoLevelHandler extends RequestHandlerBase {

    public IsTwoLevelHandler() {
        super(PreparedDataStatus.NOMATTER, RequestType.ISTWOLEVEL);
    }

    @Override
    public ValueContainer handle(HashMap<String, ValueContainer> parameters)
            throws BadRequestException, InternalException, InsufficientPrivilegesException, NotFoundException,
            DataNotOnlineException, NotImplementedException {
        
        var serviceProvider = ServiceProvider.getInstance();

        if (serviceProvider.getLogSet().contains(CallType.INFO)) {
            serviceProvider.getTransmitter().processMessage("isTwoLevel", parameters.get("ip").getString(), "{}", System.currentTimeMillis());
        }
        return new ValueContainer(twoLevel);
    }
}