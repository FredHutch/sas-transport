///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for the {@link SasTransportExporter} class.
 */
public class SasTransportExporterTest {

    /**
     * A helper class for running test which export a known XPORT file and compares it against a baseline version.
     * <p>
     * With Java 8, this could use lambda instead of anonymous classes
     * </p>
     */
    private abstract static class ExportTestRunner {
        private final SasLibraryDescription library;
        private final String expectedOutputResourceName;

        ExportTestRunner(SasLibraryDescription library, String expectedOutputResourceName) {
            this.library = library;
            this.expectedOutputResourceName = expectedOutputResourceName;
        }

        /**
         * Overridden by caller to fill a data set with observations.
         *
         * @param exporter
         *     The SAS library exporter
         *
         * @throws IOException
         */
        abstract void addObservations(SasTransportExporter exporter) throws IOException;

        void run() throws IOException {

            // Export to an in-memory byte buffer.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try (SasTransportExporter exporter = library.exportTransportDataSet(outputStream)) {
                // Add observations in a test-specific manner
                addObservations(exporter);
            }

            // Uncomment to write out a file for manual validation.
            // Files.write(Paths.get(expectedOutputResourceName), outputStream.toByteArray());

            // Check the exported file against a known (manually validated) version
            try (InputStream actualDataSet = new ByteArrayInputStream(outputStream.toByteArray());
                InputStream expectedDataSet = TestUtil.getTestResource(expectedOutputResourceName)) {
                assertStreamEquals(actualDataSet, expectedDataSet);
            }
        }

        /**
         * A helper method for writing a bad input test. It appends an observation and fails the test if the exporter
         * doesn't throw the expected IllegalArgumentException.
         *
         * @param exporter
         *     The exporter to which the observation should be written.
         * @param expectedMessage
         *     The expected message of the exception that should be thrown.
         * @param observation
         *     The observation to write. This must contain an illegal argument.
         */
        static void addObservationWithIllegalArgument(SasTransportExporter exporter, String expectedMessage,
            Object... observation) {
            Exception exception = assertThrows(//
                IllegalArgumentException.class, //
                () -> exporter.appendObservation(Arrays.asList(observation)), //
                "appending an observation with an illegal argument");
            assertEquals(expectedMessage, exception.getMessage());
        }

