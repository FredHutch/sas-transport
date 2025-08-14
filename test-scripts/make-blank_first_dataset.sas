/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *   src/test/res/blank_first_dataset.xpt
 *********************************************************************/

* Data set A;
data blank(label='Data set with no observations');
   input  VAR $ 1-20;
   format VAR $20.;
   label  VAR='Some variable';
datalines;
;
run;

* Second data set;
data second(label='Second data set');
   input  B1 $ 1-20;
   format B1 $20.;
   label  B1='Text in dataset B';
datalines;
data-in-second-dataset
;
run;


* Export both data sets to the same V5 XPORT file;
libname xportout xport 'blank_first_dataset.xpt';
proc copy in=work out=xportout memtype=data;
run;

