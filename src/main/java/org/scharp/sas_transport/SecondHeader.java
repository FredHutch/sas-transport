///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * This is a Java POJO representation of the C struct SECOND_HEADER.
 *
 * <pre>
 * struct SECOND_HEADER {
 *   char dtmod_day[2];
 *   char dtmod_month[3];
 *   char dtmod_year[2];
 *   char dtmod_colon1[1];
 *   char dtmod_hour[2];
 *   char dtmod_colon2[1];
 *   char dtmod_minute[2];
 *   char dtmod_colon2[1];
 *   char dtmod_second[2];
 *   char padding[16];
 *   char dslabel[40];
 *   char dstype[8];
 * };
 * </pre>
 *
 * This is used internally when serializing the file.
 */
class SecondHeader extends Record {

    SecondHeader(LocalDateTime createDate, String datasetLabel, String datasetType) {
        assert datasetLabel.getBytes(StandardCharsets.US_ASCII).length <= 40;
        assert datasetType.getBytes(StandardCharsets.US_ASCII).length <= 8;

        formatDate(createDate, 0); // dmod_*
        toSpacePaddedArray("                ", 16, 16); // padding[16]
        toSpacePaddedArray(datasetLabel, 32, 40); // dslabel[40]
        toSpacePaddedArray(datasetType, 32 + 40, 8); // dstype
    }

    /**
     * Constructor when reading the header from a serialized context.
     *
     * @param data
     *     The raw header (must by 80 bytes).
     */
    SecondHeader(byte[] data) {
        super(data);
    }

    LocalDateTime modifiedDate(YearMapper twoDigitYearToRealYear) throws MalformedTransportFileException {
        String formattedDate = readSpacePaddedString(0, 16);
        return parseDate(formattedDate, twoDigitYearToRealYear);
    }

    String label() {
        return readSpacePaddedString(32, 40);
    }

    String type() {
        return readSpacePaddedString(32 + 40, 8);
    }
}