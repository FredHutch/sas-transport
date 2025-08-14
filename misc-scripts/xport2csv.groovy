#!/usr/bin/env groovy
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
// This script uses the sas-transport library to convert an XPORT file into
// a CSV.  This is an API design test for how difficult it is to create a
// useful tool with the library.  This shouldn't be difficult to write.
// (if it is, then API should be fixed).
///////////////////////////////////////////////////////////////////////////////

class Xport2Csv {

    private static final Class SasLibraryDescription
    private static final Class MissingValue
    static {
        // Make sure the library has been compiled.
        // It should be relative to this script's location.
        Path scriptPath = Paths.get(Xport2Csv.protectionDomain.codeSource.location.path)
        def jarFile
        def targetDir = scriptPath.parent.parent.resolve("target")
        if (Files.exists(targetDir)) {
            targetDir.eachFileMatch(~/^sas-transport-\d+\.\d+\.\d+\.jar$/) { jarFile = it }
        }
        if (jarFile == null) {
            println "ERROR: sas-transport jar does not exist.  Run 'mvn package -DskipTests' to build it"
            System.exit(1)
        }

        Xport2Csv.classLoader.rootLoader.addURL(new URL(jarFile.toUri().toString()))
        SasLibraryDescription = Class.forName("org.scharp.sas_transport.SasLibraryDescription")
        MissingValue = Class.forName("org.scharp.sas_transport.MissingValue")
    }

    private final Path xportFile

    Xport2Csv(Path xportFile) {
        this.xportFile = xportFile
    }

    def show() {
        try {
            def importer = SasLibraryDescription.importTransportDataSet(xportFile)

            // Print the header.
            // The header values do not need to be escaped because variable names cannot have special characters.
            def variables = importer.sasLibraryDescription().dataSetDescription().variables()
            def variableNames = variables.collect { it.name() }
            println variableNames.join(',')

            // Formats value for inserting into CSV
            def format = { object ->
                MissingValue.isAssignableFrom(object.getClass()) ? '' : object.toString().trim()
            }

            // Quotes for CSV
            def csvQuote = { string -> '"' + string.replaceAll('([\"])', '\\$1') + '"' }

            try {
                def observation
                while (observation = importer.nextObservation()) {
                    println observation.collect { csvQuote(format(it)) }.join(',')
                }
            } finally {
                importer.close()
            }
        } catch (e) {
            System.err.println "ERROR: $e.message"
        }
    }

    /**
     * This is where the main script starts
     */
    static void main(String[] args) {

        def cli = new CliBuilder()
        cli.width = 100
        cli.usage = "xport2csv.groovy [--help] filename"
        cli.header = "Converts the contents of an XPORT file to a CSV"

        cli.h(longOpt: 'help', 'Shows the usage information for this tool.')

        def options = cli.parse(args)
        if (!options) {
            // The options could not be parsed.
            cli.usage()
            System.exit(1)
        }
        if (options.help) {
            // Help was explicitly requested.
            cli.usage()
            System.exit(0)
        }
        if (!options.arguments()) {
            // The XPORT filename was missing
            println "ERROR: missing filename arguments"
            cli.usage()
            System.exit(1)
        }
        if (options.arguments().size() != 1) {
            // Multiple XPORT files were given (not supported)
            println "ERROR: too many arguments given"
            cli.usage()
            System.exit(1)
        }

        Path xportFile = Path.of(options.arguments()[0])
        new Xport2Csv(xportFile).show()
    }
}