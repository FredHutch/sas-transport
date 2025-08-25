///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * A representation of a missing numeric value in a SAS7BDAT dataset.  (Missing character values are represented as the
 * empty string).
 * <p>
 * SAS supports multiple of ways of specifying that a numeric variable's value is missing in an observation.  This can
 * be used to encode <i>why</i> a value is missing.  For example, it could be that the value is unknown, or that the
 * subject declined to report a value.  If you don't need to specify multiple ways that a value is missing, use
 * {@link MissingValue#STANDARD}.
 * </p>
 */
public enum MissingValue {
    /**
     * The missing value that is represented as "{@code .}" in SAS. This only applies to only numeric values.
     */
    STANDARD('.'),

    /**
     * The missing value that is represented as "{@code ._}" in SAS. This only applies to only numeric values.
     */
    UNDERSCORE('_'),

    /**
     * The missing value that is represented as "{@code .A}" in SAS. This only applies to only numeric values.
     */
    A('A'),

    /**
     * The missing value that is represented as "{@code .B}" in SAS. This only applies to only numeric values.
     */
    B('B'),

    /**
     * The missing value that is represented as "{@code .C}" in SAS. This only applies to only numeric values.
     */
    C('C'),

    /**
     * The missing value that is represented as "{@code .D}" in SAS. This only applies to only numeric values.
     */
    D('D'),

    /**
     * The missing value that is represented as "{@code .E}" in SAS. This only applies to only numeric values.
     */
    E('E'),

    /**
     * The missing value that is represented as "{@code .F}" in SAS. This only applies to only numeric values.
     */
    F('F'),

    /**
     * The missing value that is represented as "{@code .G}" in SAS. This only applies to only numeric values.
     */
    G('G'),

    /**
     * The missing value that is represented as "{@code .H}" in SAS. This only applies to only numeric values.
     */
    H('H'),

    /**
     * The missing value that is represented as "{@code .I}" in SAS. This only applies to only numeric values.
     */
    I('I'),

    /**
     * The missing value that is represented as "{@code .J}" in SAS. This only applies to only numeric values.
     */
    J('J'),

    /**
     * The missing value that is represented as "{@code .K}" in SAS. This only applies to only numeric values.
     */
    K('K'),

    /**
     * The missing value that is represented as "{@code .L}" in SAS. This only applies to only numeric values.
     */
    L('L'),

    /**
     * The missing value that is represented as "{@code .M}" in SAS. This only applies to only numeric values.
     */
    M('M'),

    /**
     * The missing value that is represented as "{@code .N}" in SAS. This only applies to only numeric values.
     */
    N('N'),

    /**
     * The missing value that is represented as "{@code .O}" in SAS. This only applies to only numeric values.
     */
    O('O'),

    /**
     * The missing value that is represented as "{@code .P}" in SAS. This only applies to only numeric values.
     */
    P('P'),

    /**
     * The missing value that is represented as "{@code .Q}" in SAS. This only applies to only numeric values.
     */
    Q('Q'),

    /**
     * The missing value that is represented as "{@code .R}" in SAS. This only applies to only numeric values.
     */
    R('R'),

    /**
     * The missing value that is represented as "{@code .S}" in SAS. This only applies to only numeric values.
     */
    S('S'),

    /**
     * The missing value that is represented as "{@code .T}" in SAS. This only applies to only numeric values.
     */
    T('T'),

    /**
     * The missing value that is represented as "{@code .U}" in SAS. This only applies to only numeric values.
     */
    U('U'),

    /**
     * The missing value that is represented as "{@code .V}" in SAS. This only applies to only numeric values.
     */
    V('V'),

    /**
     * The missing value that is represented as "{@code .W}" in SAS. This only applies to only numeric values.
     */
    W('W'),

    /**
     * The missing value that is represented as "{@code .X}" in SAS. This only applies to only numeric values.
     */
    X('X'),

    /**
     * The missing value that is represented as "{@code .Y}" in SAS. This only applies to only numeric values.
     */
    Y('Y'),

    /**
     * The missing value that is represented as "{@code .Z}" in SAS. This only applies to only numeric values.
     */
    Z('Z');

    private final byte xportByteRepresentation;

    MissingValue(char numericByteRepresentation) {
        this.xportByteRepresentation = (byte) numericByteRepresentation;
    }

    /**
     * @return the byte that represents this missing value in a SAS XPORT file.
     */
    byte xportByteRepresentation() {
        return xportByteRepresentation;
    }

    /**
     * Find the {@code MissingValue} that corresponds to a byte from an XPORT file.
     *
     * @param xportByte
     *     A byte from an XPORT file that could represent a missing value.
     *
     * @return the {@code MissingValue} that corresponds to xportByte, or {@code null} if the byte does not represent a
     *     {@code MissingValue}.
     */
    static MissingValue fromXportByteRepresentation(byte xportByte) {

        for (MissingValue missingValue : MissingValue.values()) {
            if (missingValue.xportByteRepresentation == xportByte) {
                // found it
                return missingValue;
            }
        }

        return null;
    }

    /**
     * Gets the string representation of this missing value, formatted as SAS formats it.
     *
     * @return The string representation of this missing value.
     */
    public String toString() {
        return this == STANDARD ? "." : "." + (char) xportByteRepresentation();
    }
}