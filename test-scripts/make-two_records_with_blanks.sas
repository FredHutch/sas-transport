/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/two_records_with_blanks.xpt
 *********************************************************************/

* Each observation has only one variable.;
* The variable is 40 characters long, which means that 2 observation;
* fit in a single XPORT record (80 chars).;
* Record #1 - two missing observations;
* Record #2 - two missing observations;
* Record #3 - an observation with data, followed by missing value;
* Record #4 - two missing observations;
* Record #5 - two missing observations;

data report(label='consecuative records of missing values');
   input  TEXT $ 1-40;
   format TEXT $upcase40.;
   label  TEXT='Some text';
datalines;
.
.
.
.
Value between records with blanks
.
.
.
.
.
run;

* Export the dataset into a V5 XPORT file;
%loc2xpt(libref=work, filespec='two_records_with_blanks.xpt', format=V5);
run;

