/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *   src/test/res/not_missing_at_end_of_record.xpt
 *********************************************************************/

* Each observation has only one variable.;
* The variable is 1 characters wide.;
* Record #1 - An 'A', 78 missing values, ending with a 'Z';

data report(label='Data with non-missing value at end of record');
   input  TEXT $ 1;
   format TEXT $upcase1.;
   label  TEXT='One Byte Text';
datalines;
A
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
.
.
Z
run;

* Export the data set to a V5 XPORT file;
%loc2xpt(libref=work, filespec='not_missing_at_end_of_record.xpt', format=V5);
run;

