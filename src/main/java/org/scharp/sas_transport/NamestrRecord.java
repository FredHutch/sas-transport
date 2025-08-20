///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * This is a Java POJO representation the namestr record in section 7 of TS-140.
 *
 * <pre>
 * struct NAMESTR {
 *    short ntype;   // VARIABLE TYPE: 1=NUMERIC, 2=CHAR
 *    short nhfun;   // HASH OF NNAME (always 0)
 *    short nlng;    // LENGTH OF VARIABLE IN OBSERVATION
 *    short nvar0;   // VARNUM
 *    char8 nname;   // NAME OF VARIABLE
 *    char40 nlabel; // LABEL OF VARIABLE
 *    char8 nform;   // NAME OF FORMAT
 *    short nfl;     // FORMAT FIELD LENGTH OR 0
 *    short nfd;     // FORMAT NUMBER OF DECIMALS
 *    short nfj;     // 0=LEFT JUSTIFICATION, 1=RIGHT JUST
 *    char nfill[2]; // (UNUSED, FOR ALIGNMENT AND FUTURE)
 *    char8 niform;  // NAME OF INPUT FORMAT
 *    short nifl;    // INFORMAT LENGTH ATTRIBUTE
 *    short nifd;    // INFORMAT NUMBER OF DECIMALS
 *    long npos;     // POSITION OF VALUE IN OBSERVATION
 *    char rest[52]; // remaining fields are irrelevant
 * };
 * </pre>
 */
class NamestrRecord extends Record {

    /**
     * The constructor for a NAMESTR record to be used when writing.
     *
     * @param type
     * @param variableLength
     * @param variableNumber
     * @param variableName
     * @param label
     * @param format
     * @param justification
     * @param inputFormat
     * @param positionInObservation
     */
    NamestrRecord(VariableType type, int variableLength, int variableNumber, String variableName, String label,
        Format format, Justification justification, Format inputFormat, int positionInObservation) {
        super(140);
        assert type != null;
        short typeCode = type == VariableType.NUMERIC ? (short) 1 : (short) 2;
        short nameHash = 0;
        short nlng = toShort(variableLength);
        short nvar0 = toShort(variableNumber);

        assert variableName != null;
        assert variableName.length() <= 8 : variableName;

        assert label != null;
        assert label.length() <= 40 : label;

        assert justification != null;
        short justificationCode;
        switch (justification) {
        case LEFT:
            justificationCode = 0;
            break;

        case RIGHT:
            justificationCode = 1;
            break;

        default:
            justificationCode = 2;
            break;
        }

        // Serialize the data.
        int offset = 0;
        offset += toArray(typeCode, offset);
        offset += toArray(nameHash, offset);
        offset += toArray(nlng, offset);
        offset += toArray(nvar0, offset);
        offset += toSpacePaddedArray(variableName, offset, 8);
        offset += toSpacePaddedArray(label, offset, 40);
        offset += toArray(format, offset);
        offset += toArray(justificationCode, offset);
        offset += toArray((short) 0, offset); // nfill
        offset += toArray(inputFormat, offset);
        toArray(positionInObservation, offset);
        // the rest is already zero-filled.
    }

    NamestrRecord(byte[] data) {
        super(140);
        assert data.length == 140 : data.length;
        System.arraycopy(data, 0, this.data, 0, 140);

        // Assert the class invariant
        short typeCode = readShort(0);
        if (typeCode != 1 && typeCode != 2) {
            throw new IllegalArgumentException("Unexpected type code in NAMESTR field: " + typeCode);
        }

        int offsetInObservation = offsetInObservation();
        if (offsetInObservation < 0) {
            throw new IllegalArgumentException(
                "Bad offset in NAMESTR field (offset can't be negative): " + offsetInObservation);
        }
    }

    private short toShort(int number) {
        assert 0 <= number : number;
        assert number <= Short.MAX_VALUE : number;
        return (short) number;
    }

    private int toArray(Format format, int offset) {
        assert format != null;
        offset += toSpacePaddedArray(format.name(), offset, 8);
        offset += toArray((short) format.width(), offset);
        toArray((short) format.numberOfDigits(), offset);
        return 8 + 2 + 2;
    }

    private Format readFormat(int offset) {
        String name = readSpacePaddedString(offset, 8);
        short width = readShort(offset + 8);
        short numberOfDigits = readShort(offset + 10);

        return new Format(name, width, numberOfDigits);
    }

    VariableType type() {
        short code = readShort(0);
        switch (code) {
        case 1:
            return VariableType.NUMERIC;
        case 2:
            return VariableType.CHARACTER;
        default:
            assert false : "can't happen";
            return null;
        }
    }

    short length() {
        return readShort(4);
    }

    short number() {
        return readShort(6);
    }

    String name() {
        return readSpacePaddedString(8, 8);
    }

    String label() {
        return readSpacePaddedString(16, 40);
    }

    Format format() {
        return readFormat(16 + 40);
    }

    Justification justification() {
        short code = readShort(16 + 40 + 12);
        switch (code) {
        case 0:
            return Justification.LEFT;
        case 1:
            return Justification.RIGHT;
        default:
            return Justification.UNKNOWN;
        }
    }

    Format inputFormat() {
        return readFormat(16 + 40 + 16);
    }

    int offsetInObservation() {
        return readInt(16 + 40 + 16 + 12);
    }
}