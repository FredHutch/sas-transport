///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A structured interface for writing a SAS Transport File XPORT to an output stream.
 *
 * <p>
 * The definition of this file format is maintained by the SAS Corporation at
 * <a href="https://support.sas.com/techsup/technote/ts140.pdf">https://support.sas.com/techsup/technote/ts140.pdf</a>.
 */
public final class SasTransportExporter implements AutoCloseable {

    /**
     * The smallest possible number that can be represented in an XPORT file.
     */
    public static final double MIN_VALUE = 5.397605346934028E-79;

    /**
     * The largest possible number that can be represented in an XPORT file.
     */
    public static final double MAX_VALUE = 9.0462569716653265E+74;

    /**
     * The number of milliseconds in a second.
     */
    private final static long MILLISECONDS_PER_SECOND = 1000;

    /**
     * The number of milliseconds in a day.
     */
    private final static long MILLISECONDS_PER_DAY = 24 * 60 * 60 * MILLISECONDS_PER_SECOND;

    /**
     * The number of milliseconds between midnight on Jan 1 1960 (inclusive) and midnight on Jan 1, 1970 (exclusive).
     * There are three leap years in the 1960s (1960, 1964, and 1968).
     */
    private final static long MILLISECONDS_IN_1960S = (7 * 365 + 3 * 366) * MILLISECONDS_PER_DAY;

    /**
     * The index of the observation that is to be written next (zero-indexed)
     */
    private long observationIndex;

    private OutputStream outputStream;
    private final Variable[] variables;

    /**
     * An array of ASCII blanks (pre-computed to improve performance of padding)
     */
    private final byte[] padding;

    /**
     * An array that is exactly large enough to hold an observation. This is re-used when writing observations to avoid
     * the cost of re-allocating it.
     */
    private final byte[] observationBuffer;

    /**
     * Writes ASCII blanks to the output stream.
     *
     * @param length
     *     The number of blank bytes to write.
     *
     * @throws IOException
     *     when the padding can't be written to the output stream.
     */
    private void pad(int length) throws IOException {
        // Write out in chunks the size of the padding array.
        for (int wholeChunks = length / padding.length; 0 < wholeChunks; wholeChunks--) {
            outputStream.write(padding);
        }

        // Write out the rest.
        outputStream.write(padding, 0, length % padding.length);
    }

    SasTransportExporter(OutputStream outputStream, SasLibraryDescription dataDescription) throws IOException {
        assert outputStream != null;
        assert dataDescription != null;

        padding = new byte[8];
        Arrays.fill(padding, Record.ASCII_BLANK);

        this.observationIndex = 0;
        this.outputStream = outputStream;

        // Store the non-mutable bits of the dataset description that we'll need later.
        List<Variable> inputVariables = dataDescription.datasetDescription().variables();
        this.variables = new Variable[inputVariables.size()];
        int i = 0;
        int observationBufferLength = 0;
        for (Variable variable : inputVariables) {
            variables[i] = variable;
            observationBufferLength += variable.length();
            i++;
        }
        this.observationBuffer = new byte[observationBufferLength];

        //
        // Write the SAS library header
        //
        Record fileHeader = new Record(Record.V5_XPORT_FIRST_RECORD);
        fileHeader.write(outputStream);

        RealHeader firstHeader = new RealHeader( //
            "SAS", //
            "SAS", //
            "SASLIB", //
            dataDescription.sourceSasVersion(), // SAS version
            dataDescription.sourceOperatingSystem(), // OS
            dataDescription.createTime());
        firstHeader.write(outputStream);

        // Modified time (section 3 of TS-140)
        Record secondRealHeader = new SecondHeader(dataDescription.modifiedTime(), "", "");
        secondRealHeader.write(outputStream);

        //
        // Write the SAS dataset header.
        //
        Record memberHeader1 = new Record(Record.MEMBER_HEADER_RECORD_STANDARD);
        memberHeader1.write(outputStream);

        Record memberHeader2 = new Record(Record.DESCRIPTOR_HEADER_RECORD);
        memberHeader2.write(outputStream);

        Record memberHeaderData1 = new RealHeader(//
            "SAS", //
            dataDescription.datasetDescription().name(), //
            "SASDATA", //
            dataDescription.datasetDescription().sourceSasVersion(), //
            dataDescription.datasetDescription().sourceOperatingSystem(), //
            dataDescription.datasetDescription().createTime());
        memberHeaderData1.write(outputStream);

        Record memberHeaderData2 = new SecondHeader(//
            dataDescription.datasetDescription().modifiedTime(), //
            dataDescription.datasetDescription().label(), //
            dataDescription.datasetDescription().type());
        memberHeaderData2.write(outputStream);

        // Write the header for the field descriptions.
        Record namestrHeader = new NamestrHeaderRecord(variables.length);
        namestrHeader.write(outputStream);

        // Write each field description as a NAMESTR record.
        int positionInObservation = 0;
        for (Variable variable : variables) {
            NamestrRecord namestr = new NamestrRecord(//
                variable.type(), //
                variable.length(), //
                variable.number(), //
                variable.name(), //
                variable.label(), //
                variable.outputFormat(), //
                variable.outputFormatJustification(), //
                variable.inputFormat(), //
                positionInObservation);
            namestr.write(outputStream);

            positionInObservation += variable.length();
        }

        // Add needed padding.
        int overage = (variables.length * 140) % Record.RECORD_SIZE;
        if (overage != 0) {
            pad(Record.RECORD_SIZE - overage);
        }

        // Add the header which introduces the observations.
        Record observationHeader = new ObservationHeaderRecord();
        observationHeader.write(outputStream);
    }

