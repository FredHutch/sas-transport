/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS program that was used to create
 *   src/test/res/81_byte_observation.xpt
 *********************************************************************/

* An observation whose binary length is one byte larger than the
* the size of a record;
* There is only one observation that spans two records.;
* The second record is mostly (79 bytes) of padding.;
data report(label='Data with an observation that is larger than a record');
   infile datalines TRUNCOVER;
   input  TEXT $ 1-81;
   format TEXT $81.;
   label  TEXT='81 character text' ;
datalines;
This is an observation. After this value is EOF.
run;

* Export the data set to a V5 XPORT file;
%loc2xpt(libref=work, filespec='81_byte_observation.xpt', format=V5);
run;

