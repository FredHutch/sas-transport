///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link SasDatasetDescription} class.
 */
public class SasDatasetDescriptionTest {

    /**
     * Unit tests for basic construction and property getting of the {@link SasDatasetDescription} class.
     */
    @Test
    public void basicTest() {

        List<Variable> variables = new ArrayList<>(Arrays.asList(// a mutable list
            new Variable(//
                "VAR_1", //
                1, //
                VariableType.CHARACTER, //
                10, //
                "The Label", //
                new Format("OUTPUT", 6, 2), //
                Justification.LEFT, //
                new Format("INPUT", 5, 1)), //

            new Variable(//
                "var2", //
                2, //
                VariableType.NUMERIC, //
                6, //
                "", // label
                Format.UNSPECIFIED, //
                Justification.RIGHT, //
                Format.UNSPECIFIED)));

        final LocalDateTime createdTime = LocalDateTime.of(1999, 12, 31, 23, 59, 59, 999);
        final LocalDateTime modifiedTime = LocalDateTime.of(2001, 6, 30, 12, 59, 59, 123);

        // Create the dataset.
        SasDatasetDescription dataset = new SasDatasetDescription("DSNAME", "Dataset Label", "TYPE", "SOUREOS", "12.4",
            variables, createdTime, modifiedTime);

        // Confirm that all of its properties were set correctly.
        TestUtil.assertSasDatasetDescription(//
            dataset, //
            "DSNAME", //
            "Dataset Label", //
            "TYPE", //
            "SOUREOS", //
            "12.4", //
            createdTime, //
            modifiedTime);
        assertEquals(2, dataset.variables().size());

        // Check the variables
        TestUtil.assertVariable(//
            dataset.variables().get(0), //
            "VAR_1", // name
            1, // number
            VariableType.CHARACTER, // type
            10, // length
            "The Label", // label
            new Format("OUTPUT", 6, 2), // output format
            Justification.LEFT, // justification
            new Format("INPUT", 5, 1)); // input format name
        TestUtil.assertVariable(//
            dataset.variables().get(1), //
            "var2", // name
            2, // number
            VariableType.NUMERIC, // type
            6, // length
            "", // label
            Format.UNSPECIFIED, // output format
            Justification.RIGHT, // justification
            Format.UNSPECIFIED); // input format

        //
        // Now test the immutability of the SasDatasetDescription.
        //

        // Change the list of variable which we gave to the constructor.
        variables.clear();
        assertEquals(2, dataset.variables().size());

        // Attempt to change the variable list that is returned from accessor.
        List<Variable> datasetVariables = dataset.variables();
        assertThrows(//
            UnsupportedOperationException.class, //
            datasetVariables::clear, //
            "Was able to clear the variable returned by SasDatasetDescription");
    }

    /**
     * Test {@link SasDatasetDescription#newLibraryDescription()}.
     */
    @Test
    public void testNewLibrary() {

        List<Variable> variables = newVariableList();
        final LocalDateTime createdTime = LocalDateTime.of(1999, 12, 31, 23, 59, 59, 999);
        final LocalDateTime modifiedTime = LocalDateTime.of(2001, 6, 30, 12, 59, 59, 123);

        // Create a dataset.
        SasDatasetDescription datasetA = new SasDatasetDescription("A", "B", "C", "OS123456", "VER12345", variables,
            createdTime, modifiedTime);

        // Create a library from it.
        SasLibraryDescription libraryA1 = datasetA.newLibraryDescription();
        TestUtil.assertSasLibraryDescription(libraryA1, "OS123456", "VER12345", createdTime, modifiedTime);

        // Create another library.  It should have the same data but a different reference.
        SasLibraryDescription libraryB2 = datasetA.newLibraryDescription();
        TestUtil.assertSasLibraryDescription(libraryB2, "OS123456", "VER12345", createdTime, modifiedTime);
        assertNotSame(libraryA1, libraryB2, "newLibrary() returned same reference");

        // Create another dataset with different blank values.
        SasDatasetDescription datasetB = new SasDatasetDescription("B", "", "", "", "", variables,
            LocalDateTime.of(0, 1, 1, 0, 0), LocalDateTime.of(0, 1, 1, 0, 0));
        SasLibraryDescription libraryB1 = datasetB.newLibraryDescription();
        TestUtil.assertSasLibraryDescription(libraryB1, "", "", LocalDateTime.of(0, 1, 1, 0, 0),
            LocalDateTime.of(0, 1, 1, 0, 0));
    }

