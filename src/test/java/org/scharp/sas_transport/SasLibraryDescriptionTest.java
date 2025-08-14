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
        SasDataSetDescription dataSet = newDataSetDescription();
        SasLibraryDescription library = new SasLibraryDescription(dataSet, "SOUREOS", "12.4", createdTime,
            modifiedTime);

        // Confirm that all of its properties were set correctly.
        TestUtil.assertSasLibraryDescription(library, "SOUREOS", "12.4", createdTime, modifiedTime);
        assertEquals(dataSet, library.dataSetDescription());
    }

    private static void runConstructWithIllegalArgumentTest(SasDataSetDescription dataSetDescription,
        String sourceOperatingSystem, String sourceSasVersion, LocalDateTime createTime, LocalDateTime modifiedTime,
        String expectedExceptionMessage) {
        Exception exception = assertThrows( //
            IllegalArgumentException.class, //
            () -> new SasLibraryDescription(dataSetDescription, sourceOperatingSystem, sourceSasVersion, createTime,
                modifiedTime), //
            "creating SasLibraryDescription with a bad argument");
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    private static void runConstructWithNullArgumentTest(SasDataSetDescription dataSetDescription,
        String sourceOperatingSystem, String sourceSasVersion, LocalDateTime createTime, LocalDateTime modifiedTime,
        String expectedExceptionMessage) {
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> new SasLibraryDescription(dataSetDescription, sourceOperatingSystem, sourceSasVersion, createTime,
                modifiedTime), //
            "creating SasLibraryDescription with a null argument");
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    /**
     * @return A well-formed SAS data set, for tests that need one but don't care what it is.
     */
    private static SasDataSetDescription newDataSetDescription() {
        return new SasDataSetDescription(//
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
        return new SasLibraryDescription(newDataSetDescription(), "OS", "9.3", LocalDateTime.now(),
            LocalDateTime.now());
    }

    /**
     * Tests constructing a SAS library description with a {@code null} data set.
     */
    @Test
    public void constructWithNullDataSetDescription() {
        runConstructWithNullArgumentTest(//
            null, // ERROR: null data set
            "os", // os
            "9.3", // version
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "dataSetDescription must not be null");
    }

    /**
     * Tests constructing a SAS library description with a {@code null} operating system.
     */
    @Test
    public void constructWithNullSourceOperatingSystem() {
        runConstructWithNullArgumentTest(//
            newDataSetDescription(), // data set
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
            newDataSetDescription(), // data set
            limitOperatingSystem, // os
            "9.3", // version
            LocalDateTime.now(), // create date
            LocalDateTime.now()); // modified date
        assertEquals(limitOperatingSystem, library.sourceOperatingSystem());

        // The 9 character operating system is prohibited.
        runConstructWithIllegalArgumentTest(//
            newDataSetDescription(), // data set
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
            newDataSetDescription(), // data set
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
            newDataSetDescription(), // data set
            "OS", // os
            limitSasVersion, // version
            LocalDateTime.now(), // create date
            LocalDateTime.now()); // modified date
        assertEquals(limitSasVersion, library.sourceSasVersion());

        // The 9 character SAS version is prohibited.
        runConstructWithIllegalArgumentTest(//
            newDataSetDescription(), // data set
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
            newDataSetDescription(), // data set
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
            newDataSetDescription(), // data set
            "OS", // os
            "9.3", // SAS version
            LocalDateTime.now(), // create date
            null, // modified date
            "modifiedTime must not be null");
    }

    /**
     * Tests constructing a SAS data set exporter with a {@code null} file path.
     */
    @Test
    public void exportWithNullPath() {
        SasLibraryDescription library = newLibraryDescription();
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> library.exportTransportDataSet((Path) null), //
            "Creating a SasTransportExporter with a null Path");
        assertEquals("path must not be null", exception.getMessage());
    }

    /**
     * Tests constructing a SAS data set exporter with a {@code null} output stream.
     */
    @Test
    public void exportWithNullOutputStream() {
        SasLibraryDescription library = newLibraryDescription();
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> library.exportTransportDataSet((OutputStream) null), //
            "Creating a SasTransportExporter with a null OutputStream");
        assertEquals("outputStream must not be null", exception.getMessage());
    }

    /**
     * Tests constructing a SAS data set importer with a {@code null} file path.
     */
    @Test
    public void importWithNullPath() {
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> SasLibraryDescription.importTransportDataSet((Path) null), //
            "Creating a SasTransportImporter with a null Path");
        assertEquals("path must not be null", exception.getMessage());
    }

    /**
     * Tests constructing a SAS data set importer with a {@code null} input stream.
     */
    @Test
    public void importWithNullInputStream() {
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> SasLibraryDescription.importTransportDataSet((InputStream) null), //
            "Creating a SasTransportExporter with a null InputStream");
        assertEquals("inputStream must not be null", exception.getMessage());
    }
}