///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import java.time.LocalDateTime;

/**
 * This is a Java POJO representation of the C struct REAL_HEADER.
 *
 * <pre>
 * struct REAL_HEADER {
 *   char sas_symbol[2][8];
 *   char saslib[8];
 *   char sasver[8];
 *   char sas_os[8];
 *   char blanks[24];
 *   char sas_create[16];
 * };
 * </pre>
 *
 * This is used internally when serializing an XPORT file.
 */
class RealHeader extends Record {
    private final LocalDateTime createDate;

    /**
     * The constructor to use when writing a REAL_HEADER.
     *
     * @param symbol1
     * @param symbol2
     * @param sasLibrary
     * @param sasVersion
     * @param operatingSystem
     * @param createDate
     */
    RealHeader(String symbol1, String symbol2, String sasLibrary, String sasVersion, String operatingSystem,
        LocalDateTime createDate) {
        int offset = 0;
        offset += toSpacePaddedArray(symbol1, offset, 8);
        offset += toSpacePaddedArray(symbol2, offset, 8);
        offset += toSpacePaddedArray(sasLibrary, offset, 8);
        offset += toSpacePaddedArray(sasVersion, offset, 8);
        offset += toPaddedArray(operatingSystem, offset, 8, (byte) 0); // SAS serializes OS with NUL padding
        offset += toSpacePaddedArray("", offset, 24); // blanks (24 spaces)
        formatDate(createDate, offset); // fill in sas_create

        // Maintain the class invariant (even though this is never read by the library)
        this.createDate = createDate;
    }

    /**
     * The constructor to use when reading a REAL_HEADER.
     *
     * @param data
     *     The raw data for the REAL_HEADER.
     * @param twoDigitYearToRealYear
     *     A function which maps a two-digit year given in the SAS Transport format into a real year (usually four
     *     digits).
     *
     * @throws MalformedTransportFileException
     *     if {@code data} is not a well-formed REAL_HEADER.
     */
    RealHeader(byte[] data, YearMapper twoDigitYearToRealYear) throws MalformedTransportFileException {
        super(data);

        // Read the date now to throw a MalformedTransportFileException if it's bad.
        String formattedDate = readSpacePaddedString(8 * 5 + 24, 16);
        createDate = parseDate(formattedDate, twoDigitYearToRealYear);

        // Test that the "blanks" region is all blanks.
        // HACK: the %loc2xpt macro has an off-by-one bug that writes the OS field
        // one byte higher than it should, which puts the end of the OS (typically
        // a NUL byte) into the "blanks" section.  To be able to read V5 XPORT files
        // that were generated with %loc2xpt, we skip the first byte.
        for (int i = 41; i < 40 + 24; i++) {
            if (data[i] != ASCII_BLANK) {
                throw new MalformedTransportFileException("corrupt blanks region in REAL_HEADER");
            }
        }
    }

    String symbol1() {
        return readSpacePaddedString(0, 8);
    }

    String symbol2() {
        return readSpacePaddedString(8, 8);
    }

    String sasLibrary() {
        return readSpacePaddedString(16, 8);
    }

    String sasVersion() {
        return readSpacePaddedString(24, 8);
    }

    String operatingSystem() {
        String operatingSystemString = readSpacePaddedString(32, 8);

        // TS-140 says nothing about how these fields are padded.  In practice, SAS writes
        // all fields padded with spaces except the operating system, which is padded with
        // ASCII NUL characters.  To avoid confusing errors for the caller, where the OS
        // doesn't compare as equal to, for example "Linux", even though it prints as "Linux",
        // because it's really "Linux\0\0\0", we also remove trailing NUL characters.
        return operatingSystemString.replaceAll("\\00+$", "");
    }

    String blanks() {
        return readSpacePaddedString(40, 24);
    }

    LocalDateTime createDate() {
        return createDate;
    }
}