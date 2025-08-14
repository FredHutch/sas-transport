/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *   src/test/res/single_blank_record.xpt
 *********************************************************************/

* Each observation has only one variable.;
* The variable is 20 characters long, which means 4 fill an XPORT record (80 chars).;
* Record #1 - All missing values (also looks like padding);
libname xportout xport 'single_blank_record.xpt';
data xportout.testdata(label='Data set with single record of blanks');
   input  CHARDATA $ 20;
   format CHARDATA $20.;
   label  CHARDATA='Character data';
datalines;
.
.
.
.
;
run;
