This directory contains data that is used for testing.

cport.xpt      - A well-formed SAS CPORT file
minidata.xpt   - A well-formed V5 XPORT file with one variable, one observation that has a missing value
v8xport.xpt    - A well-formed V8 XPORT file
v5_loc2xpt.xpt - A V5 XPORT that was created with the %loc2xpt macro (not well formed)
openvms.xpt    - An XPORT that was created with the XPORT engine on SAS 9.2 on OpenVMS

# Variations for testing record-streaming and padding detection.
no_observations.xpt                          - XPORT with no observations
single_blank_record.xpt                      - XPORT with a single observation record of ASCII blanks
missing_values_or_padding.xpt                - XPORT that ends in missing values that looks like final padding
record_of_missing_values.xpt                 - XPORT with 80 characters of missing values
record_of_numeric_spaces.xpt                 - XPORT with numeric data that looks like ASCII blanks
missing_value_spans_last_record_boundary.xpt - XPORT with missing value that spans record boundary
two_records_with_blanks.xpt                  - XPORT with consecutive records filled with blanks (missing values)
not_missing_at_end_of_record.xpt             - XPORT with non-missing value at end of record of mostly blanks
80_byte_observation.xpt                      - XPORT with an observation that is 80 bytes
81_byte_observation.xpt                      - XPORT with an observation that is 81 bytes
160_byte_observation.xpt                     - XPORT with an observation that is 160 bytes (final record is fully blank)
240_byte_observation.xpt                     - XPORT with an observation that is 240 bytes (final record is fully blank)

# Variations on well-formed content
yearcutoff.xpt                         - XPORT with two-digit year in timestamps set to "60"
yearcutoff_minus_one.xpt               - XPORT with two-digit year in timestamps set to "59"
datetime_variations.xpt                - XPORT with variations on date/time data.
numeric_truncations.xpt                - XPORT with variations on numeric data and truncations 3. -> 8.
numeric_variations.xpt                 - XPORT with variations on numeric data
max_variables.xpt                      - XPORT with max number of variables in an observation (9999)
no_variables.xpt                       - XPORT with no variables
multiple_datasets.xpt                  - XPORT with two data sets (not supported)
blank_first_dataset.xpt                - XPORT with two data sets, the first has no observations (not supported)
reused_data.xpt                        - XPORT with multiple variables with overlapping locations in observation
user_defined_format.xpt                - XPORT that references user-defined formats
dataset_nonascii_type.xpt              - XPORT that has a non-ASCII character in the data set type
variable_nonascii_label.xpt            - XPORT that has a non-ASCII character in a variable's label
nonascii_value.xpt                     - XPORT that has a non-ASCII character in the variable's value
utf8.xpt                               - XPORT that has Greek characters encoded in UTF-8
binary_character_data.xpt              - XPORT that has all bytes of binary data (0-255)
max_length_variable.xpt                - XPORT that has character and numeric data at its maximum length

