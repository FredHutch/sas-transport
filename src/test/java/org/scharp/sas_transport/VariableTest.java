///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link Variable} class.
 */
public class VariableTest {

    /**
     * Unit tests for basic construction and property getting of the Variable class.
     */
    @Test
    public void basicTest() {
        // Test a variable with all fields set to values
        Variable variableA = new Variable(//
            "V", // minimum variable name length
            1, //
            VariableType.CHARACTER, //
            200, //
            "The Label", //
            new Format("OUTPUT", 6, 2), //
            Justification.LEFT, //
            new Format("INPUT", 5, 1));
        TestUtil.assertVariable(//
            variableA, //
            "V", // name
            1, // number
            VariableType.CHARACTER, // type
            200, // length
            "The Label", // label
            new Format("OUTPUT", 6, 2), // output format
            Justification.LEFT, // justification
            new Format("INPUT", 5, 1)); // input format

        // Test a variable with all optional fields left blank/unspecified.
        Variable variableB = new Variable(//
            "_2345678", // maximum variable name length
            2, //
            VariableType.NUMERIC, //
            6, //
            "", //
            Format.UNSPECIFIED, //
            Justification.RIGHT, //
            Format.UNSPECIFIED);
        TestUtil.assertVariable(//
            variableB, //
            "_2345678", // name
            2, // number
            VariableType.NUMERIC, // type
            6, // length
            "", // label
            Format.UNSPECIFIED, // output format
            Justification.RIGHT, // justification
            Format.UNSPECIFIED); // input format

        // Create a questionably-formed variable using an explicit strictness mode
        Variable basicCheckedVariable = new Variable(//
            "V", // minimum variable name length
            1, //
            VariableType.CHARACTER, //
            10, //
            "Non-Ascii \u03A3", // non-ASCII permitted
            new Format("OUTPUT", 6, 2), //
            Justification.LEFT, //
            new Format("INPUT", 5, 1), //
            StrictnessMode.BASIC);
        TestUtil.assertVariable(//
            basicCheckedVariable, //
            "V", // name
            1, // number
            VariableType.CHARACTER, // type
            10, // length
            "Non-Ascii \u03A3", // label
            new Format("OUTPUT", 6, 2), // output format
            Justification.LEFT, // justification
            new Format("INPUT", 5, 1)); // input format
    }

