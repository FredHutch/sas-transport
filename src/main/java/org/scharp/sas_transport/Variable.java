///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * A POJO that represents a variable.
 * <p>
 * This corresponds to a NAMESTR record when persisted in XPORT and a column when a dataset is viewed in a table.
 * </p>
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class Variable {
    private final VariableType type;
    private final String name;
    private final String label;
    private final int number;
    private final int length;
    private final Format outputFormat;
    private final Justification outputFormatJustification;
    private final Format inputFormat;

    /**
     * Constructs a variable object checking the input according to a strictness.
     *
     * @param variableName
     *     The name of the variable. This cannot be {@code null}. To fit into an XPORT, this should be 8 characters or
     *     fewer and only contain characters from the ASCII character set. SAS variable names must begin with a letter
     *     or underscore and may only contain letters, underscores, and digits.
     * @param variableNumber
     *     The variable's number.
     * @param type
     *     The variable's type (character or numeric). This cannot be {@code null}.
     * @param variableLength
     *     The maximum number of bytes that any value can have. For numeric data, this must be in the range from 2 to 8.
     *     For character data, this must be in the range from 1 to Short.MAX_VALUE. If {@code strictnessMode} is
     *     {@code StrictnessMode.FDA_SUBMISSION}, then character data must not be longer than 200. All of this
     *     variable's value are stored with this length. This is different from how many characters are displayed when
     *     formatting the values.
     * @param label
     *     The variable's label. This cannot be {@code null}. To fit into an XPORT, this should be 40 characters are
     *     fewer.
     *     <p>
     *     If {@code strictnessMode} is {@code StrictnessMode.FDA_SUBMISSION}, then the label must contain only ASCII
     *     characters.
     *     </p>
     * @param outputFormat
     *     The format to use when displaying this variable's values. This cannot be {@code null}, but it can be
     *     {@code Format.UNSPECIFIED}. SAS refers to this as "FORMAT".
     * @param outputFormatJustification
     *     The output format's justification (left or right).
     *     <p>
     *     This field is mentioned in the TS-140 specification, but SAS does not document its semantics. Since the
     *     alignment (left or right) is implicitly defined by {@code outputFormat.name()}, this field may have no
     *     meaning. Furthermore, SAS also translates all variable justifications to "left" if it reads and writes an
     *     XPORT with no intermediate modifications, which is further evidence that SAS ignores this field.
     *     </p>
     * @param inputFormat
     *     The format to use when reading this variable's value in. This cannot be null, but it can be
     *     {@code Format.UNSPECIFIED}. SAS refers to this as "INFORMAT".
     * @param strictness
     *     How strictly the arguments should be checked for correctness.
     *
     * @throws NullPointerException
     *     if {@code variableName}, {@code type}, {@code label}, {@code outputFormat},
     *     {@code outputFormatJustification}, or {@code inputFormat} are {@code null}.
     * @throws IllegalArgumentException
     *     if {@code variableName} is not a well-formed SAS variable name or is too long; if {@code variableLength} is
     *     out of range for {@code type}; if {@code label} is too long or doesn't adhere to the {@code strictnessMode}.
     */
    Variable(String variableName, int variableNumber, VariableType type, int variableLength, String label,
        Format outputFormat, Justification outputFormatJustification, Format inputFormat, StrictnessMode strictness) {

        ArgumentUtil.checkNotNull(type, "type");
        if (type == VariableType.NUMERIC) {
            if (variableLength < 2 || 8 < variableLength) {
                throw new IllegalArgumentException("numeric variables must have a length between 2-8");
            }
        } else {
            if (variableLength <= 0) {
                throw new IllegalArgumentException("character variables must have a positive length");
            }
            if (Short.MAX_VALUE < variableLength) {
                throw new IllegalArgumentException(
                    "character variables must not have a length greater than Short.MAX_VALUE");
            }
        }

        ArgumentUtil.checkNotNull(variableName, "variableName");
        if (!variableName.matches("[A-Za-z_][\\w_]{0,7}")) {
            // The variable name is not well-formed.  Throw the appropriate exception.
            if (variableName.isEmpty()) {
                throw new IllegalArgumentException("variable names cannot be blank");
            }
            ArgumentUtil.checkMaximumLength(variableName, 8, "variable names");
            ArgumentUtil.checkIsAscii(variableName, "variable names");
            throw new IllegalArgumentException("variable name is illegal for SAS");
        }

        ArgumentUtil.checkNotNull(label, "label");
        ArgumentUtil.checkMaximumLength(label, 40, "variable labels");

        ArgumentUtil.checkNotNull(outputFormat, "outputFormat");
        ArgumentUtil.checkNotNull(inputFormat, "inputFormat");
        ArgumentUtil.checkNotNull(outputFormatJustification, "outputFormatJustification");

        if (strictness == StrictnessMode.FDA_SUBMISSION) {
            ArgumentUtil.checkIsAscii(label, "variable labels");

            // SAS's XPORT engine truncates character variables to 200 when exporting.
            // Interestingly, if a V5 XPORT exists with a variable whose length is longer than 200,
            // a recent version of SAS's XPORT engine will import it without truncation.
            // However, since SAS will not generate such an XPORT file, it's possible that
            // other systems reading such a file would consider it malformed.
            // So in the "FDA" strictness mode, we limit variables to 200.
            if (200 < variableLength) {
                throw new IllegalArgumentException("character variables must not have a length greater than 200");
            }
        }

        // TODO: format legal for type?

        this.name = variableName;
        this.number = variableNumber;
        this.type = type;
        this.length = variableLength;
        this.label = label;
        this.outputFormat = outputFormat;
        this.outputFormatJustification = outputFormatJustification;
        this.inputFormat = inputFormat;
    }

    /**
     * Constructs a variable object.
     *
     * @param variableName
     *     The name of the variable. This cannot be {@code null}. To fit into an XPORT, this should be 8 characters or
     *     fewer and only contain characters from the ASCII character set. SAS variable names must begin with a letter
     *     or underscore and may only contain letters, underscores, and digits.
     * @param variableNumber
     *     The variable's number.
     * @param type
     *     The variable's type (character or numeric). This cannot be {@code null}.
     * @param variableLength
     *     The maximum number of bytes that any value can have. For numeric data, this must be in the range from 2 to 8.
     *     For character data, this must be in the range from 1 to 200. All of this variable's value are stored with
     *     this length. This is different from how many characters are displayed when formatting the values.
     * @param label
     *     The variable's label. This cannot be {@code null}. To fit into an XPORT, this should be 40 characters are
     *     fewer and only contain characters from the ASCII character set.
     * @param outputFormat
     *     The format to use when displaying this variable's values. This cannot be {@code null}, but it can be
     *     {@code Format.UNSPECIFIED}. SAS refers to this as "FORMAT".
     * @param outputFormatJustification
     *     The output format's justification (left or right).
     *     <p>
     *     This field is mentioned in the TS-140 specification, but SAS does not document its semantics. Since the
     *     alignment (left or right) is implicitly defined by {@code outputFormat.name()}, this field may have no
     *     meaning. Furthermore, SAS also translates all variable justifications to "left" if it reads and writes an
     *     XPORT with no intermediate modifications, which is further evidence that SAS ignores this field.
     *     </p>
     * @param inputFormat
     *     The format to use when reading this variable's value in. This cannot be {@code null}, but it can be
     *     {@code Format.UNSPECIFIED}. SAS refers to this as "INFORMAT".
     *
     * @throws NullPointerException
     *     if {@code variableName}, {@code type}, {@code label}, {@code outputFormat},
     *     {@code outputFormatJustification}, or {@code inputFormat} are {@code null}.
     * @throws IllegalArgumentException
     *     if {@code variableName} is not a well-formed SAS variable name or is too long; if {@code variableLength} is
     *     out of range for {@code type}; if {@code label} is too long or contains non-ASCII characters.
     */
    public Variable(String variableName, int variableNumber, VariableType type, int variableLength, String label,
        Format outputFormat, Justification outputFormatJustification, Format inputFormat) {
        this(variableName, variableNumber, type, variableLength, label, outputFormat, outputFormatJustification,
            inputFormat, StrictnessMode.FDA_SUBMISSION);
    }

    /**
     * Gets this variable's type (character or numeric).
     *
     * @return This variable's type. This is never {@code null}.
     */
    public VariableType type() {
        return type;
    }

    /**
     * Gets this variable's name.
     *
     * @return This variable's name. This is never {@code null} or empty.
     */
    public String name() {
        return name;
    }

    /**
     * Gets this variable's label.
     *
     * @return This variable's label. This may be the empty string but never {@code null}.
     */
    public String label() {
        return label;
    }

    /**
     * Gets this variable's number (its column order).
     *
     * @return This variable's number.
     */
    public int number() {
        return number;
    }

    /**
     * Gets this variable's length, which is number of bytes that values of this variable can occupy within an
     * observation.
     *
     * @return This variable's length.
     */
    public int length() {
        return length;
    }

    /**
     * Gets the format to use when rendering this variable's values for display. SAS refers to this as the "FORMAT".
     *
     * @return This variable's output format. This is never {@code null}.
     */
    public Format outputFormat() {
        return outputFormat;
    }

    /**
     * Gets the justification (left or right) for this variable's output format.
     * <p>
     * SAS does not document the semantics of this and since the alignment is defined by the output format's name, this
     * may have no meaning.
     * </p>
     *
     * @return the justification of the output format. This is never {@code null}.
     */
    public Justification outputFormatJustification() {
        return outputFormatJustification;
    }

    /**
     * Gets the format to use when rendering this variable's values into a SAS program. SAS refers to this as the
     * "INFORMAT".
     *
     * @return The variable's input format. This is never {@code null}.
     */
    public Format inputFormat() {
        return inputFormat;
    }
}