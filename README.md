This repository contains a Java library for reading and writing SAS Transport files.

The SAS Transport file format is the required format when submitting datasets to the
United States Food and Drug Administration (FDA), as well as what's read by the popular
data validation tool Pinnacle 21.

Quick Start
-----------

The following code shows how to use the library to read a SAS transport file a hard-coded dataset.

    List<Variable> variables = Arrays.asList(
        new Variable(
            "CITY",                  // name
            1,                       // variable number
            VariableType.CHARACTER,  // type
            20,                      // length
            "Name of city",          // label
            new Format("$CHAR", 18), // output format
            Justification.LEFT,
            Format.UNSPECIFIED),     // input format

        new Variable(
            "STATE",
            2,
            VariableType.CHARACTER,
            2,
            "Postal abbreviation of state",
            new Format("$CHAR", 2),
            Justification.LEFT,
            Format.UNSPECIFIED),

        new Variable(
            "HIGH",
            3,
            VariableType.NUMERIC,
            8,
            "Average daily high in F",
            new Format("", 5),
            Justification.LEFT,
            Format.UNSPECIFIED),

        new Variable(
            "LOW",
            4,
            VariableType.NUMERIC,
            8,
            "Average daily low in F",
            new Format("", 5),
            Justification.LEFT,
            Format.UNSPECIFIED));

        SasDatasetDescription dataset = new SasDatasetDescription(
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

The following code sample demonstrates how to read a SAS transport file and print out its contents into a table.

    Path path = ...;
   
    try (SasTransportImporter importer = SasLibraryDescription.importTransportDataset(path)) {
   
        // Get the variables.
        List<Variable> datasetVariables = importer.sasLibraryDescription().datasetDescription().variables();
   
        // Display a header using the variables.
        StringBuilder header = new StringBuilder();
        for (Variable variable : datasetVariables) {
            header.append(String.format("%-" + (variable.outputFormat().width() + 1) + "s ", variable.name()));
        }
        System.out.println(header.toString());
   
        List<Object> observation;
        while ((observation = importer.nextObservation()) != null) {
            StringBuilder row = new StringBuilder(header.length());
   
            // Render each value in the observation
            for (int i = 0; i < observation.size(); i++) {
                final Variable variable = datasetVariables.get(i);
                final Object value = observation.get(i);
   
                final String formattedValue;
                if (value instanceof MissingValue) {
                    formattedValue = String.format("%-" + (variable.outputFormat().width() + 1) + "s", ".");
                } else {
                    formattedValue = String.format("%-" + (variable.outputFormat().width() + 1) + "s", value.toString());
                }
                row.append(formattedValue + " ");
            }
   
            System.out.println(row.toString());
        }
    }

Limitations
-----------

* Only XPORT V5 is supported.
* The transport files must only have one dataset per file.
* Only ASCII strings are supported (as required by the FDA).
* Requires Java 8 or later.
* Compressed files (CPORT) are not supported.