    /**
     * Invokes the public constructor with the given arguments, expecting an IllegalArgumentException.
     *
     * @param variableName
     * @param variableNumber
     * @param type
     * @param variableLength
     * @param label
     * @param outputFormat
     * @param outputFormatJustification
     * @param inputFormat
     * @param expectedErrorMessage
     */
    private static void runConstructWithIllegalArgumentTest(String variableName, int variableNumber, VariableType type,
        int variableLength, String label, Format outputFormat, Justification outputFormatJustification,
        Format inputFormat, String expectedErrorMessage) {
        Exception exception = assertThrows(//
            IllegalArgumentException.class, //
            () -> new Variable(variableName, variableNumber, type, variableLength, label, outputFormat,
                outputFormatJustification, inputFormat), //
            "creating variable with an illegal argument");
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    /**
     * Invokes the package-private constructor with the given arguments for each of the supplied strictness modes,
     * expecting an IllegalArgumentException on each one.
     *
     * @param variableName
     * @param variableNumber
     * @param type
     * @param variableLength
     * @param label
     * @param outputFormat
     * @param outputFormatJustification
     * @param inputFormat
     * @param strictnessMode
     * @param expectedErrorMessage
     */
    private static void runConstructWithIllegalArgumentTest(String variableName, int variableNumber, VariableType type,
        int variableLength, String label, Format outputFormat, Justification outputFormatJustification,
        Format inputFormat, StrictnessMode strictnessMode, String expectedErrorMessage) {
        Exception exception = assertThrows(//
            IllegalArgumentException.class, //
            () -> new Variable(variableName, variableNumber, type, variableLength, label, outputFormat,
                outputFormatJustification, inputFormat, strictnessMode), //
            "creating variable with an illegal argument in " + strictnessMode);
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    /**
     * Invokes the public constructor and each of the package-private constructors with the given arguments for all
     * strictness modes, expecting an IllegalArgumentException on each one.
     *
     * @param variableName
     * @param variableNumber
     * @param type
     * @param variableLength
     * @param label
     * @param outputFormat
     * @param outputFormatJustification
     * @param inputFormat
     * @param expectedErrorMessage
     */
    private static void runAllConstructWithIllegalArgumentTests(String variableName, int variableNumber,
        VariableType type, int variableLength, String label, Format outputFormat,
        Justification outputFormatJustification, Format inputFormat, String expectedErrorMessage) {
        // invoke the public constructor
        runConstructWithIllegalArgumentTest(variableName, variableNumber, type, variableLength, label, outputFormat,
            outputFormatJustification, inputFormat, expectedErrorMessage);

        // invoke the package-private constructor in the requested strictness modes
        for (StrictnessMode strictnessMode : EnumSet.allOf(StrictnessMode.class)) {
            runConstructWithIllegalArgumentTest(variableName, variableNumber, type, variableLength, label, outputFormat,
                outputFormatJustification, inputFormat, strictnessMode, expectedErrorMessage);
        }
    }

    private static void runConstructWithNullArgumentTest(String variableName, int variableNumber, VariableType type,
        int variableLength, String label, Format outputFormat, Justification outputFormatJustification,
        Format inputFormat, String expectedErrorMessage) {
        // The null parameter check should apply to the public constructor
        Exception exception = assertThrows(//
            NullPointerException.class, //
            () -> new Variable(variableName, variableNumber, type, variableLength, label, outputFormat,
                outputFormatJustification, inputFormat), //
            "creating variable with a null argument");
        assertEquals(expectedErrorMessage, exception.getMessage());

        // Null parameter checking should apply in all strictness modes.
        for (StrictnessMode strictnessMode : StrictnessMode.values()) {
            exception = assertThrows(//
                NullPointerException.class, //
                () -> new Variable(variableName, variableNumber, type, variableLength, label, outputFormat,
                    outputFormatJustification, inputFormat, strictnessMode), //
                "creating variable with a null argument in " + strictnessMode);
            assertEquals(expectedErrorMessage, exception.getMessage());
        }
    }

    /**
     * Tests constructing a numeric variable with a length that's too small.
     */
    @Test
    public void constructNumericTooShort() {
        runAllConstructWithIllegalArgumentTests(//
            "var", // name
            2, // number
            VariableType.NUMERIC, // type
            1, // too short (for numeric)
            "", // label
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "numeric variables must have a length between 2-8");
    }

    /**
     * Tests constructing a numeric variable with a length that's too large.
     */
    @Test
    public void constructNumericTooLong() {
        runAllConstructWithIllegalArgumentTests(//
            "var", // name
            2, // number
            VariableType.NUMERIC, // type
            9, // too long (for numeric)
            "", // label
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "numeric variables must have a length between 2-8");
    }

    /**
     * Tests constructing a character variable with a length of 0 (too small).
     */
    @Test
    public void constructCharacterWithZeroLength() {
        runAllConstructWithIllegalArgumentTests(//
            "var", // name
            2, // number
            VariableType.CHARACTER, // type
            0, // too short (for character)
            "", // label
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "character variables must have a positive length");
    }

    private static void createValidVariable(String variableName, int variableNumber, VariableType type,
        int variableLength, String label, Format outputFormat, Justification outputFormatJustification,
        Format inputFormat, StrictnessMode strictness) {

        // Construct the variable in the requested strictness mode.
        Variable newVariable = new Variable(variableName, variableNumber, type, variableLength, label, outputFormat,
            outputFormatJustification, inputFormat, strictness);

        // All fields should be exactly as set.
        TestUtil.assertVariable(newVariable, variableName, variableNumber, type, variableLength, label, outputFormat,
            outputFormatJustification, inputFormat);
    }

    /**
     * Tests constructing a character variable with a length that's too large.
     */
    @Test
    public void constructCharacterTooLong() {
        final int limitLength = 200;

        // Confirm that the limit can be constructed.
        Variable variable = new Variable(//
            "VARIABLE", // variable name
            2, // number
            VariableType.CHARACTER, // type
            limitLength, // just barely fits (for character data)
            "label", // label
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED); // input format
        assertEquals(limitLength, variable.length(), "length was not saved");

        // Confirm that beyond the limit, variables cannot be constructed with the public constructor.
        runConstructWithIllegalArgumentTest(//
            variable.name(), // name
            variable.number(), // number
            variable.type(), // type
            variable.length() + 1, // too long (for character)
            variable.label(), // label
            variable.inputFormat(), // output format
            variable.outputFormatJustification(), // output format justification
            variable.inputFormat(), // input format
            "character variables must not have a length greater than 200");

        // Confirm that beyond the limit, variables cannot be constructed with FDA strictness
        runConstructWithIllegalArgumentTest(//
            variable.name(), // name
            variable.number(), // number
            variable.type(), // type
            variable.length() + 1, // too long (for character)
            variable.label(), // label
            variable.inputFormat(), // output format
            variable.outputFormatJustification(), // output format justification
            variable.inputFormat(), // input format
            StrictnessMode.FDA_SUBMISSION, //
            "character variables must not have a length greater than 200");

        // In BASIC strictness mode, lengths can go up to Short.MAX_VALUE
        createValidVariable(//
            variable.name(), // name
            variable.number(), // number
            variable.type(), // type
            Short.MAX_VALUE, // length
            variable.label(), // label
            variable.inputFormat(), // output format
            variable.outputFormatJustification(), // output format justification
            variable.inputFormat(), // input format
            StrictnessMode.BASIC);
    }

    /**
     * Tests constructing a variable with a name that includes a non-ASCII character.
     */
    @Test
    public void constructVariableWithNonAsciiName() {
        runAllConstructWithIllegalArgumentTests(//
            "BAD\u0080", // ERROR: has non-ASCII character
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            "", // label
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "variable names must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests constructing a variable with a blank name.
     */
    @Test
    public void constructVariableWithBlankName() {
        runAllConstructWithIllegalArgumentTests(//
            "", // ERROR: can't be blank
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            "", // label
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "variable names cannot be blank");
    }

    /**
     * Tests constructing a variable with a name that is 9 characters. This would have to be truncated if it were
     * exported to an XPORT file.
     */
    @Test
    public void testNameTooLong() {
        runAllConstructWithIllegalArgumentTests(//
            "V23456789", // ERROR: can't be 9 characters long
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            "", // label
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "variable names must not be longer than 8 characters");
    }

    /**
     * Tests constructing a variable with various names.
     */
    @Test
    public void testNameVariations() {
        String[] badNames = { "VAR A", "VAR!A", "VAR$", "1" };
        for (String name : badNames) {
            // These names are bad in all strictness modes.
            runAllConstructWithIllegalArgumentTests(//
                name, // ERROR: can't contain spaces
                2, // number
                VariableType.CHARACTER, // type
                16, // length
                "", // label
                Format.UNSPECIFIED, // output format
                Justification.LEFT, // output format justification
                Format.UNSPECIFIED, // input format
                "variable name is illegal for SAS");
        }

        String[] goodNames = { "A", "B", "Z", "_", "A10", "_N_", "_ALL_" };
        for (String name : goodNames) {
            // Create the variable with the public constructor.
            Variable variable = new Variable(name, 2, VariableType.CHARACTER, 16, "label", Format.UNSPECIFIED,
                Justification.LEFT, Format.UNSPECIFIED);
            assertEquals(name, variable.name());

            // These names should be acceptable in all strictness modes.
            for (StrictnessMode strictnessMode : StrictnessMode.values()) {
                createValidVariable(name, 2, VariableType.CHARACTER, 16, "label", Format.UNSPECIFIED,
                    Justification.LEFT, Format.UNSPECIFIED, strictnessMode);
            }
        }
    }

    /**
     * Tests constructing a variable with a label that has a non-ASCII character. This is not permitted by FDA
     * submission guidelines.
     */
    @Test
    public void testNonAsciiLabel() {

        final String nonAsciiLabel = "Non-Ascii \u03A3";

        // public constructor
        runConstructWithIllegalArgumentTest(//
            "VARIABLE", // variable name
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            nonAsciiLabel, // non-ASCII not permitted
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "variable labels must contain only ASCII (7-bit) characters");

        // permitted with basic strictness
        Variable variableA = new Variable(//
            "VARIABLE", // variable name
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            nonAsciiLabel, // non-ASCII not permitted
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            StrictnessMode.BASIC);
        assertEquals(nonAsciiLabel, variableA.label(), "non-ASCII label was not saved");

        // prohibited with FDA strictness
        runConstructWithIllegalArgumentTest(//
            "VARIABLE", // variable name
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            nonAsciiLabel, // non-ASCII not permitted
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            StrictnessMode.FDA_SUBMISSION, // prohibited in FDA submission mode.
            "variable labels must contain only ASCII (7-bit) characters");
    }

    /**
     * Tests constructing a variable with a label that is too long to fit within an XPORT.
     */
    @Test
    public void testLongLabel() {
        // Create two labels, one that just barely fits and one that's just barely too long.
        String limitLabel = TestUtil.repeatString("X", 40);
        String longLabel = limitLabel + "Y";

        Variable variable = new Variable(//
            "VARIABLE", // variable name
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            limitLabel, // just barely fits
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED); // input format
        assertEquals(limitLabel, variable.label(), "label was not saved");

        // The 41 character label is prohibited.
        runAllConstructWithIllegalArgumentTests(//
            "VARIABLE", // variable name
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            longLabel, // long label (illegal)
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "variable labels must not be longer than 40 characters");
    }

    /**
     * Tests constructing a variable with a {@code null} name.
     */
    @Test
    public void constructWithNullName() {
        runConstructWithNullArgumentTest(//
            null, // ERROR: can't be null
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            "", // label
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "variableName must not be null");
    }

    /**
     * Tests constructing a variable with a {@code null} label.
     */
    @Test
    public void constructWithNullLabel() {
        runConstructWithNullArgumentTest(//
            "VAR", // name
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            null, // ERROR: can't be null
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "label must not be null");
    }

    /**
     * Tests constructing a variable with a {@code null} output format.
     */
    @Test
    public void constructWithNullOutputFormat() {
        runConstructWithNullArgumentTest(//
            "VAR", // name
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            "", // label
            null, // output format
            Justification.LEFT, // output format justification
            Format.UNSPECIFIED, // input format
            "outputFormat must not be null");
    }

    /**
     * Tests constructing a variable with a {@code null} justification.
     */
    @Test
    public void constructWithNullJustification() {
        runConstructWithNullArgumentTest(//
            "VAR", // name
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            "", // label
            Format.UNSPECIFIED, // output format
            null, // ERROR: justification can't be null
            Format.UNSPECIFIED, // input format
            "outputFormatJustification must not be null");
    }

    /**
     * Tests constructing a variable with a {@code null} input format.
     */
    @Test
    public void constructWithNullInputFormat() {
        runConstructWithNullArgumentTest(//
            "VAR", // name
            2, // number
            VariableType.CHARACTER, // type
            16, // length
            "", // label
            Format.UNSPECIFIED, // output format
            Justification.LEFT, // output format justification
            null, // input format
            "inputFormat must not be null");
    }
}