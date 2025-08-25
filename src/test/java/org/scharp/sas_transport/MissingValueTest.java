///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@link MissingValue} */
public class MissingValueTest {

    /** Tests for {@link MissingValue#toString}. */
    @Test
    void testToString() {
        assertEquals(".", MissingValue.STANDARD.toString());
        assertEquals("._", MissingValue.UNDERSCORE.toString());
        assertEquals(".A", MissingValue.A.toString());
        assertEquals(".B", MissingValue.B.toString());
        assertEquals(".C", MissingValue.C.toString());
        assertEquals(".D", MissingValue.D.toString());
        assertEquals(".E", MissingValue.E.toString());
        assertEquals(".F", MissingValue.F.toString());
        assertEquals(".G", MissingValue.G.toString());
        assertEquals(".H", MissingValue.H.toString());
        assertEquals(".I", MissingValue.I.toString());
        assertEquals(".J", MissingValue.J.toString());
        assertEquals(".K", MissingValue.K.toString());
        assertEquals(".L", MissingValue.L.toString());
        assertEquals(".M", MissingValue.M.toString());
        assertEquals(".N", MissingValue.N.toString());
        assertEquals(".O", MissingValue.O.toString());
        assertEquals(".P", MissingValue.P.toString());
        assertEquals(".Q", MissingValue.Q.toString());
        assertEquals(".R", MissingValue.R.toString());
        assertEquals(".S", MissingValue.S.toString());
        assertEquals(".T", MissingValue.T.toString());
        assertEquals(".U", MissingValue.U.toString());
        assertEquals(".V", MissingValue.V.toString());
        assertEquals(".W", MissingValue.W.toString());
        assertEquals(".X", MissingValue.X.toString());
        assertEquals(".Y", MissingValue.Y.toString());
        assertEquals(".Z", MissingValue.Z.toString());
    }

    /** Tests for {@link MissingValue#xportByteRepresentation()}. */
    @Test
    void testXportByteRepresentation() {
        assertEquals('.', MissingValue.STANDARD.xportByteRepresentation());
        assertEquals('_', MissingValue.UNDERSCORE.xportByteRepresentation());
        assertEquals('A', MissingValue.A.xportByteRepresentation());
        assertEquals('B', MissingValue.B.xportByteRepresentation());
        assertEquals('C', MissingValue.C.xportByteRepresentation());
        assertEquals('D', MissingValue.D.xportByteRepresentation());
        assertEquals('E', MissingValue.E.xportByteRepresentation());
        assertEquals('F', MissingValue.F.xportByteRepresentation());
        assertEquals('G', MissingValue.G.xportByteRepresentation());
        assertEquals('H', MissingValue.H.xportByteRepresentation());
        assertEquals('I', MissingValue.I.xportByteRepresentation());
        assertEquals('J', MissingValue.J.xportByteRepresentation());
        assertEquals('K', MissingValue.K.xportByteRepresentation());
        assertEquals('L', MissingValue.L.xportByteRepresentation());
        assertEquals('M', MissingValue.M.xportByteRepresentation());
        assertEquals('N', MissingValue.N.xportByteRepresentation());
        assertEquals('O', MissingValue.O.xportByteRepresentation());
        assertEquals('P', MissingValue.P.xportByteRepresentation());
        assertEquals('Q', MissingValue.Q.xportByteRepresentation());
        assertEquals('R', MissingValue.R.xportByteRepresentation());
        assertEquals('S', MissingValue.S.xportByteRepresentation());
        assertEquals('T', MissingValue.T.xportByteRepresentation());
        assertEquals('U', MissingValue.U.xportByteRepresentation());
        assertEquals('V', MissingValue.V.xportByteRepresentation());
        assertEquals('W', MissingValue.W.xportByteRepresentation());
        assertEquals('X', MissingValue.X.xportByteRepresentation());
        assertEquals('Y', MissingValue.Y.xportByteRepresentation());
        assertEquals('Z', MissingValue.Z.xportByteRepresentation());
    }

    /** Tests for {@link MissingValue#fromXportByteRepresentation}. */
    @Test
    void testFromXportByteRepresentation() {
        assertEquals(MissingValue.STANDARD, MissingValue.fromXportByteRepresentation((byte) '.'));
        assertEquals(MissingValue.UNDERSCORE, MissingValue.fromXportByteRepresentation((byte) '_'));
        assertEquals(MissingValue.A, MissingValue.fromXportByteRepresentation((byte) 'A'));
        assertEquals(MissingValue.B, MissingValue.fromXportByteRepresentation((byte) 'B'));
        assertEquals(MissingValue.C, MissingValue.fromXportByteRepresentation((byte) 'C'));
        assertEquals(MissingValue.D, MissingValue.fromXportByteRepresentation((byte) 'D'));
        assertEquals(MissingValue.E, MissingValue.fromXportByteRepresentation((byte) 'E'));
        assertEquals(MissingValue.F, MissingValue.fromXportByteRepresentation((byte) 'F'));
        assertEquals(MissingValue.G, MissingValue.fromXportByteRepresentation((byte) 'G'));
        assertEquals(MissingValue.H, MissingValue.fromXportByteRepresentation((byte) 'H'));
        assertEquals(MissingValue.I, MissingValue.fromXportByteRepresentation((byte) 'I'));
        assertEquals(MissingValue.J, MissingValue.fromXportByteRepresentation((byte) 'J'));
        assertEquals(MissingValue.K, MissingValue.fromXportByteRepresentation((byte) 'K'));
        assertEquals(MissingValue.L, MissingValue.fromXportByteRepresentation((byte) 'L'));
        assertEquals(MissingValue.M, MissingValue.fromXportByteRepresentation((byte) 'M'));
        assertEquals(MissingValue.N, MissingValue.fromXportByteRepresentation((byte) 'N'));
        assertEquals(MissingValue.O, MissingValue.fromXportByteRepresentation((byte) 'O'));
        assertEquals(MissingValue.P, MissingValue.fromXportByteRepresentation((byte) 'P'));
        assertEquals(MissingValue.Q, MissingValue.fromXportByteRepresentation((byte) 'Q'));
        assertEquals(MissingValue.R, MissingValue.fromXportByteRepresentation((byte) 'R'));
        assertEquals(MissingValue.S, MissingValue.fromXportByteRepresentation((byte) 'S'));
        assertEquals(MissingValue.T, MissingValue.fromXportByteRepresentation((byte) 'T'));
        assertEquals(MissingValue.U, MissingValue.fromXportByteRepresentation((byte) 'U'));
        assertEquals(MissingValue.V, MissingValue.fromXportByteRepresentation((byte) 'V'));
        assertEquals(MissingValue.W, MissingValue.fromXportByteRepresentation((byte) 'W'));
        assertEquals(MissingValue.X, MissingValue.fromXportByteRepresentation((byte) 'X'));
        assertEquals(MissingValue.Y, MissingValue.fromXportByteRepresentation((byte) 'Y'));
        assertEquals(MissingValue.Z, MissingValue.fromXportByteRepresentation((byte) 'Z'));
    }
}