///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a Java POJO representation of a record in a SAS dataset, as described in the INTRODUCTION of TS-140.
 */
class Record {

    /**
     * The size of an XPORT record, as defined in the INTRODUCTION of TS-140.
     */
    static final int RECORD_SIZE = 80;

    /**
     * The ASCII value for a space, what TS-140 calls a "blank" and what is typically used for padding.
     */
    static final byte ASCII_BLANK = ' ';

    // The first record of a well-formed SAS V5 XPORT file.
    static final String V5_XPORT_FIRST_RECORD = //
        "HEADER RECORD*******" + //
            "LIBRARY HEADER RECORD!!!!!!!" + //
            "000000000000000000000000000000" + // 30 0s
            "  ";

    static final String V8_XPORT_FIRST_RECORD = //
        "HEADER RECORD*******" + //
            "LIBV8   HEADER RECORD!!!!!!!" + //
            "000000000000000000000000000000" + // 30 0s
            "  ";

    static final String CPORT_FIRST_RECORD = //
        "**COMPRESSED** **COMPRESSED** **COMPRESSED** " + //
            "**COMPRESSED** **COMPRESSED**" + //
            "******";

    static final String MEMBER_HEADER_RECORD_STANDARD = //
        "HEADER RECORD*******" + //
            "MEMBER  HEADER RECORD!!!!!!!" + // the two spaces between MEMBER and HEADER are undocumented
            "0000000000000000" + // 16 0s
            "0160" + // 160 bytes? (not sure what)
            "000000" + //
            "0140" + // NAMESTR record size (136 on VMS)
            "  ";

    static final String MEMBER_HEADER_RECORD_VMS = //
        "HEADER RECORD*******" + //
            "MEMBER  HEADER RECORD!!!!!!!" + // the two spaces between MEMBER and HEADER are undocumented
            "0000000000000000" + // 16 0s
            "0160" + // 160 bytes? (not sure what)
            "000000" + //
            "0136" + // NAMESTR record size
            "  ";

    static final String DESCRIPTOR_HEADER_RECORD = //
        "HEADER RECORD*******" + //
            "DSCRPTR HEADER RECORD!!!!!!!" + //
            "000000000000000000000000000000" + // 30 0s
            "  "; // padding to reach RECORD_SIZE

    final byte[] data;

    Record(int size) {
        data = new byte[size];
    }

    Record() {
        this(RECORD_SIZE);
    }

    Record(byte[] data) {
        assert data.length == RECORD_SIZE;
        this.data = data;
    }

