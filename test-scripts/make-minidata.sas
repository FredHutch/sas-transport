/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/minidata.xpt
 *********************************************************************/

* A dataset with a single numeric value;
data minidata(label='A dataset with only one value');
   input  NUMBER;
   format NUMBER 5.2;
   label  NUMBER='A number';
datalines;
.
;

* Export the dataset into an V5 XPORT file;
%loc2xpt(libref=work, filespec='minidata.xpt', format=V5);
run;

