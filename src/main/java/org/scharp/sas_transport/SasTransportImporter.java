///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class for reading SAS Transport files.
 */
public final class SasTransportImporter implements AutoCloseable {

    private final SasLibraryDescription dataDescription;
    private final int[] variableObservationOffsets;
    private final Variable[] variables;
    private final byte[] observationBuffer;

    private InputStream inputStream;
    private byte[] recordData;
    private int recordDataOffset;
    private int recordDataLimit;

    private byte[] nextRecordData;
    private int nextRecordDataLimit;
    private IOException importException;

    SasTransportImporter(InputStream inputStream) throws IOException {
        assert inputStream != null;

        this.inputStream = inputStream;
        this.recordData = new byte[Record.RECORD_SIZE];

        // Get the library header (Section 1 TS-140)
        int totalBytesRead = inputStream.read(recordData);
        if (totalBytesRead != recordData.length) {
            throw new MalformedTransportFileException("missing library header record");
        }

        // The data in the first record tell us whether this is an XPORT file.
        Record firstHeader = new Record(recordData);
        switch (firstHeader.getType()) {
        case SAS_V5_XPORT:
            // This is the only file type we support.
            break;

        case SAS_V8_XPORT:
            throw new UnsupportedTransportFileException("SAS V8 XPORT files are not supported");

        case SAS_CPORT:
            throw new UnsupportedTransportFileException("SAS CPORT files are not supported");

        default:
            // Not only do we not support this format, we don't even know what it is.
            throw new MalformedTransportFileException("First record indicates this is not SAS V5 XPORT format");
        }

        // Get the "first real header" (Section 2 TS-140)
        totalBytesRead = inputStream.read(recordData);
        if (totalBytesRead != recordData.length) {
            throw new MalformedTransportFileException("missing first real header record");
        }

        RealHeader firstRealHeader = new RealHeader(recordData, SasTransportImporter::twoDigitYearToRealYear);
        String libraryOperatingSystem = firstRealHeader.operatingSystem();
        String librarySasVersion = firstRealHeader.sasVersion();
        LocalDateTime libraryCreateDate = firstRealHeader.createDate();

        // TODO: check the contents.

        // Get the "second real header" (Section 3 TS-140)
        totalBytesRead = inputStream.read(recordData);
        if (totalBytesRead != recordData.length) {
            throw new MalformedTransportFileException("missing secondary library header record");
        }

        SecondHeader secondRealHeader = new SecondHeader(recordData);
        LocalDateTime libraryModifiedDate = secondRealHeader.modifiedDate(SasTransportImporter::twoDigitYearToRealYear);

        // TODO: check the contents.

        // Get the member header records (Section 4 TS-140)
        // HEADER RECORD*******MEMBER HEADER RECORD!!!!!!!000000000000000001600000000140
        totalBytesRead = inputStream.read(recordData);
        if (totalBytesRead != recordData.length) {
            throw new MalformedTransportFileException("missing member header record");
        }
        // TODO: check the contents.

        totalBytesRead = inputStream.read(recordData);
        if (totalBytesRead != recordData.length) {
            throw new MalformedTransportFileException("missing descriptor header record");
        }
        // TODO: check the contents.

        // Read the Member header data (Section 5, TS-140)
        totalBytesRead = inputStream.read(recordData);
        if (totalBytesRead != recordData.length) {
            throw new MalformedTransportFileException("missing first member header data record");
        }

        RealHeader dataSetHeader = new RealHeader(recordData, SasTransportImporter::twoDigitYearToRealYear);
        String dataSetName = dataSetHeader.symbol2();
        String dataSetOperatingSystem = dataSetHeader.operatingSystem();
        String dataSetSasVersion = dataSetHeader.sasVersion();
        LocalDateTime dataSetCreateDate = dataSetHeader.createDate();

        // Get the "second member header data record" (Section 5 TS-140)
        totalBytesRead = inputStream.read(recordData);
        if (totalBytesRead != recordData.length) {
            throw new MalformedTransportFileException("missing second member header data record");
        }

        SecondHeader secondMemberDataRecord = new SecondHeader(recordData);
        LocalDateTime dataSetModifiedDate = secondMemberDataRecord
            .modifiedDate(SasTransportImporter::twoDigitYearToRealYear);
        String dataSetLabel = secondMemberDataRecord.label();
        String dataSetType = secondMemberDataRecord.type();

        // Read the namestr header record (Section 6, TS-140)
        totalBytesRead = inputStream.read(recordData);
        if (totalBytesRead != recordData.length) {
            throw new MalformedTransportFileException("missing namestr header record");
        }

        final int variableCount;
        try {
            NamestrHeaderRecord namestrHeaderRecord = new NamestrHeaderRecord(recordData);
            variableCount = namestrHeaderRecord.variableCount();
        } catch (IllegalArgumentException exception) {
            throw new MalformedTransportFileException("malformed NAMESTR header record", exception);
        }
        // TODO: check the contents.

        // Parse each of the NAMESTR structures (Section 7, TS-140)
        variables = new Variable[variableCount];
        variableObservationOffsets = new int[variableCount];

        byte[] namestrRecordData = new byte[140];
        for (int i = 0; i < variableCount; i++) {
            totalBytesRead = inputStream.read(namestrRecordData);
            if (totalBytesRead != namestrRecordData.length) {
                throw new MalformedTransportFileException("missing namestr record " + (i + 1));
            }

            try {
                NamestrRecord namestrRecord = new NamestrRecord(namestrRecordData);

                variables[i] = new Variable(//
                    namestrRecord.name(), //
                    namestrRecord.number(), //
                    namestrRecord.type(), //
                    namestrRecord.length(), //
                    namestrRecord.label(), //
                    namestrRecord.format(), //
                    namestrRecord.justification(), //
                    namestrRecord.inputFormat(), //
                    StrictnessMode.BASIC); // ignore non-fatal errors

                variableObservationOffsets[i] = namestrRecord.offsetInObservation();

            } catch (IllegalArgumentException exception) {
                // If the NAMESTR record violates the Variable class's invariants,
                // then the file is malformed.
                throw new MalformedTransportFileException("Variable #" + (i + 1) + " is malformed", exception);
            }
        }

        // Skip over padding
        int overage = (variableCount * namestrRecordData.length) % Record.RECORD_SIZE;
        if (overage != 0) {
            totalBytesRead = inputStream.read(namestrRecordData, 0, Record.RECORD_SIZE - overage);
            if (totalBytesRead != Record.RECORD_SIZE - overage) {
                throw new MalformedTransportFileException("missing NAMESTR record padding");
            }

            // TODO: check the contents (padding is 0 bytes)
        }

        // Get the "observation header record" (Section 8 TS-140)
        totalBytesRead = inputStream.read(recordData);
        if (totalBytesRead != recordData.length) {
            throw new MalformedTransportFileException("missing observation header record");
        }

        // TODO: check the contents

        // Assemble all header information into a structured form.
        try {
            SasDataSetDescription dataSet = new SasDataSetDescription(//
                dataSetName, // name
                dataSetLabel, // label
                dataSetType, // type
                dataSetOperatingSystem, // OS version
                dataSetSasVersion, // SAS Version
                Arrays.asList(variables), // variables
                dataSetCreateDate, // create
                dataSetModifiedDate, // modified
                StrictnessMode.BASIC); // ignore non-fatal errors

            dataDescription = new SasLibraryDescription(//
                dataSet, //
                libraryOperatingSystem, //
                librarySasVersion, //
                libraryCreateDate, //
                libraryModifiedDate);

        } catch (IllegalArgumentException exception) {
            // If the variable list is malformed in a manner that each variable is well-formed,
            // but together they are malformed (for example, it has duplicated variable names),
            // then creating the data set throws this exception.
            throw new MalformedTransportFileException("Data set is malformed", exception);
        }

        // Determine the size of the buffer we need to hold the observations.
        int observationBufferLimit = 0;
        for (int i = 0; i < variables.length; i++) {
            int thisObservationLimit = variableObservationOffsets[i] + variables[i].length();
            if (observationBufferLimit < thisObservationLimit) {
                observationBufferLimit = thisObservationLimit;
            }
        }
        observationBuffer = new byte[observationBufferLimit];

        // At this point, we have read all headers and can read observations.
        // Maintain the invariant that there is always data in recordData until we reach EOF.
        recordDataLimit = inputStream.read(recordData);
        recordDataOffset = 0;
        importException = null;
    }

