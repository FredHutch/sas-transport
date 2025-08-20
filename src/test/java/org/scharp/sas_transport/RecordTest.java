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

    /** Unit tests for {@link Record#readInt}. */
    @Test
    public void testReadInt() {
        // Create a record some sample data.
        byte[] data = new byte[80];
        data[0] = 0;
        data[1] = 1;
        data[2] = 2;
        data[3] = 3;
        data[4] = 4;
        data[5] = (byte) 0xFE;
        data[6] = (byte) 0xFD;
        data[7] = (byte) 0xFC;
        data[8] = (byte) 0xFB;
        Record record = new Record(data);

        assertEquals(0x00_01_02_03, record.readInt(0));
        assertEquals(0x01_02_03_04, record.readInt(1));
        assertEquals(0xFE_FD_FC_FB, record.readInt(5));
        assertEquals(0, record.readInt(9));
    }

    /** Unit tests for {@link Record#toArray} overloads. */
    @Test
    public void testToArray() {
        // Create a blank record.
        byte[] data = new byte[80];
        Record record = new Record(data);

        // Write short value
        assertEquals(2, record.toArray((short) 0x12_34, 0));
        assertEquals(0x12, data[0]);
        assertEquals(0x34, data[1]);
        assertEquals(0x00, data[2]); // not changed

        // Write short value with the high bit set
        assertEquals(2, record.toArray((short) 0xFA_FB, 1));
        assertEquals(0x12, data[0]); // not changed
        assertEquals((byte) 0xFA, data[1]);
        assertEquals((byte) 0xFB, data[2]);
        assertEquals(0x00, data[3]); // not changed

        // Write an int value
        assertEquals(4, record.toArray(0x12_34_56_78, 0));
        assertEquals(0x12, data[0]);
        assertEquals(0x34, data[1]);
        assertEquals(0x56, data[2]);
        assertEquals(0x78, data[3]);
        assertEquals(0x00, data[4]); // not changed

        // Write an int value with the high bit set.
        assertEquals(4, record.toArray(0xFA_FB_FC_FD, 1));
        assertEquals(0x12, data[0]); // not changed
        assertEquals((byte) 0xFA, data[1]);
        assertEquals((byte) 0xFB, data[2]);
        assertEquals((byte) 0xFC, data[3]);
        assertEquals((byte) 0xFD, data[4]);
        assertEquals(0x00, data[5]); // not changed

        // Write a String
        assertEquals(5, record.toArray("hello", 0));
        assertEquals('h', data[0]);
        assertEquals('e', data[1]);
        assertEquals('l', data[2]);
        assertEquals('l', data[3]);
        assertEquals('o', data[4]);
        assertEquals(0x00, data[5]); // not changed

        // Write a non-ASCII string
        final String sigma = "\u03C3"; // GREEK SMALL LETTER SIGMA
        final String grin = "\uD83D\uDE01"; // GRINNING FACE WITH SMILING EYES (two chars)
        assertEquals(5, record.toArray("a" + sigma + grin + "z", 20));
        assertEquals('a', data[20]);
        assertEquals('?', data[21]);
        assertEquals('?', data[22]);
        assertEquals('z', data[23]);
        assertEquals(' ', data[24]); // may be a bug, grin should is two chars (surrogate pair)
        assertEquals(0x00, data[25]); // not changed

        // Write a String with padding
        assertEquals(5, record.toPaddedArray("HI", 1, 5, (byte) 0xFD));
        assertEquals('h', data[0]); // not changed
        assertEquals('H', data[1]);
        assertEquals('I', data[2]);
        assertEquals((byte) 0xFD, data[3]);
        assertEquals((byte) 0xFD, data[4]);
        assertEquals((byte) 0xFD, data[5]);
        assertEquals(0x00, data[6]); // not changed

        // Write a String with space padding
        assertEquals(5, record.toSpacePaddedArray("data", 2, 5));
        assertEquals('h', data[0]); // not changed
        assertEquals('H', data[1]); // not changed
        assertEquals('d', data[2]);
        assertEquals('a', data[3]);
        assertEquals('t', data[4]);
        assertEquals('a', data[5]);
        assertEquals(' ', data[6]);
        assertEquals(0x00, data[7]); // not changed
    }
}