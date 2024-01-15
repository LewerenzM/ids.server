package org.icatproject.ids.exceptions;

import java.net.HttpURLConnection;

@SuppressWarnings("serial")
public class InternalException extends IdsException {

    public InternalException(String message) {
        super(HttpURLConnection.HTTP_INTERNAL_ERROR, message);
    }
}
