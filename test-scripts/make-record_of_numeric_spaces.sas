/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *   src/test/res/record_of_numeric_spaces.xpt
 *********************************************************************/

* Each observation has only one variable.;
* The variable is 8 bytes long, which means 10 fill an XPORT record (80 bytes).;
* Record #1 - 10 specially crafted numbers whose byte pattern is all spaces;
* Record #2 - a missing value TEXT followed by 9 specially crafted numbers;
* Record #3 - 10 specially crafted numbers (treated as padding by SAS);
libname xportout xport 'record_of_numeric_spaces.xpt';
data xportout.TESTDATA(label='Data with values that looks like padding');
   input  NUMBER;
   format NUMBER $E32.;
   label  NUMBER='Numeric data';
datalines;
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
.
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
3.687825414344431e-40
;
run;

