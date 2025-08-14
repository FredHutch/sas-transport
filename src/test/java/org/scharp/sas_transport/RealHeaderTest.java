///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link RealHeader} class.
 */
public class RealHeaderTest {

    /**
     * Unit tests for basic construction and property getting of the RealHeader class.
     *
     * @throws MalformedTransportFileException
     */
    @Test
    public void basicTest() throws MalformedTransportFileException {

        // Create a simple record from data
        RealHeader realHeader = new RealHeader(//
            new byte[] { //
                's', 'y', 'm', 'b', 'o', 'l', '1', ' ', // symbol1
                'S', 'Y', 'M', 'B', 'O', 'L', '2', ' ', // symbol2
                'S', 'A', 'S', 'L', 'I', 'B', ' ', ' ', // saslib
                '1', '.', '0', ' ', ' ', ' ', ' ', ' ', // version
                'O', 'S', ' ', ' ', ' ', ' ', ' ', ' ', // OS (blank-padded)
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', // blanks (24)
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
                '2', '5', 'O', 'C', 'T', '1', '7', ':', // create date (0-7)
                '1', '3', ':', '1', '8', ':', '0', '7', // create date (8-15)
            }, //
            (i) -> i);

        // Assert that it was read correctly
        assertEquals("symbol1", realHeader.symbol1());
        assertEquals("SYMBOL2", realHeader.symbol2());
        assertEquals("SASLIB", realHeader.sasLibrary());
        assertEquals("1.0", realHeader.sasVersion());
        assertEquals("OS", realHeader.operatingSystem());
        assertEquals(LocalDateTime.of(17, 10, 25, 13, 18, 7), realHeader.createDate());

        // Create a second header using NUL padding.
        // Only the OS is stripped of this padding
        realHeader = new RealHeader(//
            new byte[] { //
                's', 'y', 'm', 'b', 'o', 'l', '1', '\0', // symbol1 (NUL-padded)
                'S', 'Y', 'M', 'B', 'O', 'L', '2', '\0', // symbol2 (NUL-padded)
                'S', 'A', 'S', 'L', 'I', 'B', '\0', '\0', // saslib (NUL-padded)
                '1', '.', '0', '\0', '\0', '\0', '\0', '\0', // version (NUL-padded)
                'O', 'S', '\0', '\0', '\0', '\0', '\0', '\0', // OS (NUL-padded)
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', // blanks (24)
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
                '2', '5', 'O', 'C', 'T', '9', '9', ':', // create date (0-7)
                '1', '3', ':', '1', '8', ':', '0', '7', // create date (8-15)
            }, //
            (i) -> i);

        // Assert that it was read correctly
        assertEquals("symbol1\0", realHeader.symbol1());
        assertEquals("SYMBOL2\0", realHeader.symbol2());
        assertEquals("SASLIB\0\0", realHeader.sasLibrary());
        assertEquals("1.0\0\0\0\0\0", realHeader.sasVersion());
        assertEquals("OS", realHeader.operatingSystem()); // NUL are trimmed
        assertEquals(LocalDateTime.of(99, 10, 25, 13, 18, 7), realHeader.createDate());

        // Create a third header simulating the bugs in %loc2xpt
        // Only the OS is stripped of this padding
        realHeader = new RealHeader(//
            new byte[] { //
                's', 'y', 'm', 'b', 'o', 'l', '1', ' ', // symbol1
                'S', 'Y', 'M', 'B', 'O', 'L', '2', ' ', // symbol2
                'S', 'A', 'S', 'L', 'I', 'B', ' ', ' ', // saslib
                '1', '.', '0', ' ', ' ', ' ', ' ', ' ', // version
                ' ', 'O', 'S', '\0', '\0', '\0', '\0', '\0', // OS (simple the off-by-one bug
                '\0', ' ', ' ', ' ', ' ', ' ', ' ', ' ', // blanks (23, because the first byte is overwritten)
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
                '2', '5', 'O', 'C', 'T', '9', '9', ':', // create date (0-7)
                '1', '3', ':', '1', '8', ':', '0', '7', // create date (8-15)
            }, //
            (i) -> i);

        // Assert that it was read correctly
        assertEquals("symbol1", realHeader.symbol1());
        assertEquals("SYMBOL2", realHeader.symbol2());
        assertEquals("SASLIB", realHeader.sasLibrary());
        assertEquals("1.0", realHeader.sasVersion());
        assertEquals(" OS", realHeader.operatingSystem()); // should this be "OS" to compensate for the bug?
        assertEquals(LocalDateTime.of(99, 10, 25, 13, 18, 7), realHeader.createDate());
    }