    Record(String data) {
        this(data.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * A helper method for comparing the data within a record.
     *
     * @param string
     *     The string to compare against the data.
     *
     * @return true, if the ASCII format of {@code string} exactly matches the data within this record.
     */
    private boolean recordMatchesString(String string) {
        assert string.length() == data.length;

        for (int i = 0; i < data.length; i++) {
            if (data[i] != string.codePointAt(i)) {
                // found a difference
                return false;
            }
        }

        // no differences found
        return true;
    }

    /**
     * Returns the file type that corresponds to this record if it were the first record in a file.
     *
     * @return the record's type, as known.
     */
    SasFileType getType() {
        if (recordMatchesString(V5_XPORT_FIRST_RECORD)) {
            return SasFileType.SAS_V5_XPORT;
        }
        if (recordMatchesString(V8_XPORT_FIRST_RECORD)) {
            return SasFileType.SAS_V8_XPORT;
        }
        if (recordMatchesString(CPORT_FIRST_RECORD)) {
            return SasFileType.SAS_CPORT;
        }

        return SasFileType.UNKNOWN;
    }

    /**
     * Reads {@code length} bytes from the internal buffer start at {@code offset}, converts it to a string, and trims
     * any trailing space characters.
     *
     * @param offset
     *     Where in the internal data we should start reading.
     * @param length
     *     How many bytes to extract from the buffer.
     *
     * @return The string (with trailing whitespace trimmed)
     */
    String readSpacePaddedString(int offset, int length) {
        // Trim any whitespace on the right of the string by not reading it.
        // This is easier and more efficient than reading it into the string, then trimming the string.
        while (length > 0) {
            if (data[offset + length - 1] != ASCII_BLANK) {
                break; // we have trimmed all whitespace
            }
            length--;
        }
        if (length == 0) {
            return ""; // don't allocate a new String for the common case
        }
        return new String(data, offset, length, StandardCharsets.US_ASCII);
    }

    short readShort(int offset) {
        // deserialized as big-endian ("IBM-style integer format" in ts140.pdf)
        int highByte = 0xFF & data[offset];
        int lowByte = 0xFF & data[offset + 1];
        return (short) ((highByte << 8) | lowByte);
    }

    int readInt(int offset) {
        // deserialized as big-endian ("IBM-style integer format" in ts140.pdf)
        int byte1 = 0xFF & data[offset];
        int byte2 = 0xFF & data[offset + 1];
        int byte3 = 0xFF & data[offset + 2];
        int byte4 = 0xFF & data[offset + 3];
        return (byte1 << 24) | (byte2 << 16) | (byte3 << 8) | byte4;
    }

    int toArray(short number, int offset) {
        // serialized as big-endian ("IBM-style integer format" in ts140.pdf)
        data[offset] = (byte) (number >> 8);
        data[offset + 1] = (byte) number;
        return 2;
    }

    int toArray(int number, int offset) {
        // serialized as big-endian ("IBM-style integer format" in ts140.pdf)
        data[offset] = (byte) (number >> 24);
        data[offset + 1] = (byte) (number >> 16);
        data[offset + 2] = (byte) (number >> 8);
        data[offset + 3] = (byte) number;
        return 4;
    }

    int toArray(String string, int offset) {
        return toSpacePaddedArray(string, offset, string.length());
    }

    int toPaddedArray(String string, int offset, int length, byte paddingByte) {
        assert offset + length <= data.length;

        byte[] stringBytes = string.getBytes(StandardCharsets.US_ASCII);
        assert stringBytes.length <= length;

        // Copy the array (as much as exists)
        System.arraycopy(stringBytes, 0, data, offset, stringBytes.length);

        // Pad the array with the padding byte
        Arrays.fill(data, offset + stringBytes.length, offset + length, paddingByte);

        return length;
    }

    int toSpacePaddedArray(String string, int offset, int length) {
        return toPaddedArray(string, offset, length, ASCII_BLANK);
    }

    /**
     * The list of string representations which SAS uses for the months.
     */
    private final static String[] MONTHS = new String[] { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP",
        "OCT", "NOV", "DEC" };

    int formatDate(LocalDateTime date, int offset) {
        // The creation/modified date are documented to be in "ddMMMyy:hh:mm:ss" format.
        // Presumably, MMM must be English abbreviations.
        // Rather than use SimpleDataFormat, which would assume that the VM has English-speaking locales,
        // we format this manually.
        String formattedDate = String.format(//
            "%02d%3s%02d:%02d:%02d:%02d", // ddMMMyy:hh:mm:ss
            date.getDayOfMonth(), //
            MONTHS[date.getMonthValue() - 1], //
            date.getYear() % 100, // always two digits (1982 -> "82")
            date.getHour(), // 24-hour
            date.getMinute(), //
            date.getSecond());

        assert formattedDate.length() == 16;
        return toSpacePaddedArray(formattedDate, offset, 16);
    }

    /**
     * Parses a date according to the format in TS-140: {@code ddMMMyy:hh:mm:ss}.
     *
     * @param formattedDate
     *     A date formatted in {@code ddMMMyy:hh:mm:ss}.
     * @param twoDigitYearToRealYear
     *     A function which maps a two-digit year given in the SAS Transport format into a real year (usually four
     *     digits).
     *
     * @return the parsed date.
     *
     * @throws MalformedTransportFileException
     *     if {@code formattedDate} is malformed or describes an impossible date.
     */
    static LocalDateTime parseDate(String formattedDate, YearMapper twoDigitYearToRealYear)
        throws MalformedTransportFileException {

        Pattern datePattern = Pattern.compile("^(\\d{2})(\\w{3})(\\d{2}):(\\d{2}):(\\d{2}):(\\d{2})$");
        Matcher dateMatcher = datePattern.matcher(formattedDate);
        if (!dateMatcher.find()) {
            throw new MalformedTransportFileException("malformed date: " + formattedDate);
        }
        String dayString = dateMatcher.group(1);
        String monthString = dateMatcher.group(2);
        String yearString = dateMatcher.group(3);
        String hourString = dateMatcher.group(4);
        String minuteString = dateMatcher.group(5);
        String secondString = dateMatcher.group(6);

        int day = Integer.parseInt(dayString, 10);

        int month = -1; // some illegal value
        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i].equals(monthString)) {
                month = i + 1;
            }
        }

        // map the two-digit year into the year real, according to the caller's preference.
        int year = twoDigitYearToRealYear.twoDigitYearToRealYear(Integer.parseInt(yearString));

        int hour = Integer.parseInt(hourString, 10);
        int minute = Integer.parseInt(minuteString, 10);
        int second = Integer.parseInt(secondString, 10);

        try {
            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (DateTimeException exception) {
            // The date is illegal.  Remap to the appropriate exception.
            throw new MalformedTransportFileException("malformed date: " + formattedDate);
        }
    }

    void write(OutputStream outputStream) throws IOException {
        outputStream.write(data);
    }
}