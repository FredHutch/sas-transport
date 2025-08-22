///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Format} class.
 */
public class FormatTest {

    private static void assertFormat(Format format, String expectedName, int expectedWidth,
        int expectedNumberOfDigits, String expectedStringForm) {

        // Test the accessor methods
        TestUtil.assertFormat(format, expectedName, expectedWidth, expectedNumberOfDigits);

        // Test toString()
        assertEquals(expectedStringForm, format.toString(), "toString() returned incorrect value");
    }

    @Test
    void basicTest() {
        // Test the three-argument constructor
        assertFormat(new Format("FORMAT", 1, 2), "FORMAT", 1, 2, "FORMAT1.2");
        assertFormat(new Format("", 5, 2), "", 5, 2, "5.2");
        assertFormat(new Format("$ASCII", 5, 0), "$ASCII", 5, 0, "$ASCII5.");
        assertFormat(new Format("$ASCII", 0, 0), "$ASCII", 0, 0, "$ASCII.");
        assertFormat(new Format("PERCENTN", 32, 31), "PERCENTN", 32, 31, "PERCENTN32.31");
        assertFormat(new Format("dollar", 15, 2), "dollar", 15, 2, "dollar15.2");
        assertFormat(new Format("LONGNAME", Short.MAX_VALUE, Short.MAX_VALUE), "LONGNAME", Short.MAX_VALUE,
            Short.MAX_VALUE, "LONGNAME32767.32767");

        // Test the two-argument constructor
        assertFormat(new Format("FORMAT", 1), "FORMAT", 1, 0, "FORMAT1.");
        assertFormat(new Format("", 1), "", 1, 0, "1.");
        assertFormat(new Format("$UPCASE", 100), "$UPCASE", 100, 0, "$UPCASE100.");
        assertFormat(new Format("PERCENTN", 6), "PERCENTN", 6, 0, "PERCENTN6.");
        assertFormat(new Format("LONGNAME", Short.MAX_VALUE), "LONGNAME", Short.MAX_VALUE, 0, "LONGNAME32767.");
    }

    /**
     * Tests the properties of {@link Format#UNSPECIFIED}.
     */
    @Test
    public void testUnspecified() {
        assertFormat(Format.UNSPECIFIED, "", 0, 0, "");
    }

    /**
     * Tests that the constructor throws an exception when given a null name.
     */
    @Test
    public void constructWithNullName() {
        Exception exception = assertThrows( //
            NullPointerException.class, //
            () -> new Format(null, 15, 0), //
            "creating VariableFormat with a null name");
        assertEquals("format name must not be null", exception.getMessage());

        exception = assertThrows( //
            NullPointerException.class, //
            () -> new Format(null, 15), //
            "creating Format with a null name");
        assertEquals("format name must not be null", exception.getMessage());
    }