    private static boolean isBlanks(byte[] data, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            if (data[i] != Record.ASCII_BLANK) {
                // found a non-blank
                return false;
            }
        }
        return true;
    }

    // TODO: make this overridable by caller
    private static int twoDigitYearToRealYear(int twoDigitYear) {
        assert 0 <= twoDigitYear;
        assert twoDigitYear <= 99;

        int baseCentury = twoDigitYear < 60 ? 2000 : 1900;
        return baseCentury + twoDigitYear;
    }

    /**
     * Gets the description of the SAS library that is being imported.
     *
     * @return A SAS Library Description.
     */
    public SasLibraryDescription sasLibraryDescription() {
        return dataDescription;
    }

    /**
     * A helper method for comparing a byte array with a string.
     *
     * @param data
     *     The byte array to compare.
     * @param string
     *     The string to compare against the data.
     *
     * @return {@code true}, if the ASCII format of {@code string} exactly matches the data within this record.
     */
    private static boolean arrayMatchesString(byte[] data, String string) {
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

    private static boolean isDataSetHeader(byte[] record) {
        return arrayMatchesString(record, Record.MEMBER_HEADER_RECORD_STANDARD)
            || arrayMatchesString(record, Record.MEMBER_HEADER_RECORD_VMS);
    }

    /**
     * <p>
     * Reads the next observation from the input stream and returns it as a list.
     * </p>
     *
     * <p>
     * The variable values in the observation are ordered in the same way that the variables are order within the
     * {@code SasLibraryDescription} that is returned from {@link #sasLibraryDescription()}.
     * </p>
     *
     * @return The list of observations, or {@code null} if we've read to the end of the XPORT file.
     *
     * @throws IOException
     *     if there was an error reading the input stream
     * @throws MultipleDataSetsNotSupportedException
     *     if, instead an observation, a new data set was found within the XPORT file. This indicates the end of the
     *     current data set and all observations imported so far is valid and complete. However, the API does not
     *     provide a way to access multiple data sets, so an exception is thrown.
     * @throws IllegalStateException
     *     if the input stream was already closed.
     */
    public List<Object> nextObservation() throws IOException {
        try {
            if (inputStream == null) {
                throw new IllegalStateException("reading from closed stream");
            }
            if (importException != null) {
                // we are already in an error state, so we don't want to return
                // anything that would contradict this.
                throw importException;
            }
            if (recordDataLimit == -1) {
                // We had previously reached EOF, so we stay at EOF.
                return null;
            }

            // Although not documented as such, the XPORT format supports multiple data sets within
            // the same transport file.  In this case, the header record for the next data set appears
            // where an observation is expected.  This is ambiguous, as the header record could be data,
            // but we resolve the ambiguous by assuming it's a second data set (which is more likely).
            if (recordDataOffset == 0 && isDataSetHeader(recordData)) {
                // The XPORT file has multiple data sets, but the API for this class only provides
                // access to one of them.  Throw an exception.
                throw new MultipleDataSetsNotSupportedException();
            }

            // TS-140 states that the final record may be padded with blanks, which looks
            // identical to missing data.  We detect the final padding here.
            if (recordDataOffset != 0 && // some data has already been read from this record (an entire record can't be padding)
                nextRecordData == null && // we haven't already proven that data exists after the current record
                // the rest of the current record is blanks
                isBlanks(recordData, recordDataOffset, Record.RECORD_SIZE - recordDataOffset)) {

                // We need to distinguish between the final padding and a long run of
                // MISSING VALUEs which only looks like padding.  The right way to do this is to probe
                // to see if the next read would return an EOF or compare the total bytes read
                // against the total file size, but because InputStream doesn't support either of
                // these operations, we probe for EOF by reading a full record and, if successful,
                // use it the next time we need to read a record.
                nextRecordData = new byte[Record.RECORD_SIZE];
                nextRecordDataLimit = inputStream.read(nextRecordData);
                if (nextRecordDataLimit == -1) {
                    // This is padding.
                    // Actually, it could also be missing data, but SAS treats it as padding.
                    recordDataLimit = -1; // set a flag so future calls know we reached EOF
                    return null;
                }

                // Even if the next record exists in the file, it's possible that the blanks were
                // the end-of-dataset padding if the next record is the header for a new data set.
                if (nextRecordDataLimit == Record.RECORD_SIZE && isDataSetHeader(nextRecordData)) {
                    // XPORT file has multiple data sets.
                    // Because the API for this class only provides access to one of them, we throw an exception.
                    throw new MultipleDataSetsNotSupportedException();
                }
            }

            //
            // Read the entire observation into a single buffer, record by record.
            //
            int totalBytesRead = 0;
            while (totalBytesRead < observationBuffer.length) {

                // We need more data for this observation.
                // If there's unprocessed data remaining in dataRecord, we'll use that.
                if (recordDataOffset == Record.RECORD_SIZE) {
                    // There is no unprocessed data in dataRecord.
                    // Read the next record from the input stream.
                    if (nextRecordData != null) {
                        // We had previously read the next record after encountering
                        // to determine if a run blanks was padding or missing values.
                        recordData = nextRecordData;
                        recordDataOffset = 0;
                        recordDataLimit = nextRecordDataLimit;
                        nextRecordData = null;
                    } else {
                        // If we need more than one record's worth of bytes, it's faster to read it
                        // directly into observationBuffer and only place the left-over in recordData.
                        int bytesNeeded = observationBuffer.length - totalBytesRead;
                        int bytesRead = inputStream.read(observationBuffer, totalBytesRead, bytesNeeded);
                        if (bytesRead != bytesNeeded) {
                            // We reached an EOF before the end of the observation.
                            throw new MalformedTransportFileException("observation truncated");
                        }
                        totalBytesRead += bytesRead;

                        // Now read the left-over portion into recordData.
                        recordDataOffset = bytesRead % Record.RECORD_SIZE;
                        int bytesNeededInDataRecord = Record.RECORD_SIZE - recordDataOffset;
                        recordDataLimit = inputStream.read(recordData, recordDataOffset, bytesNeededInDataRecord);
                        recordDataLimit += recordDataOffset;
                    }
                }
                if (recordDataLimit < Record.RECORD_SIZE) {
                    // We reached EOF when trying to read a record's worth of data.
                    // This could be because read() returned -1, or because the file was truncated
                    // in the middle of an 80-byte record.
                    //
                    // If recordDataLimit != -1, then file was truncated in the middle of an
                    // 80-byte record.  This should never happen for well-formed XPORT files
                    // (if needed, some padding should have been added to fill out the final record).
                    //
                    // If recordDataLimit == -1, then an EOF was reached at the end of an 80-byte
                    // record.  However, since we can only get to this point if some data has been
                    // read into the observation buffer, it means that the XPORT file was truncated
                    // in the middle of an observation (but on a record boundary).
                    throw new MalformedTransportFileException("observation truncated");
                }

                // Read whatever data remains in the record into the observation.
                final int bytesRemainingInRecord = Record.RECORD_SIZE - recordDataOffset;
                final int bytesRemainingInObservation = observationBuffer.length - totalBytesRead;
                int bytesToReadFromRecord = Math.min(bytesRemainingInRecord, bytesRemainingInObservation);
                System.arraycopy(recordData, recordDataOffset, observationBuffer, totalBytesRead,
                    bytesToReadFromRecord);
                totalBytesRead += bytesToReadFromRecord;
                recordDataOffset += bytesToReadFromRecord;
            }

            // Now that we have read the observation into a contiguous buffer, we can extract
            // the values in the observation according to their variable definition.
            List<Object> observations = new ArrayList<>(variables.length);
            for (int i = 0; i < variables.length; i++) {
                // useful aliases to improve readability
                final int valueOffsetInObservation = variableObservationOffsets[i];
                final int valueLength = variables[i].length();

                final Object value;
                switch (variables[i].type()) {
                case CHARACTER:
                    // For CHARACTER data, missing values are represented by blanks.
                    if (isBlanks(observationBuffer, valueOffsetInObservation, valueLength)) {
                        value = MissingValue.STANDARD;
                    } else {
                        // This was not a missing value, so treat it as a string.
                        value = new String(//
                            observationBuffer, //
                            valueOffsetInObservation, //
                            valueLength, //
                            StandardCharsets.US_ASCII);
                    }
                    break;

                case NUMERIC:
                    // Numeric values can have a length from 2-8 bytes in a data set, even though
                    // they're always 8 bytes in memory.
                    assert 2 <= variables[i].length() && variables[i].length() <= 8 : variables[i].length();

                    // Copy the amount of data that is given and pad the rest with zeros.
                    byte[] valueArray = new byte[8]; // zero-padded
                    System.arraycopy(observationBuffer, valueOffsetInObservation, valueArray, 0, valueLength);

                    // Translate the value's binary representation into a structured representation.
                    try {
                        value = DoubleConverter.xportToDouble(valueArray);
                    } catch (IllegalArgumentException exception) {
                        throw new MalformedTransportFileException("Malformed numeric value: " + exception.getMessage());
                    }
                    break;

                default:
                    throw new AssertionError("can't happen");
                }

                observations.add(value);
            }

            return observations;
        } catch (IOException exception) {
            // Save any exceptions that were encountered so that we can re-throw it
            // if we are called again.
            importException = exception;
            throw exception;
        }
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
    }
}