/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *   src/test/res/missing_values_or_padding.xpt
 *********************************************************************/

* Each observation has only one variable.;
* The variable is 8 characters long, which means 10 fill an XPORT record (80 chars).;
* Record #1 - "TEXT" followed by 9 missing values (treated as padding by SAS);
libname xportout xport 'missing_values_or_padding.xpt';
data xportout.testdata(label='Data that ends in missing values');
   input  CHARDATA $ 1-8;
   format CHARDATA $upcase8.;
   label  CHARDATA='Character data';
datalines;
TEXT
.
.
.
.
.
.
.
.
.
;
run;

