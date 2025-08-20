///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * The justification of a SAS variable.
 * <p>
 * This corresponds to the {@code nfj} field of the NAMESTR record in TS-140.
 * </p>
 * <p>
 * The SAS documentation makes no further references to its semantics and seems to ignore whatever value is set. It is
 * likely that this is documented incorrectly in TS-140 and really should be considered padding and ignored.
 * </p>
 * <p>
 * This enum is defined only for completeness. It is recommended that you always set this to {@code Justification.LEFT}
 * when exporting an XPORT and ignore it when importing an XPORT.
 * </p>
 */
public enum Justification {
    /**
     * Left-justified
     */
    LEFT,

    /**
     * Right-justified
     */
    RIGHT,

    /**
     * The justification was some unexpected value within the XPORT file. This is not defined within TS-140 and is only
     * provided so that unexpected values can be read without translation into a good value and without throwing an
     * exception.
     */
    UNKNOWN,
}