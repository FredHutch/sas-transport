/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *   src/test/res/no_observations.xpt
 *********************************************************************/

* A dataset with no observations;
data report(label='A dataset with no observations');
   input  TEXT $ 1-8;
   format TEXT $upcase8.;
   label  TEXT='Some text';
datalines;
run;

* Export the dataset to a V5 XPORT file;
%loc2xpt(libref=work, filespec='no_observations.xpt', format=V5);
run;

