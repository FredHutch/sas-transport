///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sample;

import org.junit.jupiter.api.Test;
import org.scharp.sas_transport.Format;
import org.scharp.sas_transport.Justification;
import org.scharp.sas_transport.MissingValue;
import org.scharp.sas_transport.SasDatasetDescription;
import org.scharp.sas_transport.SasLibraryDescription;
import org.scharp.sas_transport.SasTransportExporter;
import org.scharp.sas_transport.SasTransportImporter;
import org.scharp.sas_transport.Variable;
import org.scharp.sas_transport.VariableType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains the sample code for using the library that is cited in the {@link org.scharp.sas_transport}
 * package JavaDoc comment.
 * <p>
 * It is included as a test to ensure that the code continues to compile and run without error.
 * </p>
 */
public class SampleCodeTest {

    private void exportSampleTransportFile(Path path) throws IOException {

        List<Variable> variables = Arrays.asList(//
            new Variable(//
                "CITY", // name
                1, // variable number
                VariableType.CHARACTER, //
                20, // length
                "Name of city", //
                new Format("$CHAR", 18), //
                Justification.LEFT, //
                Format.UNSPECIFIED), //

            new Variable(//
                "STATE", // name
                2, // variable number
                VariableType.CHARACTER, //
                2, // length
                "Postal abbreviation of state", //
                new Format("$CHAR", 2), //,
                Justification.LEFT, //
                Format.UNSPECIFIED), //

            new Variable(//
                "HIGH", //
                3, // variable number
                VariableType.NUMERIC, //
                8, //
                "Average daily high in F", //
                new Format("", 5), //
                Justification.LEFT, //
                Format.UNSPECIFIED), //

            new Variable(//
                "LOW", //
                4, //
                VariableType.NUMERIC, //
                8, //
                "Average daily low in F", //
                new Format("", 5), //
                Justification.LEFT, //
                Format.UNSPECIFIED));

        SasDatasetDescription dataset = new SasDatasetDescription(//
            "TEMP", // name
            "Average daily temperatures", // label
            "", // type
            "Java", // OS version
            "5.0", // SAS Version
            variables, // variables
            LocalDateTime.now(), // create
            LocalDateTime.now()); // modified

        try (SasTransportExporter exporter = dataset.newLibraryDescription().exportTransportDataset(path)) {
            exporter.appendObservation(Arrays.asList("Atlanta", "GA", 72, 53));
            exporter.appendObservation(Arrays.asList("Austin", "TX", 80, 5));
            exporter.appendObservation(Arrays.asList("Baltimore", "MD", 65, 45));
            exporter.appendObservation(Arrays.asList("Birmingham", "AL", 74, 53));
            exporter.appendObservation(Arrays.asList("Boston", "MA", 59, MissingValue.STANDARD));
            exporter.appendObservation(Arrays.asList("Buffalo", "NY", 56, 40));
            // ...
            exporter.appendObservation(Arrays.asList("Virginia Beach", "VA", 68, 52));
            exporter.appendObservation(Arrays.asList("Washington", "DC", 68, 52));
        }
    }

    private String padRight(Variable variable, String value) {
        int fullWidth = Math.max(variable.outputFormat().width(), variable.name().length());
        return String.format("%-" + fullWidth + "s ", value.trim());
    }

    private void importSampleTransportFile(Path path) throws IOException {

        try (SasTransportImporter importer = SasLibraryDescription.importTransportDataset(path)) {

            // Get the variables.
            List<Variable> datasetVariables = importer.sasLibraryDescription().datasetDescription().variables();

            // Display a header using the variable names.
            for (Variable variable : datasetVariables) {
                System.out.print(padRight(variable, variable.name()));
            }
            System.out.println();

            List<Object> observation;
            while ((observation = importer.nextObservation()) != null) {

                // Render each value in the observation.
                for (int i = 0; i < observation.size(); i++) {
                    final Variable variable = datasetVariables.get(i);
                    final Object value = observation.get(i);

                    final String formattedValue;
                    if (value instanceof MissingValue) {
                        formattedValue = padRight(variable, ".");
                    } else {
                        formattedValue = padRight(variable, value.toString());
                    }

                    System.out.print(formattedValue);
                }

                System.out.println();
            }
        }
    }

    @Test
    public void runSampleCode() throws IOException {

        Path targetLocation = Files.createTempFile("sas-transport-sample", ".xpt");

        try {
            // Write a dataset.
            exportSampleTransportFile(targetLocation);

            // Read the dataset.
            importSampleTransportFile(targetLocation);

        } finally {
            // Always clean up
            Files.deleteIfExists(targetLocation);
        }
    }
}