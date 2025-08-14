/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS program that was used to create
 *   src/test/res/160_byte_observation.xpt
 *********************************************************************/

* Create a XPORT file with observations whose binary length is;
* exactly twice the size of a record (160 bytes);
*
* Record #1-#2 - observation #1 (both values set);
* Record #3-#4 - observation #2 (both values set);
* Record #4-#6 - observation #3 (second value missing);
libname xportout xport '160_byte_observation.xpt';
data xportout.data(label='Data Set with 160 byte observation');
   infile datalines FLOWOVER;
   input  TEXT1 $ 1-80 TEXT2 $ 81-160;
   format TEXT1   $80. TEXT2 $80.;
   label
      TEXT1='80 character text'
      TEXT2='80 character text'
   ;
datalines;
This is TEXT1 in observation #1.
This is TEXT2 in observation #1.
This is TEXT1 in observation #2.
This is TEXT2 in observation #2.
This is TEXT1 in observation #3.
.
run;
