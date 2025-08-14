///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * How strictly the input should be checked for well-formedness.
 */
enum StrictnessMode {
    /**
     * Throw exceptions for any data loss or anything that would cause SAS to not be able to read an XPORT.
     */
    BASIC,

    /**
     * Throw exceptions for any data that does not adhere to FDA submission guidelines.
     */
    FDA_SUBMISSION,
}