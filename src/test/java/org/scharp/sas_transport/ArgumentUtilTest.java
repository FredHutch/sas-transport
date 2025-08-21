///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests for {@link ArgumentUtil} */
public class ArgumentUtilTest {

    /** Tests for {@link ArgumentUtil#checkNotNull} */
    @Test
    void testCheckNotNull() {
        // The empty string is not null
        ArgumentUtil.checkNotNull("", "arg");

        // Zero is not null
        ArgumentUtil.checkNotNull(0, "arg");

        // Pass in null
        Exception exception = assertThrows(NullPointerException.class, () -> ArgumentUtil.checkNotNull(null, "arg"));
        assertEquals("arg must not be null", exception.getMessage());

        // Different argument name
        exception = assertThrows(NullPointerException.class, () -> ArgumentUtil.checkNotNull(null, "the argument"));
        assertEquals("the argument must not be null", exception.getMessage());
    }

    /** Tests for {@link ArgumentUtil#checkIsAscii} */
    @Test
    void testCheckIsAscii() {
        // The empty string is ASCII
        ArgumentUtil.checkIsAscii("", "arg");

        // Test all ASCII characters
        StringBuilder allAscii = new StringBuilder();
        for (int codePoint = 0; codePoint < 128; codePoint++) {
            allAscii.append(Character.toChars(codePoint));
        }
        ArgumentUtil.checkIsAscii(allAscii.toString(), "arg");

        // Non-ASCII
        String nonAsciiString = new String(Character.toChars(128));
        Exception exception = assertThrows( //
            IllegalArgumentException.class, //
            () -> ArgumentUtil.checkIsAscii(nonAsciiString, "arg"));
        assertEquals("arg must contain only ASCII (7-bit) characters", exception.getMessage());

        // Try something that's not on the BMP.
        String grinningFace = new String(Character.toChars(0x1F600));
        exception = assertThrows( //
            IllegalArgumentException.class, //
            () -> ArgumentUtil.checkIsAscii(grinningFace, "myArg"));
        assertEquals("myArg must contain only ASCII (7-bit) characters", exception.getMessage());
    }

    /** Tests for {@link ArgumentUtil#checkMaximumLength} */
    @Test
    void testCheckMaximumLength() {
        // empty string fits into 0 bytes.
        ArgumentUtil.checkMaximumLength("", 0, "arg");

        // empty string fits into 1 byte.
        ArgumentUtil.checkMaximumLength("", 1, "arg");
        ArgumentUtil.checkMaximumLength("", 1, "arg");

        ArgumentUtil.checkMaximumLength("hello", 5, "arg");

        Exception exception = assertThrows( //
            IllegalArgumentException.class, //
            () -> ArgumentUtil.checkMaximumLength("hello", 4, "arg"));
        assertEquals("arg must not be longer than 4 characters", exception.getMessage());

        // different length/argument name
        exception = assertThrows( //
            IllegalArgumentException.class, //
            () -> ArgumentUtil.checkMaximumLength("hello", 3, "myArg"));
        assertEquals("myArg must not be longer than 3 characters", exception.getMessage());
    }

    /** Tests for {@link ArgumentUtil#checkNotNegative} */
    @Test
    void testCheckNotNegative() {
        // non-negative tests
        ArgumentUtil.checkNotNegative(0, "arg");
        ArgumentUtil.checkNotNegative(0, "argument");
        ArgumentUtil.checkNotNegative(1, "argument");
        ArgumentUtil.checkNotNegative(Integer.MAX_VALUE, "argument");

        Exception exception = assertThrows(
            IllegalArgumentException.class,
            () -> ArgumentUtil.checkNotNegative(-1, "arg"));
        assertEquals("arg must not be negative", exception.getMessage());

        exception = assertThrows(
            IllegalArgumentException.class,
            () -> ArgumentUtil.checkNotNegative(Integer.MIN_VALUE, "myArg"));
        assertEquals("myArg must not be negative", exception.getMessage());
    }
}