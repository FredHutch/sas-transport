///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * A file type in which SAS data is stored.
 */
public enum SasFileType {
    /**
     * An unknown or malformed file.
     */
    UNKNOWN,

    /**
     * A SAS V5 XPORT file.
     */
    SAS_V5_XPORT, //

    /**
     * A SAS V8 XPORT file.
     */
    SAS_V8_XPORT, //

    /**
     * A SAS CPORT file (compressed XPORT).
     */
    SAS_CPORT, //
}