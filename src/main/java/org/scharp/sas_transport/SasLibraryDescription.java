///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * A simple class which describes a SAS library but does not contain any observations.
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class SasLibraryDescription {
    private final SasDataSetDescription dataSetDescription;
    private final String sourceOperatingSystem;
    private final String sourceSasVersion;
    private final LocalDateTime createTime;
    private final LocalDateTime modifiedTime;

    /**
     * Constructs a new SAS library.
     *
     * @param dataSetDescription
     *     A description of the library's one and only data set. This must not be {@code null}.
     * @param sourceOperatingSystem
     *     The operating system on which the data set was created. For example, "Linux", "SunOS", or "LIN X64".
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
     * @param createTime
     *     The date and time on which this library was created. This must not be {@code null}.
     *     <p>
     *     In an XPORT file, this date is stored with second granularity and the year is stored as a two digit number.
     *     </p>
     * @param modifiedTime
     *     The date and time on which this library was last modified. This must not be {@code null}.
     *     <p>
     *     In an XPORT file, this date is stored with second granularity and the year is stored as a two digit number.
     *     </p>
     *
     * @throws NullPointerException
     *     if any of the arguments are {@code null}.
     * @throws IllegalArgumentException
     *     if {@code sourceOperatingSystem} or {@code sourceSasVersion} are longer than 8 characters or contain
     *     non-ASCII characters.
     */
    public SasLibraryDescription(SasDataSetDescription dataSetDescription, String sourceOperatingSystem,
        String sourceSasVersion, LocalDateTime createTime, LocalDateTime modifiedTime) {

        ArgumentUtil.checkNotNull(dataSetDescription, "dataSetDescription");

        ArgumentUtil.checkNotNull(sourceOperatingSystem, "sourceOperatingSystem");
        ArgumentUtil.checkMaximumLength(sourceOperatingSystem, 8, "sourceOperatingSystem");
        ArgumentUtil.checkIsAscii(sourceOperatingSystem, "library operating system");

        ArgumentUtil.checkNotNull(sourceSasVersion, "sourceSasVersion");
        ArgumentUtil.checkMaximumLength(sourceSasVersion, 8, "sourceSasVersion");
        ArgumentUtil.checkIsAscii(sourceSasVersion, "library SAS version");

        ArgumentUtil.checkNotNull(createTime, "createTime");
        ArgumentUtil.checkNotNull(modifiedTime, "modifiedTime");

        // TODO: add builder pattern
        this.dataSetDescription = dataSetDescription;
        this.sourceOperatingSystem = sourceOperatingSystem;
        this.sourceSasVersion = sourceSasVersion;
        this.createTime = createTime;
        this.modifiedTime = modifiedTime;
    }

    /**
     * @return The operating system on which this library was created. This is never {@code null}.
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
     * @return This library's one and only data set. This is never {@code null}.
     */
    public SasDataSetDescription dataSetDescription() {
        return dataSetDescription;
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
     * Creates an exporter for writing this library to a SAS V5 XPORT file according to the default export policy:
     *
     * <ul>
     * <li>Writing data which doesn't fit into the size/type permitted by XPORT throws an
     * {@code IllegalArgumentException}.</li>
     * <li>Writing non-ASCII characters throws an {@code IllegalArgumentException}.</li>
     * </ul>
     *
     * @param path
     *     The location of the file to which this library is to be written.
     *
     * @return An exporter object to which observations can be added. This is never {@code null}.
     *
     * @throws NullPointerException
     *     if {@code path} is {@code null}.
     * @throws IOException
     *     if there was a problem writing to {@code path}.
     */
    public SasTransportExporter exportTransportDataSet(Path path) throws IOException {
        ArgumentUtil.checkNotNull(path, "path");
        return exportTransportDataSet(new BufferedOutputStream(Files.newOutputStream(path)));
    }

    /**
     * Creates an exporter for writing this library to a SAS V5 XPORT file according to the default export policy:
     *
     * <ul>
     * <li>Writing data which doesn't fit into the size/type permitted by XPORT throws an
     * {@code IllegalArgumentException}.</li>
     * <li>Writing non-ASCII characters throws an {@code IllegalArgumentException}.</li>
     * </ul>
     *
     * @param outputStream
     *     The output stream to which this library description is to be written. Closing the returned exporter will
     *     close the stream.
     *
     * @return An exporter object to which observations can be added. This is never {@code null}.
     *
     * @throws NullPointerException
     *     if {@code outputStream} is {@code null}.
     * @throws IOException
     *     if there was a problem writing to {@code outputStream}.
     */
    public SasTransportExporter exportTransportDataSet(OutputStream outputStream) throws IOException {
        ArgumentUtil.checkNotNull(outputStream, "outputStream");
        return new SasTransportExporter(outputStream, this);
    }

    /**
     * Constructs an importer object for reading SAS data from an V5 XPORT file according to the default import policy:
     *
     * <ul>
     * <li>non-ASCII characters are replaced with the Unicode REPLACEMENT CHARACTER (U+FFFD).</li>
     * <li>Two-digit dates for the creation/modified in the headers are based at 1900 if &gt;=60 and based at 2000
     * otherwise.</li>
     * <li>trailing whitespace is trimmed from header fields.</li>
     * <li>trailing whitespace retained in observation values.</li>
     * </ul>
     *
     * @param path
     *     The path to the XPORT file to read.
     *
     * @return An importer object from which a SAS library description and observations from the library's dataset can
     *     be read. This is never {@code null}.
     *
     * @throws IOException
     *     if there was a problem reading from {@code path}.
     * @throws NullPointerException
     *     if {@code path} is {@code null}.
     * @throws MalformedTransportFileException
     *     if {@code path} is not a well-formed V5 XPORT file.
     * @throws UnsupportedTransportFileException
     *     if {@code path} is some other kind of SAS file but not a V5 XPORT file.
     */
    public static SasTransportImporter importTransportDataSet(Path path) throws IOException {
        ArgumentUtil.checkNotNull(path, "path");
        return importTransportDataSet(new BufferedInputStream(Files.newInputStream(path)));
    }

    /**
     * Constructs an importer object for reading SAS data from an V5 XPORT file according to the default import policy:
     *
     * <ul>
     * <li>non-ASCII characters are replaced with the Unicode REPLACEMENT CHARACTER (U+FFFD).</li>
     * <li>Two-digit dates for the creation/modified in the headers are based at 1900 if &gt;=60 and based at 2000
     * otherwise.</li>
     * <li>Two-digit dates for the creation/modified in the headers are assumed to be in the default time zone.</li>
     * <li>trailing whitespace is trimmed from header fields.</li>
     * <li>trailing whitespace retained in observation values.</li>
     * </ul>
     *
     * @param inputStream
     *     The input stream from which a SAS V5 XPORT is to be read. Closing the returned importer will close the
     *     stream.
     *
     * @return An importer object from which a SAS library description and observations from the library's dataset can
     *     be read. This is never {@code null}.
     *
     * @throws IOException
     *     if there was a problem reading from {@code inputStream}.
     * @throws NullPointerException
     *     if {@code inputStream} is {@code null}.
     * @throws MalformedTransportFileException
     *     if {@code inputStream} is not a well-formed V5 XPORT file.
     * @throws UnsupportedTransportFileException
     *     if {@code inputStream} is some other kind of SAS file but not a V5 XPORT file.
     */
    public static SasTransportImporter importTransportDataSet(InputStream inputStream) throws IOException {
        ArgumentUtil.checkNotNull(inputStream, "inputStream");
        return new SasTransportImporter(inputStream);
    }
}