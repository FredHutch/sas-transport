///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class TestUtil {

    // prevent this class from being instantiated.
    private TestUtil() {
    }

    /**
     * A simple shim for reading test resources. If the resource cannot be found, this fails the test.
     *
     * @param relativeResourceName
     *     The name of the resource (relative to this class).
     *
     * @return An InputStream to the resource. This must be closed by the caller.
     */
    static InputStream getTestResource(String relativeResourceName) {
        // from "mvn test"
        InputStream stream = ClassLoader.getSystemResourceAsStream("org/scharp/sas_transport/" + relativeResourceName);
        if (stream != null) {
            return stream;
        }

        // from Eclipse if "mvn package" hasn't been run recently.
        try {
            return Files.newInputStream(
                Paths.get("src/test/resources/org/scharp/sas_transport/" + relativeResourceName));
        } catch (IOException exception) {
            throw new AssertionError("Unable to locate " + relativeResourceName, exception);
        }
    }

    /**
     * Fails the current test if any of the fields of a {@link Format} are not as expected.
     *
     * @param format
     *     The format to test.
     * @param expectedName
     *     The expected name of the format.
     * @param expectedWidth
     *     The expected width of the format.
     * @param expectedNumberOfDigits
     *     The expected number of digits of the format.
     */
    static void assertFormat(Format format, String expectedName, int expectedWidth, int expectedNumberOfDigits) {
        assertNotNull(format);
        assertEquals(expectedName, format.name(), "incorrect format name");
        assertEquals(expectedWidth, format.width(), "incorrect format width");
        assertEquals(expectedNumberOfDigits, format.numberOfDigits(), "incorrect format digits");
    }

    /**
     * Fails the current test if any of the fields of a {@link Variable} are not as expected.
     *
     * @param variable
     * @param expectedName
     * @param expectedNumber
     * @param expectedType
     * @param expectedLength
     * @param expectedLabel
     * @param expectedOutputFormat
     * @param expectedJustification
     * @param expectedInputFormat
     */
    static void assertVariable(Variable variable, String expectedName, int expectedNumber, VariableType expectedType,
        int expectedLength, String expectedLabel, Format expectedOutputFormat, Justification expectedJustification,
        Format expectedInputFormat) {
        assertNotNull(variable);
        assertEquals(expectedName, variable.name(), "incorrect variable name");
        assertEquals(expectedNumber, variable.number(), " incorrect variable number");
        assertEquals(expectedType, variable.type(), "incorrect variable type");
        assertEquals(expectedLength, variable.length(), "incorrect variable length");
        assertEquals(expectedLabel, variable.label(), "incorrect variable label");
        assertFormat(//
            variable.outputFormat(), //
            expectedOutputFormat.name(), //
            expectedOutputFormat.width(), //
            expectedOutputFormat.numberOfDigits());
        assertEquals(//
            expectedJustification, //
            variable.outputFormatJustification(), //
            "incorrect variable justification");
        assertFormat(//
            variable.inputFormat(), //
            expectedInputFormat.name(), //
            expectedInputFormat.width(), //
            expectedInputFormat.numberOfDigits());
    }

    /**
     * Fails the current test if any of the fields of a {@link SasDatasetDescription} are not as expected.
     * <p>
     * This does NOT check the variables.
     * </p>
     *
     * @param datasetDescription
     *     The SAS dataset whose properties should be tested.
     * @param expectedName
     *     The expected value of the name property.
     * @param expectedLabel
     *     The expected value of the label property.
     * @param expectedType
     *     The expected value of the type property.
     * @param expectedSourceOperatingSystem
     *     The expected value of the sourceOperatingSystem property.
     * @param expectedSourceSasVersion
     *     The expected value of the sourceSasVersion property.
     * @param expectedCreateTime
     *     The expected creation timestamp.
     * @param expectedModifiedTime
     *     The expected modified timestamp.
     */
    static void assertSasDatasetDescription(SasDatasetDescription datasetDescription, String expectedName,
        String expectedLabel, String expectedType, String expectedSourceOperatingSystem,
        String expectedSourceSasVersion, LocalDateTime expectedCreateTime, LocalDateTime expectedModifiedTime) {
        assertNotNull(datasetDescription);
        assertEquals(expectedName, datasetDescription.name());
        assertEquals(expectedLabel, datasetDescription.label());
        assertEquals(expectedType, datasetDescription.type());
        assertEquals(expectedSourceOperatingSystem, datasetDescription.sourceOperatingSystem());
        assertEquals(expectedSourceSasVersion, datasetDescription.sourceSasVersion());
        assertEquals(expectedCreateTime, datasetDescription.createTime());
        assertEquals(expectedModifiedTime, datasetDescription.modifiedTime());
    }

    /**
     * Fails the current test if any of the properties of a {@link SasLibraryDescription} are not as expected.
     * <p>
     * This does NOT check the dataset.
     * </p>
     *
     * @param libraryDescription
     *     The SAS Library whose properties should be tested.
     * @param expectedSourceOperatingSystem
     *     The expected value of the sourceOperatingSystem property.
     * @param expectedSourceSasVersion
     *     The expected value of the sourceSasVersion property.
     * @param expectedCreateTime
     *     The expected creation timestamp.
     * @param expectedModifiedTime
     *     The expected modified timestamp.
     */
    static void assertSasLibraryDescription(SasLibraryDescription libraryDescription,
        String expectedSourceOperatingSystem, String expectedSourceSasVersion, LocalDateTime expectedCreateTime,
        LocalDateTime expectedModifiedTime) {
        assertNotNull(libraryDescription);
        assertEquals(expectedSourceOperatingSystem, libraryDescription.sourceOperatingSystem(), "incorrect Source OS");
        assertEquals(expectedSourceSasVersion, libraryDescription.sourceSasVersion(), "incorrect SAS version");
        assertEquals(expectedCreateTime, libraryDescription.createTime(), "incorrect Create timestamp");
        assertEquals(expectedModifiedTime, libraryDescription.modifiedTime(), "incorrect Modify timestamp");
    }

    static String repeatString(String string, int repeat) {
        StringBuilder builder = new StringBuilder(string.length() * repeat);
        for (int i = 0; i < repeat; i++) {
            builder.append(string);
        }
        return builder.toString();
    }
}