    /**
     * Tests that {@link SasDatasetDescription} throws the correct exception when two variables share the same name.
     */
    @Test
    public void constructWithRepeatedVariableNames() {

        runConstructWithIllegalArgumentTest(//
            "NAME", //
            "Label", //
            "TYPE", //
            "OS", //
            "12.4", //
            Arrays.asList( //
                new Variable(//
                    "repeat", //
                    1, //
                    VariableType.NUMERIC, //
                    6, //
                    "", // label
                    Format.UNSPECIFIED, //
                    Justification.RIGHT, //
                    Format.UNSPECIFIED),

                new Variable(//
                    "VAR2", //
                    2, //
                    VariableType.CHARACTER, //
                    10, //
                    "The Label", //
                    Format.UNSPECIFIED, //
                    Justification.LEFT, //
                    Format.UNSPECIFIED), //

                new Variable(//
                    "REPEAT", // ERROR: name of variable #1 (in upper-case)
                    3, //
                    VariableType.CHARACTER, //
                    10, //
                    "The Label", //
                    new Format("OUTPUT", 6, 2), //
                    Justification.LEFT, //
                    new Format("INPUT", 5, 1)),

                new Variable(//
                    "VAR4", //
                    4, //
                    VariableType.CHARACTER, //
                    10, //
                    "The Label", //
                    Format.UNSPECIFIED, //
                    Justification.LEFT, //
                    Format.UNSPECIFIED)),
            LocalDateTime.now(), //
            LocalDateTime.now(), //
            "multiple variables have the same name: REPEAT");
    }

