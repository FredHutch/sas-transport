///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link Record} class.
 */
public class RecordTest {

    private static void runParseBadDateTest(String badDate, YearMapper twoDigitYearToRealYear) {
        Exception exception = assertThrows(//
            MalformedTransportFileException.class, //
            () -> Record.parseDate(badDate, twoDigitYearToRealYear), //
            "Parsing " + badDate);
        assertEquals("malformed date: " + badDate, exception.getMessage());
    }

    /**
     * Unit tests for {@link Record#parseDate} on bad input.
     */
    @Test
    public void testParseBadDates() {

        String[] badDates = new String[] { //
            "bad date", // completely wrong
            "1000", // pure numbers aren't dates
            "2015-01-01T00:00:00", // only one date format is supported
            "1JAN98:00:00:00", // day is not two digits
            "001JAN98:00:00:00", // day is not two digits
            "001JAN98:00:00:0", // day is not two digits (with correct overall length)
            "32JAN98:00:00:00", // day does not exist (Jan 32nd)
            "00JAN98:00:00:00", // day does not exist (Jan 0th)
            "01XXX98:00:00:00", // month completely wrong
            "01Jan98:00:00:00", // month isn't all caps
            "01JA98:00:00:00", // month too short
            "01JUNE98:00:00:00", // month too long
            "01JAN5:00:00:00", // year must be two digits
            "01JAN98T00:00:00", // incorrect year-hour separator "T"
            "01JAN032:00:00:00", // year must be two digits
            "01JAN032:00:00:0", // year must be two digits (with correct overall length)
            "01JAN32:0:00:00", // hour must be two digits
            "01JAN15:012:00:0", // hour must be two digits (with correct overall length)
            "01JAN32:24:00:00", // hour out of range
            "01JAN32:12-00:00", // bad hour-minute separator
            "01JAN32:12:1:00", // minute must be two digits
            "01JAN32:12:001:00", // minute must be two digits
            "01JAN15:12:001:0", // minute must be two digits (with correct overall length)
            "01JAN32:12:60:00", // minute out of range
            "01JAN32:12:00.00", // bad minute.second separator
            "01JAN32:12:00:0", // second must be two digits
            "01JAN32:12:01:001", // second must be two digits
            "01JAN15:12:001:0", // second must be two digits (with correct overall length)
            "01JAN32:12:00:60", // second out of range
            "30FEB00:12:00:00", // impossible date
        };
        for (String badDate : badDates) {
            runParseBadDateTest(badDate, (i) -> i);
        }
    }

    private static void runParseDateTest(String dateToParse, YearMapper twoDigitYearToRealYear,
        LocalDateTime expectedDate) throws MalformedTransportFileException {
        LocalDateTime actualDate = Record.parseDate(dateToParse, twoDigitYearToRealYear);
        assertEquals(expectedDate, actualDate, "parseDate(" + dateToParse + ") returned incorrect date");
    }

    /**
     * Unit tests for {@link Record#parseDate(String, YearMapper)} on good input.
     *
     * @throws MalformedTransportFileException
     */
    @Test
    public void testParseDate() throws MalformedTransportFileException {

        runParseDateTest("01JAN00:00:00:00", (i) -> i, LocalDateTime.of(0, 1, 1, 0, 0, 0));
        runParseDateTest("31DEC99:23:59:59", (i) -> i, LocalDateTime.of(99, 12, 31, 23, 59, 59));

        // test parsing of each month
        runParseDateTest("01JAN00:00:00:00", (i) -> i, LocalDateTime.of(0, 1, 1, 0, 0));
        runParseDateTest("01FEB00:00:00:00", (i) -> i, LocalDateTime.of(0, 2, 1, 0, 0));
        runParseDateTest("01MAR00:00:00:00", (i) -> i, LocalDateTime.of(0, 3, 1, 0, 0));
        runParseDateTest("01APR00:00:00:00", (i) -> i, LocalDateTime.of(0, 4, 1, 0, 0));
        runParseDateTest("01MAY00:00:00:00", (i) -> i, LocalDateTime.of(0, 5, 1, 0, 0));
        runParseDateTest("01JUN00:00:00:00", (i) -> i, LocalDateTime.of(0, 6, 1, 0, 0));
        runParseDateTest("01JUL00:00:00:00", (i) -> i, LocalDateTime.of(0, 7, 1, 0, 0));
        runParseDateTest("01AUG00:00:00:00", (i) -> i, LocalDateTime.of(0, 8, 1, 0, 0));
        runParseDateTest("01SEP00:00:00:00", (i) -> i, LocalDateTime.of(0, 9, 1, 0, 0));
        runParseDateTest("01OCT00:00:00:00", (i) -> i, LocalDateTime.of(0, 10, 1, 0, 0));
        runParseDateTest("01NOV00:00:00:00", (i) -> i, LocalDateTime.of(0, 11, 1, 0, 0));
        runParseDateTest("01DEC00:00:00:00", (i) -> i, LocalDateTime.of(0, 12, 1, 0, 0));
    }

    @Test
    public void testParseDateLeapYear() throws MalformedTransportFileException {
        // test leap year for years 0-4 (no adjustment)
        runParseDateTest("29FEB00:00:00:00", (i) -> i, LocalDateTime.of(0, 2, 29, 0, 0));
        runParseBadDateTest("29FEB01:00:00:00", (i) -> i);
        runParseBadDateTest("29FEB02:00:00:00", (i) -> i);
        runParseBadDateTest("29FEB03:00:00:00", (i) -> i);
        runParseDateTest("29FEB04:00:00:00", (i) -> i, LocalDateTime.of(4, 2, 29, 0, 0));

        // test leap year for years 1900-1904
        runParseBadDateTest("29FEB00:00:00:00", (i) -> 1900 + i); // 1900 is not a leap year
        runParseBadDateTest("29FEB01:00:00:00", (i) -> 1900 + i);
        runParseBadDateTest("29FEB02:00:00:00", (i) -> 1900 + i);
        runParseBadDateTest("29FEB03:00:00:00", (i) -> 1900 + i);
        runParseDateTest("29FEB04:00:00:00", (i) -> 1900 + i, LocalDateTime.of(1904, 2, 29, 0, 0));

        // test leap year for years 2000-2004
        runParseDateTest("29FEB00:00:00:00", (i) -> 2000 + i, LocalDateTime.of(2000, 2, 29, 0, 0));
        runParseBadDateTest("29FEB01:00:00:00", (i) -> 2000 + i);
        runParseBadDateTest("29FEB02:00:00:00", (i) -> 2000 + i);
        runParseBadDateTest("29FEB03:00:00:00", (i) -> 2000 + i);
        runParseDateTest("29FEB04:00:00:00", (i) -> 2000 + i, LocalDateTime.of(2004, 2, 29, 0, 0));
    }
}