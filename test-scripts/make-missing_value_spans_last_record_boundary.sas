/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/missing_value_spans_last_record_boundary.xpt
 *********************************************************************/

* Each observation has only one variable.;
* The variable is 35 characters long, which means that between 2 and 3;
* fit in a single XPORT record (80 chars).;
* Record #1 - missing observation, observation with value, start of observation 3.
* Record #2 - rest of observation #3 (missing value);
libname xportout xport 'missing_value_spans_last_record_boundary.xpt';
data xportout.report(label='Missing value spans record boundary');
   input  TEXT $ 1-35;
   format TEXT $upcase35.;
   label  TEXT='Some text';
datalines;
.
next value spans record boundary
.
run;

