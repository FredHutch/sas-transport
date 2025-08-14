///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A description of a SAS Data Set.
 *
 * <p>
 * This includes a description of the metadata but no observations.
 * </p>
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class SasDataSetDescription {

    /**
     * The maximum number of variables that can fit into a data set within a V5 XPORT file.
     */
    private static final int MAX_V5_XPORT_VARIABLES = 9999;

    private final String name;
    private final String type;
    private final String sourceOperatingSystem;
    private final String sourceSasVersion;
    private final LocalDateTime createTime;
    private final LocalDateTime modifiedTime;
    private final String label;
    private final List<Variable> variables;

    /**
     * Creates a description of a data set. This includes the description of the variables within the data set but no
     * observations.
     *
     * @param name
     *     The name of the data set. This is required.
     *     <p>
     *     To fit into an XPORT file, this should be 8 characters or fewer and only contain characters from the ASCII
     *     character set.
     *     </p>
     * @param label
     *     This data set's label. This may be blank but not {@code null}.
     *     <p>
     *     To fit into an XPORT file, this must be 40 characters or fewer and only contain characters from the ASCII
     *     character set.
     *     </p>
     * @param type
     *     The data set's type. For example, "BOXPLOT" or "TREE". SAS defines a few special SAS dataset upon which
     *     specialized applications can work. See the SAS documentation for more information. If specified, the data
     *     within the data set should conform to the type.
     *     <p>
     *     This is typically left blank. This must not be {@code null}.
     *     </p>
     *     <p>
     *     To fit into an XPORT file, this must be 8 characters or fewer. If {@code strictnessMode} is
     *     {@code StrictnessMode.FDA_SUBMISSION}, then this must contain only ASCII characters.
     *     </p>
     * @param sourceOperatingSystem
     *     The operating system on which the data set was created. For example, "Linux", "SunOS", or "LIN X64". This
     *     must not be {@code null}.
     *     <p>
     *     To fit into an XPORT file, this must be 8 characters or fewer and only contain characters from the ASCII
     *     character set.
     *     </p>
     * @param sourceSasVersion
     *     The version of SAS used to create this data set. This must not be {@code null}.
     *     <p>
     *     This is meaningless for data sets created with this library, but for compatibility, specify a SAS version,
     *     such as "5.2".
     *     </p>
     *     <p>
     *     To fit into an XPORT file, this must be 8 characters or fewer and only contain characters from the ASCII
     *     character set.
     *     </p>
     * @param variables
     *     A list of variable within the data set. This must not be {@code null}. This list is copied into the
     *     constructed data set so further modifications to the provided list do not affect the constructed data set.
     *     <p>
     *     A V5 XPORT file can only hold 9999 variables.
     *     </p>
     * @param createTime
     *     The date and time on which this data set was created. This must not be {@code null}.
     *     <p>
     *     In an XPORT file, this date is stored with second granularity and the year is stored as a two digit number.
     *     </p>
     * @param modifiedTime
     *     The date and time on which this data set was last modified. This must not be {@code null}.
     *     <p>
     *     In an XPORT file, this date is stored with second granularity and the year is stored as a two digit number.
     *     </p>
     * @param strictness
     *     How strictly the arguments should be checked for correctness.
     *
     * @throws NullPointerException
     *     if any of the arguments are {@code null}.
     * @throws IllegalArgumentException
     *     if {@code name} is empty, longer than 8 character, or contains non-ASCII characters; if {@code label} is
     *     longer than 40 characters or contains non-ASCII characters; if {@code type} is longer than 8 characters; if
     *     {@code sourceOperatingSystem} is longer than 8 characters; or if {@code sourceSasVersion} is longer than 8
     *     characters. if {@code variables} has more variables than 9999.
     */
    SasDataSetDescription(String name, String label, String type, String sourceOperatingSystem, String sourceSasVersion,
        Collection<Variable> variables, LocalDateTime createTime, LocalDateTime modifiedTime,
        StrictnessMode strictness) {

        ArgumentUtil.checkNotNull(name, "name");
        if (!name.matches("[A-Za-z_][\\w_]{0,7}")) {
            // The variable name is not well-formed.  Throw the appropriate exception.
            if (name.isEmpty()) {
                throw new IllegalArgumentException("data set names cannot be blank");
            }
            ArgumentUtil.checkMaximumLength(name, 8, "data set names");
            ArgumentUtil.checkIsAscii(name, "data set names");
            throw new IllegalArgumentException("data set name is illegal for SAS");
        }

        ArgumentUtil.checkNotNull(label, "label");
        ArgumentUtil.checkMaximumLength(label, 40, "data set labels");

        ArgumentUtil.checkNotNull(type, "type");
        ArgumentUtil.checkMaximumLength(type, 8, "data set types");

        ArgumentUtil.checkNotNull(sourceOperatingSystem, "sourceOperatingSystem");
        ArgumentUtil.checkMaximumLength(sourceOperatingSystem, 8, "data set operating system");
        ArgumentUtil.checkIsAscii(sourceOperatingSystem, "data set operating system");

        ArgumentUtil.checkNotNull(sourceSasVersion, "sourceSasVersion");
        ArgumentUtil.checkMaximumLength(sourceSasVersion, 8, "data set SAS versions");
        ArgumentUtil.checkIsAscii(sourceSasVersion, "data set SAS versions");

        ArgumentUtil.checkNotNull(variables, "variables");
        if (MAX_V5_XPORT_VARIABLES < variables.size()) {
            throw new IllegalArgumentException(
                "variables must not have more than " + MAX_V5_XPORT_VARIABLES + " variables");
        }

        ArgumentUtil.checkNotNull(createTime, "createTime");
        ArgumentUtil.checkNotNull(modifiedTime, "modifiedTime");

        if (strictness == StrictnessMode.FDA_SUBMISSION) {
            ArgumentUtil.checkIsAscii(type, "data set type");
        }

        // Check that none of the variables have the same name.
        // In SAS, case of names is preserved, but the names are case-insensitive.
        Set<String> variableNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Variable variable : variables) {
            if (!variableNames.add(variable.name())) {
                throw new IllegalArgumentException("multiple variables have the same name: " + variable.name());
            }
        }

        this.name = name;
        this.label = label;
        this.type = type;
        this.sourceOperatingSystem = sourceOperatingSystem;
        this.sourceSasVersion = sourceSasVersion;
        this.createTime = createTime;
        this.modifiedTime = modifiedTime;
        this.variables = new ArrayList<>(variables);
    }

    /**
     * Creates a description of a data set. This includes the description of the variables within the data set but no
     * observations.
     *
     * @param name
     *     The name of the data set. This is required.
     *     <p>
     *     To fit into an XPORT file, this should be 8 characters or fewer and only contain characters from the ASCII
     *     character set.
     *     </p>
     * @param label
     *     This data set's label. This may be blank but not {@code null}.
     *     <p>
     *     To fit into an XPORT file, this must be 40 characters or fewer and only contain characters from the ASCII
     *     character set.
     *     </p>
     * @param type
     *     The data set's type. For example, "BOXPLOT" or "TREE". SAS defines a few special SAS dataset upon which
     *     specialized applications can work. See the SAS documentation for more information. If specified, the data
     *     within the data set should conform to the type.
     *     <p>
     *     This is typically left blank. This must not be {@code null}.
     *     </p>
     *     <p>
     *     To fit into an XPORT file, this must be 8 characters or fewer and only contain characters from the ASCII
     *     character set.
     *     </p>
     * @param sourceOperatingSystem
     *     The operating system on which the data set was created. For example, "Linux", "SunOS", or "LIN X64". This
     *     must not be {@code null}.
     *     <p>
     *     To fit into an XPORT file, this must be 8 characters or fewer and only contain characters from the ASCII
     *     character set.
     *     </p>
     * @param sourceSasVersion
     *     The version of SAS used to create this data set. This must not be {@code null}.
     *     <p>
     *     This is meaningless for data sets created with this library, but for compatibility, specify a SAS version,
     *     such as "5.2".
     *     </p>
     *     <p>
     *     To fit into an XPORT file, this must be 8 characters or fewer and only contain characters from the ASCII
     *     character set.
     *     </p>
     * @param variables
     *     A list of variable within the data set. This must not be {@code null}. This list is copied into the
     *     constructed data set so further modifications to the provided list do not affect the constructed data set.
     *     <p>
     *     A V5 XPORT file can only hold 9999 variables.
     *     </p>
     * @param createTime
     *     The date and time on which this data set was created. This must not be {@code null}.
     *     <p>
     *     In an XPORT file, this date is stored with second granularity and the year is stored as a two digit number.
     *     </p>
     * @param modifiedTime
     *     The date and time on which this data set was last modified. This must not be {@code null}.
     *     <p>
     *     In an XPORT file, this date is stored with second granularity and the year is stored as a two digit number.
     *     </p>
     *
     * @throws NullPointerException
     *     if any of the arguments are {@code null}.
     * @throws IllegalArgumentException
     *     if {@code name} is empty, longer than 8 character, or contains non-ASCII characters; if {@code label} is
     *     longer than 40 characters or contains non-ASCII characters; if {@code type} is longer than 8 characters; if
     *     {@code sourceOperatingSystem} is longer than 8 characters; or if {@code sourceSasVersion} is longer than 8
     *     characters. if {@code variables} has more variables than 9999.
     */
    public SasDataSetDescription(String name, String label, String type, String sourceOperatingSystem,
        String sourceSasVersion, Collection<Variable> variables, LocalDateTime createTime, LocalDateTime modifiedTime) {
        this(name, label, type, sourceOperatingSystem, sourceSasVersion, variables, createTime, modifiedTime,
            StrictnessMode.FDA_SUBMISSION);
    }

    /**
     * @return This data set's name. This is never {@code null}.
     */
    public String name() {
        return name;
    }

    /**
     * @return This data set's type. For example, "BOXPLOT" or "TREE". This is never {@code null}.
     */
    public String type() {
        return type;
    }

    /**
     * @return The operating system on which this data set was created. This is never {@code null}.
     */
    public String sourceOperatingSystem() {
        return sourceOperatingSystem;
    }

    /**
     * @return The version of SAS on which this data set was last modified. This is never {@code null}.
     */
    public String sourceSasVersion() {
        return sourceSasVersion;
    }

    /**
     * @return This data set's label. This is never null.
     */
    public String label() {
        return label;
    }

    /**
     * @return An unmodifiable list of this data's variables. This is never {@code null}.
     */
    public List<Variable> variables() {
        return Collections.unmodifiableList(variables);
    }

    /**
     * @return A date on which this data set was created. This is never {@code null}.
     */
    public LocalDateTime createTime() {
        return createTime;
    }

    /**
     * @return A date on which this data set was most recently modified. This is never {@code null}.
     */
    public LocalDateTime modifiedTime() {
        return modifiedTime;
    }

    /**
     * Creates a new SAS Library description that wraps this data set description. The library description has the same
     * operating system, SAS version, create time, and modification time as this data set description.
     *
     * @return A new SAS library description that wraps this data set description.
     */
    public SasLibraryDescription newLibraryDescription() {
        return new SasLibraryDescription(this, sourceOperatingSystem, sourceSasVersion, createTime(), modifiedTime());
    }
}