    /**
     * Closes this exporter and the input stream that was passed to this exporter's constructor. After this method
     * returns, you can no longer append observation.
     */
    @Override
    public void close() throws IOException {
        if (outputStream != null) {

            // Section 9 of TS-140:
            // There is ASCII blank padding at the end of the last record if necessary.
            // [to reach the 80 byte record alignment]
            int overage = (int) ((observationIndex * observationBuffer.length) % Record.RECORD_SIZE);
            if (overage != 0) {
                pad(Record.RECORD_SIZE - overage);
            }

            outputStream.close();
            outputStream = null;
        }
    }

    /**
     * Computes a positive modulo
     *
     * @param dividend
     *     The number to be divided
     * @param divisor
     *     The number to divide by
     *
     * @return {@code dividend} MOD {@code divisor}, which is always positive.
     */
    private long mod(long dividend, long divisor) {
        return (dividend % divisor + divisor) % divisor;
    }

    /**
     * Appends an observation to the XPORT file's dataset.
     *
     * @param observation
     *     The observation (list of variable values) to append to the dataset. These must be given in the same order as
     *     the variables were given in this object's constructor.
     *
     *     <p>
     *     A {@code null} value is always written as {@code MissingValue.STANDARD}.
     *     </p>
     *
     *     <p>
     *     If the corresponding variable's type is numeric, then the object must be a {@code MissingValue},
     *     {@code Number}, {@code java.sql.Timestamp}, {@code java.sql.Date}, or {@code java.sql.Time}. The values are
     *     persisted according to the table.
     *     </p>
     *
     *     <table>
     *     <caption>Persistence Rules</caption>
     *     <tr>
     *     <th>Type</th>
     *     <th>Persistence Rules</th>
     *     </tr>
     *     <tr>
     *     <td>{@link MissingValue}</td>
     *     <td>persisted using the byte sequence that corresponds to the missing value</td>
     *     </tr>
     *     <tr>
     *     <td>{@link Number}</td>
     *     <td>converted to a Double and persisted</td>
     *     </tr>
     *     <tr>
     *     <td>{@link java.sql.Timestamp}</td>
     *     <td>converted to seconds since Jan 1, 1960 GMT (the SAS Epoch) and persisted as a Double</td>
     *     </tr>
     *     <tr>
     *     <td>{@link java.sql.Date}</td>
     *     <td>converted to days since Jan 1, 1960 GMT (the SAS Epoch) and persisted as a Double</td>
     *     </tr>
     *     <tr>
     *     <td>{@link java.sql.Time}</td>
     *     <td>converted to seconds since midnight on the corresponding day and persisted as a Double</td>
     *     </tr>
     *     </table>
     *
     *     <p>
     *     If the corresponding variable's type is {@code VariableType.CHARACTER}, then the object must be a
     *     String that contains only ASCII characters and fit within the variable's length. Note that the empty string
     *     and a value consisting of only space characters are identical in the XPORT format.
     *     </p>
     *
     * @throws IOException
     *     if there was a problem writing to the output stream.
     * @throws IllegalStateException
     *     if this exporter has already been closed or if {@code observations} doesn't match the variables from the
     *     dataset description that was provided in this object's constructor.
     * @throws NullPointerException
     *     if one of the values in {@code observation} is {@code null}.
     * @throws IllegalArgumentException
     *     if the values in {@code observation} does not conform to the variables in the dataset description.
     */
    public void appendObservation(List<Object> observation) throws IOException {

        if (outputStream == null) {
            throw new IllegalStateException("Writing to a closed exporter");
        }

        if (observation.size() != variables.length) {
            throw new IllegalArgumentException(
                "observation has different number of values than the dataset has variables");
        }

        // Write the values in the observation to an "observation buffer" according to the definition
        // of the corresponding variable.  We write to an intermediate buffer instead of directly
        // to the output stream so that if we throw an exception due to bad input, the output
        // stream remains in a known state (instead of having a partial observation written).
        int observationBufferOffset = 0;
        for (int i = 0; i < variables.length; i++) {
            final Variable variable = variables[i];
            final Object value = observation.get(i);

            if (value == null) {
                // This could be mapped MissingValue.STANDARD, but rather than
                // assuming something for the caller, we throw a bad input exception.
                throw new NullPointerException("values in observation must not be null");
            }

            int paddingNeeded = variable.length();
            if (value instanceof MissingValue) {
                switch (variable.type()) {
                case CHARACTER:
                    // MissingValue is only for numeric variables.  CHARACTER variables use the empty string.
                    throw new IllegalArgumentException("CHARACTER variables use the empty string for missing values");

                case NUMERIC:
                    // write the missing value byte
                    byte missingValueByte = ((MissingValue) value).xportByteRepresentation();
                    observationBuffer[observationBufferOffset] = missingValueByte;

                    // numeric values pad with NUL.
                    Arrays.fill(//
                        observationBuffer, //
                        observationBufferOffset + 1, //
                        observationBufferOffset + variable.length(), //
                        (byte) 0);
                    observationBufferOffset += variable.length();
                    paddingNeeded = 0;
                    break;
                }
            } else {
                final byte[] bytes;
                switch (variable.type()) {
                case CHARACTER:
                    // check for bad input
                    if (!(value instanceof String)) {
                        throw new IllegalArgumentException("values for character variables must be String");
                    }

                    String stringValue = (String) value;
                    ArgumentUtil.checkIsAscii(stringValue, "values of character variables");
                    if (variable.length() < stringValue.length()) {
                        // We cannot store this without data loss.
                        throw new IllegalArgumentException(
                            "value length exceeds maximum length for variable " + variable.name());
                    }

                    // value is an ASCII String.
                    bytes = stringValue.getBytes(StandardCharsets.US_ASCII);
                    break;

                case NUMERIC:
                    double number;
                    if (value instanceof Number) {
                        number = ((Number) value).doubleValue();
                    } else if (value instanceof java.sql.Timestamp) {
                        long javaDateTime = ((Date) value).getTime();

                        // The Java Epoch is Jan 1, 1970.  The SAS Epoch is Jan 1, 1960.
                        long sasDateTime = javaDateTime + MILLISECONDS_IN_1960S;

                        // Java times millisecond granularity and SAS times are in seconds
                        // (but retain millisecond granularity by being floating point).
                        number = (double) sasDateTime / MILLISECONDS_PER_SECOND;

                    } else if (value instanceof java.sql.Time) {
                        long javaDateTime = ((Date) value).getTime();

                        // Java times have millisecond granularity and SAS times are in seconds.
                        // SAS requires that times are between 0 and 86400 (seconds in a day),
                        // so we translate the Java time to millisecond-in-day by taking the
                        // modulo of MILLISECONDS_PER_DAY.
                        long sasDateTime = mod(javaDateTime, MILLISECONDS_PER_DAY);

                        // Java times millisecond granularity and SAS times are in seconds
                        // (but retain millisecond granularity by being floating point).
                        number = (double) sasDateTime / MILLISECONDS_PER_SECOND;

                    } else if (value instanceof java.sql.Date) {
                        long javaDateTime = ((Date) value).getTime();

                        // The Java Epoch is Jan 1, 1970.  The SAS Epoch is Jan 1, 1960.
                        long sasDateTime = javaDateTime + MILLISECONDS_IN_1960S;

                        // Java dates are given in milliseconds and SAS dates are in days.
                        // (but retain millisecond granularity by being floating point).
                        number = (double) sasDateTime / MILLISECONDS_PER_DAY;

                        // TODO: truncate time portion (so it's an even date)?
                        // TODO: throw an exception if out of acceptable range (1582 - 19900)?
                    } else {
                        // We have a non-numeric value in a numeric value.
                        throw new IllegalArgumentException("non-numeric value given for a numeric variable");
                    }

                    // The Variable class guarantees that the length cannot be >8 for numeric types.
                    // As a result, we never need to pad a converted numeric value (only truncate).
                    assert variable.length() <= 8 : variable.length();
                    bytes = DoubleConverter.doubleToXport(number);
                    break;

                default:
                    throw new AssertionError("can't happen");
                }
                int lengthToWrite = Math.min(variable.length(), bytes.length);
                System.arraycopy(bytes, 0, observationBuffer, observationBufferOffset, lengthToWrite);
                observationBufferOffset += lengthToWrite;
                paddingNeeded -= lengthToWrite;
            }

            // Pad the value with spaces, if needed.
            if (paddingNeeded != 0) {
                Arrays.fill(observationBuffer, observationBufferOffset, observationBufferOffset + paddingNeeded,
                    Record.ASCII_BLANK);
                observationBufferOffset += paddingNeeded;
            }
        }

        // We have successfully filled the observation buffer without throwing any exceptions.
        // We can now write it to the stream.
        assert observationBufferOffset == observationBuffer.length : observationBufferOffset;
        outputStream.write(observationBuffer);
        observationIndex++;
    }
}