    /**
     * Invokes the public constructor with the given arguments, expecting an IllegalArgumentException.
     *
     * @param name
     * @param label
     * @param type
     * @param sourceOperatingSystem
     * @param sourceSasVersion
     * @param variables
     * @param createTime
     * @param modifiedTime
     * @param expectedErrorMessage
     */
    private static void runConstructWithIllegalArgumentTest(String name, String label, String type,
        String sourceOperatingSystem, String sourceSasVersion, Collection<Variable> variables, LocalDateTime createTime,
        LocalDateTime modifiedTime, String expectedErrorMessage) {
        Exception exception = assertThrows( //
            IllegalArgumentException.class, //
            () -> new SasDatasetDescription(name, label, type, sourceOperatingSystem, sourceSasVersion, variables,
                createTime, modifiedTime), //
            "creating SasDatasetDescription with a bad argument");
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    /**
     * Invokes the package-private constructor with the given arguments for each of the supplied strictness modes,
     * expecting an IllegalArgumentException on each one.
     *
     * @param name
     * @param label
     * @param type
     * @param sourceOperatingSystem
     * @param sourceSasVersion
     * @param variables
     * @param createTime
     * @param modifiedTime
     * @param strictnessModesToTest
     * @param expectedErrorMessage
     */
    private static void runConstructWithIllegalArgumentTest(String name, String label, String type,
        String sourceOperatingSystem, String sourceSasVersion, Collection<Variable> variables, LocalDateTime createTime,
        LocalDateTime modifiedTime, Collection<StrictnessMode> strictnessModesToTest, String expectedErrorMessage) {
        for (StrictnessMode strictnessMode : strictnessModesToTest) {
            Exception exception = assertThrows( //
                IllegalArgumentException.class, //
                () -> new SasDatasetDescription(name, label, type, sourceOperatingSystem, sourceSasVersion, variables,
                    createTime, modifiedTime, strictnessMode), //
                "creating SasDatasetDescription with a bad argument");
            assertEquals(expectedErrorMessage, exception.getMessage());
        }
    }

    /**
     * Invokes the public constructor and each of the package-private constructors with the given arguments for all
     * strictness modes, expecting an IllegalArgumentException on each one.
     *
     * @param name
     * @param label
     * @param type
     * @param sourceOperatingSystem
     * @param sourceSasVersion
     * @param variables
     * @param createTime
     * @param modifiedTime
     * @param expectedErrorMessage
     */
    private static void runAllConstructWithIllegalArgumentTests(String name, String label, String type,
        String sourceOperatingSystem, String sourceSasVersion, Collection<Variable> variables, LocalDateTime createTime,
        LocalDateTime modifiedTime, String expectedErrorMessage) {
        // invoke the public constructor
        runConstructWithIllegalArgumentTest(name, label, type, sourceOperatingSystem, sourceSasVersion, variables,
            createTime, modifiedTime, expectedErrorMessage);

        // invoke the package-private constructor
        runConstructWithIllegalArgumentTest(name, label, type, sourceOperatingSystem, sourceSasVersion, variables,
            createTime, modifiedTime, EnumSet.allOf(StrictnessMode.class), expectedErrorMessage);
    }

    private static void runConstructWithNullArgumentTest(String name, String label, String type,
        String sourceOperatingSystem, String sourceSasVersion, Collection<Variable> variables, LocalDateTime createTime,
        LocalDateTime modifiedTime, String expectedErrorMessage) {
        // The null parameter check should apply to the public constructor
        Exception exception = assertThrows(//
            NullPointerException.class, //
            () -> new SasDatasetDescription(name, label, type, sourceOperatingSystem, sourceSasVersion, variables,
                createTime, modifiedTime), //
            "creating SasDatasetDescription with a null argument");
        assertEquals(expectedErrorMessage, exception.getMessage());

        // Null parameter checking should apply in all strictness modes.
        for (StrictnessMode strictnessMode : StrictnessMode.values()) {
            exception = assertThrows(//
                NullPointerException.class, //
                () -> new SasDatasetDescription(name, label, type, sourceOperatingSystem, sourceSasVersion, variables,
                    createTime, modifiedTime, strictnessMode), //
                "Creating SasDatasetDescription with a null argument in " + strictnessMode);
            assertEquals(expectedErrorMessage, exception.getMessage());
        }
    }

    /**
     * @return A well-formed variable list, for tests that need one but don't care what it is.
     */
    private static List<Variable> newVariableList() {
        return Collections.singletonList(//
            new Variable(//
                "VAR", //
                1, //
                VariableType.NUMERIC, //
                8, //
                "label", //
                Format.UNSPECIFIED, //
                Justification.LEFT, //
                Format.UNSPECIFIED)); //
    }

    /**
     * Tests constructing a SAS dataset description with the empty string for the name.
     */
    @Test
    public void constructWithBlankName() {
        runAllConstructWithIllegalArgumentTests(//
            "", // ERROR: name must not be blank
            "label", // label
            "type", // type
            "OS", // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now(), // modification date
            "dataset names cannot be blank");
    }

    /**
     * Tests constructing a SAS dataset description with the empty string for the name.
     */
    @Test
    public void constructWithNonAsciiName() {
        runAllConstructWithIllegalArgumentTests(//
            "COPY\u00A9", // ERROR: name only contain ASCII characters
            "label", // label
            "type", // type
            "OS", // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now(), // modification date
            "dataset names must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests constructing a SAS dataset description with a name that's too long.
     */
    @Test
    public void constructWithLongName() {
        runAllConstructWithIllegalArgumentTests(//
            "DATASET1X", // 9 characters is too long
            "label", // label
            "type", // type
            "OS", // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now(), // modification date
            "dataset names must not be longer than 8 characters");
    }

    /**
     * Tests constructing a variable with various names.
     */
    @Test
    public void testNameVariations() {
        final String label = "label";
        final String type = "";
        final String os = "OS";
        final String version = "9.3";
        final List<Variable> variables = newVariableList();
        final LocalDateTime date = LocalDateTime.now();

        String[] badNames = { "DATA SET", "DATA!SET", "DATA$SET", "1DATA" };
        for (String name : badNames) {
            runAllConstructWithIllegalArgumentTests(//
                name, //name
                label, // label
                type, // type
                os, // OS
                version, // SAS version
                variables, // variable list
                date, // creation date
                date, // modification date
                "dataset name is illegal for SAS");
        }

        String[] goodNames = { "DATA_SET", "A", "B", "Z", "_", "A10", "a", "z" };
        for (String name : goodNames) {
            // Create the dataset
            SasDatasetDescription dataset = new SasDatasetDescription(//
                name, //name
                label, // label
                type, // type
                os, // OS
                version, // SAS version
                variables, // variable list
                date, // creation date
                date); // modification date
            assertEquals(name, dataset.name());
        }
    }

    /**
     * Tests constructing a SAS dataset description with a label that's too long.
     */
    @Test
    public void constructWithLongLabel() {
        // Create two labels, one that just barely fits and one that's just barely too long.
        String limitLabel = TestUtil.repeatString("X", 40);
        String longLabel = limitLabel + "Y";

        // The 40 character label is permitted.
        SasDatasetDescription dataset = new SasDatasetDescription(//
            "DATASET", // name
            limitLabel, // label
            "type", // type
            "OS", // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now()); // modification date
        assertEquals(limitLabel, dataset.label());

        // The 41 character label is prohibited.
        runAllConstructWithIllegalArgumentTests(//
            "DATASET", // name
            longLabel, // label
            "type", // type
            "OS", // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now(), // modification date
            "dataset labels must not be longer than 40 characters");
    }

    /**
     * Tests constructing a SAS dataset description with a type that's too long.
     */
    @Test
    public void constructWithLongType() {
        // Create two types, one that just barely fits and one that's just barely too long.
        String limitType = TestUtil.repeatString("T", 8);
        String longType = limitType + "X";

        // The 8 character type is permitted.
        SasDatasetDescription dataset = new SasDatasetDescription(//
            "DATASET", // name
            "label", // label
            limitType, // type
            "OS", // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now()); // modification date
        assertEquals(limitType, dataset.type());

        // The 9 character type is prohibited.
        runAllConstructWithIllegalArgumentTests(//
            "DATASET", // name
            "label", // label
            longType, // type
            "OS", // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now(), // modification date
            "dataset types must not be longer than 8 characters");
    }

    /**
     * Tests constructing a SAS dataset description with a type that contains non-ASCII characters.
     */
    @Test
    public void constructWithNonAsciiType() {
        String name = "DATASET";
        String label = "label";
        String nonAsciiType = "BAD\u0080"; // ERROR
        String os = "OS";
        String version = "9.4";
        List<Variable> variables = newVariableList();
        LocalDateTime date = LocalDateTime.now();

        // public constructor should throw exception
        runConstructWithIllegalArgumentTest(//
            name, // name
            label, // label
            nonAsciiType, // type
            os, // OS
            version, // SAS version
            variables, // variable list
            date, // creation date
            date, // modification date
            "dataset type must contain only ASCII (7-bit) characters");

        // the "FDA" strictness should prohibit this
        runConstructWithIllegalArgumentTest(//
            name, // name
            label, // label
            nonAsciiType, // type
            os, // OS
            version, // SAS version
            variables, // variable list
            date, // creation date
            date, // modification date
            EnumSet.of(StrictnessMode.FDA_SUBMISSION), //
            "dataset type must contain only ASCII (7-bit) characters");

        // the "basic" strictness should permit this
        SasDatasetDescription dataset = new SasDatasetDescription(//
            name, // name
            label, // label
            nonAsciiType, // type
            os, // OS
            version, // SAS version
            variables, // variable list
            date, // creation date
            date, // modification date
            StrictnessMode.BASIC);
        assertEquals(nonAsciiType, dataset.type(), "non-ASCII type was not preserved");
    }

    /**
     * Tests constructing a SAS dataset description with a source operating system that's too long.
     */
    @Test
    public void constructWithLongSourceOperatingSystem() {
        // Create two operating systems, one that just barely fits and one that's just barely too long.
        String limitOperatingSystem = "OS_45678";
        String longOperatingSystem = limitOperatingSystem + "Y";

        // The 8 character operating system is permitted.
        SasDatasetDescription dataset = new SasDatasetDescription(//
            "DATASET", // name
            "label", // label
            "", // type
            limitOperatingSystem, // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now()); // modification date
        assertEquals(limitOperatingSystem, dataset.sourceOperatingSystem());

        // The 9 character operating system is prohibited.
        runAllConstructWithIllegalArgumentTests(//
            "DATASET", // name
            "label", // label
            "", // OS
            longOperatingSystem, // type
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now(), // modification date
            "dataset operating system must not be longer than 8 characters");
    }

    /**
     * Tests constructing a SAS dataset description with a source operating system that contains non-ASCII characters.
     */
    @Test
    public void constructWithNonAsciiSourceOperatingSystem() {
        runAllConstructWithIllegalArgumentTests(//
            "DATASET", // name
            "label", // label
            "", // type
            "ERROR\u0080", // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now(), // modification date
            "dataset operating system must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests constructing a SAS dataset description with a source SAS version that's too long.
     */
    @Test
    public void constructWithLongSourceSasVersion() {
        // Create two versions, one that just barely fits and one that's just barely too long.
        String limitVersion = "12345678";
        String longVersion = limitVersion + "9";

        // The 8 character SAS version is permitted.
        SasDatasetDescription dataset = new SasDatasetDescription(//
            "DATASET", // name
            "label", // label
            "", // type
            "OS", // OS
            limitVersion, // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now()); // modification date
        assertEquals(limitVersion, dataset.sourceSasVersion());

        // The 9 character SAS version is prohibited.
        runConstructWithIllegalArgumentTest(//
            "DATASET", // name
            "label", // label
            "", // type
            "OS", // OS
            longVersion, // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now(), // modification date
            "dataset SAS versions must not be longer than 8 characters");
    }

    /**
     * Tests constructing a SAS dataset description with one too many variables. The XPORT format only supports 9999.
     */
    @Test
    public void constructWithTooManyVariables() {
        // Create a list of variables that has exactly as many as permitted.
        List<Variable> limitVariables = new LinkedList<>();
        for (int i = 1; i <= 9999; i++) {
            Variable variable = new Variable(//
                "VAR" + i, // name
                i, //
                VariableType.NUMERIC, //
                8, //
                "label", //
                Format.UNSPECIFIED, //
                Justification.LEFT, //
                Format.UNSPECIFIED); //
            limitVariables.add(variable);
        }

        // Create a list of variables that has one more than permitted.
        List<Variable> tooManyVariables = new LinkedList<>(limitVariables);
        tooManyVariables.add(new Variable(//
            "VAR10000", // name
            10000, // number
            VariableType.NUMERIC, //
            8, //
            "label", //
            Format.UNSPECIFIED, //
            Justification.LEFT, //
            Format.UNSPECIFIED));

        // The 9,999 variable list is permitted.
        SasDatasetDescription dataset = new SasDatasetDescription(//
            "DATASET", // name
            "label", // label
            "", // type
            "OS", // OS
            "9.2", // SAS version
            limitVariables, // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now()); // modification date
        assertEquals(limitVariables.size(), dataset.variables().size());

        // The 10,000 variable list is prohibited.
        runConstructWithIllegalArgumentTest(//
            "DATASET", // name
            "label", // label
            "", // type
            "OS", // OS
            "9.2", // SAS version
            tooManyVariables, // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now(), // modification date
            "variables must not have more than 9999 variables");
    }

    /**
     * Tests constructing a SAS dataset description with a SAS version that contains non-ASCII characters.
     */
    @Test
    public void constructWithNonAsciiSourceSasVersion() {
        runAllConstructWithIllegalArgumentTests(//
            "DATASET", // name
            "label", // label
            "", // type
            "OS", // OS
            "9.\u00802", // ERROR: non-ASCII SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            LocalDateTime.now(), // modification date
            "dataset SAS versions must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests constructing a SAS dataset description with a null name.
     */
    @Test
    public void constructWithNullName() {
        runConstructWithNullArgumentTest(//
            null, // ERROR: null name
            "label", // label
            "type", // type
            "os", // os
            "9.3", // version
            newVariableList(), // variable list
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "name must not be null");
    }

    /**
     * Tests constructing a SAS dataset description with a null label.
     */
    @Test
    public void constructWithNullLabel() {
        runConstructWithNullArgumentTest(//
            "name", // name
            null, // ERROR: null label
            "type", // type
            "os", // os
            "9.3", // version
            newVariableList(), // variable list
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "label must not be null");
    }

    /**
     * Tests constructing a SAS dataset description with a null type.
     */
    @Test
    public void constructWithNullType() {
        runConstructWithNullArgumentTest(//
            "name", // name
            "label", // label
            null, // ERROR: null type
            "os", // os
            "9.3", // version
            newVariableList(), // variable list
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "type must not be null");
    }

    /**
     * Tests constructing a SAS dataset description with a null source operating system.
     */
    @Test
    public void constructWithNullSourceOperatingSystem() {
        runConstructWithNullArgumentTest(//
            "name", // name
            "label", // label
            "type", // type
            null, // ERROR: null OS
            "9.3", // version
            newVariableList(), // variable list
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "sourceOperatingSystem must not be null");
    }

    /**
     * Tests constructing a SAS dataset description with a null source SAS version.
     */
    @Test
    public void constructWithNullSourceSasVersion() {
        runConstructWithNullArgumentTest(//
            "name", // name
            "label", // label
            "type", // type
            "OS", // OS
            null, // ERROR: null SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "sourceSasVersion must not be null");
    }

    /**
     * Tests constructing a SAS dataset description with a null variable list.
     */
    @Test
    public void constructWithNullVariables() {
        runConstructWithNullArgumentTest(//
            "name", // name
            "label", // label
            "type", // type
            "OS", // OS
            "9.2", //version
            null, // ERROR: null variable list
            LocalDateTime.now(), // create date
            LocalDateTime.now(), // modified date
            "variables must not be null");
    }

    /**
     * Tests constructing a SAS dataset description with a null creation date.
     */
    @Test
    public void constructWithNullCreationDate() {
        runConstructWithNullArgumentTest(//
            "name", // name
            "label", // label
            "type", // type
            "OS", // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            null, // ERROR: create date
            LocalDateTime.now(), // modified date
            "createTime must not be null");
    }

    /**
     * Tests constructing a SAS dataset description with a null modification date.
     */
    @Test
    public void constructWithNullModificationDate() {
        runConstructWithNullArgumentTest(//
            "name", // name
            "label", // label
            "type", // type
            "OS", // OS
            "9.4", // SAS version
            newVariableList(), // variable list
            LocalDateTime.now(), // creation date
            null, // ERROR: modification date
            "modifiedTime must not be null");
    }
}