#!/usr/bin/env groovy
///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
// This script uses the sas-transport library to render the contents of an
// XPORT file.  It is intended at a debugging aid.
///////////////////////////////////////////////////////////////////////////////

import java.nio.file.Path

class XportView {

    private static final Class SasLibraryDescription
    private static final Class MultipleDatasetsNotSupportedException
    static {
        // Make sure the library has been compiled.
        // It should be relative to this script's location.
        def scriptName = XportView.protectionDomain.codeSource.location.path
        def scriptPath = Path.of(scriptName)
        def jarFile
        scriptPath.parent.parent.resolve("target").eachFileMatch(~/^sas-transport-\d+\.\d+\.\d+\.jar$/) { jarFile = it }
        if (jarFile == null) {
            println "ERROR: sas-transport jar does not exist.  Run 'mvn package -DskipTests' to build it"
            System.exit(1)
        }

        XportView.classLoader.rootLoader.addURL(new URL(jarFile.toUri().toString()))
        SasLibraryDescription = Class.forName("org.scharp.sas_transport.SasLibraryDescription")
        MultipleDatasetsNotSupportedException = Class.forName("org.scharp.sas_transport.MultipleDatasetsNotSupportedException")
    }

    private final Path xportFile

    XportView(Path xportFile) {
        this.xportFile = xportFile
    }

    def show() {

        def importer = SasLibraryDescription.importTransportDataset(xportFile)

        def library = importer.sasLibraryDescription()
        println "Library OS             : ${library.sourceOperatingSystem()}"
        println "Library SAS Version    : ${library.sourceSasVersion()}"
        println "Library created        : ${library.createTime()}"
        println "Library last modified  : ${library.modifiedTime()}"
        println "------------------------------------------------------------"

        def datasetDescription = library.datasetDescription()
        println "Dataset Name          : ${datasetDescription.name()}"
        println "Dataset Label         : ${datasetDescription.label()}"
        println "Dataset Type          : ${datasetDescription.type()}"
        println "Dataset OS            : ${datasetDescription.sourceOperatingSystem()}"
        println "Dataset SAS Version   : ${datasetDescription.sourceSasVersion()}"
        println "Dataset created       : ${datasetDescription.createTime()}"
        println "Dataset last modified : ${datasetDescription.modifiedTime()}"
        println "------------------------------------------------------------"

        for (def variable : datasetDescription.variables()) {
            println "Variable Number        : ${variable.number()}"
            println "Variable Name          : ${variable.name()}"
            println "Variable Type          : ${variable.type()}"
            println "Variable Label         : ${variable.label()}"
            println "Variable Length        : ${variable.length()}"
            println "Variable Output Format : ${variable.outputFormat().name()}${variable.outputFormat().width()}.${variable.outputFormat().numberOfDigits()}"
            println "Variable Justification : ${variable.outputFormatJustification()}"
            println "Variable Input Format  : ${variable.inputFormat().name()}${variable.inputFormat().width()}.${variable.inputFormat().numberOfDigits()}"
            println ""
        }
        println "------------------------------------------------------------"

        int i = 1
        try {
            for (def observation = importer.nextObservation();
                 observation != null;
                 observation = importer.nextObservation()) {
                println String.format("%05d: %s", i, observation.collect { it.toString().trim() }.join('|'))
                i++
            }
        } catch (IOException error) {
            if (error.getClass() == MultipleDatasetsNotSupportedException) {
                // handle this exception so it doesn't look like a crash
                println "\nERROR: multiple datasets were found."
            }
        } finally {
            importer.close()
        }
    }

    /**
     * This is where the main script starts
     */
    static void main(String[] args) {

        def cli = new CliBuilder()
        cli.width = 100
        cli.usage = "xportview.groovy [--help] filename"
        cli.header = "Prints the headers contents of an XPORT file to the console"

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
        new XportView(xportFile).show()
    }
}