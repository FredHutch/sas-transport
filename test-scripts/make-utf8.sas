/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/utf8.xpt
 *********************************************************************/

* Create a XPORT file from a SAS dataset using the XPORT engine;
* Unlike most engines, the XPORT engine does not implement OUTENCODING;

libname xportout xport 'utf8.xpt';
data xportout.data(label='Data set with UTF-8 data');
   input  TEXT $20.;
   format TEXT $30.;
   label  TEXT='Character data';
datalines;
ελληνικά
;
run;
