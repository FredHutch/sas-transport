/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *   src/test/res/record_of_missing_values.xpt
 *********************************************************************/

* Each observation has only one variable.;
* The variable is 8 characters long, which means 10 fill an XPORT record (80 chars).;
* Record #1 - 10 missing values;
* Record #2 - TEXT followed by 9 missing values;
* Record #3 - 10 missing values (treated as padding by SAS);
libname xportout xport 'record_of_missing_values.xpt';
data xportout.testdata(label='Data with a full record missing values');
   input  CHARDATA $ 1-8;
   format CHARDATA $upcase8.;
   label  CHARDATA='Character data';
datalines;
.
.
.
.
.
.
.
.
.
.
TEXT
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
.
run;