    private static void runConstruct3WithIllegalArgumentTest(String name, int width, int numberOfDigits,
        String expectedErrorMessage) {
        Exception exception = assertThrows(//
            IllegalArgumentException.class, //
            () -> new Format(name, width, numberOfDigits), //
            "creating Format with an illegal argument");
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    private static void runConstruct2WithIllegalArgumentTest(String name, int length, String expectedErrorMessage) {
        Exception exception = assertThrows(//
            IllegalArgumentException.class, //
            () -> new Format(name, length), //
            "creating Format with an illegal argument");
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    /**
     * Tests constructing a format with a name that's too long.
     */
    @Test
    public void constructWithLongName() {
        runConstruct3WithIllegalArgumentTest("LONGNAMEX", 0, 0, "format name must not be longer than 8 characters");
        runConstruct2WithIllegalArgumentTest("LONGNAMEX", 0, "format name must not be longer than 8 characters");
    }

    /**
     * Tests constructing a format with a name that's not entirely ASCII characters.
     */
    @Test
    public void constructWithNonAsciiName() {
        final String expectedMessage = "format name must contain only ASCII (7-bit) characters";
        runConstruct3WithIllegalArgumentTest("BADNAME\u0080", 0, 0, expectedMessage);
        runConstruct2WithIllegalArgumentTest("BADNAME\u0080", 0, expectedMessage);
    }

    /**
     * Tests constructing a format with a negative length.
     */
    @Test
    public void constructWithNegativeLength() {
        runConstruct3WithIllegalArgumentTest("FORMAT", -1, 0, "format width must not be negative");
        runConstruct2WithIllegalArgumentTest("FORMAT", -1, "format width must not be negative");
    }

    /**
     * Tests constructing a format with a width that is too large.
     */
    @Test
    public void constructWithLargeLength() {
        final int largeWidth = Short.MAX_VALUE + 1;
        final String expectedMessage = "format width must not be greater than Short.MAX_VALUE";
        runConstruct3WithIllegalArgumentTest("FORMAT", largeWidth, 0, expectedMessage);
        runConstruct2WithIllegalArgumentTest("FORMAT", largeWidth, expectedMessage);
    }

    /**
     * Tests constructing a format with a negative numberOfDigits.
     */
    @Test
    public void constructWithNegativeDecimals() {
        runConstruct3WithIllegalArgumentTest("FORMAT", 10, -1, "format numberOfDigits must not be negative");
    }

    /**
     * Tests constructing a format with a numberOfDigits that is too large.
     */
    @Test
    public void constructWithLargeDecimals() {
        runConstruct3WithIllegalArgumentTest("FORMAT", Short.MAX_VALUE, Short.MAX_VALUE + 1,
            "format numberOfDigits must not be greater than Short.MAX_VALUE");
    }

    static private String copy(String string) {
        return new String(string);
    }

    /**
     * Tests {@link Format#hashCode()}.
     */
    @Test
    public void testHashCode() {
        // Create a format.
        Format format = new Format("MyFormat", 10, 2);

        // Create a copy that has the same name but from a different String reference.
        Format formCopy = new Format(copy("MyFormat"), 10, 2);

        // The copy must hash to the same value as the original.
        assertEquals(format.hashCode(), formCopy.hashCode());

        // Create a pair of equal formats from different constructors.
        Format ascii10 = new Format("$ASCII", 10);
        Format ascii10Copy = new Format("$ASCII", 10, 0);

        // The copy must hash to the same value as the original.
        assertEquals(ascii10.hashCode(), ascii10Copy.hashCode());

        // Create a pair of formats that have empty strings.
        Format emptyFormat = new Format("", 0, 0);
        Format emptyFormatCopy = new Format(copy(""), 0);

        // The copy must hash to the same value as the original.
        assertEquals(emptyFormat.hashCode(), emptyFormatCopy.hashCode());
    }

    /**
     * Tests {@link Format#equals(Object)}.
     */
    @SuppressWarnings({ "unlikely-arg-type", "EqualsBetweenInconvertibleTypes" })
    @Test
    public void testEquals() {
        // Create a format.
        Format format = new Format("MyFormat", 10, 2);

        // Create a copy that has the same name but from a different String reference.
        Format formatCopy = new Format(copy("MyFormat"), 10, 2);

        // Create another pair for different values (differs in case)
        Format format2 = new Format("myFormat", 10, 2);
        Format format2Copy = new Format(copy("myFormat"), 10, 2);

        // Create a pair of equal formats from different constructors.
        Format ascii10 = new Format("$ASCII", 10);
        Format ascii10Copy = new Format("$ASCII", 10, 0);

        // Create a pair of formats that have empty strings.
        Format emptyFormat = new Format("", 0, 0);
        Format emptyFormatCopy = new Format(copy(""), 0);

        // Create formats that only differ in exactly one field (and only by case for strings).
        Format differentName = new Format("myFormat", 10, 2);
        Format differentWidth = new Format("MyFormat", 9, 2);
        Format differentNumberOfDigits = new Format("MyFormat", 10, 1);

        List<Format> allFormats = Arrays.asList( //
            format, //
            formatCopy, //
            format2,  //
            format2Copy, //
            ascii10, //
            ascii10Copy, //
            emptyFormat, //
            emptyFormatCopy, //
            differentName, //
            differentWidth, //
            differentNumberOfDigits);

        // Equals is reflexive (special case)
        for (Format currentFormat : allFormats) {
            assertTrue(currentFormat.equals(currentFormat));
        }

        // Equivalent formats are equal.
        assertTrue(format.equals(formatCopy));
        assertTrue(format2.equals(format2Copy));
        assertTrue(ascii10.equals(ascii10Copy));
        assertTrue(emptyFormat.equals(emptyFormatCopy));

        // Different formats are not equal.
        assertFalse(format.equals(format2));
        assertFalse(format.equals(format2Copy));
        assertFalse(format.equals(ascii10));
        assertFalse(format.equals(ascii10Copy));
        assertFalse(format.equals(emptyFormatCopy));
        assertFalse(format.equals(emptyFormatCopy));
        assertFalse(format.equals(differentName));
        assertFalse(format.equals(differentWidth));
        assertFalse(format.equals(differentNumberOfDigits));
        assertFalse(differentName.equals(format));
        assertFalse(differentWidth.equals(format));
        assertFalse(differentNumberOfDigits.equals(differentName));

        // Equality is symmetric.
        assertTrue(formatCopy.equals(format));
        assertTrue(format2Copy.equals(format2));
        assertTrue(ascii10Copy.equals(ascii10));
        assertTrue(emptyFormatCopy.equals(emptyFormat));

        // Nothing is equal to null.
        for (Format currentFormat : allFormats) {
            assertFalse(currentFormat.equals(null));
        }

        // Test comparing against something that isn't a Format
        assertFalse(format.equals(format.name()));
    }
}