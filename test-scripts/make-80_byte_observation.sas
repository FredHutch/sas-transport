/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS program that was used to create
 *   src/test/res/80_byte_observation.xpt
 *********************************************************************/

* An observation whose binary length is exactly the same size as a;
* record  (80 bytes);
* Record #1 - observation with a value;
* Record #2 - observation with a missing value;

data report(label='Data with a full record missing values');
   input  TEXT $ 1-80;
   format TEXT $80.;
   label  TEXT='80 character text';
datalines;
This is an observation.  The next observation is missing.  After that is EOF.
.
run;

* Export the dataset to a V5 XPORT file;
%loc2xpt(libref=work, filespec='80_byte_observation.xpt', format=V5);
run;

