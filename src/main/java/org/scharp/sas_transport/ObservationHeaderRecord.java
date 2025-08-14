///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * This is a Java POJO of the header that begins the observations (data) that is described in Section 8 of TS-140.
 * <p>
 * This is used internally when serializing an XPORT file.
 */
class ObservationHeaderRecord extends Record {

    ObservationHeaderRecord() {
        super("HEADER RECORD*******OBS     HEADER RECORD!!!!!!!000000000000000000000000000000  ");
    }
}