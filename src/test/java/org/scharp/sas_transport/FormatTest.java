///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link Format} class.
 */
public class FormatTest {

    /**
     * Unit tests for basic construction and property getting of the Format class.
     */
    @Test
    public void basicTest() {
        // Basic tests for full constructor
        TestUtil.assertFormat(new Format("", 10, 5), "", 10, 5);
        TestUtil.assertFormat(new Format("$CHAR", 10, 0), "$CHAR", 10, 0);

        // Test the overload with no decimals
        TestUtil.assertFormat(new Format("", 10), "", 10, 0);

        // Longest possible format name
        TestUtil.assertFormat(new Format("LONGNAME", 10, 0), "LONGNAME", 10, 0);
        TestUtil.assertFormat(new Format("LONGNAME", 10), "LONGNAME", 10, 0);

        // Min length
        TestUtil.assertFormat(new Format("FORMAT", 0, 0), "FORMAT", 0, 0);
        TestUtil.assertFormat(new Format("FORMAT", 0), "FORMAT", 0, 0);

        // Max length
        TestUtil.assertFormat(new Format("$TEXT", Short.MAX_VALUE, 0), "$TEXT", Short.MAX_VALUE, 0);
        TestUtil.assertFormat(new Format("$TEXT", Short.MAX_VALUE), "$TEXT", Short.MAX_VALUE, 0);
    }

    /**
     * Tests the properties of {@link Format#UNSPECIFIED}.
     */
    @Test
    public void testUnspecified() {
        TestUtil.assertFormat(Format.UNSPECIFIED, "", 0, 0);
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
}