        /**
         * Asserts that two InputStream objects are equals byte-for-byte.
         *
         * @param actualStream
         *     One of the input streams to compare.
         * @param expectedStream
         *     The expected other file to compare
         */
        private static void assertStreamEquals(InputStream actualStream, InputStream expectedStream)
            throws IOException {
            final int BLOCK_SIZE = 1024;
            byte[] actualBuffer = new byte[BLOCK_SIZE];
            byte[] expectedBuffer = new byte[BLOCK_SIZE];

            int index = 0;
            int actualBytesRead;
            int expectedBytesRead;
            do {
                actualBytesRead = actualStream.read(actualBuffer);
                expectedBytesRead = expectedStream.read(expectedBuffer);

                if (actualBytesRead != expectedBytesRead) {
                    // we found a difference.
                    if (actualBytesRead < expectedBytesRead) {
                        fail("input stream is too small");
                    } else {
                        fail("input stream has too much data");
                    }
                }
                if (actualBytesRead < 0) {
                    // we reached the end of both streams.
                    return;
                }

                for (int i = 0; i < actualBytesRead; i++) {
                    assertEquals(expectedBuffer[i], actualBuffer[i],
                        "Streams are not identical at offset " + (index + i));
                }

                index += actualBytesRead;
            } while (actualBytesRead > 0 && expectedBytesRead > 0);
        }
    }

    /**
     * Exports a small XPORT that contains interesting variations.
     *
     * @throws IOException
     */
    @Test
    public void smokeTest() throws IOException {

        List<Variable> variables = Arrays.asList(//
            new Variable(//
                "VAR01", // name
                1, // variable number
                VariableType.NUMERIC, //
                8, // length
                "The label for Var 1", //
                new Format("", 10, 2), //
                Justification.LEFT, //
                Format.UNSPECIFIED), //
            new Variable(//
                "second", //
                2, // variable number
                VariableType.NUMERIC, //
                8, // length
                "Label for second var", //
                new Format("DOLLAR", 10), //
                Justification.LEFT, //
                new Format("", 0)), //
            new Variable(//
                "TEXT", //
                3, //
                VariableType.CHARACTER, //
                15, //
                "Var 3", //
                new Format("$CHAR", 16), //
                Justification.LEFT, //
                new Format("", 0)));

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "mydata", // name
            "label for test dataset", // label
            "DATA", // type
            "Java", // OS version
            "6.7", // SAS Version
            variables, // variables
            LocalDateTime.of(1999, 12, 23, 23, 59, 59), // create
            LocalDateTime.of(2015, 2, 2, 3, 4, 5)); // modified

        // Insert the data set into a library with distinct OS/version/create/modified values.
        SasLibraryDescription library = new SasLibraryDescription(//
            dataSet, //
            "SunOS", //
            "5.4", //
            LocalDateTime.of(2015, 1, 1, 0, 0), // create
            LocalDateTime.of(2015, 1, 1, 0, 0)); // modified

        new ExportTestRunner(library, "SasTransportExporterTest_smokeTest.xpt") {
            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {
                exporter.appendObservation(Arrays.asList(15.2, 5, "first row"));
                exporter.appendObservation(Arrays.asList(0, 10000, "second row"));
                exporter.appendObservation(Arrays.asList(-400, 10000, MissingValue.STANDARD));
                exporter.appendObservation(Arrays.asList(MissingValue.B, 10000, "final row"));
            }
        }.run();
    }

    /**
     * Tests exporting all interesting variations of numeric data.
     *
     * @throws IOException
     */
    @Test
    public void testNumericVariations() throws IOException {

        List<Variable> variables = Collections.singletonList(//
            new Variable(//
                "NUMBER", // name
                1, // variable number
                VariableType.NUMERIC, //
                8, // length
                "A number", //
                new Format("E", 32, 0), //
                Justification.LEFT, //
                Format.UNSPECIFIED));

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "NUMBERS", // name
            "Variations of numeric data", // label
            "", // type
            " LIN X64", // OS version
            "9.1", // SAS Version
            variables, // variables
            LocalDateTime.of(1999, 12, 23, 23, 59, 59), // create
            LocalDateTime.of(2015, 2, 2, 3, 4, 5)); // modified

        // The original intent of this test case was to generate numeric_variations.xpt (which was
        // generated by SAS), but this is not possible due to bugs in SAS.  Instead, this uses a
        // baseline value that was generated earlier and validated manually.
        new ExportTestRunner(dataSet.newLibraryDescription(), "SasTransportExporterTest_testNumericVariations.xpt") {

            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {
                // Test all variations of missing data
                exporter.appendObservation(Collections.singletonList(MissingValue.STANDARD));
                exporter.appendObservation(Collections.singletonList(MissingValue.UNDERSCORE));
                exporter.appendObservation(Collections.singletonList(MissingValue.A));
                exporter.appendObservation(Collections.singletonList(MissingValue.B));
                exporter.appendObservation(Collections.singletonList(MissingValue.C));
                exporter.appendObservation(Collections.singletonList(MissingValue.D));
                exporter.appendObservation(Collections.singletonList(MissingValue.E));
                exporter.appendObservation(Collections.singletonList(MissingValue.F));
                exporter.appendObservation(Collections.singletonList(MissingValue.G));
                exporter.appendObservation(Collections.singletonList(MissingValue.H));
                exporter.appendObservation(Collections.singletonList(MissingValue.I));
                exporter.appendObservation(Collections.singletonList(MissingValue.J));
                exporter.appendObservation(Collections.singletonList(MissingValue.K));
                exporter.appendObservation(Collections.singletonList(MissingValue.L));
                exporter.appendObservation(Collections.singletonList(MissingValue.M));
                exporter.appendObservation(Collections.singletonList(MissingValue.N));
                exporter.appendObservation(Collections.singletonList(MissingValue.O));
                exporter.appendObservation(Collections.singletonList(MissingValue.P));
                exporter.appendObservation(Collections.singletonList(MissingValue.Q));
                exporter.appendObservation(Collections.singletonList(MissingValue.R));
                exporter.appendObservation(Collections.singletonList(MissingValue.S));
                exporter.appendObservation(Collections.singletonList(MissingValue.T));
                exporter.appendObservation(Collections.singletonList(MissingValue.U));
                exporter.appendObservation(Collections.singletonList(MissingValue.V));
                exporter.appendObservation(Collections.singletonList(MissingValue.W));
                exporter.appendObservation(Collections.singletonList(MissingValue.X));
                exporter.appendObservation(Collections.singletonList(MissingValue.Y));
                exporter.appendObservation(Collections.singletonList(MissingValue.Z));

                exporter.appendObservation(Collections.singletonList(Double.valueOf(-10)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(0)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(10)));

                // Test the boundary condition of the smallest possible positive number.
                exporter.appendObservation(Collections.singletonList(Double.valueOf(SasTransportExporter.MIN_VALUE)));

                // Same as above but negative.
                exporter.appendObservation(Collections.singletonList(Double.valueOf(-SasTransportExporter.MIN_VALUE)));

                // Test the boundary condition of the largest possible positive number.
                exporter.appendObservation(Collections.singletonList(Double.valueOf(SasTransportExporter.MAX_VALUE)));

                // Same as above but negative.
                exporter.appendObservation(Collections.singletonList(Double.valueOf(-SasTransportExporter.MAX_VALUE)));

                // Rising exponents
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-78)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-77)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-76)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-75)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-74)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-73)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-72)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-71)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-70)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-69)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-68)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-67)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-66)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-65)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-64)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-63)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-62)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-61)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-60)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-59)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-58)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-57)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-56)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-55)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-54)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-53)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-52)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-51)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-50)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-49)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-48)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-47)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-46)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-45)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-44)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-43)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-42)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-41)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-40)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-39)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-38)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-37)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-36)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-35)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-34)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-33)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-32)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-31)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-30)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-29)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-28)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-27)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-26)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-25)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-24)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-23)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-22)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-21)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-20)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-19)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-18)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-17)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-16)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-15)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-14)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-13)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-12)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-11)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-10)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-09)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-08)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-07)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-06)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-05)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-04)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-03)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-02)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-01)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E-00)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+01)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+02)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+03)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+04)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+05)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+06)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+07)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+08)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+09)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+10)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+11)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+12)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+13)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+14)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+15)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+16)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+17)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+18)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+19)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+20)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+21)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+22)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+23)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+24)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+25)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+26)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+27)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+28)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+29)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+30)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+31)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+32)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+33)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+34)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+35)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+36)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+37)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+38)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+39)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+40)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+41)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+42)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+43)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+44)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+45)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+46)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+47)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+48)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+49)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+50)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+51)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+52)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+53)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+54)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+55)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+56)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+57)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+58)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+59)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+60)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+61)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+62)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+63)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+64)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+65)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+66)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+67)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+68)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+69)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+70)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+71)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+72)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+73)));
                exporter.appendObservation(Collections.singletonList(Double.valueOf(1.23E+74)));

                // Write each of the type that is accepted as numeric
                exporter.appendObservation(Collections.singletonList(Double.valueOf(12.3)));
                exporter.appendObservation(Collections.singletonList(Float.valueOf(1.23F)));
                exporter.appendObservation(Collections.singletonList(Byte.valueOf(Byte.MAX_VALUE)));
                exporter.appendObservation(Collections.singletonList(Short.valueOf(Short.MAX_VALUE)));
                exporter.appendObservation(Collections.singletonList(Integer.valueOf(Integer.MAX_VALUE)));
                exporter.appendObservation(Collections.singletonList(Long.valueOf(0x100000000L)));
            }
        }.run();
    }

    private static List<Object> repeat(int repeatCount, Object value) {
        Object[] array = new Object[repeatCount];
        Arrays.fill(array, value);
        return Arrays.asList(array);
    }

    /**
     * Tests exporting all interesting variations of the truncation of numeric data.
     *
     * @throws IOException
     */
    @Test
    public void testNumericTruncations() throws IOException {

        // Add variations on numeric truncations from length=2 to length=8.
        List<Variable> variables = new ArrayList<>();
        for (int i = 2; i <= 8; i++) { //
            variables.add(new Variable(//
                "N" + i, // name
                i, // variable number
                VariableType.NUMERIC, //
                i, // length
                "Truncated to " + i + " bytes", //
                new Format("E", 22, 0), //
                Justification.LEFT, //
                Format.UNSPECIFIED));
        }

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "NUMBERS", // name
            "Variations of truncation of numeric data", // label
            "", // type
            "Linux\0\0", // OS version
            "9.1", // SAS Version
            variables, // variables
            LocalDateTime.of(1999, 12, 23, 23, 59, 59), // create
            LocalDateTime.of(2015, 2, 2, 3, 4, 5)); // modified

        new ExportTestRunner(dataSet.newLibraryDescription(), "SasTransportExporterTest_testNumericTruncations.xpt") {
            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {
                // Test all variations of missing data
                exporter.appendObservation(repeat(variables.size(), MissingValue.STANDARD));
                exporter.appendObservation(repeat(variables.size(), MissingValue.UNDERSCORE));
                exporter.appendObservation(repeat(variables.size(), MissingValue.A));
                exporter.appendObservation(repeat(variables.size(), MissingValue.B));
                exporter.appendObservation(repeat(variables.size(), MissingValue.C));
                exporter.appendObservation(repeat(variables.size(), MissingValue.D));
                exporter.appendObservation(repeat(variables.size(), MissingValue.E));
                exporter.appendObservation(repeat(variables.size(), MissingValue.F));
                exporter.appendObservation(repeat(variables.size(), MissingValue.G));
                exporter.appendObservation(repeat(variables.size(), MissingValue.H));
                exporter.appendObservation(repeat(variables.size(), MissingValue.I));
                exporter.appendObservation(repeat(variables.size(), MissingValue.J));
                exporter.appendObservation(repeat(variables.size(), MissingValue.K));
                exporter.appendObservation(repeat(variables.size(), MissingValue.L));
                exporter.appendObservation(repeat(variables.size(), MissingValue.M));
                exporter.appendObservation(repeat(variables.size(), MissingValue.N));
                exporter.appendObservation(repeat(variables.size(), MissingValue.O));
                exporter.appendObservation(repeat(variables.size(), MissingValue.P));
                exporter.appendObservation(repeat(variables.size(), MissingValue.Q));
                exporter.appendObservation(repeat(variables.size(), MissingValue.R));
                exporter.appendObservation(repeat(variables.size(), MissingValue.S));
                exporter.appendObservation(repeat(variables.size(), MissingValue.T));
                exporter.appendObservation(repeat(variables.size(), MissingValue.U));
                exporter.appendObservation(repeat(variables.size(), MissingValue.V));
                exporter.appendObservation(repeat(variables.size(), MissingValue.W));
                exporter.appendObservation(repeat(variables.size(), MissingValue.X));
                exporter.appendObservation(repeat(variables.size(), MissingValue.Y));
                exporter.appendObservation(repeat(variables.size(), MissingValue.Z));

                exporter.appendObservation(repeat(variables.size(), Double.valueOf(-10)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(0)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(10)));

                // Tests the smallest possible positive number.
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(SasTransportExporter.MIN_VALUE)));

                // Same as above but negative.
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(-SasTransportExporter.MIN_VALUE)));

                // Test the largest possible positive number.
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(SasTransportExporter.MAX_VALUE)));

                // Same as above but negative.
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(-SasTransportExporter.MAX_VALUE)));

                // Rising exponents
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-78)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-77)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-76)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-75)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-74)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-73)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-72)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-71)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-70)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-69)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-68)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-67)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-66)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-65)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-64)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-63)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-62)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-61)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-60)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-59)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-58)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-57)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-56)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-55)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-54)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-53)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-52)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-51)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-50)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-49)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-48)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-47)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-46)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-45)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-44)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-43)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-42)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-41)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-40)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-39)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-38)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-37)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-36)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-35)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-34)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-33)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-32)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-31)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-30)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-29)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-28)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-27)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-26)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-25)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-24)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-23)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-22)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-21)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-20)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-19)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-18)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-17)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-16)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-15)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-14)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-13)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-12)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-11)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-10)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-09)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-08)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-07)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-06)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-05)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-04)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-03)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-02)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-01)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E-00)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+01)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+02)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+03)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+04)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+05)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+06)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+07)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+08)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+09)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+10)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+11)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+12)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+13)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+14)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+15)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+16)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+17)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+18)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+19)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+20)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+21)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+22)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+23)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+24)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+25)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+26)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+27)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+28)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+29)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+30)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+31)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+32)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+33)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+34)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+35)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+36)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+37)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+38)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+39)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+40)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+41)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+42)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+43)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+44)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+45)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+46)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+47)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+48)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+49)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+50)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+51)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+52)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+53)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+54)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+55)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+56)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+57)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+58)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+59)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+60)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+61)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+62)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+63)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+64)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+65)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+66)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+67)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+68)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+69)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+70)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+71)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+72)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+73)));
                exporter.appendObservation(repeat(variables.size(), Double.valueOf(1.234567890123456E+74)));
            }
        }.run();
    }

    /**
     * Tests when performing operations on an exporter that is already closed.
     */
    @Test
    public void testClosedExporter() throws IOException {

        LocalDateTime fixedTimestamp = LocalDateTime.of(2015, 1, 1, 0, 0);

        // Add variations on numeric truncations from length=2 to length=8.
        List<Variable> variables = Collections.singletonList(//
            new Variable(//
                "text", // name
                1, // variable number
                VariableType.CHARACTER, //
                16, // length
                "Text", //
                new Format("$", 16, 0), //
                Justification.LEFT, //
                Format.UNSPECIFIED));

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "DATA", // name
            "Test data set", // label
            "", // type
            "Linux\0\0", // OS version
            "9.1", // SAS Version
            variables, // variables
            fixedTimestamp, // create
            fixedTimestamp); // modified

        // Export to an in-memory byte buffer.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SasTransportExporter exporter = dataSet.newLibraryDescription().exportTransportDataSet(outputStream);

        // Close the exporter.
        exporter.close();

        // Writing an observation should not work.
        Exception exception = assertThrows(//
            IllegalStateException.class, //
            () -> exporter.appendObservation(Collections.singletonList("data")), //
            "appendObservation() on a closed exporter");
        assertEquals("Writing to a closed exporter", exception.getMessage());

        // Closing redundantly should not throw an exception.
        exporter.close();
    }

    /**
     * Exports a XPORT that contains the most number of variables which an XPORT can have (9999). Each of these
     * variables has the maximum length that a well-formed XPORT can have (200).
     *
     * @throws IOException
     */
    @Test
    public void testMaxVariables() throws IOException {

        LocalDateTime time = LocalDateTime.of(2015, 1, 1, 12, 0);

        List<Variable> variables = new ArrayList<>(10_000);
        for (int i = 1; i <= 9999; i++) {
            Variable variable = new Variable(//
                String.format("VAR%d", i), // name
                1, // variable number
                VariableType.CHARACTER, //
                200, // length
                "Variable #" + i, //
                new Format("$", 0), //
                Justification.LEFT, //
                Format.UNSPECIFIED);

            variables.add(variable);
        }

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "DATA", // name
            "label for test dataset", // label
            "", // type
            "Java", // OS version
            "6.7", // SAS Version
            variables, // variables
            time, // create
            time); // modified

        new ExportTestRunner(dataSet.newLibraryDescription(), "SasTransportExporterTest_testMaxVariables.xpt") {

            final String validCharacters = //
                "abcdefghijklmnopqrstuvqxyz" + //
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + //
                    "0123456789" + //
                    "~!@#$%^&*()_+<>?,.";

            final int TOTAL_OBSERVATIONS = 2;

            int nextCharIndex = 0;

            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {
                for (int observationIndex = 0; observationIndex <= TOTAL_OBSERVATIONS; observationIndex++) {
                    List<Object> observation = new ArrayList<>(variables.size());
                    for (Variable variable : variables) {
                        // Build a predictable value in a way that will use different values each time.
                        StringBuilder value = new StringBuilder(variable.length());
                        for (int j = 0; j < variable.length(); j++) {
                            value.append(validCharacters.charAt(nextCharIndex % validCharacters.length()));
                            nextCharIndex++;
                        }
                        observation.add(value.toString());
                    }
                    exporter.appendObservation(observation);
                }
            }
        }.run();
    }

    /**
     * Exports a XPORT that contains the minimum number of variables which an XPORT can have (0).
     *
     * @throws IOException
     */
    @Test
    public void testMinVariables() throws IOException {

        LocalDateTime time = LocalDateTime.of(2015, 1, 1, 12, 0);

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "DATA", // name
            "label for test dataset", // label
            "", // type
            "Java", // OS version
            "6.7", // SAS Version
            Collections.emptyList(), // variables
            time, // create
            time); // modified

        new ExportTestRunner(dataSet.newLibraryDescription(), "SasTransportExporterTest_testMinVariables.xpt") {
            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {
                // Since there are no variables, there's no meaningful way to export an observation.
                exporter.appendObservation(Collections.emptyList());
                exporter.appendObservation(Collections.emptyList());
            }
        }.run();
    }

    /**
     * Exports a XPORT that contains legal variations on the variable types.
     *
     * @throws IOException
     */
    @Test
    public void testVariableVariations() throws IOException {

        LocalDateTime time = LocalDateTime.of(2015, 1, 1, 12, 0);

        List<Variable> variables = Arrays.asList(//
            new Variable(//
                "lowrcase", // name
                1, // variable number
                VariableType.CHARACTER, //
                10, // max length
                "A variable with lower-case", //
                new Format("$UPCASE", 15), //
                Justification.LEFT, //
                Format.UNSPECIFIED),

            new Variable(//
                "MAXIMUMX", // name
                Short.MAX_VALUE, // variable number
                VariableType.CHARACTER, //
                200, // max length
                "", //
                new Format("ABCDEFGH", Short.MAX_VALUE, Short.MAX_VALUE), //
                Justification.RIGHT, //
                new Format("ABCDEFGH", Short.MAX_VALUE, Short.MAX_VALUE)),

            new Variable(//
                "A", // name
                2, // variable number
                VariableType.CHARACTER, //
                1, // max length
                "", //
                Format.UNSPECIFIED, //
                Justification.UNKNOWN, //
                Format.UNSPECIFIED));

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "VARYVARS", // name
            "Variations on variable", // label
            "", // type
            "Java", // OS version
            "6.7", // SAS Version
            variables, // variables
            time, // create
            time); // modified

        new ExportTestRunner(dataSet.newLibraryDescription(), "SasTransportExporterTest_testVariableVariations.xpt") {
            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {
                exporter.appendObservation(Arrays.asList("lower", "max", "A"));
            }
        }.run();
    }

    /**
     * Tests exporting all interesting variations of the date/times.
     *
     * @throws IOException
     */
    @Test
    public void testDateTimeVariations() throws IOException {

        List<Variable> variables = Arrays.asList(//
            new Variable(//
                "DATETIME", // name
                1, // variable number
                VariableType.NUMERIC, //
                8, // length
                "Date", //
                new Format("DATETIME", 24, 4), //
                Justification.LEFT, //
                Format.UNSPECIFIED), //
            new Variable(//
                "DATE", // name
                2, // variable number
                VariableType.NUMERIC, //
                8, // length
                "Date", //
                new Format("DATE", 11), //
                Justification.LEFT, //
                Format.UNSPECIFIED), //
            new Variable(//
                "TIME", // name
                3, // variable number
                VariableType.NUMERIC, //
                8, // length
                "Date", //
                new Format("TIME", 13, 4), //
                Justification.LEFT, //
                Format.UNSPECIFIED));

        LocalDateTime timestamp = LocalDateTime.of(1999, 12, 23, 23, 59, 59);

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "DATETIME", // name
            "Variations of date/time data", // label
            "", // type
            "Linux\0\0", // OS version
            "9.1", // SAS Version
            variables, // variables
            timestamp, // create
            timestamp); // modified

        new ExportTestRunner(dataSet.newLibraryDescription(), "SasTransportExporterTest_testDateTimeVariations.xpt") {

            private java.sql.Date sqlDateOf(Instant instant) {
                return new java.sql.Date(instant.toEpochMilli());
            }

            private java.sql.Time sqlTimeOf(Instant instant) {
                return new java.sql.Time(instant.toEpochMilli());
            }

            private java.sql.Timestamp sqlTimestampOf(Instant instant) {
                return java.sql.Timestamp.from(instant);
            }

            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {
                // Test all variations of missing data
                exporter.appendObservation(repeat(variables.size(), MissingValue.STANDARD));
                exporter.appendObservation(repeat(variables.size(), MissingValue.UNDERSCORE));
                exporter.appendObservation(repeat(variables.size(), MissingValue.A));
                exporter.appendObservation(repeat(variables.size(), MissingValue.B));
                exporter.appendObservation(repeat(variables.size(), MissingValue.C));
                exporter.appendObservation(repeat(variables.size(), MissingValue.D));
                exporter.appendObservation(repeat(variables.size(), MissingValue.E));
                exporter.appendObservation(repeat(variables.size(), MissingValue.F));
                exporter.appendObservation(repeat(variables.size(), MissingValue.G));
                exporter.appendObservation(repeat(variables.size(), MissingValue.H));
                exporter.appendObservation(repeat(variables.size(), MissingValue.I));
                exporter.appendObservation(repeat(variables.size(), MissingValue.J));
                exporter.appendObservation(repeat(variables.size(), MissingValue.K));
                exporter.appendObservation(repeat(variables.size(), MissingValue.L));
                exporter.appendObservation(repeat(variables.size(), MissingValue.M));
                exporter.appendObservation(repeat(variables.size(), MissingValue.N));
                exporter.appendObservation(repeat(variables.size(), MissingValue.O));
                exporter.appendObservation(repeat(variables.size(), MissingValue.P));
                exporter.appendObservation(repeat(variables.size(), MissingValue.Q));
                exporter.appendObservation(repeat(variables.size(), MissingValue.R));
                exporter.appendObservation(repeat(variables.size(), MissingValue.S));
                exporter.appendObservation(repeat(variables.size(), MissingValue.T));
                exporter.appendObservation(repeat(variables.size(), MissingValue.U));
                exporter.appendObservation(repeat(variables.size(), MissingValue.V));
                exporter.appendObservation(repeat(variables.size(), MissingValue.W));
                exporter.appendObservation(repeat(variables.size(), MissingValue.X));
                exporter.appendObservation(repeat(variables.size(), MissingValue.Y));
                exporter.appendObservation(repeat(variables.size(), MissingValue.Z));

                Instant sasEpoch = LocalDateTime.of(1960, 1, 1, 0, 0).toInstant(ZoneOffset.UTC);
                Instant sasEpochMinusOneDay = sasEpoch.minus(1, ChronoUnit.DAYS);
                Instant sasEpochMinusOneSecond = sasEpoch.minusSeconds(1);
                Instant sasEpochMinusOneMillisecond = sasEpoch.minusMillis(1);
                Instant sasEpochPlusOneMillisecond = sasEpoch.plusMillis(1);
                Instant sasEpochPlusOneSecond = sasEpoch.plusSeconds(1);
                Instant sasEpochPlusOneDay = sasEpoch.plus(1, ChronoUnit.DAYS);

                // write the SAS Epoch - 1 second
                exporter.appendObservation(Arrays.asList(//
                    sqlTimestampOf(sasEpochMinusOneSecond), // minus one second
                    sqlDateOf(sasEpochMinusOneDay), // minus one day
                    sqlTimeOf(sasEpochMinusOneSecond))); // minus one second

                // write the SAS Epoch
                exporter.appendObservation(Arrays.asList(//
                    sqlTimestampOf(sasEpoch), //
                    sqlDateOf(sasEpoch), //
                    sqlTimeOf(sasEpoch)));

                // write the SAS Epoch + 1 second/day
                exporter.appendObservation(Arrays.asList(//
                    sqlTimestampOf(sasEpochPlusOneSecond), // plus one second
                    sqlDateOf(sasEpochPlusOneDay), // plus one day
                    sqlTimeOf(sasEpochPlusOneSecond))); // plus one second

                // write the SAS Epoch - 1 millisecond
                exporter.appendObservation(Arrays.asList(//
                    sqlTimestampOf(sasEpochMinusOneMillisecond), // minus one millisecond
                    sqlDateOf(sasEpochMinusOneMillisecond), // minus one millisecond
                    sqlTimeOf(sasEpochMinusOneMillisecond))); // minus one millisecond

                // write the SAS Epoch + 1 millisecond
                exporter.appendObservation(Arrays.asList(//
                    sqlTimestampOf(sasEpochPlusOneMillisecond), // plus one millisecond
                    sqlDateOf(sasEpochPlusOneMillisecond), // plus one millisecond
                    sqlTimeOf(sasEpochPlusOneMillisecond))); // plus one millisecond

                // the Epoch of the Gregorian calendar.
                Instant gregorianEpoch = LocalDateTime.of(1601, 1, 1, 0, 0).toInstant(ZoneOffset.UTC);
                exporter.appendObservation(Arrays.asList(//
                    sqlTimestampOf(gregorianEpoch), //
                    sqlDateOf(gregorianEpoch), //
                    new java.sql.Time(0))); // this is always mapped between 0-86400 before persisting

                // The minimum supported date documented by SAS is 1582 in the Gregorian Calendar
                // https://support.sas.com/documentation/cdl/en/basess/58133/HTML/default/viewer.htm#a001397898.htm
                //
                // When using GregorianCalendar, this can be computed as
                //   GregorianCalendar minDay = new GregorianCalendar(1582, Calendar.JANUARY, 1, 0, 0, 0);
                //   minDay.setTimeZone(TimeZone.getTimeZone("UTC"));
                //
                // When using LocalDateTime, you have to add 10 days.  This isn't a Julian to Gregorian
                // calendar conversion because that would be 13 days, so it's not clear where the 10 days
                // comes from.
                Instant minDay = LocalDateTime.of(1582, 1, 11, 0, 0).toInstant(ZoneOffset.UTC);
                exporter.appendObservation(Arrays.asList(//
                    sqlTimestampOf(minDay), //
                    sqlDateOf(minDay), //
                    new java.sql.Time(0))); // this is always mapped between 0-86400 before persisting

                // The last second with a four digit year, accounting for what is probably
                // a bug in SAS which mis-calculates Dec. 29th as Dec. 31st.
                // I suspect that it's not handling the century leap years correctly.
                Instant buggyMaxFourDigitYear = LocalDateTime.of(9999, 12, 29, 23, 59, 59).toInstant(ZoneOffset.UTC);
                exporter.appendObservation(Arrays.asList(//
                    sqlTimestampOf(buggyMaxFourDigitYear), //
                    sqlDateOf(buggyMaxFourDigitYear), //
                    new java.sql.Time(0))); // this is always mapped between 0-86400 before persisting

                // last second with a four digit year.
                Instant maxFourDigitYear = LocalDateTime.of(9999, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC);
                exporter.appendObservation(Arrays.asList(//
                    sqlTimestampOf(maxFourDigitYear), //
                    sqlDateOf(maxFourDigitYear), //
                    new java.sql.Time(0))); // this is always mapped between 0-86400 before persisting

                // maximum supported dates documented by SAS
                Instant maxDay = LocalDateTime.of(19999, 12, 31, 23, 59, 59).toInstant(ZoneOffset.UTC);
                exporter.appendObservation(Arrays.asList(//
                    sqlTimestampOf(maxDay), //
                    sqlDateOf(maxDay), //
                    new java.sql.Time(0))); // this is always mapped between 0-86400 before persisting

                // raw decimal values (to see how SAS handles them)
                exporter.appendObservation(repeat(3, 0.5));
                exporter.appendObservation(repeat(3, 0.1));
                exporter.appendObservation(repeat(3, 0.01));
                exporter.appendObservation(repeat(3, 0.001));
            }
        }.run();
    }

    /**
     * Try to write an observation that contains null values.
     *
     * @throws IOException
     */
    @Test
    public void testNullValues() throws IOException {

        List<Variable> variables = Arrays.asList(//
            new Variable(//
                "NUMBER", // name
                1, // variable number
                VariableType.NUMERIC, //
                8, // length
                "The label for Var 1", //
                new Format("", 10, 2), //
                Justification.LEFT, //
                Format.UNSPECIFIED), //
            new Variable(//
                "TEXT", //
                2, // variable number
                VariableType.CHARACTER, //
                16, // length
                "Label for second var", //
                new Format("DOLLAR", 10), //
                Justification.LEFT, //
                new Format("", 0)));

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "DATA", // name
            "testNullValues", // label
            "", // type
            "Java", // OS version
            "6.7", // SAS Version
            variables, // variables
            LocalDateTime.of(1999, 12, 23, 23, 59, 59), // create
            LocalDateTime.of(2015, 2, 2, 3, 4, 5)); // modified

        new ExportTestRunner(dataSet.newLibraryDescription(), "SasTransportExporterTest_testNullValues.xpt") {
            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {

                // Try to write a null numeric value
                Exception exception = assertThrows(//
                    NullPointerException.class, //
                    () -> exporter.appendObservation(Arrays.asList(null, "first row")), //
                    "writing a null value");
                assertEquals("values in observation must not be null", exception.getMessage());

                // Try to write a null character value
                exception = assertThrows(//
                    NullPointerException.class, //
                    () -> exporter.appendObservation(Arrays.asList(13.2, null)), //
                    "writing a null value");
                assertEquals("values in observation must not be null", exception.getMessage());

                // Write a well-formed row (to confirm that the exceptions failed without
                // writing a partial row).
                exporter.appendObservation(Arrays.asList(12.3, "row"));
            }
        }.run();
    }

    /**
     * Try to write an observation that contains illegal values for a character variable.
     *
     * @throws IOException
     */
    @Test
    public void testBadCharacterValues() throws IOException {

        List<Variable> variables = Arrays.asList(//
            new Variable(//
                "TEXT1", // name
                1, // variable number
                VariableType.CHARACTER, //
                10, // length
                "", //label
                Format.UNSPECIFIED, //
                Justification.LEFT, //
                Format.UNSPECIFIED), //
            new Variable(//
                "TEXT2", //
                2, // variable number
                VariableType.CHARACTER, //
                10, // length
                "", // label
                Format.UNSPECIFIED, //
                Justification.LEFT, //
                Format.UNSPECIFIED));

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "DATA", // name
            "testBadMissingValues", // label
            "", // type
            "Java", // OS version
            "6.7", // SAS Version
            variables, // variables
            LocalDateTime.of(1999, 12, 23, 23, 59, 59), // create
            LocalDateTime.of(2015, 2, 2, 3, 4, 5)); // modified

        new ExportTestRunner(dataSet.newLibraryDescription(), "SasTransportExporterTest_testBadCharacterValues.xpt") {
            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {

                // Try writing each of the illegal missing values.
                // Note that the illegal argument is second, to confirm that the unit-under-test
                // doesn't write the first (well-formed) value to the output stream when any
                // of the subsequent values is bad.
                for (MissingValue badMissingValue : EnumSet.complementOf(EnumSet.of(MissingValue.STANDARD))) {
                    addObservationWithIllegalArgument(//
                        exporter, //
                        "CHARACTER variables can only use MissingValue.STANDARD for missing values", //
                        "first", //
                        badMissingValue);
                }

                // Try writing a value that exceeds the length of the variable.
                addObservationWithIllegalArgument(//
                    exporter, //
                    "value length exceeds maximum length for variable TEXT2", //
                    "first", //
                    "0123456789X");

                // Try writing an object that isn't a String class
                addObservationWithIllegalArgument(//
                    exporter, //
                    "values for character variables must be String", //
                    "first", //
                    new Date());
                addObservationWithIllegalArgument(//
                    exporter, //
                    "values for character variables must be String", //
                    "first", //
                    Double.valueOf(12.3));

                // Try writing a string that isn't fully ASCII
                addObservationWithIllegalArgument(//
                    exporter, //
                    "values of character variables must contain only ASCII (7-bit) characters", //
                    "first", //
                    "BAD\u00B5");

                // Write a missing value (successfully)
                // This is a well-formed row to confirm that the exceptions failed without writing
                // a partial row.
                exporter.appendObservation(Arrays.asList("0123456789", MissingValue.STANDARD));
            }
        }.run();
    }

    /**
     * Try to write an observation that contains illegal arguments for numeric data.
     *
     * @throws IOException
     */
    @Test
    public void testBadNumericValues() throws IOException {

        List<Variable> variables = Arrays.asList(//
            new Variable(//
                "NUMBER1", // name
                1, // variable number
                VariableType.NUMERIC, //
                8, // length
                "", //label
                Format.UNSPECIFIED, //
                Justification.LEFT, //
                Format.UNSPECIFIED), //
            new Variable(//
                "NUMBER2", //
                2, // variable number
                VariableType.NUMERIC, //
                8, // length
                "", // label
                Format.UNSPECIFIED, //
                Justification.LEFT, //
                Format.UNSPECIFIED));

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "DATA", // name
            "testBadNumericValues", // label
            "", // type
            "Java", // OS version
            "6.7", // SAS Version
            variables, // variables
            LocalDateTime.of(1999, 12, 23, 23, 59, 59), // create
            LocalDateTime.of(2015, 2, 2, 3, 4, 5)); // modified

        new ExportTestRunner(dataSet.newLibraryDescription(), "SasTransportExporterTest_testBadNumericValues.xpt") {
            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {

                final Integer goodNumber = Integer.valueOf(-1);

                // Try writing some non-numeric data into numeric variables.
                List<Object> badNumbers = Arrays.asList(//
                    "not a number", //
                    "1034", // strings are not numbers, even if they could be parsed as such
                    ".", // not a missing value
                    ".A", // also not a missing value
                    "", //
                    new int[] { 100 }, // an array of numbers is not a number
                    Collections.singletonList(100)); // a list of numbers is not a number
                for (Object badNumber : badNumbers) {
                    addObservationWithIllegalArgument(//
                        exporter, //
                        "non-numeric value given for a numeric variable", //
                        goodNumber, //
                        badNumber);
                }

                // The next set of numbers are tests for boundary conditions of the smallest possible
                // positive number.
                double[] smallNumbers = new double[] { //
                    SasTransportExporter.MIN_VALUE - 0.1E-79, // smallest possible XPORT number
                    5.39760534693390E-79, // slightly smaller than the smallest possible XPORT number
                    1E-80, // much smaller than the smallest possible XPORT number
                    Double.MIN_NORMAL, // smallest possible number in Java (much too small)
                };
                for (double smallNumber : smallNumbers) {
                    // positive version fails
                    addObservationWithIllegalArgument(//
                        exporter, //
                        "XPORT format cannot store numbers smaller than pow(2, -260)", //
                        goodNumber, //
                        Double.valueOf(smallNumber));

                    // negative version also fails
                    addObservationWithIllegalArgument(//
                        exporter, //
                        "XPORT format cannot store numbers smaller than pow(2, -260)", //
                        goodNumber, //
                        Double.valueOf(-smallNumber));
                }

                // The next set of numbers are tests for boundary conditions of the largest
                // possible positive numbers that can be exported.
                double[] largeNumbers = new double[] { //
                    7.23700557733232E+75, // largest possible XPORT number (can't be converted from Java)
                    7.23700557733300E+75, // slightly larger
                    1E+76 }; // much larger
                for (double largeNumber : largeNumbers) {
                    // positive version fails
                    addObservationWithIllegalArgument(//
                        exporter, //
                        "XPORT format cannot store numbers larger than pow(2, 248)", //
                        goodNumber, //
                        Double.valueOf(largeNumber));

                    // negative version also fails
                    addObservationWithIllegalArgument(//
                        exporter, //
                        "XPORT format cannot store numbers larger than pow(2, 248)", //
                        goodNumber, //
                        Double.valueOf(-largeNumber));
                }

                // Write a well-formed observation to confirm that the bad exception didn't
                // leave the object in a bad state.
                exporter.appendObservation(Arrays.asList(goodNumber, MissingValue.Z));
            }
        }.run();
    }

    /**
     * Try to write an observation that contains the incorrect number of values.
     *
     * @throws IOException
     */
    @Test
    public void testMisshapenObservation() throws IOException {

        List<Variable> variables = Arrays.asList(//
            new Variable(//
                "TEXT", //
                1, // variable number
                VariableType.CHARACTER, //
                16, // length
                "Label for second var", //
                new Format("DOLLAR", 10), //
                Justification.LEFT, //
                Format.UNSPECIFIED),
            new Variable(//
                "NUMBER", // name
                2, // variable number
                VariableType.NUMERIC, //
                8, // length
                "The label for Var 1", //
                new Format("", 10, 2), //
                Justification.LEFT, //
                Format.UNSPECIFIED));

        SasDataSetDescription dataSet = new SasDataSetDescription(//
            "DATA", // name
            "testMisshapenObservation", // label
            "", // type
            "Java", // OS version
            "6.7", // SAS Version
            variables, // variables
            LocalDateTime.of(1999, 12, 23, 23, 59, 59), // create
            LocalDateTime.of(2015, 2, 2, 3, 4, 5)); // modified

        new ExportTestRunner(dataSet.newLibraryDescription(), "SasTransportExporterTest_testMisshapenObservation.xpt") {
            @Override
            void addObservations(SasTransportExporter exporter) throws IOException {

                // Try to write too many values in an observation.
                addObservationWithIllegalArgument(//
                    exporter, //
                    "observation has different number of values than data set has variables", //
                    "text", //
                    1.2, //
                    "too many");

                // Try to write too few values in an observation.
                addObservationWithIllegalArgument(//
                    exporter, //
                    "observation has different number of values than data set has variables", //
                    "too few");

                // Write a well-formed row (to confirm that the exceptions failed without
                // writing a partial row).
                exporter.appendObservation(Arrays.asList("row", 12.3));
            }
        }.run();
    }
}