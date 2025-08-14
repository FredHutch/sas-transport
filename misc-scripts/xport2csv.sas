/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * Call as:
 *    sas xport2csv.sas -sysparm "filename datasetname"
 *
 * Example:
 * To dump the "mydataset" dataset within myfile.xpt, run as
 *    sas xport2csv.sas -sysparm "myfile.xpt mydataset"
 *********************************************************************/

%macro readsysparm;

  %global xportfile csvfile dataset;

  %if %sysevalf(%superq(sysparm)=,boolean) %then %do;
    * If run without parameters;
    %let xportfile = sample.xpt;
    %let dataset   = test;
  %end;
  %else %do;
    %let xportfile = %scan(&sysparm, 1, ' ');
    %let dataset   = %scan(&sysparm, 2, ' ');
  %end;

  * csvfile is xportfile with ".csv" instead of ".xpt";
  %let xportfilelen   = %sysfunc(length(&xportfile));
  %let extensionlen   = %sysfunc(length(.xpt));
  %let extensionindex = %sysevalf(&xportfilelen - &extensionlen);
  %let csvfile        = %sysfunc(substr(&xportfile, 1, &extensionindex)).csv;

%mend;
%readsysparm

/********************************************************************
* Convert SAS XPORT file to a CSV.
********************************************************************/

* Read the XPORT file into a temporary SAS dataset;
libname xptfile xport "&xportfile";
proc copy in=xptfile out=work memtype=data;
run;

* Write the SAS dataset out as a CSV.;
proc export data=work.&dataset
    outfile="&csvfile"
    dbms=csv
    replace;
run;

