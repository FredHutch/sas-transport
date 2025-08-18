///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link SasTransportImporter}.
 */
public class SasTransportImporterTest {

    private static void assertNextObservation(SasTransportImporter importer, Object[] expectedValues)
        throws IOException {
        List<Object> observation = importer.nextObservation(); // invoke method-under-test
        assertNotNull(observation, "importer unexpected reached EOF"); // we should not get the EOF indicator
        assertEquals(expectedValues.length, observation.size()); // correct number of values
        for (int i = 0; i < expectedValues.length; i++) {
            assertEquals(expectedValues[i], observation.get(i), "value #" + (i + 1) + " incorrect");
        }
    }

    private static void assertEndOfObservations(SasTransportImporter importer) throws IOException {
        List<Object> observation = importer.nextObservation();
        assertEquals(null, observation, "nextObservation() did not return null");

        // reading past EOF is not an error (it stays at EOF)
        observation = importer.nextObservation();
        assertEquals(null, observation, "second invocation of nextObservation() did not return null");
    }

    private static <T extends Exception> void assertNextObservationsThrowsException(SasTransportImporter importer,
        Class<T> expectedExceptionClass, String expectedExceptionMessage) {

        // Try to get an observation, excepting an exception to be thrown.
        Exception exception = assertThrows(//
            expectedExceptionClass, //
            importer::nextObservation, //
            "No " + expectedExceptionClass.getSimpleName() + " thrown when reading an observation");

        // Confirm that the correct message was returned.
        assertEquals(expectedExceptionMessage, exception.getMessage());

        // We shouldn't be able to start reading the next observation after
        // an error was detected and reported.
        exception = assertThrows(//
            expectedExceptionClass, //
            importer::nextObservation, //
            "No " + expectedExceptionClass.getSimpleName() + " thrown when reading an observation after an error");
        // The same exception should be thrown.
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    /**
     * Calls {@link SasLibraryDescription#importTransportDataSet(InputStream)} with a {@code null} {@code InputStream}
     * argument.
     */
    @Test
    public void importNullStream() {
        Exception exception = assertThrows(//
            NullPointerException.class, //
            () -> SasLibraryDescription.importTransportDataSet((InputStream) null));
        assertEquals("inputStream must not be null", exception.getMessage());
    }

    /**
     * Calls {@link SasLibraryDescription#importTransportDataSet(Path)} with a {@code null} {@code Path} argument.
     */
    @Test
    public void importNullPath() {
        Exception exception = assertThrows(//
            NullPointerException.class, //
            () -> SasLibraryDescription.importTransportDataSet((Path) null));
        assertEquals("path must not be null", exception.getMessage());
    }

    /**
     * Tests when performing operations on an importer that is already closed.
     */
    @Test
    public void testClosedImporter() throws IOException {

        InputStream testStream = TestUtil.getTestResource("minidata.xpt");
        SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream);

        // Close the importer.
        importer.close();

        // Getting an observation should not work.
        assertNextObservationsThrowsException(importer, IllegalStateException.class, "reading from closed stream");

        // Closing redundantly should not throw an exception.
        importer.close();

        // Getting the library description should still work.
        LocalDateTime expectedTimestamp = LocalDateTime.of(2017, 8, 23, 8, 56, 39);
        SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
        TestUtil.assertSasLibraryDescription(//
            libraryDescription, //
            "Linux", // OS
            "9.4", // SAS version
            expectedTimestamp, // creation time
            expectedTimestamp); // modified time

        // Getting the data set description should still work.
        SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();
        TestUtil.assertSasDataSetDescription(//
            dataSetDescription, //
            "MINIDATA", // name
            "Data set with only one value", // label
            "", // type
            "Linux", // OS
            "9.4", // SAS version
            expectedTimestamp, // creation time
            expectedTimestamp); // modified time

        // Getting the variables should still work.
        List<Variable> dataSetVariables = dataSetDescription.variables();
        assertNotNull(dataSetVariables);
        assertEquals(1, dataSetVariables.size()); // only one variable is expected.

        // Check the variable
        TestUtil.assertVariable(//
            dataSetVariables.get(0), //
            "NUMBER", // name
            1, // number (1-indexed)
            VariableType.NUMERIC, // type
            8, // length
            "A number", // label
            new Format("", 5, 2), // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED); // input format
    }

    /**
     * Reads an XPORT that has no observations.
     */
    @Test
    public void testNoObservations() throws IOException {

        LocalDateTime expectedTimestamp = LocalDateTime.of(2017, 9, 19, 9, 8, 35);

        InputStream testStream = TestUtil.getTestResource("no_observations.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            TestUtil.assertSasLibraryDescription(//
                libraryDescription, //
                "LIN X64", // OS
                "9.1", // SAS version
                expectedTimestamp, // creation time
                expectedTimestamp); // modified time

            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();
            TestUtil.assertSasDataSetDescription(//
                dataSetDescription, //
                "REPORT", // name
                "Data set with no observations", // label
                "", // type
                " LIN X64", // OS
                "9.1", // SAS version
                expectedTimestamp, // creation time
                expectedTimestamp); // modified time

            List<Variable> dataSetVariables = dataSetDescription.variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            // Check the variable
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "TEXT", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                8, // length
                "Some text", // label
                new Format("$UPCASE", 8, 0), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            // There are no observations.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Reads a very small XPORT file (one variable, one observation, one value).
     */
    @Test
    public void testReadingOneValue() throws IOException {

        LocalDateTime expectedTimestamp = LocalDateTime.of(2017, 8, 23, 8, 56, 39);

        InputStream testStream = TestUtil.getTestResource("minidata.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            TestUtil.assertSasLibraryDescription(//
                libraryDescription, //
                "Linux", // OS
                "9.4", // SAS version
                expectedTimestamp, // creation time
                expectedTimestamp); // modified time

            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();
            TestUtil.assertSasDataSetDescription(//
                dataSetDescription, //
                "MINIDATA", // name
                "Data set with only one value", // label
                "", // type
                "Linux", // OS
                "9.4", // SAS version
                expectedTimestamp, // creation time
                expectedTimestamp); // modified time

            List<Variable> dataSetVariables = dataSetDescription.variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            // Check the variable
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "NUMBER", // name
                1, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "A number", // label
                new Format("", 5, 2), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            // Get the one (and only) observation (a missing value)
            assertNextObservation(importer, new Object[] { MissingValue.STANDARD });

            // There is only one observation.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT that has variations on NUMERIC data that was exported by SAS.
     */
    @Test
    public void testNumericVariations() throws IOException {

        InputStream testStream = TestUtil.getTestResource("numeric_variations.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();
            List<Variable> dataSetVariables = dataSetDescription.variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "NUMBER", // name
                1, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "A number", // label
                new Format("E", 32, 0), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            // Read the data
            assertNextObservation(importer, new Object[] { MissingValue.STANDARD });
            assertNextObservation(importer, new Object[] { MissingValue.UNDERSCORE });
            assertNextObservation(importer, new Object[] { MissingValue.A });
            assertNextObservation(importer, new Object[] { MissingValue.B });
            assertNextObservation(importer, new Object[] { MissingValue.C });
            assertNextObservation(importer, new Object[] { MissingValue.D });
            assertNextObservation(importer, new Object[] { MissingValue.E });
            assertNextObservation(importer, new Object[] { MissingValue.F });
            assertNextObservation(importer, new Object[] { MissingValue.G });
            assertNextObservation(importer, new Object[] { MissingValue.H });
            assertNextObservation(importer, new Object[] { MissingValue.I });
            assertNextObservation(importer, new Object[] { MissingValue.J });
            assertNextObservation(importer, new Object[] { MissingValue.K });
            assertNextObservation(importer, new Object[] { MissingValue.L });
            assertNextObservation(importer, new Object[] { MissingValue.M });
            assertNextObservation(importer, new Object[] { MissingValue.N });
            assertNextObservation(importer, new Object[] { MissingValue.O });
            assertNextObservation(importer, new Object[] { MissingValue.P });
            assertNextObservation(importer, new Object[] { MissingValue.Q });
            assertNextObservation(importer, new Object[] { MissingValue.R });
            assertNextObservation(importer, new Object[] { MissingValue.S });
            assertNextObservation(importer, new Object[] { MissingValue.T });
            assertNextObservation(importer, new Object[] { MissingValue.U });
            assertNextObservation(importer, new Object[] { MissingValue.V });
            assertNextObservation(importer, new Object[] { MissingValue.W });
            assertNextObservation(importer, new Object[] { MissingValue.X });
            assertNextObservation(importer, new Object[] { MissingValue.Y });
            assertNextObservation(importer, new Object[] { MissingValue.Z });
            assertNextObservation(importer, new Object[] { Double.valueOf(-10.0) });
            assertNextObservation(importer, new Object[] { Double.valueOf(0) });
            assertNextObservation(importer, new Object[] { Double.valueOf(10.0) });

            // These next set of numbers are tests for boundary conditions of the smallest possible
            // positive number.  The first one is slightly larger than the smallest number and
            // is rounded due to information loss inherent in conversion from XPORT to JVM format.
            // The next three are different within the SAS file that generated the XPORT but were
            // persisted into the XPORT with the same value.
            assertNextObservation(importer, new Object[] { Double.valueOf(5.39760534693411E-79) });
            assertNextObservation(importer, new Object[] { Double.valueOf(5.397605346934028E-79) });
            assertNextObservation(importer, new Object[] { Double.valueOf(5.397605346934028E-79) });
            assertNextObservation(importer, new Object[] { Double.valueOf(5.397605346934028E-79) });

            // Same as above but negative.
            assertNextObservation(importer, new Object[] { Double.valueOf(-5.39760534693411E-79) });
            assertNextObservation(importer, new Object[] { Double.valueOf(-5.397605346934028E-79) });
            assertNextObservation(importer, new Object[] { Double.valueOf(-5.397605346934028E-79) });
            assertNextObservation(importer, new Object[] { Double.valueOf(-5.397605346934028E-79) });

            // These next set of numbers are tests for boundary conditions of the largest possible
            // positive number.  The first one is slightly smaller than the largest number and
            // is rounded due to information loss inherent in conversion from XPORT to JVM format.
            // The next three are different within the SAS file that generated the XPORT but were
            // persisted into the XPORT with the same value.
            assertNextObservation(importer, new Object[] { Double.valueOf(7.23700557733221E75) });
            assertNextObservation(importer, new Object[] { Double.valueOf(7.2370055773322614E75) });
            assertNextObservation(importer, new Object[] { Double.valueOf(7.2370055773322614E75) });
            assertNextObservation(importer, new Object[] { Double.valueOf(7.2370055773322614E75) });

            // Same as above but negative.
            assertNextObservation(importer, new Object[] { Double.valueOf(-7.23700557733221E75) });
            assertNextObservation(importer, new Object[] { Double.valueOf(-7.2370055773322614E75) });
            assertNextObservation(importer, new Object[] { Double.valueOf(-7.2370055773322614E75) });
            assertNextObservation(importer, new Object[] { Double.valueOf(-7.2370055773322614E75) });

            // Rising exponents
            assertNextObservation(importer, new Object[] { Double.valueOf(5.397605346934028E-79) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-78) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-77) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-76) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-75) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-74) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-73) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-72) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-71) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-70) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-69) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-68) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-67) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-66) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-65) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-64) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-63) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-62) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-61) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-60) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-59) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-58) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-57) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-56) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-55) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-54) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-53) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-52) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-51) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-50) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-49) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-48) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-47) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-46) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-45) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-44) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-43) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-42) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-41) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-40) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-39) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-38) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-37) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-36) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-35) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-34) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-33) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-32) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-31) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-30) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-29) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-28) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-27) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-26) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-25) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-24) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-23) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-22) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-21) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-20) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-19) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-18) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-17) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-16) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-15) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-14) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-13) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-12) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-11) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-10) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-09) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-08) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-07) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-06) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-05) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-04) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-03) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-02) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E-01) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+01) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+02) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+03) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+04) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+05) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+06) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+07) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+08) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+09) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+10) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+11) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+12) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+13) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+14) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+15) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+16) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+17) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+18) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+19) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+20) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+21) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+22) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+23) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+24) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+25) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+26) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+27) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+28) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+29) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+30) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+31) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+32) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+33) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+34) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+35) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+36) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+37) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+38) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+39) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+40) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+41) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+42) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+43) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+44) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+45) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+46) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+47) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+48) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+49) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+50) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+51) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+52) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+53) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+54) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+55) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+56) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+57) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+58) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+59) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+60) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+61) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+62) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+63) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+64) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+65) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+66) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+67) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+68) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+69) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+70) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+71) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+72) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+73) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+74) });
            assertNextObservation(importer, new Object[] { Double.valueOf(1.23E+75) });

            // EOF
            assertEndOfObservations(importer);
        }
    }

    private Object[] repeat(int repeatCount, Object value) {
        Object[] array = new Object[repeatCount];
        Arrays.fill(array, value);
        return array;
    }

    /**
     * Tests importing an XPORT with different variations on the size of numeric fields.
     */
    @Test
    public void testNumericTruncation() throws IOException {
        InputStream testStream = TestUtil.getTestResource("numeric_truncations.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // The first 28 values are variations on MissingValue
            assertNextObservation(importer, repeat(6, MissingValue.STANDARD));
            assertNextObservation(importer, repeat(6, MissingValue.UNDERSCORE));
            assertNextObservation(importer, repeat(6, MissingValue.A));
            assertNextObservation(importer, repeat(6, MissingValue.B));
            assertNextObservation(importer, repeat(6, MissingValue.C));
            assertNextObservation(importer, repeat(6, MissingValue.D));
            assertNextObservation(importer, repeat(6, MissingValue.E));
            assertNextObservation(importer, repeat(6, MissingValue.F));
            assertNextObservation(importer, repeat(6, MissingValue.G));
            assertNextObservation(importer, repeat(6, MissingValue.H));
            assertNextObservation(importer, repeat(6, MissingValue.I));
            assertNextObservation(importer, repeat(6, MissingValue.J));
            assertNextObservation(importer, repeat(6, MissingValue.K));
            assertNextObservation(importer, repeat(6, MissingValue.L));
            assertNextObservation(importer, repeat(6, MissingValue.M));
            assertNextObservation(importer, repeat(6, MissingValue.N));
            assertNextObservation(importer, repeat(6, MissingValue.O));
            assertNextObservation(importer, repeat(6, MissingValue.P));
            assertNextObservation(importer, repeat(6, MissingValue.Q));
            assertNextObservation(importer, repeat(6, MissingValue.R));
            assertNextObservation(importer, repeat(6, MissingValue.S));
            assertNextObservation(importer, repeat(6, MissingValue.T));
            assertNextObservation(importer, repeat(6, MissingValue.U));
            assertNextObservation(importer, repeat(6, MissingValue.V));
            assertNextObservation(importer, repeat(6, MissingValue.W));
            assertNextObservation(importer, repeat(6, MissingValue.X));
            assertNextObservation(importer, repeat(6, MissingValue.Y));
            assertNextObservation(importer, repeat(6, MissingValue.Z));

            // Now we start reading data that is 1.2345678901234e?? across the full range of exponents.
            // This is where we can see the impact of truncation.
            assertNextObservation(importer, repeat(6, 5.397605346934028E-79)); // all min values
            assertNextObservation(importer, repeat(6, 5.397605346934028E-79)); // all min values
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344913791522943E-78, // N3
                    1.2345675630046184E-78, // N4
                    1.2345678887484970E-78, // N5
                    1.2345678901151887E-78, // N6
                    1.2345678901234115E-78, // N7
                    1.2345678901234570E-78, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344913791522943E-77, // N3
                    1.2345671512000112E-77, // N4
                    1.2345678879441912E-77, // N5
                    1.2345678901183305E-77, // N6
                    1.2345678901234360E-77, // N7
                    1.2345678901234570E-77, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345335479440672E-76, // N3
                    1.2345678100873827E-76, // N4
                    1.2345678898745253E-76, // N5
                    1.2345678901158171E-76, // N6
                    1.2345678901234360E-76, // N7
                    1.2345678901234569E-76, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344323428438122E-75, // N3
                    1.2345672829774855E-75, // N4
                    1.2345678883302580E-75, // N5
                    1.2345678901158170E-75, // N6
                    1.2345678901234203E-75, // N7
                    1.2345678901234569E-75, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345402949507509E-74, // N3
                    1.2345672829774855E-74, // N4
                    1.2345678891538673E-74, // N5
                    1.2345678901190343E-74, // N6
                    1.2345678901234077E-74, // N7
                    1.2345678901234568E-74, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344539332651999E-73, // N3
                    1.2345672829774855E-73, // N4
                    1.2345678891538673E-73, // N5
                    1.2345678901216081E-73, // N6
                    1.2345678901234178E-73, // N7
                    1.2345678901234569E-73, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344884779394203E-72, // N3
                    1.2345672829774855E-72, // N4
                    1.2345678859912080E-72, // N5
                    1.2345678901092540E-72, // N6
                    1.2345678901234097E-72, // N7
                    1.2345678901234570E-72, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345437494181729E-71, // N3
                    1.2345670670732717E-71, // N4
                    1.2345678868345837E-71, // N5
                    1.2345678901158428E-71, // N6
                    1.2345678901234097E-71, // N7
                    1.2345678901234570E-71, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345437494181729E-70, // N3
                    1.2345672397966428E-70, // N4
                    1.2345678875092844E-70, // N5
                    1.2345678901132073E-70, // N6
                    1.2345678901234200E-70, // N7
                    1.2345678901234569E-70, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344022544325662E-69, // N3
                    1.2345669634392490E-69, // N4
                    1.2345678875092844E-69, // N5
                    1.2345678901068820E-69, // N6
                    1.2345678901234200E-69, // N7
                    1.2345678901234568E-69, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345154504210516E-68, // N3
                    1.2345676266969940E-68, // N4
                    1.2345678892365180E-68, // N5
                    1.2345678901136290E-68, // N6
                    1.2345678901234332E-68, // N7
                    1.2345678901234569E-68, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344701720256574E-67, // N3
                    1.2345678035657260E-67, // N4
                    1.2345678892365181E-67, // N5
                    1.2345678901217254E-67, // N6
                    1.2345678901234543E-67, // N7
                    1.2345678901234570E-67, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344701720256574E-66, // N3
                    1.2345675205757548E-66, // N4
                    1.2345678875783737E-66, // N5
                    1.2345678901174073E-66, // N6
                    1.2345678901234120E-66, // N7
                    1.2345678901234570E-66, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344701720256574E-65, // N3
                    1.2345670677918009E-65, // N4
                    1.2345678884627174E-65, // N5
                    1.2345678901208618E-65, // N6
                    1.2345678901234526E-65, // N7
                    1.2345678901234568E-65, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344238069487738E-64, // N3
                    1.2345672489053825E-64, // N4
                    1.2345678884627174E-64, // N5
                    1.2345678901208618E-64, // N6
                    1.2345678901234526E-64, // N7
                    1.2345678901234568E-64, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344238069487738E-63, // N3
                    1.2345675386871130E-63, // N4
                    1.2345678873307575E-63, // N5
                    1.2345678901075966E-63, // N6
                    1.2345678901234180E-63, // N7
                    1.2345678901234570E-63, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344238069487738E-62, // N3
                    1.2345675386871130E-62, // N4
                    1.2345678900474612E-62, // N5
                    1.2345678901182087E-62, // N6
                    1.2345678901234042E-62, // N7
                    1.2345678901234570E-62, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344238069487738E-61, // N3
                    1.2345677241474205E-61, // N4
                    1.2345678893230069E-61, // N5
                    1.2345678901153788E-61, // N6
                    1.2345678901234264E-61, // N7
                    1.2345678901234570E-61, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344238069487738E-60, // N3
                    1.2345674274109285E-60, // N4
                    1.2345678887434434E-60, // N5
                    1.2345678901199067E-60, // N6
                    1.2345678901234440E-60, // N7
                    1.2345678901234569E-60, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343934211319874E-59, // N3
                    1.2345671900217348E-59, // N4
                    1.2345678873524911E-59, // N5
                    1.2345678901199067E-59, // N6
                    1.2345678901234157E-59, // N7
                    1.2345678901234569E-59, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344906557457040E-58, // N3
                    1.2345673799330897E-58, // N4
                    1.2345678873524911E-58, // N5
                    1.2345678901228045E-58, // N6
                    1.2345678901234384E-58, // N7
                    1.2345678901234570E-58, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344906557457040E-57, // N3
                    1.2345678357203415E-57, // N4
                    1.2345678879459641E-57, // N5
                    1.2345678901158497E-57, // N6
                    1.2345678901234565E-57, // N7
                    1.2345678901234569E-57, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343973105165360E-56, // N3
                    1.2345674710905400E-56, // N4
                    1.2345678888955210E-56, // N5
                    1.2345678901121405E-56, // N6
                    1.2345678901234420E-56, // N7
                    1.2345678901234570E-56, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344470946387590E-55, // N3
                    1.2345676655597675E-55, // N4
                    1.2345678873762300E-55, // N5
                    1.2345678901180752E-55, // N6
                    1.2345678901234536E-55, // N7
                    1.2345678901234569E-55, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344869219365373E-54, // N3
                    1.2345678211351495E-54, // N4
                    1.2345678891993790E-54, // N5
                    1.2345678901204491E-54, // N6
                    1.2345678901234536E-54, // N7
                    1.2345678901234569E-54, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344550600983146E-53, // N3
                    1.2345675722145383E-53, // N4
                    1.2345678872546868E-53, // N5
                    1.2345678901109536E-53, // N6
                    1.2345678901234165E-53, // N7
                    1.2345678901234568E-53, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345060390394709E-52, // N3
                    1.2345673730780495E-52, // N4
                    1.2345678895883175E-52, // N5
                    1.2345678901231079E-52, // N6
                    1.2345678901234402E-52, // N7
                    1.2345678901234570E-52, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344244727336209E-51, // N3
                    1.2345678510056228E-51, // N4
                    1.2345678883437145E-51, // N5
                    1.2345678901231079E-51, // N6
                    1.2345678901234497E-51, // N7
                    1.2345678901234569E-51, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343265931666010E-50, // N3
                    1.2345672137688583E-50, // N4
                    1.2345678868501908E-50, // N5
                    1.2345678901172738E-50, // N6
                    1.2345678901234118E-50, // N7
                    1.2345678901234569E-50, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344832004738329E-49, // N3
                    1.2345672137688583E-49, // N4
                    1.2345678892398287E-49, // N5
                    1.2345678901110508E-49, // N6
                    1.2345678901234482E-49, // N7
                    1.2345678901234570E-49, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344832004738329E-48, // N3
                    1.2345673769014700E-48, // N4
                    1.2345678892398287E-48, // N5
                    1.2345678901160292E-48, // N6
                    1.2345678901234190E-48, // N7
                    1.2345678901234569E-48, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345500195915852E-47, // N3
                    1.2345677684197381E-47, // N4
                    1.2345678866908816E-47, // N5
                    1.2345678901160292E-47, // N6
                    1.2345678901234346E-47, // N7
                    1.2345678901234570E-47, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343896537089796E-46, // N3
                    1.2345675596099951E-46, // N4
                    1.2345678891378708E-46, // N5
                    1.2345678901192154E-46, // N6
                    1.2345678901234470E-46, // N7
                    1.2345678901234570E-46, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345179464150640E-45, // N3
                    1.2345673925622008E-45, // N4
                    1.2345678884853403E-45, // N5
                    1.2345678901166665E-45, // N6
                    1.2345678901234371E-45, // N7
                    1.2345678901234569E-45, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343468894736182E-44, // N3
                    1.2345671252857298E-44, // N4
                    1.2345678895293890E-44, // N5
                    1.2345678901166665E-44, // N6
                    1.2345678901234212E-44, // N7
                    1.2345678901234570E-44, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344563659161435E-43, // N3
                    1.2345675529280834E-43, // N4
                    1.2345678870236721E-43, // N5
                    1.2345678901166665E-43, // N6
                    1.2345678901234467E-43, // N7
                    1.2345678901234568E-43, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345439470701638E-42, // N3
                    1.2345672108142005E-42, // N4
                    1.2345678896964368E-42, // N5
                    1.2345678901140563E-42, // N6
                    1.2345678901234365E-42, // N7
                    1.2345678901234568E-42, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345439470701638E-41, // N3
                    1.2345669371230942E-41, // N4
                    1.2345678864891192E-41, // N5
                    1.2345678901140563E-41, // N6
                    1.2345678901234528E-41, // N7
                    1.2345678901234568E-41, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344878951315908E-40, // N3
                    1.2345675939817493E-40, // N4
                    1.2345678881996886E-40, // N5
                    1.2345678901107154E-40, // N6
                    1.2345678901234528E-40, // N7
                    1.2345678901234568E-40, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343982120298740E-39, // N3
                    1.2345677691440574E-39, // N4
                    1.2345678895681441E-39, // N5
                    1.2345678901133881E-39, // N6
                    1.2345678901234528E-39, // N7
                    1.2345678901234568E-39, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343264655485006E-38, // N3
                    1.2345674888843645E-38, // N4
                    1.2345678873786153E-38, // N5
                    1.2345678901155264E-38, // N6
                    1.2345678901234110E-38, // N7
                    1.2345678901234570E-38, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344986571037969E-37, // N3
                    1.2345677130921188E-37, // N4
                    1.2345678882544268E-37, // N5
                    1.2345678901155264E-37, // N6
                    1.2345678901234377E-37, // N7
                    1.2345678901234570E-37, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344527393557179E-36, // N3
                    1.2345675337259154E-36, // N4
                    1.2345678896557253E-36, // N5
                    1.2345678901155264E-36, // N6
                    1.2345678901234377E-36, // N7
                    1.2345678901234570E-36, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345629419511075E-35, // N3
                    1.2345675337259154E-35, // N4
                    1.2345678879741671E-35, // N5
                    1.2345678901111473E-35, // N6
                    1.2345678901233950E-35, // N7
                    1.2345678901234570E-35, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345041672335663E-34, // N3
                    1.2345675337259154E-34, // N4
                    1.2345678888709982E-34, // N5
                    1.2345678901181538E-34, // N6
                    1.2345678901234087E-34, // N7
                    1.2345678901234570E-34, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345511870075992E-33, // N3
                    1.2345673500549230E-33, // N4
                    1.2345678895884630E-33, // N5
                    1.2345678901153512E-33, // N6
                    1.2345678901234524E-33, // N7
                    1.2345678901234569E-33, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344007237306940E-32, // N3
                    1.2345676439285107E-32, // N4
                    1.2345678872925756E-32, // N5
                    1.2345678901086250E-32, // N6
                    1.2345678901234087E-32, // N7
                    1.2345678901234568E-32, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345210943522182E-31, // N3
                    1.2345671737307704E-31, // N4
                    1.2345678900476404E-31, // N5
                    1.2345678901193870E-31, // N6
                    1.2345678901234227E-31, // N7
                    1.2345678901234568E-31, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345210943522182E-30, // N3
                    1.2345677379680588E-30, // N4
                    1.2345678878435885E-30, // N5
                    1.2345678901165170E-30, // N6
                    1.2345678901234227E-30, // N7
                    1.2345678901234568E-30, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344440571544427E-29, // N3
                    1.2345674370415050E-29, // N4
                    1.2345678884313357E-29, // N5
                    1.2345678901211088E-29, // N6
                    1.2345678901234406E-29, // N7
                    1.2345678901234569E-29, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345673166708835E-28, // N3
                    1.2345673166708835E-28, // N4
                    1.2345678884313357E-28, // N5
                    1.2345678901211088E-28, // N6
                    1.2345678901234047E-28, // N7
                    1.2345678901234570E-28, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345673166708835E-27, // N3
                    1.2345673166708835E-27, // N4
                    1.2345678884313357E-27, // N5
                    1.2345678901122926E-27, // N6
                    1.2345678901234506E-27, // N7
                    1.2345678901234570E-27, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345673166708835E-26, // N3
                    1.2345673166708835E-26, // N4
                    1.2345678896350420E-26, // N5
                    1.2345678901146436E-26, // N6
                    1.2345678901234231E-26, // N7
                    1.2345678901234568E-26, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344095444898393E-25, // N3
                    1.2345673166708835E-25, // N4
                    1.2345678867461470E-25, // N5
                    1.2345678901165244E-25, // N6
                    1.2345678901234010E-25, // N7
                    1.2345678901234568E-25, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345105186857076E-24, // N3
                    1.2345673166708835E-24, // N4
                    1.2345678898276350E-24, // N5
                    1.2345678901165244E-24, // N6
                    1.2345678901234363E-24, // N7
                    1.2345678901234569E-24, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344701290073602E-23, // N3
                    1.2345673166708835E-23, // N4
                    1.2345678885950398E-23, // N5
                    1.2345678901165244E-23, // N6
                    1.2345678901234457E-23, // N7
                    1.2345678901234569E-23, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345670642353938E-22, // N3
                    1.2345670642353938E-22, // N4
                    1.2345678885950398E-22, // N5
                    1.2345678901203763E-22, // N6
                    1.2345678901234457E-22, // N7
                    1.2345678901234568E-22, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345670642353938E-21, // N3
                    1.2345678720289608E-21, // N4
                    1.2345678878061789E-21, // N5
                    1.2345678901111318E-21, // N6
                    1.2345678901234096E-21, // N7
                    1.2345678901234568E-21, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344843461741385E-20, // N3
                    1.2345677104702474E-20, // N4
                    1.2345678896994450E-20, // N5
                    1.2345678901135970E-20, // N6
                    1.2345678901234192E-20, // N7
                    1.2345678901234569E-20, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345505206231427E-19, // N3
                    1.2345670642353938E-19, // N4
                    1.2345678881848320E-19, // N5
                    1.2345678901096527E-19, // N6
                    1.2345678901233961E-19, // N7
                    1.2345678901234568E-19, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345505206231427E-18, // N3
                    1.2345670642353938E-18, // N4
                    1.2345678881848320E-18, // N5
                    1.2345678901159636E-18, // N6
                    1.2345678901234085E-18, // N7
                    1.2345678901234568E-18, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344658173284173E-17, // N3
                    1.2345677259798838E-17, // N4
                    1.2345678888310670E-17, // N5
                    1.2345678901134392E-17, // N6
                    1.2345678901234183E-17, // N7
                    1.2345678901234568E-17, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343641733747468E-16, // N3
                    1.2345674612820878E-16, // N4
                    1.2345678872801033E-16, // N5
                    1.2345678901073808E-16, // N6
                    1.2345678901234104E-16, // N7
                    1.2345678901234568E-16, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344725935919953E-15, // N3
                    1.2345674612820878E-15, // N4
                    1.2345678881072840E-15, // N5
                    1.2345678901106120E-15, // N6
                    1.2345678901234357E-15, // N7
                    1.2345678901234570E-15, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344292255050960E-14, // N3
                    1.2345674612820878E-14, // N4
                    1.2345678900925174E-14, // N5
                    1.2345678901131969E-14, // N6
                    1.2345678901234559E-14, // N7
                    1.2345678901234568E-14, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] {
                    1.2342904476270178E-13, // N3
                    1.2345669191810016E-13, // N4
                    1.2345678890337262E-13, // N5
                    1.2345678901090610E-13, // N6
                    1.2345678901234074E-13, // N7
                    1.2345678901234570E-13, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343459587782490E-12, // N3
                    1.2345671360214360E-12, // N4
                    1.2345678881866932E-12, // N5
                    1.2345678901189872E-12, // N6
                    1.2345678901234333E-12, // N7
                    1.2345678901234569E-12, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343903676992340E-11, // N3
                    1.2345673094937837E-11, // N4
                    1.2345678895419460E-11, // N5
                    1.2345678901136932E-11, // N6
                    1.2345678901234540E-11, // N7
                    1.2345678901234568E-11, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343548405624460E-10, // N3
                    1.2345668931601494E-10, // N4
                    1.2345678862893394E-10, // N5
                    1.2345678901179284E-10, // N6
                    1.2345678901234208E-10, // N7
                    1.2345678901234568E-10, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344116839813069E-09, // N3
                    1.2345671152047544E-09, // N4
                    1.2345678888914247E-09, // N5
                    1.2345678901111520E-09, // N6
                    1.2345678901234340E-09, // N7
                    1.2345678901234568E-09, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345481081865728E-08, // N3
                    1.2345672928404383E-08, // N4
                    1.2345678895853140E-08, // N5
                    1.2345678901165731E-08, // N6
                    1.2345678901234340E-08, // N7
                    1.2345678901234569E-08, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2342934496700764E-07, // N3
                    1.2345674349489855E-07, // N4
                    1.2345678879199795E-07, // N5
                    1.2345678901230783E-07, // N6
                    1.2345678901234171E-07, // N7
                    1.2345678901234568E-07, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344680726528168E-06, // N3
                    1.2345672075753100E-06, // N4
                    1.2345678896963364E-06, // N5
                    1.2345678901126700E-06, // N6
                    1.2345678901234036E-06, // N7
                    1.2345678901234570E-06, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345612049102783E-05, // N3
                    1.2345677532721311E-05, // N4
                    1.2345678896963364E-05, // N5
                    1.2345678901182211E-05, // N6
                    1.2345678901234253E-05, // N7
                    1.2345678901234570E-05, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344121932983398E-04, // N3
                    1.2345670256763697E-04, // N4
                    1.2345678896963364E-04, // N5
                    1.2345678901226620E-04, // N6
                    1.2345678901234253E-04, // N7
                    1.2345678901234570E-04, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345314025878906E-03, // N3
                    1.2345677241683006E-03, // N4
                    1.2345678878773470E-03, // N5
                    1.2345678901226620E-03, // N6
                    1.2345678901234391E-03, // N7
                    1.2345678901234570E-03, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344360351562500E-02, // N3
                    1.2345671653747559E-02, // N4
                    1.2345678900601342E-02, // N5
                    1.2345678901169777E-02, // N6
                    1.2345678901234170E-02, // N7
                    1.2345678901234568E-02, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344360351562500E-01, // N3
                    1.2345677614212036E-01, // N4
                    1.2345678894780576E-01, // N5
                    1.2345678901147039E-01, // N6
                    1.2345678901234436E-01, // N7
                    1.2345678901234569E-01, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343750000000000E+00, // N3
                    1.2345676422119140E+00, // N4
                    1.2345678880810738E+00, // N5
                    1.2345678901183420E+00, // N6
                    1.2345678901234010E+00, // N7
                    1.2345678901234570E+00, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343750000000000E+01, // N3
                    1.2345672607421875E+01, // N4
                    1.2345678895711899E+01, // N5
                    1.2345678901183419E+01, // N6
                    1.2345678901234350E+01, // N7
                    1.2345678901234570E+01, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345312500000000E+02, // N3
                    1.2345678710937500E+02, // N4
                    1.2345678877830505E+02, // N5
                    1.2345678901206702E+02, // N6
                    1.2345678901234350E+02, // N7
                    1.2345678901234570E+02, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345000000000000E+03, // N3
                    1.2345673828125000E+03, // N4
                    1.2345678863525390E+03, // N5
                    1.2345678901225328E+03, // N6
                    1.2345678901234060E+03, // N7
                    1.2345678901234570E+03, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344000000000000E+04, // N3
                    1.2345671875000000E+04, // N4
                    1.2345678894042969E+04, // N5
                    1.2345678901195526E+04, // N6
                    1.2345678901234176E+04, // N7
                    1.2345678901234569E+04, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345600000000000E+05, // N3
                    1.2345675000000000E+05, // N4
                    1.2345678881835938E+05, // N5
                    1.2345678901195526E+05, // N6
                    1.2345678901234269E+05, // N7
                    1.2345678901234569E+05, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344320000000000E+06, // N3
                    1.2345670000000000E+06, // N4
                    1.2345678867187500E+06, // N5
                    1.2345678901214600E+06, // N6
                    1.2345678901234270E+06, // N7
                    1.2345678901234570E+06, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345344000000000E+07, // N3
                    1.2345672000000000E+07, // N4
                    1.2345678875000000E+07, // N5
                    1.2345678901123047E+07, // N6
                    1.2345678901234150E+07, // N7
                    1.2345678901234569E+07, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345344000000000E+08, // N3
                    1.2345676800000000E+08, // N4
                    1.2345678900000000E+08, // N5
                    1.2345678901171875E+08, // N6
                    1.2345678901234436E+08, // N7
                    1.2345678901234569E+08, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.234436096000000E+09, // N3
                    1.234567168000000E+09, // N4
                    1.234567888000000E+09, // N5
                    1.234567890109375E+09, // N6
                    1.234567890123413E+09, // N7
                    1.234567890123457E+09, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343836672000000E+10, // N3
                    1.2345671680000000E+10, // N4
                    1.2345678880000000E+10, // N5
                    1.2345678901125000E+10, // N6
                    1.2345678901234375E+10, // N7
                    1.2345678901234570E+10, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344675532800000E+11, // N3
                    1.2345678233600000E+11, // N4
                    1.2345678899200000E+11, // N5
                    1.2345678901200000E+11, // N6
                    1.2345678901234375E+11, // N7
                    1.2345678901234569E+11, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345346621440000E+12, // N3
                    1.2345671680000000E+12, // N4
                    1.2345678888960000E+12, // N5
                    1.2345678901120000E+12, // N6
                    1.2345678901234375E+12, // N7
                    1.2345678901234568E+12, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343736008704000E+13, // N3
                    1.2345673777152000E+13, // N4
                    1.2345678888960000E+13, // N5
                    1.2345678901120000E+13, // N6
                    1.2345678901234500E+13, // N7
                    1.2345678901234568E+13, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345453995622400E+14, // N3
                    1.2345675454873600E+14, // N4
                    1.2345678888960000E+14, // N5
                    1.2345678901145600E+14, // N6
                    1.2345678901234400E+14, // N7
                    1.2345678901234569E+14, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.234476680085504E+15, // N3
                    1.234566874398720E+15, // N4
                    1.234567889420288E+15, // N5
                    1.234567890108416E+15, // N6
                    1.234567890123456E+15, // N7
                    1.234567890123457E+15, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345316556668928E+16, // N3
                    1.2345677333921792E+16, // N4
                    1.2345678877425664E+16, // N5
                    1.2345678901149696E+16, // N6
                    1.2345678901234176E+16, // N7
                    1.2345678901234568E+16, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.23444369473667072E+17, // N3
                    1.23456738979479552E+17, // N4
                    1.23456788908474368E+17, // N5
                    1.23456789012283392E+17, // N6
                    1.23456789012344832E+17, // N7
                    1.23456789012345690E+17, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.23454924785293722E+18, // N3
                    1.23456684003898163E+18, // N4
                    1.23456788801100186E+18, // N5
                    1.23456789010815386E+18, // N6
                    1.23456789012342374E+18, // N7
                    1.23456789012345677E+18, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344366578622530E+19, // N3
                    1.2345677196482839E+19, // N4
                    1.2345678880110019E+19, // N5
                    1.2345678901182202E+19, // N6
                    1.2345678901234106E+19, // N7
                    1.2345678901234570E+19, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345267298548004E+20, // N3
                    1.2345675437264234E+20, // N4
                    1.2345678900725862E+20, // N5
                    1.2345678901155358E+20, // N6
                    1.2345678901234211E+20, // N7
                    1.2345678901234568E+20, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.23449070105778140E+21, // N3
                    1.23456726225144670E+21, // N4
                    1.23456788677405130E+21, // N5
                    1.23456789010694600E+21, // N6
                    1.23456789012345472E+21, // N7
                    1.23456789012345680E+21, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345483471330117E+22, // N3
                    1.2345672622514467E+22, // N4
                    1.2345678885332699E+22, // N5
                    1.2345678901138178E+22, // N6
                    1.2345678901234278E+22, // N7
                    1.2345678901234570E+22, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344561134126432E+23, // N3
                    1.2345678026834020E+23, // N4
                    1.2345678899406448E+23, // N5
                    1.2345678901165666E+23, // N6
                    1.2345678901234386E+23, // N7
                    1.2345678901234569E+23, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343085394600535E+24, // N3
                    1.2345667938770855E+24, // N4
                    1.2345678882517950E+24, // N5
                    1.2345678901165666E+24, // N6
                    1.2345678901234386E+24, // N7
                    1.2345678901234568E+24, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344265986221253E+25, // N3
                    1.2345677162142891E+25, // N4
                    1.2345678891525148E+25, // N5
                    1.2345678901095297E+25, // N6
                    1.2345678901234386E+25, // N7
                    1.2345678901234568E+25, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344265986221253E+26, // N3
                    1.2345675317468484E+26, // N4
                    1.2345678891525148E+26, // N5
                    1.2345678901207887E+26, // N6
                    1.2345678901234276E+26, // N7
                    1.2345678901234570E+26, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344643775539882E+27, // N3
                    1.2345676793208010E+27, // N4
                    1.2345678891525148E+27, // N5
                    1.2345678901162851E+27, // N6
                    1.2345678901234276E+27, // N7
                    1.2345678901234569E+27, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345550469904593E+28, // N3
                    1.2345673251433148E+28, // N4
                    1.2345678896136834E+28, // N5
                    1.2345678901180866E+28, // N6
                    1.2345678901234346E+28, // N7
                    1.2345678901234568E+28, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344583329248901E+29, // N3
                    1.2345671362486555E+29, // N4
                    1.2345678888758137E+29, // N5
                    1.2345678901209689E+29, // N6
                    1.2345678901234459E+29, // N7
                    1.2345678901234570E+29, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345357041773455E+30, // N3
                    1.2345677407115653E+30, // N4
                    1.2345678894661095E+30, // N5
                    1.2345678901209689E+30, // N6
                    1.2345678901234549E+30, // N7
                    1.2345678901234568E+30, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344738071753812E+31, // N3
                    1.2345676198189833E+31, // N4
                    1.2345678880493995E+31, // N5
                    1.2345678901154349E+31, // N6
                    1.2345678901234477E+31, // N7
                    1.2345678901234570E+31, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343747719722384E+32, // N3
                    1.2345674263908522E+32, // N4
                    1.2345678888049782E+32, // N5
                    1.2345678901154349E+32, // N6
                    1.2345678901234130E+32, // N7
                    1.2345678901234569E+32, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345332282972669E+33, // N3
                    1.2345672716483473E+33, // N4
                    1.2345678882005153E+33, // N5
                    1.2345678901177960E+33, // N6
                    1.2345678901234408E+33, // N7
                    1.2345678901234569E+33, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344381545022498E+34, // N3
                    1.2345669002663355E+34, // N4
                    1.2345678867498043E+34, // N5
                    1.2345678901196850E+34, // N6
                    1.2345678901234039E+34, // N7
                    1.2345678901234570E+34, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343874484782407E+35, // N3
                    1.2345672964071480E+35, // N4
                    1.2345678875235168E+35, // N5
                    1.2345678901227073E+35, // N6
                    1.2345678901234157E+35, // N7
                    1.2345678901234568E+35, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344685781166553E+36, // N3
                    1.2345674548634730E+36, // N4
                    1.2345678881424868E+36, // N5
                    1.2345678901154538E+36, // N6
                    1.2345678901234251E+36, // N7
                    1.2345678901234568E+36, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344685781166553E+37, // N3
                    1.2345669478032330E+37, // N4
                    1.2345678866569588E+37, // N5
                    1.2345678901231910E+37, // N6
                    1.2345678901234327E+37, // N7
                    1.2345678901234570E+37, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345205010852406E+38, // N3
                    1.2345675562755210E+38, // N4
                    1.2345678890338036E+38, // N5
                    1.2345678901231909E+38, // N6
                    1.2345678901234327E+38, // N7
                    1.2345678901234568E+38, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345205010852406E+39, // N3
                    1.2345678807940747E+39, // N4
                    1.2345678883999783E+39, // N5
                    1.2345678901231909E+39, // N6
                    1.2345678901234230E+39, // N7
                    1.2345678901234568E+39, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343211168858730E+40, // N3
                    1.2345672317569674E+40, // N4
                    1.2345678889070386E+40, // N5
                    1.2345678901113067E+40, // N6
                    1.2345678901234385E+40, // N7
                    1.2345678901234568E+40, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343742860057043E+41, // N3
                    1.2345678548325905E+41, // N4
                    1.2345678872844458E+41, // N5
                    1.2345678901113067E+41, // N6
                    1.2345678901234385E+41, // N7
                    1.2345678901234568E+41, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345444271891647E+42, // N3
                    1.2345676886790910E+42, // N4
                    1.2345678885825200E+42, // N5
                    1.2345678901138420E+42, // N6
                    1.2345678901234286E+42, // N7
                    1.2345678901234569E+42, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345444271891647E+43, // N3
                    1.2345678216018906E+43, // N4
                    1.2345678880632903E+43, // N5
                    1.2345678901077572E+43, // N6
                    1.2345678901234127E+43, // N7
                    1.2345678901234568E+43, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343810916530427E+44, // N3
                    1.2345673962489320E+44, // N4
                    1.2345678880632903E+44, // N5
                    1.2345678901142476E+44, // N6
                    1.2345678901234254E+44, // N7
                    1.2345678901234569E+44, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345553162249062E+45, // N3
                    1.2345675663901154E+45, // N4
                    1.2345678880632903E+45, // N5
                    1.2345678901194399E+45, // N6
                    1.2345678901234558E+45, // N7
                    1.2345678901234570E+45, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343462467386700E+46, // N3
                    1.2345672941642218E+46, // N4
                    1.2345678896583640E+46, // N5
                    1.2345678901069784E+46, // N6
                    1.2345678901233990E+46, // N7
                    1.2345678901234568E+46, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343462467386700E+47, // N3
                    1.2345675119449367E+47, // N4
                    1.2345678896583640E+47, // N5
                    1.2345678901103015E+47, // N6
                    1.2345678901234380E+47, // N7
                    1.2345678901234568E+47, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343908482290670E+48, // N3
                    1.2345678603940804E+48, // N4
                    1.2345678876166697E+48, // N5
                    1.2345678901156184E+48, // N6
                    1.2345678901234276E+48, // N7
                    1.2345678901234570E+48, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2342838046521140E+49, // N3
                    1.2345670241161354E+49, // N4
                    1.2345678865277662E+49, // N5
                    1.2345678901177451E+49, // N6
                    1.2345678901233944E+49, // N7
                    1.2345678901234570E+49, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345121642829470E+50, // N3
                    1.2345674701310394E+50, // N4
                    1.2345678882700119E+50, // N5
                    1.2345678901211480E+50, // N6
                    1.2345678901234342E+50, // N7
                    1.2345678901234569E+50, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344208204306139E+51, // N3
                    1.2345678269429626E+51, // N4
                    1.2345678882700119E+51, // N5
                    1.2345678901211480E+51, // N6
                    1.2345678901234449E+51, // N7
                    1.2345678901234568E+51, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343842828896806E+52, // N3
                    1.2345669705943470E+52, // N4
                    1.2345678893850491E+52, // N5
                    1.2345678901167923E+52, // N6
                    1.2345678901233938E+52, // N7
                    1.2345678901234568E+52, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344427429551738E+53, // N3
                    1.2345678840328703E+53, // N4
                    1.2345678876009895E+53, // N5
                    1.2345678901098234E+53, // N6
                    1.2345678901234347E+53, // N7
                    1.2345678901234570E+53, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344895110075684E+54, // N3
                    1.2345677013451656E+54, // N4
                    1.2345678897418610E+54, // N5
                    1.2345678901209737E+54, // N6
                    1.2345678901234564E+54, // N7
                    1.2345678901234570E+54, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343772676818214E+55, // N3
                    1.2345678474953294E+55, // N4
                    1.2345678886000630E+55, // N5
                    1.2345678901165136E+55, // N6
                    1.2345678901234130E+55, // N7
                    1.2345678901234570E+55, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343772676818214E+56, // N3
                    1.2345671459745434E+56, // N4
                    1.2345678876866244E+56, // N5
                    1.2345678901129455E+56, // N6
                    1.2345678901234268E+56, // N7
                    1.2345678901234569E+56, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344251581674735E+57, // N3
                    1.2345673330467530E+57, // N4
                    1.2345678884173752E+57, // N5
                    1.2345678901186545E+57, // N6
                    1.2345678901234268E+57, // N7
                    1.2345678901234570E+57, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344251581674735E+58, // N3
                    1.2345676323622884E+58, // N4
                    1.2345678895865765E+58, // N5
                    1.2345678901163709E+58, // N6
                    1.2345678901234357E+58, // N7
                    1.2345678901234569E+58, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343332084350215E+59, // N3
                    1.2345678718147166E+59, // N4
                    1.2345678867804934E+59, // N5
                    1.2345678901127170E+59, // N6
                    1.2345678901234500E+59, // N7
                    1.2345678901234570E+59, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344312881496370E+60, // N3
                    1.2345676802527740E+60, // N4
                    1.2345678897736487E+60, // N5
                    1.2345678901127171E+60, // N6
                    1.2345678901234500E+60, // N7
                    1.2345678901234570E+60, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345489838071754E+61, // N3
                    1.2345673737536658E+61, // N4
                    1.2345678885763866E+61, // N5
                    1.2345678901197323E+61, // N6
                    1.2345678901234226E+61, // N7
                    1.2345678901234569E+61, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343292852464370E+62, // N3
                    1.2345676189529524E+62, // N4
                    1.2345678871396720E+62, // N5
                    1.2345678901178616E+62, // N6
                    1.2345678901234153E+62, // N7
                    1.2345678901234569E+62, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345301525019693E+63, // N3
                    1.2345678151123816E+63, // N4
                    1.2345678886721676E+63, // N5
                    1.2345678901208548E+63, // N6
                    1.2345678901234270E+63, // N7
                    1.2345678901234570E+63, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344498055997563E+64, // N3
                    1.2345678151123816E+64, // N4
                    1.2345678886721676E+64, // N5
                    1.2345678901184602E+64, // N6
                    1.2345678901234363E+64, // N7
                    1.2345678901234570E+64, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343855280779860E+65, // N3
                    1.2345673129442428E+65, // N4
                    1.2345678896529647E+65, // N5
                    1.2345678901127134E+65, // N6
                    1.2345678901234289E+65, // N7
                    1.2345678901234570E+65, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345397941302348E+66, // N3
                    1.2345671120769872E+66, // N4
                    1.2345678872990516E+66, // N5
                    1.2345678901188434E+66, // N6
                    1.2345678901234408E+66, // N7
                    1.2345678901234570E+66, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344575189023688E+67, // N3
                    1.2345674334645961E+67, // N4
                    1.2345678879267617E+67, // N5
                    1.2345678901139394E+67, // N6
                    1.2345678901234408E+67, // N7
                    1.2345678901234569E+67, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345233390846616E+68, // N3
                    1.2345675620196396E+68, // N4
                    1.2345678874245936E+68, // N5
                    1.2345678901080546E+68, // N6
                    1.2345678901234408E+68, // N7
                    1.2345678901234570E+68, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344706829388273E+69, // N3
                    1.2345677677077093E+69, // N4
                    1.2345678898350007E+69, // N5
                    1.2345678901111931E+69, // N6
                    1.2345678901234531E+69, // N7
                    1.2345678901234570E+69, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344285580221599E+70, // N3
                    1.2345674386067978E+70, // N4
                    1.2345678885494502E+70, // N5
                    1.2345678901162148E+70, // N6
                    1.2345678901234335E+70, // N7
                    1.2345678901234570E+70, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2344959578888278E+71, // N3
                    1.2345675702471624E+71, // N4
                    1.2345678870067897E+71, // N5
                    1.2345678901081801E+71, // N6
                    1.2345678901234335E+71, // N7
                    1.2345678901234568E+71, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345498777821620E+72, // N3
                    1.2345675702471624E+72, // N4
                    1.2345678894750465E+72, // N5
                    1.2345678901178218E+72, // N6
                    1.2345678901234460E+72, // N7
                    1.2345678901234568E+72, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2345498777821621E+73, // N3
                    1.2345674017474957E+73, // N4
                    1.2345678888168447E+73, // N5
                    1.2345678901229640E+73, // N6
                    1.2345678901234460E+73, // N7
                    1.2345678901234569E+73, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343083166600244E+74, // N3
                    1.2345671321480290E+74, // N4
                    1.2345678861840374E+74, // N5
                    1.2345678901167933E+74, // N6
                    1.2345678901234140E+74, // N7
                    1.2345678901234568E+74, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    1.2343635306307988E+75, // N3
                    1.2345671321480290E+75, // N4
                    1.2345678870265358E+75, // N5
                    1.2345678901200843E+75, // N6
                    1.2345678901234268E+75, // N7
                    1.2345678901234569E+75, // N8
                });
            assertNextObservation(//
                importer, //
                new Object[] { //
                    7.23689514939071363E+75, // N3
                    7.23700514597311550E+75, // N4
                    7.23700557564726550E+75, // N5
                    7.23700557732568000E+75, // N6
                    7.23700557733223650E+75, // N7
                    7.23700557733226140E+75, // N8
                });

            // EOF
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT that has variations on date/data that was exported by SAS.
     */
    @Test
    public void testDateTimeVariations() throws IOException {

        InputStream testStream = TestUtil.getTestResource("datetime_variations.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();
            List<Variable> dataSetVariables = dataSetDescription.variables();
            assertNotNull(dataSetVariables);
            assertEquals(3, dataSetVariables.size());

            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "DATETIME", // name
                1, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "A datetime", // label
                new Format("DATETIME", 22, 3), // output format
                Justification.LEFT, // output format justification
                new Format("DATETIME", 22, 3));// input format

            TestUtil.assertVariable(//
                dataSetVariables.get(1), //
                "DATE", // name
                2, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "A date (no time)", // label
                new Format("DATE", 11, 0), // output format
                Justification.LEFT, // output format justification
                new Format("DATE", 11, 0)); // input format

            TestUtil.assertVariable(//
                dataSetVariables.get(2), //
                "TIME", // name
                3, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "A time (no date)", // label
                new Format("TIME", 12, 3), // output format
                Justification.LEFT, // output format justification
                new Format("TIME", 12, 3)); // input format

            // The first 28 values are variations on MissingValue
            assertNextObservation(importer, repeat(3, MissingValue.STANDARD));
            assertNextObservation(importer, repeat(3, MissingValue.UNDERSCORE));
            assertNextObservation(importer, repeat(3, MissingValue.A));
            assertNextObservation(importer, repeat(3, MissingValue.B));
            assertNextObservation(importer, repeat(3, MissingValue.C));
            assertNextObservation(importer, repeat(3, MissingValue.D));
            assertNextObservation(importer, repeat(3, MissingValue.E));
            assertNextObservation(importer, repeat(3, MissingValue.F));
            assertNextObservation(importer, repeat(3, MissingValue.G));
            assertNextObservation(importer, repeat(3, MissingValue.H));
            assertNextObservation(importer, repeat(3, MissingValue.I));
            assertNextObservation(importer, repeat(3, MissingValue.J));
            assertNextObservation(importer, repeat(3, MissingValue.K));
            assertNextObservation(importer, repeat(3, MissingValue.L));
            assertNextObservation(importer, repeat(3, MissingValue.M));
            assertNextObservation(importer, repeat(3, MissingValue.N));
            assertNextObservation(importer, repeat(3, MissingValue.O));
            assertNextObservation(importer, repeat(3, MissingValue.P));
            assertNextObservation(importer, repeat(3, MissingValue.Q));
            assertNextObservation(importer, repeat(3, MissingValue.R));
            assertNextObservation(importer, repeat(3, MissingValue.S));
            assertNextObservation(importer, repeat(3, MissingValue.T));
            assertNextObservation(importer, repeat(3, MissingValue.U));
            assertNextObservation(importer, repeat(3, MissingValue.V));
            assertNextObservation(importer, repeat(3, MissingValue.W));
            assertNextObservation(importer, repeat(3, MissingValue.X));
            assertNextObservation(importer, repeat(3, MissingValue.Y));
            assertNextObservation(importer, repeat(3, MissingValue.Z));

            assertNextObservation(importer, new Object[] { -1D, -1D, 86399D }); // EPOCH - 1
            assertNextObservation(importer, new Object[] { 0D, 0D, 0D }); // EPOCH
            assertNextObservation(importer, new Object[] { 1D, 1D, 1D }); // EPOCH + 1
            assertNextObservation(importer, new Object[] { -0.0010000000038417056, -1D, 86399.999D }); // EPOCH - 0.001
            assertNextObservation(importer, new Object[] { 0.001, 0D, 0.001D }); // EPOCH + 0.001 (where possible)

            assertNextObservation(importer, new Object[] { -11928470400D, -138061.0D, 0D }); // min supported date
            assertNextObservation(importer, new Object[] { 253717747199D, 2936547.0D, 0D }); // max supported date

            assertEndOfObservations(importer); // EOF
        }
    }

    /**
     * Reads an XPORT file that has the minimum number of variables supported by the format (0).
     * <p>
     * The SAS XPORT engine can generate this but reports an error when reading it. However, since it can be generated,
     * the API should do the "right thing" in presenting it how it is.
     */
    @Test
    public void testNoVariables() throws IOException {

        InputStream testStream = TestUtil.getTestResource("no_variables.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();

            // There should be no variables
            List<Variable> dataSetVariables = dataSetDescription.variables();
            assertNotNull(dataSetVariables);
            assertEquals(0, dataSetVariables.size());

            // There should be no variables, there cannot be any observations.
            // Rather than returning the empty list infinitely, we return EOF.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Reads an XPORT file that has the maximum number of variables supported by the format (9999).
     */
    @Test
    public void testMaxVariables() throws IOException {

        final int TOTAL_VARIABLES = 9999;

        InputStream testStream = TestUtil.getTestResource("max_variables.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();

            List<Variable> dataSetVariables = dataSetDescription.variables();
            assertNotNull(dataSetVariables);
            assertEquals(TOTAL_VARIABLES, dataSetVariables.size());

            // Confirm that each of the variables was read correctly.
            for (int i = 0; i < TOTAL_VARIABLES; i++) {
                TestUtil.assertVariable(//
                    dataSetVariables.get(i), //
                    String.format("V%04d", i + 1), // name
                    i + 1, // number (1-indexed)
                    VariableType.CHARACTER, // type
                    11, // length
                    "", // label
                    Format.UNSPECIFIED, // output format
                    Justification.LEFT, // output format justification
                    Format.UNSPECIFIED); // input format
            }

            // Confirm that the observations are read correctly.
            Object[] expectedObservation1 = new Object[TOTAL_VARIABLES];
            Object[] expectedObservation2 = new Object[TOTAL_VARIABLES];
            for (int i = 0; i < TOTAL_VARIABLES; i++) {
                // Start with the expected value
                StringBuilder value1 = new StringBuilder("val" + (i + 1));
                StringBuilder value2 = new StringBuilder("r2val" + (i + 1));

                // pad with spaces
                while (value1.length() != dataSetVariables.get(i).length()) {
                    value1.append(' ');
                }
                while (value2.length() != dataSetVariables.get(i).length()) {
                    value2.append(' ');
                }

                expectedObservation1[i] = value1.toString();
                expectedObservation2[i] = value2.toString();
            }
            assertNextObservation(importer, expectedObservation1);
            assertNextObservation(importer, expectedObservation2);

            // There are only two observations.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT with a variables whose length is the maximum supported for its type.
     */
    @Test
    public void testValuesWithLongLength() throws IOException {
        InputStream testStream = TestUtil.getTestResource("max_length_variable.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {
            List<Variable> variables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertEquals(3, variables.size());

            // The first character variable was specified as being 32767 long
            // in the data set, but the XPORT engine truncated it to 200.
            TestUtil.assertVariable(//
                variables.get(0), //
                "LONGTEXT", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                200, // length
                "Extra long char to be truncated by XPORT", // label
                Format.UNSPECIFIED, // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED);// input format

            TestUtil.assertVariable(//
                variables.get(1), //
                "NUMBER", // name
                2, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Numeric var as long as XPORT supports", // label
                Format.UNSPECIFIED, // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED);// input format

            // Character data that was specified as the maximum supported by XPORT.
            TestUtil.assertVariable(//
                variables.get(2), //
                "TEXT", // name
                3, // number (1-indexed)
                VariableType.CHARACTER, // type
                200, // length
                "Char var as long as XPORT supports", // label
                Format.UNSPECIFIED, // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED);// input format

            assertNextObservation(//
                importer, //
                new Object[] { //
                    "00000:ABCDEFGHIJKLMNOPQRSTUVWXYZ" + //
                        "00032:ABCDEFGHIJKLMNOPQRSTUVWXYZ" + //
                        "00064:ABCDEFGHIJKLMNOPQRSTUVWXYZ" + //
                        "00096:ABCDEFGHIJKLMNOPQRSTUVWXYZ" + //
                        "00128:ABCDEFGHIJKLMNOPQRSTUVWXYZ" + //
                        "00160:ABCDEFGHIJKLMNOPQRSTUVWXYZ" + //
                        "00192:AB", //
                    12.34, //
                    "ThisIsMoreTextForSecondVar      " + //
                        "                                " + //
                        "                                " + //
                        "                                " + //
                        "                                " + //
                        "                                " + //
                        "        ", //
                });

            assertNextObservation(//
                importer, //
                new Object[] { //
                    "This_is_long_text(butShortOne)  " + //
                        "                                " + //
                        "                                " + //
                        "                                " + //
                        "                                " + //
                        "                                " + //
                        "        ", //
                    32.0, //
                    "LastVal                         " + //
                        "                                " + //
                        "                                " + //
                        "                                " + //
                        "                                " + //
                        "                                " + //
                        "        ", //
                });

            assertEndOfObservations(importer); // EOF
        }
    }

    /**
     * Tests that default handling of the two digit year is to treat 1960 as the cutoff.
     */
    @Test
    public void testYearCutoff() throws IOException {
        InputStream testStream = TestUtil.getTestResource("yearcutoff.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            TestUtil.assertSasLibraryDescription(//
                libraryDescription, //
                "LIN X64", // OS
                "9.1", // SAS version
                LocalDateTime.of(1960, 1, 1, 0, 0, 0), // creation time
                LocalDateTime.of(1960, 12, 31, 23, 59, 59)); // modified time

            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();
            TestUtil.assertSasDataSetDescription(//
                dataSetDescription, //
                "REPORT", // name
                "Data set created in year '60", // label
                "", // type
                " LIN X64", // OS
                "9.1", // SAS version
                LocalDateTime.of(1960, 1, 1, 1, 2, 3), // creation time
                LocalDateTime.of(1960, 1, 1, 2, 3, 4)); // modified time
        }

        testStream = TestUtil.getTestResource("yearcutoff_minus_one.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            TestUtil.assertSasLibraryDescription(//
                libraryDescription, //
                "LIN X64", // OS
                "9.1", // SAS version
                LocalDateTime.of(2059, 1, 1, 0, 0, 0), // creation time
                LocalDateTime.of(2059, 12, 31, 23, 59, 59)); // modified time

            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();
            TestUtil.assertSasDataSetDescription(//
                dataSetDescription, //
                "REPORT", // name
                "Data set created in year '59", // label
                "", // type
                " LIN X64", // OS
                "9.1", // SAS version
                LocalDateTime.of(2059, 1, 1, 1, 2, 3), // creation time
                LocalDateTime.of(2059, 1, 1, 2, 3, 4)); // modified time
        }
    }

    /**
     * Tests importing an XPORT with that has user-defined formats. This should be okay. The format only matters when
     * trying to format the data.
     */
    @Test
    public void testUserDefinedFormat() throws IOException {
        InputStream testStream = TestUtil.getTestResource("user_defined_format.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // Check the variables
            List<Variable> dataSetVariables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertNotNull(dataSetVariables);
            assertEquals(4, dataSetVariables.size());

            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "ID", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                8, // length
                "A unique identifier for the person", // label
                new Format("$UPCASE", 9), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            TestUtil.assertVariable(//
                dataSetVariables.get(1), //
                "SEX", // name
                2, // number (1-indexed)
                VariableType.CHARACTER, // type
                8, // length
                "The gender at birth", // label
                new Format("$SEX", 0), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            TestUtil.assertVariable(//
                dataSetVariables.get(2), //
                "INCOME", // name
                3, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Annual income", // label
                new Format("INCOME", 0), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            TestUtil.assertVariable(//
                dataSetVariables.get(3), //
                "SMOKER", // name
                4, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Code for cigarette smoking status", // label
                new Format("SMOKER", 0, 0), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            // The data should appear as raw (unformatted) data.
            assertNextObservation(importer, new Object[] { "31ABC   ", "M       ", 45000.0, 0.0 });
            assertNextObservation(importer, new Object[] { "21JKL   ", "F       ", 200000.0, 1.0 });
            assertNextObservation(importer, new Object[] { "382JI   ", "F       ", 131000.0, 2.0 });
            assertNextObservation(importer, new Object[] { "MRX12   ", "X       ", 6000.0, 3.0 });
            assertEndOfObservations(importer);
        }
    }

    /**
     * Reads an XPORT file with a non-ASCII character in a variable label. SAS can read and write such files, but the
     * FDA submission guidelines are to disallow such labels.
     */
    @Test
    public void testReadingNonAsciiVariableLabel() throws IOException {

        InputStream testStream = TestUtil.getTestResource("variable_nonascii_label.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            List<Variable> dataSetVariables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            // Check the variable
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "TEXT", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                8, // length
                "Non-Ascii Label: copyright: \uFFFD", // label (non-ASCII characters converted to <?>
                new Format("$", 10), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            // Get the one (and only) observation
            assertNextObservation(importer, new Object[] { "mydata  " });

            // There is only one observation.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Reads an XPORT file with a non-ASCII character in a variable value. SAS can read and write such files, but the
     * FDA submission guidelines are to disallow it.
     */
    @Test
    public void testReadingNonAsciiValue() throws IOException {

        InputStream testStream = TestUtil.getTestResource("nonascii_value.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            List<Variable> dataSetVariables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            // Check the variable
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "TEXT", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                15, // length
                "Character data", // label
                new Format("$", 15), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            // Get the one (and only) observation.  Non-ASCII characters converted to <?>.
            assertNextObservation(importer, new Object[] { "MICROS \uFFFD\uFFFD      " });

            // There is only one observation.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests that reading a SAS XPORT file that has data encoded in UTF-8.
     */
    @Test
    public void testUtf8Value() throws IOException {
        InputStream testStream = TestUtil.getTestResource("utf8.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // Getting the variables should still work.
            List<Variable> dataSetVariables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            // Check the variable
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "TEXT", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                20, // length
                "Character data", // label
                new Format("$", 30), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            // Get the one (and only) observation.  Non-ASCII bytes converted to <?>.
            assertNextObservation( //
                importer, //
                new Object[] { //
                    "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD    ",
                });

            // There is only one observation.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT that contains binary data.
     */
    @Test
    public void testBinaryData() throws IOException {
        InputStream testStream = TestUtil.getTestResource("binary_character_data.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // We should see only the first data set.
            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();

            LocalDateTime expectedTimestamp = LocalDateTime.of(2017, 10, 27, 7, 41, 32);
            TestUtil.assertSasDataSetDescription(//
                dataSetDescription, //
                "TESTDATA", // name
                "Data set of binary data (all bytes)", // label
                "", // type
                "Linux", // OS
                "9.4", // SAS version
                expectedTimestamp, // creation time
                expectedTimestamp); // modified time

            List<Variable> dataSetVariables = dataSetDescription.variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size());

            // Confirm that each of the variables was read correctly.
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "BINARY", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                16, // length
                "16 bytes of binary data", // label
                new Format("$BASE64X", 24, 0), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            // Each line has 16 bytes, given in order.
            // Because Java string literals don't support quoting hex characters, the first line must be
            // done using a mixture of the Java escape character and String literal escape character
            // (The Java escape for newline cannot be used, because that would put a newline into the source code).
            assertNextObservation( //
                importer, //
                new Object[] { "\0\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000B\f\r\u000E\u000F" });
            assertNextObservation( //
                importer, //
                new Object[] {
                    "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F"//
                });
            assertNextObservation(importer, new Object[] { " !\"#$%&'()*+,-./" });
            assertNextObservation(importer, new Object[] { "0123456789:;<=>?" });
            assertNextObservation(importer, new Object[] { "@ABCDEFGHIJKLMNO" });
            assertNextObservation(importer, new Object[] { "PQRSTUVWXYZ[\\]^_" });
            assertNextObservation(importer, new Object[] { "`abcdefghijklmno" });
            assertNextObservation(importer, new Object[] { "pqrstuvwxyz{|}~\u007F" });

            // After the ASCII set, all characters are mapped to the REPLACE CHARACTER.
            Object[] nonAsciiString = new Object[] { //
                "\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD" //
            };
            assertNextObservation(importer, nonAsciiString);
            assertNextObservation(importer, nonAsciiString);
            assertNextObservation(importer, nonAsciiString);
            assertNextObservation(importer, nonAsciiString);
            assertNextObservation(importer, nonAsciiString);
            assertNextObservation(importer, nonAsciiString);
            assertNextObservation(importer, nonAsciiString);
            assertNextObservation(importer, nonAsciiString);

            assertEndOfObservations(importer);
        }
    }

    private <T extends Throwable> void runImportMalformedXportTest(String resourceName, Class<T> expectedExceptionClass,
        String expectedExceptionMessage, String expectedExceptionCauseMessage) {
        // Read the file
        InputStream testStream = TestUtil.getTestResource(resourceName);

        // importing should throw an exception
        Throwable exception = assertThrows(//
            expectedExceptionClass, //
            () -> SasLibraryDescription.importTransportDataSet(testStream));

        // Confirm that the correct exception was thrown.
        assertEquals(expectedExceptionMessage, exception.getMessage());
        if (expectedExceptionCauseMessage != null) {
            assertNotNull(exception.getCause());
            assertEquals(expectedExceptionCauseMessage, exception.getCause().getMessage());
        } else {
            assertNull(exception.getCause());
        }
    }

    /**
     * Tests that reading a SAS V8 XPORT file throws a clear exception that the format is not supported.
     */
    @Test
    public void testReadingV8Xport() {
        runImportMalformedXportTest(//
            "v8xport.xpt", //
            UnsupportedTransportFileException.class, //
            "SAS V8 XPORT files are not supported", //
            null);
    }

    /**
     * Tests that reading a SAS CPORT file throws a clear exception that the format is not supported.
     */
    @Test
    public void testReadingCport() {
        runImportMalformedXportTest(//
            "cport.xpt", //
            UnsupportedTransportFileException.class, //
            "SAS CPORT files are not supported", //
            null);
    }

    /**
     * Tests importing an XPORT that has multiple data sets. This is not supported.
     */
    @Test
    public void testMultipleDatasets() throws IOException {
        InputStream testStream = TestUtil.getTestResource("multiple_datasets.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // We should see only the first data set.
            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();

            LocalDateTime expectedTimestamp = LocalDateTime.of(2017, 9, 27, 10, 27, 42);
            TestUtil.assertSasDataSetDescription(//
                dataSetDescription, //
                "A", // name
                "Data set A", // label
                "", // type
                "Linux", // OS
                "9.4", // SAS version
                expectedTimestamp, // creation time
                expectedTimestamp); // modified time

            List<Variable> dataSetVariables = dataSetDescription.variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size());

            // Confirm that each of the variables was read correctly.
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "A1", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                20, // length
                "Text in dataset A", // label
                new Format("$", 20, 0), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            // The first data set has one observation.
            assertNextObservation(importer, new Object[] { "data-in-dataset-a   " });

            // As we go to the next data set, an exception should be thrown
            assertNextObservationsThrowsException(importer, MultipleDataSetsNotSupportedException.class, null);
        }
    }

    /**
     * Tests importing an XPORT that has multiple data sets where the first one has no observations. There is specific
     * code to handle this case. This is not supported.
     */
    @Test
    public void testBlankFirstDataset() throws IOException {
        InputStream testStream = TestUtil.getTestResource("blank_first_dataset.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // We should see only the first data set.
            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();

            LocalDateTime expectedTimestamp = LocalDateTime.of(2017, 9, 27, 14, 28, 12);
            TestUtil.assertSasDataSetDescription(//
                dataSetDescription, //
                "BLANK", // name
                "Data set with no observations", // label
                "", // type
                "Linux", // OS
                "9.4", // SAS version
                expectedTimestamp, // creation time
                expectedTimestamp); // modified time

            List<Variable> dataSetVariables = dataSetDescription.variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size());

            // Confirm that variable of the first data set was processed correctly.
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "VAR", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                20, // length
                "Some variable", // label
                new Format("$", 20, 0), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            // The first data set has no observations, so the first observation that is
            // read should pass to the next data set and throw a "not supported" exception.
            assertNextObservationsThrowsException(importer, MultipleDataSetsNotSupportedException.class, null);
        }
    }

    /**
     * Tests that reading a comma-separated data file throws a malformed file exception.
     */
    @Test
    public void testReadingCsv() {
        String sampleData = "" //
            + "ITEM,MATERIAL,COST,PROFIT\n" //
            + "SHIRTS,COTTON,\"$2,256,354.00\",\"$83,952,175.00\"\n" //
            + "TIES,SILK,\"$498,678.00\",\"$2,349,615.00\"\n"//
            + "SUITS,SILK,\"$9,482,146.00\",\"$69,839,563.00\"\n" //
            + "BELTS,LEATHER,\"$7,693.00\",\"$14,893.00\"\n" //
            + "SHOES,LEATHER,\"$7,936,712.00\",\"$22,964.00\"\n";

        InputStream csvInputStream = new ByteArrayInputStream(sampleData.getBytes(StandardCharsets.US_ASCII));
        Exception exception = assertThrows(//
            MalformedTransportFileException.class, //
            () -> SasLibraryDescription.importTransportDataSet(csvInputStream), //
            "No exception was thrown about not supporting the format");
        assertEquals("First record indicates this is not SAS V5 XPORT format", exception.getMessage());
    }

    /**
     * Tests importing an XPORT that is truncated within the first header throws a malformed file exception.
     */
    @Test
    public void testTruncatedInFirstHeader() {
        runImportMalformedXportTest(//
            "truncated_libraryheader.xpt", //
            MalformedTransportFileException.class, //
            "missing library header record", //
            null);
    }

    /**
     * Tests importing an XPORT has an inconsequential corruption in the library header. SAS does not generate such a
     * file, but ignores the corruption when reading it.
     */
    @Test
    public void testCorruptionInFirstHeader() {
        runImportMalformedXportTest(//
            "malformed_corrupt_libraryheader.xpt", //
            MalformedTransportFileException.class, //
            "First record indicates this is not SAS V5 XPORT format", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated within the second header record.
     */
    @Test
    public void testTruncatedInFirstRealHeader() {
        runImportMalformedXportTest(//
            "truncated_firstrealheader.xpt", //
            MalformedTransportFileException.class, //
            "missing first real header record", //
            null);
    }

    /**
     * Tests importing an XPORT has an inconsequential corruption in the "first real header". SAS does not generate such
     * a file, but ignores the corruption when reading it.
     */
    @Test
    public void testCorruptionInFirstRealHeader() {
        runImportMalformedXportTest(//
            "malformed_corrupt_firstrealheader.xpt", //
            MalformedTransportFileException.class, //
            "corrupt blanks region in REAL_HEADER", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated within the library modification time header.
     */
    @Test
    public void testTruncatedInSecondRealHeader() {
        runImportMalformedXportTest(//
            "truncated_librarymodifyheader.xpt", //
            MalformedTransportFileException.class, //
            "missing secondary library header record", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated within the first data set (member) header.
     */
    @Test
    public void testTruncatedInMemberHeader() {
        runImportMalformedXportTest(//
            "truncated_memberheader.xpt", //
            MalformedTransportFileException.class, //
            "missing member header record", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated within the second data set (member) header.
     */
    @Test
    public void testTruncatedInMemberDescriptor() {
        runImportMalformedXportTest(//
            "truncated_memberdescriptor.xpt", //
            MalformedTransportFileException.class, //
            "missing descriptor header record", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated within the first member header data record.
     */
    @Test
    public void testTruncatedInMemberHeaderData1() {
        runImportMalformedXportTest(//
            "truncated_memberheaderdata1.xpt", //
            MalformedTransportFileException.class, //
            "missing first member header data record", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated within the second member header data record.
     */
    @Test
    public void testTruncatedInMemberHeaderData2() {
        runImportMalformedXportTest(//
            "truncated_memberheaderdata2.xpt", //
            MalformedTransportFileException.class, //
            "missing second member header data record", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated within the NAMESTR header record.
     */
    @Test
    public void testTruncatedInNamestrHeader() {
        runImportMalformedXportTest(//
            "truncated_namestrheader.xpt", //
            MalformedTransportFileException.class, //
            "missing namestr header record", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated within the first NAMESTR record.
     */
    @Test
    public void testTruncatedInNamestr1() {
        runImportMalformedXportTest(//
            "truncated_namestr1.xpt", //
            MalformedTransportFileException.class, //
            "missing namestr record 1", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated within the first NAMESTR record.
     */
    @Test
    public void testTruncatedInNamestr2() {
        runImportMalformedXportTest(//
            "truncated_namestr2.xpt", //
            MalformedTransportFileException.class, //
            "missing namestr record 2", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated between the padding of the NAMESTR record and the observation header.
     */
    @Test
    public void testTruncatedInNamestrPadding() {
        runImportMalformedXportTest(//
            "truncated_namestrpadding.xpt", //
            MalformedTransportFileException.class, //
            "missing NAMESTR record padding", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated in the observation header.
     */
    @Test
    public void testTruncatedInObservationHeader() {
        runImportMalformedXportTest(//
            "truncated_observationheader.xpt", //
            MalformedTransportFileException.class, //
            "missing observation header record", //
            null);
    }

    /**
     * Tests importing an XPORT that is truncated within an observation.
     */
    @Test
    public void testTruncatedObservation() throws IOException {
        InputStream testStream = TestUtil.getTestResource("truncated_observation.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {
            // try to read the truncated observation.
            assertNextObservationsThrowsException(//
                importer, //
                MalformedTransportFileException.class, //
                "observation truncated");
        }
    }

    /**
     * Tests importing an XPORT that is truncated within an observation at a record boundary (80 byte multiple).
     */
    @Test
    public void testTruncatedObservationAtRecordBoundary() throws IOException {
        InputStream testStream = TestUtil.getTestResource("truncated_observation_at_record.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // The first observation can be read.
            assertNextObservation( //
                importer, //
                new Object[] { Double.valueOf(15.2), Double.valueOf(5.0), "first row      " });

            // The second observation can be read.
            assertNextObservation( //
                importer, //
                new Object[] { Double.valueOf(0.0), Double.valueOf(10000.0), "second row     " });

            // Now try to read the truncated observation.
            assertNextObservationsThrowsException(//
                importer, //
                MalformedTransportFileException.class, //
                "observation truncated");
        }
    }

    /**
     * Tests importing an XPORT that is truncated within an observation that is larger than 80 bytes. There is a special
     * case to handle this.
     */
    @Test
    public void testTruncatedBigObservation() throws IOException {
        InputStream testStream = TestUtil.getTestResource("truncated_big_observation.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // The first observation is complete.
            String value1 = "This is TEXT1 in observation #1.                                                ";
            String value2 = "This is TEXT2 in observation #1.                                                ";
            String value3 = "This is TEXT3 in observation #1.                                                ";
            assertEquals(80, value1.length(), "TEST BUG");
            assertEquals(80, value2.length(), "TEST BUG");
            assertEquals(80, value3.length(), "TEST BUG");
            assertNextObservation(importer, new Object[] { value1, value2, value3 });

            // Now try to read the truncated observation.
            assertNextObservationsThrowsException(//
                importer, //
                MalformedTransportFileException.class, //
                "observation truncated");
        }
    }

    /**
     * Tests importing an XPORT that, in its creation had a record of missing values for character data. This is
     * ambiguous, but SAS treats the first one as part of an observation and the rest as padding.
     * <p>
     * This is a regression test for a specific line of code in the unit-under-test.
     */
    @Test
    public void testSingleBlankRecord() throws IOException {
        InputStream testStream = TestUtil.getTestResource("single_blank_record.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // Read the first observation.
            assertNextObservation(importer, new Object[] { " " });

            // Even though the dataset that generated this XPORT has three missing
            // values after this, because the missing values look like padding, SAS
            // treats it as the final record padding.  Therefore, we do the same
            // and report the EOF.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT that, in its creation, ended in a series of missing values for character data. This is
     * ambiguous, but SAS treats it as padding and does not return it as an observation.
     */
    @Test
    public void testMissingValuesAtEndOfFile() throws IOException {
        InputStream testStream = TestUtil.getTestResource("missing_values_or_padding.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // Read the one (and only) non-missing value in the file.
            assertNextObservation(importer, new Object[] { "TEXT    " });

            // Even though the dataset that generated this XPORT has missing
            // values after this, because the missing values look like padding, SAS
            // treats it as the final record padding.  Therefore, we do the same
            // and report the EOF.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT has a full record of missing values for a character variable (all spaces) and has
     * missing values at the end of the file (this is ambiguous, but SAS treats it as padding).
     */
    @Test
    public void testRecordOfMissingValues() throws IOException {
        InputStream testStream = TestUtil.getTestResource("record_of_missing_values.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // The first 80 bytes (10 records) have only missing values.
            for (int i = 0; i < 10; i++) {
                assertNextObservation(importer, new Object[] { "        " }); // all values are missing
            }

            // Read the one (and only) non-missing value in the file.
            assertNextObservation(importer, new Object[] { "TEXT    " });

            // The rest of the record (9 more observations) are also missing values.
            for (int i = 0; i < 9; i++) {
                assertNextObservation(importer, new Object[] { "        " });
            }

            // The final record in this file is nothing but ASCII blanks.
            // By the specification, this CANNOT be padding because you can only pad
            // out the remainder of a record, not create an entire record of padding.
            // However, SAS treats this entire record as padding.

            // BUG?: we follow the specification and assume that one observation was
            // written.  Should we do what SAS does instead?
            assertNextObservation(importer, new Object[] { "        " });

            // Even though the dataset that generated this XPORT has missing
            // values after this, because the missing values look like padding, SAS
            // treats it as the final record padding.  Therefore, we do the same
            // and report the EOF.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT has a full record of numeric data whose binary representation happens to be eight
     * spaces, including nothing but these numbers for the final record. (This is ambiguous, but SAS treats it as
     * padding).
     */
    @Test
    public void testRecordOfNumericSpaces() throws IOException {
        InputStream testStream = TestUtil.getTestResource("record_of_numeric_spaces.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            final Object[] numericDataThatLooksLikeSpaces = { 3.687825414344431E-40 };

            // The first 80 bytes (10 records) have only missing values.
            for (int i = 0; i < 10; i++) {
                assertNextObservation(importer, numericDataThatLooksLikeSpaces);
            }

            // Read the one (and only) value that isn't the special number.
            assertNextObservation(importer, new Object[] { MissingValue.STANDARD });

            // The rest of the record and the entire next record have the same number.
            for (int i = 0; i < 9; i++) {
                assertNextObservation(importer, numericDataThatLooksLikeSpaces);
            }

            // The final record in this file is nothing but the numeric data that
            // looks like ASCII blanks. By the specification, this CANNOT be padding
            // because you can only pad out the remainder of a record, not create an
            // entire record of padding.
            // However, SAS treats this entire record as padding.

            // BUG?: we follow the specification.  Should we do what SAS does instead?
            assertNextObservation(importer, numericDataThatLooksLikeSpaces);

            // Even though the dataset that generated this XPORT has missing
            // values after this, because the missing values look like padding, SAS
            // treats it as the final record padding.  Therefore, we do the same
            // and report the EOF.
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT has an observation full of padding (missing values), followed by blanks to the end of a
     * record and the following record is truncated.
     * <p>
     * This is a test for a specific branch condition in the unit under test.
     * </p>
     */
    @Test
    public void testTruncatedMidRecordAfterBlankData() throws IOException {
        InputStream testStream = TestUtil.getTestResource("truncated_after_blank_observation.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // The variable is 20 bytes (1/4 of a record).
            // The first record is blank, so there are four missing values.
            assertNextObservation(importer, new Object[] { "                    " });
            assertNextObservation(importer, new Object[] { "                    " });
            assertNextObservation(importer, new Object[] { "                    " });
            assertNextObservation(importer, new Object[] { "                    " });

            // The subsequent record is truncated by one byte, so the XPORT is provably malformed.
            // SAS reads one more missing value from the XPORT file and exits without error.
            // The unit-under-test throws a "malformed file" exception, which I think is better behavior.
            assertNextObservationsThrowsException(//
                importer, //
                MalformedTransportFileException.class, //
                "observation truncated");
        }
    }

    /**
     * Tests importing an XPORT whose final observation has a missing value that spans the boundary between two records.
     * This tests that the missing value isn't interpreted as padding (and thus ignored)
     */
    @Test
    public void testMissingValueSpansFinalRecordBoundary() throws IOException {
        InputStream testStream = TestUtil.getTestResource("missing_value_spans_last_record_boundary.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // The first observation is a missing value
            assertNextObservation(importer, new Object[] { "                                   " });

            // The second observation is some text
            assertNextObservation(importer, new Object[] { "next value spans record boundary   " });

            // The third observation is a missing value and spans the record boundary.
            assertNextObservation(importer, new Object[] { "                                   " });

            // EOF
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT which has two consecutive records (80 byte blocks) that are filled with ASCII blanks.
     * This tests that the missing value isn't interpreted as padding (and thus ignored).
     */
    @Test
    public void testConsecutiveRecordsOfMissingValues() throws IOException {
        InputStream testStream = TestUtil.getTestResource("two_records_with_blanks.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // The first two records have observations with blank values.
            assertNextObservation(importer, new Object[] { "                                        " });
            assertNextObservation(importer, new Object[] { "                                        " });
            assertNextObservation(importer, new Object[] { "                                        " });
            assertNextObservation(importer, new Object[] { "                                        " });

            // The third record has an observation with some text.
            assertNextObservation(importer, new Object[] { "Value between records with blanks       " });
            assertNextObservation(importer, new Object[] { "                                        " });

            // The fourth record has two observations of missing values.
            assertNextObservation(importer, new Object[] { "                                        " });
            assertNextObservation(importer, new Object[] { "                                        " });

            // The fifth record has two observations of missing values, but SAS
            // interprets the last one as padding.
            assertNextObservation(importer, new Object[] { "                                        " });
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT which has 79 consecutive values that are filled with ASCII blanks, followed by one that
     * is not. This tests a boundary condition in the "final padding" detection logic.
     */
    @Test
    public void testRecordsOfMissingValues() throws IOException {
        InputStream testStream = TestUtil.getTestResource("not_missing_at_end_of_record.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // The first value exists.
            assertNextObservation(importer, new Object[] { "A" });

            // The next 78 observations have blank values.
            for (int i = 0; i < 78; i++) {
                assertNextObservation(importer, new Object[] { " " });
            }

            // The final (80th) observation has a value.
            assertNextObservation(importer, new Object[] { "Z" });

            // EOF
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT with an observation size that's exactly 80 bytes. This tests a boundary condition in the
     * "final padding" detection logic.
     */
    @Test
    public void test80ByteObservation() throws IOException {
        InputStream testStream = TestUtil.getTestResource("80_byte_observation.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // The first observation is some explanatory text.
            String value = "This is an observation.  The next observation is missing.  After that is EOF.   ";
            assertEquals(80, value.length(), "TEST BUG");
            assertNextObservation(importer, new Object[] { value });

            // The second observation should be a missing value.
            String missing = "                                                                                ";
            assertNextObservation(importer, new Object[] { missing });

            // EOF
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT with an observation size that's one byte larger than 80 bytes. This tests a boundary
     * condition in the "final padding" detection logic.
     */
    @Test
    public void test81ByteObservation() throws IOException {
        InputStream testStream = TestUtil.getTestResource("81_byte_observation.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // The first observation is some explanatory text.
            String value = "This is an observation. After this value is EOF.                                 ";
            assertEquals(81, value.length(), "TEST BUG");
            assertNextObservation(importer, new Object[] { value });

            // EOF
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT with a malformed library creation date. SAS cannot generate such an XPORT. For the most
     * part, SAS ignores this field, but in the places where it reads it, it shows the date as a missing value.
     */
    @Test
    public void testLibraryWithMalformedCreationDate() {
        runImportMalformedXportTest(//
            "malformed_library_createtime.xpt", //
            MalformedTransportFileException.class, //
            "malformed date: 23AUG17:8:35:00", //
            null);
    }

    /**
     * Tests importing an XPORT with a malformed data set "last modified" date. SAS cannot generate such an XPORT. For
     * the most part, SAS ignores this field, but in the places where it reads it, it shows the date as a missing
     * value.
     */
    @Test
    public void testLibraryWithMalformedModificationDate() {
        runImportMalformedXportTest(//
            "malformed_library_modifiedtime.xpt", //
            MalformedTransportFileException.class, //
            "malformed date: 32AUG17:08:56:39", //
            null);
    }

    /**
     * Tests importing an XPORT with an OS field in the library header that has a non-ASCII character. SAS does not
     * generate such a field and completely ignores this field when reading data sets.
     */
    @Test
    public void testLibraryOperatingSystemWithNonAsciiCharacter() {
        runImportMalformedXportTest(//
            "malformed_library_nonascii_os.xpt", //
            MalformedTransportFileException.class, //
            "Data set is malformed", //
            "library operating system must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests importing an XPORT with a SAS Version field in the library header that has a non-ASCII character. SAS does
     * not generate such a field and completely ignores this field when reading data sets.
     */
    @Test
    public void testLibrarySasVersionWithNonAsciiCharacter() {
        runImportMalformedXportTest(//
            "malformed_library_nonascii_version.xpt", //
            MalformedTransportFileException.class, //
            "Data set is malformed", //
            "library SAS version must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests importing an XPORT with a malformed data set creation date. SAS cannot generate such an XPORT. For the most
     * part, SAS ignores this field, but in the places where it reads it, it shows the date as a missing value.
     */
    @Test
    public void testDataSetWithMalformedCreationDate() {
        runImportMalformedXportTest(//
            "malformed_dataset_createtime.xpt", //
            MalformedTransportFileException.class, //
            "malformed date: 23AUX17:08:56:39", //
            null);
    }

    /**
     * Tests importing an XPORT with a malformed data set "last modified" date. SAS cannot generate such an XPORT. For
     * the most part, SAS ignores this field, but in the places where it reads it, it shows the date as a missing
     * value.
     */
    @Test
    public void testDataSetWithMalformedModificationDate() {
        runImportMalformedXportTest(//
            "malformed_dataset_modifiedtime.xpt", //
            MalformedTransportFileException.class, //
            "malformed date: 23AUG17:08:56939", //
            null);
    }

    /**
     * Tests that reading a SAS XPORT file whose NAMESTR header claims that it has a negative number of variables. SAS
     * cannot generate such a file. SAS reports an error when converting the file to a CSV, but does not provide any
     * details.
     */
    @Test
    public void testNegativeNumberOfVariables() {
        runImportMalformedXportTest(//
            "malformed_variables_negative.xpt", //
            MalformedTransportFileException.class, //
            "malformed NAMESTR header record", //
            "Illegal variable count in NAMESTR HEADER RECORD: -100");
    }

    /**
     * Tests that reading a SAS XPORT file whose NAMESTR header has a variable count that is padded with spaces. SAS
     * cannot generate such a file. SAS reads this file, ignoring the spaces. We also ignore the spaces, but it would be
     * acceptable to consider it malformed.
     */
    @Test
    public void testPaddedNumberOfVariables() throws IOException {
        InputStream testStream = TestUtil.getTestResource("malformed_variables_padded.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {
            List<Variable> variables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertEquals(1, variables.size());

            TestUtil.assertVariable(//
                variables.get(0), //
                "NUMBER", // name
                1, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "A number", // label
                new Format("", 5, 2), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED);// input format

            // Despite the corruption, data should still be readable.
            assertNextObservation(importer, new Object[] { Double.valueOf(1.0) });

            assertEndOfObservations(importer); // EOF
        }
    }

    /**
     * Tests that reading a SAS XPORT file whose NAMESTR header claims that it has a "1XXX" total of variables. SAS
     * cannot generate such a file. SAS reads this file, ignoring the garbage appended to the variable count.
     */
    @Test
    public void testBadNumberOfVariables() {
        runImportMalformedXportTest(//
            "malformed_variables_badnum.xpt", //
            MalformedTransportFileException.class, //
            "malformed NAMESTR header record", //
            "Unparsable variable count in NAMESTR HEADER RECORD: 1XXX");
    }

    /**
     * Tests that reading a SAS XPORT file whose NAMESTR header claims that it has a "NAN!" total of variables. SAS
     * cannot generate such a file. SAS reads the file as if it had zero variables. That is, it treats the malformed
     * number as 0.
     */
    @Test
    public void testGarbageNumberOfVariables() {
        runImportMalformedXportTest(//
            "malformed_variables_nonnumeric_count.xpt", //
            MalformedTransportFileException.class, //
            "malformed NAMESTR header record", //
            "Unparsable variable count in NAMESTR HEADER RECORD: NAN!");
    }

    /**
     * Tests that reading a SAS XPORT file whose NAMESTR header claims that it has 1 NAMESTR record, but it, in fact,
     * has 4. SAS cannot generate such a file. SAS reads the file as if it had 1 variable and no observations.
     */
    @Test
    public void testTooManyNamestrRecords() throws IOException {
        InputStream testStream = TestUtil.getTestResource("malformed_variables_count_too_small.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {
            List<Variable> variables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertEquals(1, variables.size());

            TestUtil.assertVariable(//
                variables.get(0), //
                "ITEM", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                6, // length
                "The item of clothing that was sold", // label
                new Format("$UPCASE", 9), // output format
                Justification.LEFT, // output format justification
                new Format("$CHAR", 0));// input format

            // BUG: the observations are corrupt
            // It would be better to have no observations (like SAS) or to reject the file
            // as malformed.
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 1 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 8, 0, 3 }) });
            assertNextObservation(importer, new Object[] { "COST  " });
            assertNextObservation(importer, new Object[] { "  Doll" });
            assertNextObservation(importer, new Object[] { "ars to" });
            assertNextObservation(importer, new Object[] { " creat" });
            assertNextObservation(importer, new Object[] { "e item" });
            assertNextObservation(importer, new Object[] { "/mater" });
            assertNextObservation(importer, new Object[] { "ial   " });
            assertNextObservation(importer, new Object[] { "      " });
            assertNextObservation(importer, new Object[] { "DOLLAR" });
            assertNextObservation(importer, new Object[] { new String(new char[] { ' ', ' ', 0, 15, 0, 2 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 'C', 'O' }) });
            assertNextObservation(importer, new Object[] { "MMA   " });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 10, 0, 2, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, '\r', 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 1, 0, 0, 0, 8 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 4, 'P', 'R', 'O', 'F' }) });
            assertNextObservation(importer, new Object[] { "IT  Do" });
            assertNextObservation(importer, new Object[] { "llars " });
            assertNextObservation(importer, new Object[] { "of pro" });
            assertNextObservation(importer, new Object[] { "fit fo" });
            assertNextObservation(importer, new Object[] { "r item" });
            assertNextObservation(importer, new Object[] { "/mater" });
            assertNextObservation(importer, new Object[] { "ial   " });
            assertNextObservation(importer, new Object[] { "  DOLL" });
            assertNextObservation(importer, new Object[] { new String(new char[] { 'A', 'R', ' ', ' ', 0, 15 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 2, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { "DOLLAR" });
            assertNextObservation(importer, new Object[] { new String(new char[] { ' ', ' ', 0, 10, 0, 2 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 21, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 'H', 'E', 'A', 'D' }) });
            assertNextObservation(importer, new Object[] { "ER REC" });
            assertNextObservation(importer, new Object[] { "ORD***" });
            assertNextObservation(importer, new Object[] { "****OB" });
            assertNextObservation(importer, new Object[] { "S     " });
            assertNextObservation(importer, new Object[] { "HEADER" });
            assertNextObservation(importer, new Object[] { " RECOR" });
            assertNextObservation(importer, new Object[] { "D!!!!!" });
            assertNextObservation(importer, new Object[] { "!!0000" });
            assertNextObservation(importer, new Object[] { "000000" });
            assertNextObservation(importer, new Object[] { "000000" });
            assertNextObservation(importer, new Object[] { "000000" });
            assertNextObservation(importer, new Object[] { "000000" });
            assertNextObservation(importer, new Object[] { "00  sh" });
            assertNextObservation(importer, new Object[] { "irtsco" });
            assertNextObservation(importer, new Object[] { "tton F" });
            assertNextObservation(importer, new Object[] { "\"m\uFFFD\0\0\0" });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 'G', 'P', 16, 34, '\uFFFD' }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 't', 'i', 'e', }) });
            assertNextObservation(importer, new Object[] { "s  sil" });
            assertNextObservation(importer, new Object[] { "k   Ey" });
            assertNextObservation(importer, new Object[] { new String(new char[] { '\uFFFD', 96, 0, 0, 0, 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 'F', '#', '\uFFFD', 47, 0, 0 }) });
            assertNextObservation(importer, new Object[] { "\0\0suit" });
            assertNextObservation(importer, new Object[] { "s silk" });
            assertNextObservation(importer, new Object[] { "   F\uFFFD\uFFFD" });
            assertNextObservation(importer, new Object[] { new String(new char[] { '\uFFFD', 0, 0, 0, 0, 'G' }) });
            assertNextObservation(importer, new Object[] { "B\uFFFD\uFFFD\uFFFD\0\0" });
            assertNextObservation(importer, new Object[] { "\0belts" });
            assertNextObservation(importer, new Object[] { " leath" });
            assertNextObservation(importer, new Object[] { new String(new char[] { 'e', 'r', 'D', 30, 13, 0 }) });
            assertNextObservation(importer, new Object[] { "\0\0\0\0D:" });
            assertNextObservation(importer, new Object[] { "-\0\0\0\0\0" });
            assertNextObservation(importer, new Object[] { "shoes " });
            assertNextObservation(importer, new Object[] { "leathe" });
            assertNextObservation(importer, new Object[] { new String(new char[] { 'r', 'F', 'y', 26, '\uFFFD', 0 }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 'D', 'Y', '\uFFFD' }) });
            assertNextObservation(importer, new Object[] { new String(new char[] { 0, 0, 0, 0, 0, ' ' }) });

            assertEndOfObservations(importer); // EOF
        }
    }

    /**
     * Tests that reading a SAS XPORT file whose NAMESTR header claims that it has 10 NAMESTR records, but it, in fact,
     * has 4. SAS cannot generate such a file. SAS can read/write the file as if it had 1 variable and no observations
     * but when attempting to export it to a CSV, appears to reinterpret the HEADER after the NAMESTR records as the
     * header of a new data set. In short, SAS has no special logic to correct for this, and it handles it however its
     * logic happens to handle it.
     */
    @Test
    public void testTooFewNamestrRecords() {
        runImportMalformedXportTest(//
            "malformed_variables_count_too_large.xpt", //
            MalformedTransportFileException.class, //
            "Variable #5 is malformed", //
            "Unexpected type code in NAMESTR field: 18501");
    }

    /**
     * Tests importing an XPORT that has multiple variables which refer to the regions within the observation. SAS
     * probably cannot generate such a file, but one could be created by an external tool to save space the same data
     * within an observation needed to be formatted in multiple ways. SAS can read such a file without any problem. If
     * it reads in and then writes out an XPORT, it will "correct" the variables by duplicating the data and changing
     * the variable to use non-overlapping regions.
     */
    @Test
    public void testVariablesWithOverlappingRegionsInObservation() throws IOException {

        InputStream testStream = TestUtil.getTestResource("reused-data.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {
            List<Variable> variables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertEquals(4, variables.size());

            TestUtil.assertVariable(//
                variables.get(0), //
                "ITEMUP", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                6, // length
                "The item of clothing that was sold", // label
                new Format("$UPCASE", 9), // output format
                Justification.LEFT, // output format justification
                new Format("$CHAR", 0));// input format

            TestUtil.assertVariable(//
                variables.get(1), //
                "ITEM1ST", // name
                2, // number (1-indexed)
                VariableType.CHARACTER, // type
                1, // length (first char of ITEMUP)
                "First char of ITEMUP", // label
                new Format("$", 1, 0), // output format
                Justification.LEFT, // output format justification
                new Format("$", 9, 0)); // input format

            TestUtil.assertVariable(//
                variables.get(2), //
                "NUMBER", // name
                3, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Non-consecutive data", // label
                Format.UNSPECIFIED, // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            TestUtil.assertVariable(//
                variables.get(3), //
                "DOLLAR", // name
                4, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Variable formatted as dollars", // label
                new Format("DOLLAR", 15, 2), // output format
                Justification.LEFT, // output format justification
                new Format("DOLLAR", 10, 2)); // input format

            // Despite the corruption, data should still be readable.
            assertNextObservation(importer, new Object[] { "shirts", "s", 83952175.0, 83952175.0 });
            assertNextObservation(importer, new Object[] { "ties  ", "t", 2349615.0, 2349615.0 });
            assertNextObservation(importer, new Object[] { "suits ", "s", 69839563.0, 69839563.0 });
            assertNextObservation(importer, new Object[] { "belts ", "b", 14893.0, 14893.0 });
            assertNextObservation(importer, new Object[] { "shoes ", "s", 22964.0, 22964.0 });

            assertEndOfObservations(importer); // EOF
        }
    }

    /**
     * Tests importing an XPORT that has variables which are listed from right-to-left in the observation (so that the
     * first variable listed reads data from the last part of the observation). SAS won't generate such a file, but
     * there is nothing in TS-140 that would prohibit it. SAS garbles the even the first observation when reading this,
     * which makes me think that SAS does not honor the "POSITION OF VALUE IN OBSERVATION" field of NAMESTR.
     */
    @Test
    public void testVariablesInReverseOrderInObservation() throws IOException {

        InputStream testStream = TestUtil.getTestResource("malformed_variables_reverse_order.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {
            List<Variable> variables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertEquals(4, variables.size());

            TestUtil.assertVariable(//
                variables.get(0), //
                "PROFIT", // name
                4, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Dollars of profit for item/material", // label
                new Format("DOLLAR", 15, 2), // output format
                Justification.LEFT, // output format justification
                new Format("DOLLAR", 10, 2)); // input format

            TestUtil.assertVariable(//
                variables.get(1), //
                "COST", // name
                3, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Dollars to create item/material", // label
                new Format("DOLLAR", 15, 2), // output format
                Justification.LEFT, // output format justification
                new Format("COMMA", 10, 2)); // input format

            TestUtil.assertVariable(//
                variables.get(2), //
                "MATERIAL", // name
                2, // number (1-indexed)
                VariableType.CHARACTER, // type
                7, // length
                "What the item is made of", // label
                new Format("$UPCASE", 9, 0), // output format
                Justification.LEFT, // output format justification
                new Format("$", 9, 0)); // input format

            TestUtil.assertVariable(//
                variables.get(3), //
                "ITEM", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                6, // length
                "The item of clothing that was sold", // label
                new Format("$UPCASE", 9), // output format
                Justification.LEFT, // output format justification
                new Format("$CHAR", 0));// input format

            // Despite the corruption, data should still be readable.
            assertNextObservation(importer, new Object[] { 83952175.0, 2256354.0, "cotton ", "shirts" });
            assertNextObservation(importer, new Object[] { 2349615.0, 498678.0, "silk   ", "ties  " });
            assertNextObservation(importer, new Object[] { 69839563.0, 9482146.0, "silk   ", "suits " });
            assertNextObservation(importer, new Object[] { 14893.0, 7693.0, "leather", "belts " });
            assertNextObservation(importer, new Object[] { 22964.0, 7936712.0, "leather", "shoes " });

            assertEndOfObservations(importer); // EOF
        }
    }

    /**
     * Tests importing an XPORT with a blank data set name. SAS cannot generate such an XPORT. It cannot read this
     * XPORT, but that's only because there's no other data set within the XPORT library that has a non-blank name.
     * Regardless, SAS cannot read the dataset.
     */
    @Test
    public void testBlankDataSetName() {
        runImportMalformedXportTest(//
            "malformed_blank_name.xpt", //
            MalformedTransportFileException.class, //
            "Data set is malformed", //
            "data set names cannot be blank");
    }

    /**
     * Tests importing an XPORT with a non-ASCII character in its name. By default, SAS cannot generate such an XPORT
     * (it must be set into a mode that permits such names). The FDA prohibits such data set names.
     */
    @Test
    public void testNonAsciiDataSetName() {
        runImportMalformedXportTest(//
            "malformed_nonascii_name.xpt", //
            MalformedTransportFileException.class, //
            "Data set is malformed", //
            "data set names must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests importing an XPORT with a space in its name. By default, SAS cannot generate such an XPORT (it must be set
     * into a mode that permits such names). The FDA prohibits such data set names.
     */
    @Test
    public void testDataSetWithSpaceInName() {
        runImportMalformedXportTest(//
            "malformed_blank_in_name.xpt", //
            MalformedTransportFileException.class, //
            "Data set is malformed", //
            "data set name is illegal for SAS");
    }

    /**
     * Tests importing an XPORT with a SAS Version field in the data set header that has a non-ASCII character. SAS does
     * not generate such a field and completely ignores this field when reading data sets.
     */
    @Test
    public void testDataSetSasVersionWithNonAsciiCharacter() {
        runImportMalformedXportTest(//
            "malformed_dataset_nonascii_version.xpt", //
            MalformedTransportFileException.class, //
            "Data set is malformed", //
            "data set SAS versions must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests importing an XPORT with an OS field in the data set header that has a non-ASCII character. SAS does not
     * generate such a field and completely ignores this field when reading data sets.
     */
    @Test
    public void testDataSetOperatingSystemWithNonAsciiCharacter() {
        runImportMalformedXportTest(//
            "malformed_dataset_nonascii_os.xpt", //
            MalformedTransportFileException.class, //
            "Data set is malformed", //
            "data set operating system must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests importing an XPORT with a "type" field in the data set header that has a non-ASCII character. SAS can
     * generate such a field. It ignores it when reading data sets.
     */
    @Test
    public void testDataSetTypeWithNonAsciiCharacter() throws IOException {

        InputStream testStream = TestUtil.getTestResource("dataset_nonascii_type.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // Getting the library description should still work.
            LocalDateTime expectedTimestamp = LocalDateTime.of(2017, 10, 19, 9, 33, 22);
            SasLibraryDescription libraryDescription = importer.sasLibraryDescription();
            TestUtil.assertSasLibraryDescription(//
                libraryDescription, //
                "Linux", // OS
                "9.4", // SAS version
                expectedTimestamp, // creation time
                expectedTimestamp); // modified time

            // Getting the data set description should still work.
            SasDataSetDescription dataSetDescription = libraryDescription.dataSetDescription();
            TestUtil.assertSasDataSetDescription(//
                dataSetDescription, //
                "DATA", // name
                "Data Set with non-ASCII char in type", // label
                "MICRO\uFFFD\uFFFD", // type
                "Linux", // OS
                "9.4", // SAS version
                expectedTimestamp, // creation time
                expectedTimestamp); // modified time

            // Getting the variables should still work.
            List<Variable> dataSetVariables = dataSetDescription.variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            List<Variable> variables = dataSetDescription.variables();
            assertEquals(1, variables.size());

            TestUtil.assertVariable(//
                variables.get(0), //
                "NUMBER", // name
                1, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "numeric data", // label
                new Format("", 5, 2), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            assertNextObservation(importer, new Object[] { 999.0 }); // Data should still be readable.
            assertEndOfObservations(importer); // EOF
        }
    }

    /**
     * Tests importing an XPORT with a variable whose type is neither 1 or 2 (neither character nor numeric). SAS can
     * read the file but has a segmentation fault when exporting it as a CSV. Presumably, SAS should have had an error
     * reading the file.
     */
    @Test
    public void testVariableWithUnknownType() {
        runImportMalformedXportTest(//
            "malformed_variable_type_unknown.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "Unexpected type code in NAMESTR field: 0");
    }

    /**
     * Tests importing an XPORT with a variable that has a "name hash" that is not 0. TS-140 states that this is always
     * zero, but SAS ignores it when reading.
     */
    @Test
    public void testVariableWithNonZeroNameHash() throws IOException {
        InputStream testStream = TestUtil.getTestResource("malformed_hash_not_zero.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            List<Variable> dataSetVariables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            // Check the variable
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "NUMBER", // name
                1, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Variable with non-zero name hashcode", // label
                new Format("", 5, 2), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            assertNextObservation(importer, new Object[] { MissingValue.STANDARD });
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT that has multiple variables all with the same number. SAS handles this without any
     * problem.
     */
    @Test
    public void testVariablesWithSameVarNum() throws IOException {

        InputStream testStream = TestUtil.getTestResource("malformed_variables_same_number.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {
            List<Variable> variables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertEquals(4, variables.size());

            final int variableNumber = 1; // all variable have varnum=1

            TestUtil.assertVariable(//
                variables.get(0), //
                "ITEM", // name
                variableNumber, // number (1-indexed)
                VariableType.CHARACTER, // type
                6, // length
                "The item of clothing that was sold", // label
                new Format("$UPCASE", 9), // output format
                Justification.LEFT, // output format justification
                new Format("$CHAR", 0));// input format

            TestUtil.assertVariable(//
                variables.get(1), //
                "MATERIAL", // name
                variableNumber, // number (1-indexed)
                VariableType.CHARACTER, // type
                7, // length
                "What the item is made of", // label
                new Format("$UPCASE", 9, 0), // output format
                Justification.LEFT, // output format justification
                new Format("$", 9, 0)); // input format

            TestUtil.assertVariable(//
                variables.get(2), //
                "COST", // name
                variableNumber, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Dollars to create item/material", // label
                new Format("DOLLAR", 15, 2), // output format
                Justification.LEFT, // output format justification
                new Format("COMMA", 10, 2)); // input format

            TestUtil.assertVariable(//
                variables.get(3), //
                "PROFIT", // name
                variableNumber, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Dollars of profit for item/material", // label
                new Format("DOLLAR", 15, 2), // output format
                Justification.LEFT, // output format justification
                new Format("DOLLAR", 10, 2)); // input format

            // Despite the corruption, data should still be readable.
            assertNextObservation(importer, new Object[] { "shirts", "cotton ", 2256354.0, 83952175.0 });
            assertNextObservation(importer, new Object[] { "ties  ", "silk   ", 498678.0, 2349615.0 });
            assertNextObservation(importer, new Object[] { "suits ", "silk   ", 9482146.0, 69839563.0 });
            assertNextObservation(importer, new Object[] { "belts ", "leather", 7693.0, 14893.0 });
            assertNextObservation(importer, new Object[] { "shoes ", "leather", 7936712.0, 22964.0 });

            assertEndOfObservations(importer); // EOF
        }
    }

    /**
     * Tests importing an XPORT that has multiple variables with the same name (different case). SAS can read and write
     * an XPORT file without any problem but ignores the first variable when manipulating the data set.
     */
    @Test
    public void testVariablesWithSameName() {
        // The API is expressive enough to be able to provide access to each variable separately,
        // but it's easier to reject such a pathological file than to expose callers to it.
        runImportMalformedXportTest(//
            "malformed_variables_same_name.xpt", //
            MalformedTransportFileException.class, //
            "Data set is malformed", //
            "multiple variables have the same name: item");
    }

    /**
     * Tests importing an XPORT that has a negative variable number. SAS handles this without any problem.
     */
    @Test
    public void testVariablesWithNegativeVarNum() throws IOException {
        InputStream testStream = TestUtil.getTestResource("malformed_variable_negative_number.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {
            List<Variable> variables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertEquals(1, variables.size());

            TestUtil.assertVariable(//
                variables.get(0), //
                "NEGNUM", // name
                -7, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "A variable with a negative varnum (-7)", // label
                new Format("", 5, 2), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED);// input format

            // Despite the corruption, data should still be readable.
            assertNextObservation(importer, new Object[] { MissingValue.STANDARD });
            assertEndOfObservations(importer); // EOF
        }
    }

    /**
     * Tests importing an XPORT with a numeric variable that has a length-in-observation of 9. SAS has a hard error when
     * reading this.
     */
    @Test
    public void testNumberTooLong() {
        runImportMalformedXportTest(//
            "malformed_number_too_long.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "numeric variables must have a length between 2-8");
    }

    /**
     * Tests importing an XPORT with a numeric variable that has a length-in-observation of 1. SAS has a hard error when
     * reading this.
     */
    @Test
    public void testNumberTooShort() {
        runImportMalformedXportTest(//
            "malformed_number_too_short.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "numeric variables must have a length between 2-8");
    }

    /**
     * Tests importing an XPORT that has a character variable with length=0 (and the entire observation has a 0 length).
     * SAS is able to import this but gives strange errors when trying to manipulate the data set. SAS cannot generate
     * such an XPORT file.
     */
    @Test
    public void testCharacterVariableWithZeroLength() {

        // Since the entire observation is zero length, it's ambiguous how many observations
        // there are.  We could arbitrarily resolve this ambiguity by saying there are no observations,
        // giving the program a chance to process the header, but it's easier to consider it malformed.
        runImportMalformedXportTest(//
            "malformed_variable_zero_length.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "character variables must have a positive length");
    }

    /**
     * Tests importing an XPORT that has a character variable with a negative length. SAS is able to import this but
     * gives strange errors when trying to manipulate the data set. SAS cannot generate such an XPORT file.
     */
    @Test
    public void testCharacterVariableWithNegativeLength() {
        runImportMalformedXportTest(//
            "malformed_variable_negative_length.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "character variables must have a positive length");
    }

    /**
     * Tests importing an XPORT with a character variable whose length is over 200. SAS cannot generate such an XPORT.
     * SAS 9.2 can read such data without data loss.
     */
    @Test
    public void testCharacterVariableWithLongLength() throws IOException {
        InputStream testStream = TestUtil.getTestResource("malformed_variable_length_max_short.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {
            List<Variable> variables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertEquals(2, variables.size());

            TestUtil.assertVariable(//
                variables.get(0), //
                "LONGTEXT", // name
                1, // number (1-indexed)
                VariableType.CHARACTER, // type
                Short.MAX_VALUE, // length
                "Some very long text", // label
                new Format("$F", Short.MAX_VALUE), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED);// input format

            TestUtil.assertVariable(//
                variables.get(1), //
                "NUMBER", // name
                4, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "A number after the text", // label
                new Format("", 5), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED);// input format

            // Despite the corruption, data should still be readable.
            for (int i = 0; i < 20; i++) {
                // The over-long variable is formed as
                // <observation_index>:AAA....AAAB
                StringBuilder longValueBuilder = new StringBuilder(variables.get(0).length());
                longValueBuilder.append(i).append(':');
                while (longValueBuilder.length() < variables.get(0).length() - 1) {
                    longValueBuilder.append('A');
                }
                longValueBuilder.append('B');

                // The number after the long value is the observation index.
                Double numberValue = Double.valueOf(i);

                assertNextObservation(importer, new Object[] { longValueBuilder.toString(), numberValue });
            }

            assertEndOfObservations(importer); // EOF
        }
    }

    /**
     * Tests importing an XPORT with a variable that has a non-ASCII character in its name. SAS has a hard error when
     * reading this.
     */
    @Test
    public void testVariableWithNonAsciiName() {
        runImportMalformedXportTest(//
            "malformed_variable_nonascii_name.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "variable names must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests importing an XPORT with a variable with a blank name. SAS has a hard error when reading this.
     */
    @Test
    public void testVariableWithBlankName() {
        runImportMalformedXportTest(//
            "malformed_variable_empty_name.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "variable names cannot be blank");
    }

    /**
     * Tests importing an XPORT with a variable with a blank in its name. SAS has a hard error when reading this.
     */
    @Test
    public void testVariableWithBlankInName() {
        runImportMalformedXportTest(//
            "malformed_variable_name_with_blank.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "variable name is illegal for SAS");
    }

    /**
     * Tests importing an XPORT with a variable named _N_. SAS says not to create such variables, as it conflicts with
     * the automatic variables. However, SAS can read such an XPORT file, so this library should also be able to read
     * it.
     */
    @Test
    public void testVariableWithReservedName() throws IOException {
        InputStream testStream = TestUtil.getTestResource("malformed_variable_reserved_name.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // Getting the variables should still work.
            List<Variable> dataSetVariables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            // Check the variable
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "_N_", // name
                1, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "variable named _N_ (this is reserved)", // label
                new Format("", 5, 2), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            assertNextObservation(importer, new Object[] { MissingValue.STANDARD });
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT with a variable that has a format with a negative width. SAS can read this (it treats
     * the width as 0).
     */
    @Test
    public void testOutputFormatWithNegativeWidth() {
        // BUG? should we treat this a width=0?
        runImportMalformedXportTest(//
            "malformed_format_negative_width.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "format width must not be negative");
    }

    /**
     * Tests importing an XPORT with a variable that has a format with a negative number of digits. SAS can read this
     * but has an unhandled exception when trying to format data upon writing it.
     */
    @Test
    public void testOutputFormatWithNegativeNumberOfDigits() {
        runImportMalformedXportTest(//
            "malformed_format_negative_digits.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "format numberOfDigits must not be negative");
    }

    /**
     * Tests importing an XPORT with that has the "Fw.d" format with w==d. SAS has a clear error when trying to write
     * use this format.
     */
    @Test
    public void testOutputFormatWithDigitsEqualsWidth() throws IOException {
        // Currently this library does not have format-specific w/d checks.
        // If such checks are added, then this test will need to be updated.
        InputStream testStream = TestUtil.getTestResource("malformed_format_width_equals_digits.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // Getting the variables should still work.
            List<Variable> dataSetVariables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            // Check the variable
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "BADVAR", // name
                1, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Variable with format width==digits", // label
                new Format("F", 5, 5), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            assertNextObservation(importer, new Object[] { MissingValue.STANDARD });
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT with a variable that has a format whose name has a non-ASCII character. SAS has a hard
     * error when reading this.
     */
    @Test
    public void testOutputFormatWithNonAsciiCharacter() {
        runImportMalformedXportTest(//
            "malformed_format_nonascii_name.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "format name must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests importing an XPORT with a variable that has an unknown justification field (neither left nor right). TS-140
     * suggests that this is malformed, but since SAS ignores the justification, this library must be able to read such
     * an XPORT file.
     */
    @Test
    public void testVariableWithUnknownJustification() throws IOException {
        InputStream testStream = TestUtil.getTestResource("malformed_variable_justification.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // Getting the variables should still work.
            List<Variable> dataSetVariables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            // Check the variable
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "BADJUST", // name
                1, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "Variable with an unknown justification 2", // label
                new Format("F", 5, 2), // output format
                Justification.UNKNOWN, // output format justification
                Format.UNSPECIFIED); // input format

            assertNextObservation(importer, new Object[] { MissingValue.STANDARD });
            assertEndOfObservations(importer);
        }
    }

    /**
     * Tests importing an XPORT with a variable that has a negative offset in the observation. SAS has a "recursive
     * segmentation fault" error when reading this.
     */
    @Test
    public void testVariableWithNegativeOffset() {
        runImportMalformedXportTest(//
            "malformed_variable_negative_offset.xpt", //
            MalformedTransportFileException.class, //
            "Variable #1 is malformed", //
            "Bad offset in NAMESTR field (offset can't be negative): -53687092");
    }

    /**
     * Tests that reading a SAS XPORT file that has a malformed numeric value (it looks like an {@code @} missing
     * value).
     */
    @Test
    public void testReadingMalformedMissingValue_At() throws IOException {
        InputStream testStream = TestUtil.getTestResource("malformed_missing_value_at.xpt");
        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(testStream)) {

            // Getting the variables should still work.
            List<Variable> dataSetVariables = importer.sasLibraryDescription().dataSetDescription().variables();
            assertNotNull(dataSetVariables);
            assertEquals(1, dataSetVariables.size()); // only one variable is expected.

            // Check the variable
            TestUtil.assertVariable(//
                dataSetVariables.get(0), //
                "NUMBER", // name
                1, // number (1-indexed)
                VariableType.NUMERIC, // type
                8, // length
                "A number", // label
                new Format("", 5, 2), // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED); // input format

            assertNextObservationsThrowsException(//
                importer, //
                MalformedTransportFileException.class, //
                "Malformed numeric value: mantissa is zero but value is not 0 or a MissingValue");
        }
    }
}