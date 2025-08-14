///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import java.io.IOException;

/**
 * An exception that indicates the supplied file format is not supported.
 */
public class UnsupportedTransportFileException extends IOException {

    /**
     * Standard serialization UID.
     */
    private static final long serialVersionUID = 1L;

    UnsupportedTransportFileException() {
    }

    UnsupportedTransportFileException(String message) {
        super(message);
    }
}