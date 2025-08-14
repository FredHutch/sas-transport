/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS program that was used to create
 *   src/test/res/240_byte_observation.xpt
 *********************************************************************/

* Create a XPORT file with observations whose binary length is;
* exactly three times the size of a record (240 bytes);
*
* Record #1-#3 - observation #1 (all values set);
* Record #4-#6 - observation #2 (all values set);
* Record #6-#9 - observation #3 (third value missing);
libname xportout xport '240_byte_observation.xpt';
data xportout.data(label='Data Set with 240 byte observation');
   infile datalines FLOWOVER;
   input  TEXT1 $ 1-80 TEXT2 $ 81-160 TEXT3 $ 161-240;
   format TEXT1   $80. TEXT2 $80.     TEXT3 $80.;
   label
      TEXT1='80 character text'
      TEXT2='80 character text'
      TEXT3='80 character text'
   ;
datalines;
This is TEXT1 in observation #1.
This is TEXT2 in observation #1.
This is TEXT3 in observation #1.
This is TEXT1 in observation #2.
This is TEXT2 in observation #2.
This is TEXT3 in observation #2.
This is TEXT1 in observation #3.
This is TEXT2 in observation #3.
.
run;
