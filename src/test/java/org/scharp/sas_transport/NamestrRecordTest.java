///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the {@link NamestrRecord} class.
 */
public class NamestrRecordTest {

    /**
     * Unit tests for basic construction and property getting of the NamestrRecord class.
     */
    @Test
    public void basicTest() {

        // Create a simple record from data
        NamestrRecord namestrRecord = new NamestrRecord(new byte[] { //
            0, 1, // numeric
            0, 0, // hash
            0, 16, // length in observation
            0, 5, // variable number
            'v', 'a', 'r', 'i', 'a', 'b', 'l', 'e', // variable name
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', // label (1-10)
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', // label (11-20)
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', // label (21-30)
            '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', // label (31-40)
            'F', 'O', 'R', 'M', 'A', 'T', 'X', 'Y', // output format name
            0, 15, // output format length
            0, 0, // output format decimals
            0, 0, // LEFT justification
            0, 0, // filler
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', // input format name
            0, 0, // input format length
            0, 0, // input format decimals
            0, 0, 0, 80, // position in observation
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, // ignored (1-20)
            31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, // ignored (21-50)
            51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62,// ignored (51-62)
        });

        // Assert that it was read correctly
        assertEquals(VariableType.NUMERIC, namestrRecord.type());
        assertEquals(16, namestrRecord.length());
        assertEquals(5, namestrRecord.number());
        assertEquals("variable", namestrRecord.name());
        assertEquals("0123456789abcdefghijABCDEFGHIJ!@#$%^&*()", namestrRecord.label());
        assertEquals(Justification.LEFT, namestrRecord.justification());
        assertEquals(80, namestrRecord.offsetInObservation());
        TestUtil.assertFormat(namestrRecord.format(), "FORMATXY", 15, 0);
        TestUtil.assertFormat(namestrRecord.inputFormat(), "", 0, 0);

        // Now create a record that has numbers that don't fit into a single byte
        NamestrRecord largeNumberRecord = new NamestrRecord(new byte[] { //
            0, 2, // character
            0, 0, // hash
            1, (byte) 0xFF, // length in observation
            0x7F, (byte) 0xFF, // variable number
            'b', 'i', 'g', 'v', 'a', 'r', ' ', ' ', // variable name
            'b', 'i', '2', '3', '4', '5', '6', '7', '8', '9', // label (1-10)
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', // label (11-20)
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', // label (21-30)
            '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', // label (31-40)
            'F', 'O', 'R', 'M', 'A', 'T', 'X', 'Y', // output format name
            0x7F, (byte) 0xFF, // output format length
            0x7F, (byte) 0xFF, // output format decimals
            0, 1, // RIGHT justification
            0, 0, // filler
            'I', 'N', 'F', 'O', 'R', 'M', 'A', 'T', // input format name
            0x7F, (byte) 0xFF, // input format length
            0x7F, (byte) 0xFF, // input format decimals
            0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, // position in observation
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, // ignored (1-20)
            31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, // ignored (21-50)
            51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62,// ignored (51-62)
        });
        // Assert that it was read correctly
        assertEquals(VariableType.CHARACTER, largeNumberRecord.type());
        assertEquals(0x1FF, largeNumberRecord.length());
        assertEquals(Short.MAX_VALUE, largeNumberRecord.number());
        assertEquals("bigvar", largeNumberRecord.name());
        assertEquals("0123456789abcdefghijABCDEFGHIJ!@#$%^&*()", namestrRecord.label());
        assertEquals(Justification.RIGHT, largeNumberRecord.justification());
        assertEquals(Integer.MAX_VALUE, largeNumberRecord.offsetInObservation());
        TestUtil.assertFormat(largeNumberRecord.format(), "FORMATXY", Short.MAX_VALUE, Short.MAX_VALUE);
        TestUtil.assertFormat(largeNumberRecord.inputFormat(), "INFORMAT", Short.MAX_VALUE, Short.MAX_VALUE);

        // Now create a record has an unknown justification and other strangeness that is ignored.
        NamestrRecord slightlyMalformedRecord = new NamestrRecord(new byte[] { //
            0, 1, // numeric
            55, 55, // hash (ignored)
            0, 16, // length in observation
            0, 5, // variable number
            'n', 'o', 'a', 's', 'c', 'i', 'i', (byte) 0xFF, // variable name
            'n', 'o', 'n', '-', 'A', 'S', 'C', 'I', 'I', ':', // label (1-10)
            ' ', (byte) 0xFF, ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', // label (11-20)
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', // label (21-30)
            ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', // label (31-40)
            'f', 'o', 'r', 'm', 'a', 't', ' ', ' ', // output format name
            0, 15, // output format length
            0, 0, // output format decimals
            0, 99, // UNKNOWN justification
            0, 0, // filler
            'I', 'N', 'F', 'O', 'R', 'M', 'A', 'T', // input format name
            0, 0, // input format length
            0, 0, // input format decimals
            0, 0, 0, 0, // position in observation
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, // ignored (1-20)
            31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, // ignored (21-50)
            51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62,// ignored (51-62)
        });
        // Assert that it was read correctly
        assertEquals(VariableType.NUMERIC, slightlyMalformedRecord.type());
        assertEquals(16, slightlyMalformedRecord.length());
        assertEquals(5, slightlyMalformedRecord.number());
        assertEquals("noascii\uFFFD", slightlyMalformedRecord.name());
        assertEquals("non-ASCII: \uFFFD", slightlyMalformedRecord.label());
        assertEquals(Justification.UNKNOWN, slightlyMalformedRecord.justification());
        assertEquals(0, slightlyMalformedRecord.offsetInObservation());
        TestUtil.assertFormat(slightlyMalformedRecord.format(), "format", 15, 0);
        TestUtil.assertFormat(slightlyMalformedRecord.inputFormat(), "INFORMAT", 0, 0);
    }
}