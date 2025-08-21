/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/nonascii_value.xpt
 *********************************************************************/

* create a XPORT file from a SAS dataset using the XPORT engine;
libname xportout xport 'nonascii_value.xpt';
data xportout.data(label='A dataset with a non-ASCII character in a value');
   input  TEXT $hex30.;
   format TEXT $15.;
   label  TEXT='Character data';
datalines;
4D4943524F5320B5B5
;
run;

