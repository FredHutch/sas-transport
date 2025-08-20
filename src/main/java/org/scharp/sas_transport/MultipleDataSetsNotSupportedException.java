///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * An exception that represents that the XPORT has multiple data sets which is not supported by the API.
 */
public class MultipleDataSetsNotSupportedException extends UnsupportedTransportFileException {

    /**
     * Standard serialization UID.
     */
    private static final long serialVersionUID = 1L;

    MultipleDataSetsNotSupportedException() {
    }
}