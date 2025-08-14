///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * This is a Java POJO representation the NAMESTR header record described in section 6 of TS-140.
 * <p>
 * This is used internally when serializing XPORT files.
 */
class NamestrHeaderRecord extends Record {
    private final int variableCount;

    /**
     * The constructor used when writing a NAMESTR header record.
     *
     * @param totalVariables
     *     The total number of NAMESTR records. This must be between 0 and 9999.
     */
    NamestrHeaderRecord(int totalVariables) {
        assert 0 <= totalVariables;
        assert totalVariables <= 9999;

        // format the total number of fields as a four character decimal.
        String totalFieldsString = String.format("%04d", totalVariables);

        int offset = 0;
        offset += toArray("HEADER RECORD*******NAMESTR HEADER RECORD!!!!!!!000000", offset);
        offset += toArray(totalFieldsString, offset);
        toArray("00000000000000000000  ", offset);

        this.variableCount = totalVariables;
    }

    /**
     * The constructor used when reading a NAMESTR header record.
     *
     * @param data
     *     The raw data the NAMESTR header record.
     *
     * @throws IllegalArgumentException
     *     if {@code data} is not a well-formed NAMESTR header record.
     */
    NamestrHeaderRecord(byte[] data) {
        super(data);

        // Maintain the class invariant by checking the variable count.
        String variableCountString = readSpacePaddedString(54, 4);
        try {
            // Parse the variable count.
            variableCount = Integer.parseInt(variableCountString, 10);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                "Unparsable variable count in NAMESTR HEADER RECORD: " + variableCountString);
        }

        // Confirm that the variable count is non-negative.
        if (variableCount < 0) {
            throw new IllegalArgumentException(
                "Illegal variable count in NAMESTR HEADER RECORD: " + variableCountString);
        }
    }

    /**
     * @return the number of NAMESTR records that follows this header. This is between 0 and 9999.
     */
    int variableCount() {
        return variableCount;
    }
}