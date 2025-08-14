/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *   src/test/res/cport.xpt
 *********************************************************************/

data data(label='A simple CPORT dataset');
   input  NUMBER;
   format NUMBER 5.2;
   label  NUMBER='A number';
datalines;
10
;
run;


* Export the dataset into a CPORT file using PROC CPORT;
filename cport 'cport.xpt';
proc cport data=work.data file=cport;
run;
