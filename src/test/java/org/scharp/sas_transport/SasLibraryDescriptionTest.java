///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link SasLibraryDescription} class.
 */
public class SasLibraryDescriptionTest {

    /**
     * Unit tests for basic construction and property getting of the {@link SasLibraryDescription} class.
     */
    @Test
    public void basicTest() {

        // Create a library.
        final LocalDateTime createdTime = LocalDateTime.of(1999, 12, 31, 23, 59, 59, 999);
        final LocalDateTime modifiedTime = LocalDateTime.of(2001, 6, 30, 12, 59, 59, 123);
        SasDatasetDescription dataset = newDatasetDescription();
        SasLibraryDescription library = new SasLibraryDescription(dataset, "SOUREOS", "12.4", createdTime,
            modifiedTime);

        // Confirm that all of its properties were set correctly.
        TestUtil.assertSasLibraryDescription(library, "SOUREOS", "12.4", createdTime, modifiedTime);
        assertEquals(dataset, library.datasetDescription());
    }

    private static void runConstructWithIllegalArgumentTest(SasDatasetDescription datasetDescription,
        String sourceOperatingSystem, String sourceSasVersion, LocalDateTime createTime, LocalDateTime modifiedTime,
        String expectedExceptionMessage) {
        Exception exception = assertThrows( //
            IllegalArgumentException.class, //
            () -> new SasLibraryDescription(datasetDescription, sourceOperatingSystem, sourceSasVersion, createTime,
                modifiedTime), //
            "creating SasLibraryDescription with a bad argument");
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    private static void runConstructWithNullArgumentTest(SasDatasetDescription datasetDescription,
        String sourceOperatingSystem, String sourceSasVersion, LocalDateTime createTime, LocalDateTime modifiedTime,
        String expectedExceptionMessage) {
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> new SasLibraryDescription(datasetDescription, sourceOperatingSystem, sourceSasVersion, createTime,
                modifiedTime), //
            "creating SasLibraryDescription with a null argument");
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    /**
     * @return A well-formed SAS dataset, for tests that need one but don't care what it is.
     */
    private static SasDatasetDescription newDatasetDescription() {
        return new SasDatasetDescription(//
            "TESTDATA", // name
            "label", // label
            "type", // type
            "OS", // OS
            "9.4", // SAS version
            Collections.singletonList(//
                new Variable(//
                    "VAR", //
                    1, //
                    VariableType.NUMERIC, //
                    8, //
                    "label", //
                    Format.UNSPECIFIED, //
                    Justification.LEFT, //
                    Format.UNSPECIFIED)), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now()); // modification date
    }

    /**
     * @return A well-formed SAS library description, for tests that need one but don't care what it is.
     */
    private SasLibraryDescription newLibraryDescription() {
        return new SasLibraryDescription(newDatasetDescription(), "OS", "9.3", LocalDateTime.now(),
            LocalDateTime.now());
    }

    /**
     * Tests constructing a SAS library description with a {@code null} dataset.
     */
    @Test
    public void constructWithNullDatasetDescription() {
        runConstructWithNullArgumentTest(//
            null, // ERROR: null dataset
            "os", // os
            "9.3", // version
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "datasetDescription must not be null");
    }

    /**
     * Tests constructing a SAS library description with a {@code null} operating system.
     */
    @Test
    public void constructWithNullSourceOperatingSystem() {
        runConstructWithNullArgumentTest(//
            newDatasetDescription(), // dataset
            null, // ERROR: null os
            "9.3", // version
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "sourceOperatingSystem must not be null");
    }

    /**
     * Tests constructing a SAS library description with a source OS that's too long.
     */
    @Test
    public void constructWithLongSourceOperatingSystem() {
        // Create two operating systems, one that just barely fits and one that's just barely too long.
        String limitOperatingSystem = "OS_45678";
        String longOperatingSystem = limitOperatingSystem + "Y";

        // The 8 character operating system is permitted.
        SasLibraryDescription library = new SasLibraryDescription(//
            newDatasetDescription(), // dataset
            limitOperatingSystem, // os
            "9.3", // version
            LocalDateTime.now(), // create date
            LocalDateTime.now()); // modified date
        assertEquals(limitOperatingSystem, library.sourceOperatingSystem());

        // The 9 character operating system is prohibited.
        runConstructWithIllegalArgumentTest(//
            newDatasetDescription(), // dataset
            longOperatingSystem, // os
            "9.3", // version
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "sourceOperatingSystem must not be longer than 8 characters");
    }

    /**
     * Tests constructing a SAS library description with a {@code null} source SAS version.
     */
    @Test
    public void constructWithNullSourceSasVersion() {
        runConstructWithNullArgumentTest(//
            newDatasetDescription(), // dataset
            "OS", // os
            null, // ERROR: bad version
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "sourceSasVersion must not be null");
    }

    /**
     * Tests constructing a SAS library description with a source SAS Version that's too long.
     */
    @Test
    public void constructWithLongSourceSasVersion() {
        // Create two SAS versions, one that just barely fits and one that's just barely too long.
        String limitSasVersion = "SAS45678";
        String longSasVersion = limitSasVersion + "Y";

        // The 8 character SAS version is permitted.
        SasLibraryDescription library = new SasLibraryDescription(//
            newDatasetDescription(), // dataset
            "OS", // os
            limitSasVersion, // version
            LocalDateTime.now(), // create date
            LocalDateTime.now()); // modified date
        assertEquals(limitSasVersion, library.sourceSasVersion());

        // The 9 character SAS version is prohibited.
        runConstructWithIllegalArgumentTest(//
            newDatasetDescription(), // dataset
            "OS", // os
            longSasVersion, // version
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "sourceSasVersion must not be longer than 8 characters");
    }

    /**
     * Tests constructing a SAS library description with a {@code null} creation date.
     */
    @Test
    public void constructWithNullCreationDate() {
        runConstructWithNullArgumentTest(//
            newDatasetDescription(), // dataset
            "OS", // os
            "9.3", // SAS version
            null, // ERROR: invalid create date
            LocalDateTime.now(), // modified date
            "createTime must not be null");
    }

    /**
     * Tests constructing a SAS library description with a {@code null} modification date.
     */
    @Test
    public void constructWithNullModificationDate() {
        runConstructWithNullArgumentTest(//
            newDatasetDescription(), // dataset
            "OS", // os
            "9.3", // SAS version
            LocalDateTime.now(), // create date
            null, // modified date
            "modifiedTime must not be null");
    }

    /**
     * Tests constructing a SAS dataset exporter with a {@code null} file path.
     */
    @Test
    public void exportWithNullPath() {
        SasLibraryDescription library = newLibraryDescription();
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> library.exportTransportDataset((Path) null), //
            "Creating a SasTransportExporter with a null Path");
        assertEquals("path must not be null", exception.getMessage());
    }

    /**
     * Tests constructing a SAS dataset exporter with a {@code null} output stream.
     */
    @Test
    public void exportWithNullOutputStream() {
        SasLibraryDescription library = newLibraryDescription();
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> library.exportTransportDataset((OutputStream) null), //
            "Creating a SasTransportExporter with a null OutputStream");
        assertEquals("outputStream must not be null", exception.getMessage());
    }

    /**
     * Tests constructing a SAS dataset importer with a {@code null} file path.
     */
    @Test
    public void importWithNullPath() {
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> SasLibraryDescription.importTransportDataset((Path) null), //
            "Creating a SasTransportImporter with a null Path");
        assertEquals("path must not be null", exception.getMessage());
    }

    /**
     * Tests constructing a SAS dataset importer with a {@code null} input stream.
     */
    @Test
    public void importWithNullInputStream() {
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> SasLibraryDescription.importTransportDataset((InputStream) null), //
            "Creating a SasTransportExporter with a null InputStream");
        assertEquals("inputStream must not be null", exception.getMessage());
    }
}