/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS program that was used to generate
 *  src/test/res/variable_nonascii_label.xpt
 ********************************************************************/

libname xportout xport 'variable_nonascii_label.xpt';
data xportout.testdata(label='Data set w/ non-ASCII label on variable');
   input  TEXT $;
   format TEXT $10.;
   label  TEXT=Non-Ascii Label: copyright: '9A'x; * 9A is (c);
datalines;
mydata
;
run;