# Variations on badly-formed content (manually edited)
malformed_library_nonascii_os.xpt        - XPORT with a non-ASCII character in the library OS
malformed_library_nonascii_version.xpt   - XPORT with a non-ASCII character in the library SAS version
malformed_library_createtime.xpt         - XPORT with malformed library creation timestamp
malformed_library_modifiedtime.xpt       - XPORT with malformed library modification timestamp
malformed_blank_name.xpt                 - XPORT with a blank data set name
malformed_first_dataset_blank_name.xpt   - XPORT with multiple datasets and the first one has a blank name
malformed_nonascii_name.xpt              - XPORT with a non-ASCII character in the data set name
malformed_blank_in_name.xpt              - XPORT with a space character in the data set name
malformed_dataset_nonascii_os.xpt        - XPORT with a non-ASCII character in the data set OS
malformed_dataset_nonascii_version.xpt   - XPORT with a non-ASCII character in the data set SAS version
malformed_dataset_createtime.xpt         - XPORT with a malformed timestamp for the data set creation time
malformed_dataset_modifiedtime.xpt       - XPORT with a malformed timestamp for the data set modification time
malformed_variable_empty_name.xpt        - XPORT with a variable with a blank name
malformed_variable_nonascii_name.xpt     - XPORT with a non-ASCII character in a variable name
malformed_number_too_long.xpt            - XPORT with a numeric variable whose length is 9
malformed_number_too_short.xpt           - XPORT with a numeric variable whose length is 1
malformed_variable_negative_length.xpt   - XPORT with a character variable whose length is negative
malformed_variable_zero_length.xpt       - XPORT with a character variable whose length is zero
malformed_variable_name_with_blank.xpt   - XPORT with a variable named "VAR A"
malformed_variable_reserved_name.xpt     - XPORT with a variable named "_N_"
malformed_hash_not_zero.xpt              - XPORT with a variable whose name hash is not 0
malformed_variable_type_unknown.xpt      - XPORT with a variable whose type code is 0 (only 1 and 2 are defined)
malformed_format_negative_width.xpt      - XPORT with a character variable and a negative format width
malformed_format_negative_digits.xpt     - XPORT with a numeric variable and a negative "d" in its format
malformed_format_width_equals_digits.xpt - XPORT with a Fw.d with a "w" that equals the "d" in its format
malformed_format_nonascii_name.xpt       - XPORT with a non-ASCII character in a format name
malformed_variables_same_number.xpt      - XPORT with four variables, all of which have the same varnum (1)
malformed_variables_same_name.xpt        - XPORT with multiple variables that have the same name (ITEM)
malformed_variables_reverse_order.xpt    - XPORT whose variables are given in the opposite order as in the observation
malformed_variables_negative.xpt         - XPORT with a data set that claims to have a negative number of variables
malformed_variables_badnum.xpt           - XPORT with a data set that claims to have "1XXX" as the number of variables
malformed_variables_padded.xpt           - XPORT with a data set that claims to have "1   " as the number of variables
malformed_variables_nonnumeric_count.xpt - XPORT with a data set that claims to have "NAN!" as the number of variables
malformed_variables_count_too_small.xpt  - XPORT with a data set that claims to have 1 NAMESTR records, but has 4
malformed_variables_count_too_large.xpt  - XPORT with a data set that claims to have 10 NAMESTR records, but has 4
malformed_variable_negative_offset.xpt   - XPORT with a variable that has a large negative offset in the observation
malformed_variable_justification.xpt     - XPORT with a variable that has an unknown justification code (3)
malformed_variable_negative_number.xpt   - XPORT with a variable with a negative varnum
malformed_variable_length_max_short.xpt  - XPORT with character data whose length is 32767 (export limit is 200)
malformed_missing_value_at.xpt           - XPORT with malformed numeric value (@ missing value, one less than 'A')
truncated_libraryheader.xpt              - malformed XPORT (truncated one byte before the end of the first record)
truncated_firstrealheader.xpt            - malformed XPORT (truncated one byte before the end of the second record)
truncated_librarymodifyheader.xpt        - malformed XPORT (truncated one byte before the end of the third record)
truncated_memberheader.xpt               - malformed XPORT (truncated one byte before the end of the member header record)
truncated_memberdescriptor.xpt           - malformed XPORT (truncated one byte before the end of the member descriptor record)
truncated_memberheaderdata1.xpt          - malformed XPORT (truncated one byte before the end of the first member header data record)
truncated_memberheaderdata2.xpt          - malformed XPORT (truncated one byte before the end of the second member header data record)
truncated_namestrheader.xpt              - malformed XPORT (truncated one byte before the end of the NAMESTR header record)
truncated_namestr1.xpt                   - malformed XPORT (truncated one byte before the end of the first NAMESTR record)
truncated_namestr2.xpt                   - malformed XPORT (truncated one byte before the end of the second NAMESTR record)
truncated_namestrpadding.xpt             - malformed XPORT (truncated one byte before the end of the NAMESTR padding)
truncated_observationheader.xpt          - malformed XPORT (truncated one byte before the end of the observation header)
truncated_observation.xpt                - malformed XPORT (truncated one byte before the end an observation)
truncated_observation_at_record.xpt      - malformed XPORT (truncated within observation at record boundary)
truncated_big_observation.xpt            - malformed XPORT (truncated within observation that spans three records)
truncated_after_blank_observation.xpt    - malformed XPORT (truncated one byte before record boundry after padding record)
malformed_corrupt_libraryheader.xpt      - XPORT with single byte corruption in the library header (first record)
malformed_corrupt_firstrealheader.xpt    - XPORT with single byte corruption in the first real header


# Manually verified baseline XPORT files for the exporter test
SasTransportExporterTest_smokeTest.xpt                - data for SasTransportExporterTest.smokeTest()
SasTransportExporterTest_testNumericVariations.xpt    - data for SasTransportExporterTest.testNumericVariations()
SasTransportExporterTest_testNumericTruncations.xpt   - data for SasTransportExporterTest.testNumericTruncations()
SasTransportExporterTest_testMaxVariables.xpt         - data for SasTransportExporterTest.testMaxVariables()
SasTransportExporterTest_testMinVariables.xpt         - data for SasTransportExporterTest.testMinVariables()
SasTransportExporterTest_testVariableVariations.xpt   - data for SasTransportExporterTest.testVariableVariations()
SasTransportExporterTest_testDateTimeVariations.xpt   - data for SasTransportExporterTest.testDateTimeVariations()
SasTransportExporterTest_testNullValues.xpt           - data for SasTransportExporterTest.testNullValues()
SasTransportExporterTest_testBadCharaterValues.xpt    - data for SasTransportExporterTest.testBadCharaterValues()
SasTransportExporterTest_testBadNumericValues.xpt     - data for SasTransportExporterTest.testBadNumericValues()
SasTransportExporterTest_testMisshapenObservation.xpt - data for SasTransportExporterTest.testMisshapenObservation()
