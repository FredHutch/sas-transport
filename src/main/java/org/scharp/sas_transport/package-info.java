///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
/**
 * <p>
 * This is a library for reading and writing V5 SAS transport (XPORT) files. The V5 SAS XPORT transport format is
 * primarily used as a data interchange format in the pharmaceutical industry and it is the only format accepted by the
 * US Food And Drug Administration (FDA) when submitting data from a clinical trail for an Investigational New Drug
 * (IND).
 * </p>
 *
 * <p>
 * The library serves as a bridge between SAS and Java by reading and writing XPORT files. The API design was optimized
 * for the primary use case of submission to the FDA.
 * </p>
 *
 * <p>
 * The general usage paradigm is to have a structured in-memory representation of the XPORT header information (a
 * {@link org.scharp.sas_transport.SasLibraryDescription}), which can be read from an XPORT and written to an XPORT. The
 * rows of data (called "observations" in SAS) are then streamed into or out of the XPORT one at a time for scalability
 * (so that a large XPORT file the API doesn't require that a large XPORT file be held in entirely in memory).
 * </p>
 *
 * <h2>SAS for Java Programmers</h2>
 *
 * <p>
 * In SAS, a row of data is called an observation. Observations with an identical structure are organized into "data
 * sets", which corresponds to the {@link org.scharp.sas_transport.SasDataSetDescription} class. Zero or more SAS data
 * sets are organized into a SAS library, which corresponds to the
 * {@link org.scharp.sas_transport.SasLibraryDescription} class. However, as the FDA requires that all submitted XPORT
 * files contain only one data set, the many-to-one relationship between data sets and SAS libraries is not supported by
 * this library.
 * </p>
 *
 * <p>
 * The V5 XPORT has limitations that SAS data sets do not have, so exporting to an XPORT may have data loss. For
 * example, variable names may only be 8 characters, variable labels may only be 40 characters long, and all numbers are
 * represented in an old mainframe floating point format that has oscillating precision loss as the exponent increases.
 * As a result, the V5 XPORT format is not the preferred format used by SAS programmers.
 * </p>
 *
 * <p>
 * SAS is loosely typed. It has only two types, "numeric" and "character". Numeric data is always double precision
 * floating point. Higher level types, such as "currency", "dates", or "times" are merely formats on these two base
 * types. For example, the number "1", could be "$1.00", "2-JAN-1960", or "1-JAN-1960:00:00:01" depending on the format.
 * There are over 100 built-in formats (many of which are only slight variations), but the format system is extensible.
 * For example, if a SAS programmer wanted to create the equivalent of an {@code enum}, they could do so with a
 * user-defined format. For example, they might create an enum for "smoking status" and format 1 as "Never Smoked", 2 as
 * "Quit", and 3 as "Active Smoker". The name of this format can be persisted in an XPORT, but the formatting cannot. So
 * any code which processes the data subsequently will only see that the data has the SMOKER format and has data "1.0",
 * "2.0", and "3.0" (remember, all numeric values are double-precision floating point).
 * </p>
 *
 * <p>
 * The "V5" in the name "SAS V5 XPORT format" comes from the version of SAS which first supported this format. An
 * extension with less restrictive limitations was created in SAS version 8 in SAS macros {@code %loc2xpt} and
 * {@code %xpt2loc}, leading to a "V8 XPORT". However, the FDA does not accept these, and so neither does this library.
 * </p>
 *
 * <p>
 * The "XPORT" in "SAS V5 XPORT format" is short for "transport" (not "export"). The idea is that if you were going to
 * transport a SAS library across machines, you would need a machine-independent format for doing so. Normally, SAS
 * stores libraries in a machine-specific "local" format, which optimizes for the local machine's architecture. (This
 * was more important in the 1960s and 1970s, when computers had more varied architectures, different endianess,
 * different word sizes, and different floating point representations.) Nowadays, most machines are 64-bit,
 * little-endian, with IEEE 754 floating point representation, so even a "local" format is probably portable across
 * machines. However, there is no public specification for the local format, so it can't be considered a standard.
 * </p>
 *
 * <p>
 * The XPORT format is publicly documented, but there is no specifications committee and the specification is vague,
 * incomplete, and incorrect at times. As a result, SAS still remains the <i>de facto</i> definition and this library
 * follows what SAS does whenever it conflicts with the standard.
 * </p>
 *
 * <p>
 * SAS implements two different ways to read and write V5 XPORT files. One is the XPORT engine and the other is a pair
 * of SAS macros {@code %loc2xpt} and {@code %xpt2loc}. The XPORT engine was the first implementation. The macros are a
 * complete re-implementation of the serialization logic and have slightly different behavior for edge conditions (that
 * is, they have bugs). Therefore, when using SAS to resolve ambiguity in the specification, the behavior of the XPORT
 * engine was always used.
 * </p>
 *
 * <h2>Error Handling Strategy</h2>
 * <p>
 * This library enforces strict input checking to prevent unintentional data loss or unintentionally creating a
 * malformed XPORT file. Unfortunately, the TS-140 specification does not state how malformed XPORT files should be
 * handled during processing and SAS has a range of behavior for different classes of badly formed input: ignoring it,
 * crashing during processing, silent data loss, and logging errors. In some cases, SAS can generate XPORT files that
 * don't conform to the specification. In many more cases, SAS can import a non-conforming XPORT file.
 * </p>
 *
 * <p>
 * To account for the lack of strictness in the XPORT standard, this design of this library follows the robustness
 * principal; it is strict in what it generates and (somewhat) tolerant of what it reads.
 * </p>
 *
 * <p>
 * When creating an XPORT file, this library throws clear exceptions as soon as possible (fail-fast). In most of these
 * cases, callers can work-around the problem by having partial data loss (simple truncation or encoding Unicode data
 * into ASCII), if that's desirable for their particular use case.
 * </p>
 *
 * <p>
 * In contrast, this library is somewhat lenient about when reading. It can read any XPORT file that SAS's XPORT engine
 * can generate, even malformed ones and even ones which it, itself, would not export. When reading a malformed XPORT
 * that SAS, itself, can't generate but can read, this library generally treats it as well-formed and reads it the same
 * way SAS does. Such files might have been generated with other libraries or by older SAS implementations. For a
 * malformed XPORT file which SAS cannot read, this library generally throws an exception.
 * </p>
 *
 * <h2>Writing an XPORT file</h2>
 * <p>
 * The following code sample demonstrates how to write a SAS transport using hard-coded data.
 * </p>
 *
 * <pre>
 * List&lt;Variable&gt; variables = Arrays.asList(
 *
 *     new Variable(
 *         "CITY",                  // name
 *         1,                       // variable number
 *         VariableType.CHARACTER,  // type
 *         20,                      // length
 *         "Name of city",          // label
 *         new Format("$CHAR", 18), // output format
 *         Justification.LEFT,
 *         Format.UNSPECIFIED),     // input format
 *
 *     new Variable(
 *         "STATE",
 *         2,
 *         VariableType.CHARACTER,
 *         2,
 *         "Postal abbreviation of state",
 *         new Format("$CHAR", 2),
 *         Justification.LEFT,
 *         Format.UNSPECIFIED),
 *
 *     new Variable(
 *         "HIGH",
 *         3,
 *         VariableType.NUMERIC,
 *         8,
 *         "Average daily high in F",
 *         new Format("", 5),
 *         Justification.LEFT,
 *         Format.UNSPECIFIED),
 *
 *     new Variable(
 *         "LOW",
 *         4,
 *         VariableType.NUMERIC,
 *         8,
 *         "Average daily low in F",
 *         new Format("", 5),
 *         Justification.LEFT,
 *         Format.UNSPECIFIED));
 *
 * SasDataSetDescription dataSet = new SasDataSetDescription(
 *     "TEMP", // name
 *     "Average daily temperatures", // label
 *     "", // type
 *     "Java", // OS version
 *     "5.0", // SAS Version
 *     variables, // variables
 *     LocalDateTime.now(), // create
 *     LocalDateTime.now()); // modified
 *
 * try (SasTransportExporter exporter = dataSet.newLibraryDescription().exportTransportDataSet(path)) {
 *     exporter.appendObservation(List.of("Atlanta", "GA", 72, 53));
 *     exporter.appendObservation(List.of("Austin", "TX", 80, 5));
 *     exporter.appendObservation(List.of("Baltimore", "MD", 65, 45));
 *     exporter.appendObservation(List.of("Birmingham", "AL", 74, 53));
 *     exporter.appendObservation(List.of("Boston", "MA", 59, MissingValue.STANDARD));
 *     exporter.appendObservation(List.of("Buffalo", "NY", 56, 40));
 *     // ...
 *     exporter.appendObservation(List.of("Virginia Beach", "VA", 68, 52));
 *     exporter.appendObservation(List.of("Washington", "DC", 68, 52));
 * }
 * </pre>
 *
 * <h2>Reading an XPORT file</h2>
 *
 * <p>
 * The following code sample demonstrates how to read a SAS transport file and print out its contents into a table.
 * </p>
 *
 * <pre>
 * Path path = ...;
 *
 * try (SasTransportImporter importer = SasLibraryDescription.importTransportDataSet(path)) {
 *
 *     // Get the variables.
 *     List&lt;Variable&gt; dataSetVariables = importer.sasLibraryDescription().dataSetDescription().variables();
 *
 *     // Display a header using the variables.
 *     StringBuilder header = new StringBuilder();
 *     for (Variable variable : dataSetVariables) {
 *         header.append(String.format("%-" + (variable.outputFormat().width() + 1) + "s ", variable.name()));
 *     }
 *     System.out.println(header.toString());
 *
 *     List&lt;Object&gt; observation;
 *     while ((observation = importer.nextObservation()) != null) {
 *         StringBuilder row = new StringBuilder(header.length());
 *
 *         // Render each value in the observation.
 *         for (int i = 0; i &lt; observation.size(); i++) {
 *             final Variable variable = dataSetVariables.get(i);
 *             final Object value = observation.get(i);
 *
 *             final String formattedValue;
 *             if (value instanceof MissingValue) {
 *                 formattedValue = String.format("%-" + (variable.outputFormat().width() + 1) + "s", ".");
 *             } else {
 *                 formattedValue = String.format("%-" + (variable.outputFormat().width() + 1) + "s",
 *                     value.toString());
 *             }
 *             row.append(formattedValue + " ");
 *         }
 *
 *         System.out.println(row.toString());
 *     }
 * }
 * </pre>
 */
package org.scharp.sas_transport;