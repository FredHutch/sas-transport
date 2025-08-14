/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/dataset_nonascii_type.xpt
 *********************************************************************/

* create a XPORT file from a SAS dataset using the XPORT engine;
libname xportout xport 'dataset_nonascii_type.xpt';
data xportout.data(label='Data Set with non-ASCII char in type' type='4D4943524FB5B5'x);

   input  number;
   format number 5.2;
   label  number=numeric data;

datalines;
999
;
run;
