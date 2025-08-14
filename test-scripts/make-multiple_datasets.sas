/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/multiple_datasets.xpt
 *********************************************************************/

* Data set A;
data a(label='Data set A');
   input  A1 $ 1-20;
   format A1 $20.;
   label  A1='Text in dataset A';
datalines;
data-in-dataset-a
;
run;

* Data set B;
data b(label='Data set B');
   input  B1 $ 1-20;
   format B1 $20.;
   label  B1='Text in dataset B';
datalines;
data-in-dataset-b
;
run;


* Export both data sets to the same V5 XPORT file;
libname xportout xport 'multiple_datasets.xpt';
proc copy in=work out=xportout memtype=data;
run;

