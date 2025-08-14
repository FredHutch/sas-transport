///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import java.io.IOException;

/**
 * An exception that represents an error because the supplied data was critically malformed.
 * <p>
 * This exception is thrown independent of the {@link StrictnessMode}.
 */
public class MalformedTransportFileException extends IOException {

    /**
     * Standard serialization UID.
     */
    private static final long serialVersionUID = 1L;

    MalformedTransportFileException() {
    }

    MalformedTransportFileException(String message) {
        super(message);
    }

    MalformedTransportFileException(String message, Throwable cause) {
        super(message, cause);
    }
}