    /**
     * Unit test for mapping the two digit year into a four digit year.
     *
     * @throws MalformedTransportFileException
     */
    @Test
    public void testConstructWithYearAdjustment() throws MalformedTransportFileException {

        RealHeader realHeader = new RealHeader(//
            new byte[] { //
                's', 'y', 'm', 'b', 'o', 'l', '1', ' ', // symbol1
                'S', 'Y', 'M', 'B', 'O', 'L', '2', ' ', // symbol2
                'S', 'A', 'S', 'L', 'I', 'B', ' ', ' ', // saslib
                '1', '.', '0', ' ', ' ', ' ', ' ', ' ', // version
                'O', 'S', ' ', ' ', ' ', ' ', ' ', ' ', // OS (blank-padded)
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', // blanks (24)
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
                '2', '5', 'O', 'C', 'T', '1', '5', ':', // create date (0-7)
                '1', '3', ':', '1', '8', ':', '0', '7', // create date (8-15)
            }, //
            (i) -> i + 2000); // simple offset

        // Assert that it was read correctly
        assertEquals("symbol1", realHeader.symbol1());
        assertEquals("SYMBOL2", realHeader.symbol2());
        assertEquals("SASLIB", realHeader.sasLibrary());
        assertEquals("1.0", realHeader.sasVersion());
        assertEquals("OS", realHeader.operatingSystem());
        assertEquals(LocalDateTime.of(2015, 10, 25, 13, 18, 7), realHeader.createDate());
    }

    /**
     * Tests creating a RealHeader with data that has a malformed creation time.
     */
    @Test
    public void testConstructWithMalformedCreateDate() {
        byte[] malformedFirstHeader = new byte[] { //
            's', 'y', 'm', 'b', 'o', 'l', '1', ' ', // symbol1
            'S', 'Y', 'M', 'B', 'O', 'L', '2', ' ', // symbol2
            'S', 'A', 'S', 'L', 'I', 'B', ' ', ' ', // saslib
            '1', '.', '0', ' ', ' ', ' ', ' ', ' ', // version
            'O', 'S', ' ', ' ', ' ', ' ', ' ', ' ', // OS (blank-padded)
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', // blanks (24)
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
            'X', '5', 'O', 'C', 'T', '1', '7', ':', // start time (0-7)
            '1', '3', ':', '1', '8', ':', '0', '7', // start time (8-15)
        };
        Exception exception = assertThrows(//
            MalformedTransportFileException.class, //
            () -> new RealHeader(malformedFirstHeader, (i) -> i), //
            "Creating RealHeader with malformed data");
        assertEquals("malformed date: X5OCT17:13:18:07", exception.getMessage());
    }

    /**
     * Tests creating a RealHeader with data that has malformed blanks.
     */
    @Test
    public void testConstructWithMalformedBlanks() {
        byte[] malformedFirstHeader = new byte[] { //
            's', 'y', 'm', 'b', 'o', 'l', '1', ' ', // symbol1
            'S', 'Y', 'M', 'B', 'O', 'L', '2', ' ', // symbol2
            'S', 'A', 'S', 'L', 'I', 'B', ' ', ' ', // saslib
            '1', '.', '0', ' ', ' ', ' ', ' ', ' ', // version
            'O', 'S', '\0', '\0', '\0', '\0', '\0', '\0', // OS (NUL-padded)
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', // blanks (24)
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', //
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', '!', // <--- CORRUPTION
            '2', '5', 'O', 'C', 'T', '1', '7', ':', // start time (0-7)
            '1', '3', ':', '1', '8', ':', '0', '7', // start time (8-15)
        };
        Exception exception = assertThrows(//
            MalformedTransportFileException.class, //
            () -> new RealHeader(malformedFirstHeader, (i) -> i), //
            "Creating RealHeader with malformed data");
        assertEquals("corrupt blanks region in REAL_HEADER", exception.getMessage());
    }
}