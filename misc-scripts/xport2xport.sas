/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * This is a helper program that reads an XPORT file into a SAS,
 * and then writes out the exact same library to an XPORT file.
 * In most cases, the output should match the input.
 * This is an exploratory tool to determine which parts of an XPORT
 * file SAS ignores.
 *
 * Call as:
 *    sas xport2xport.sas -sysparm filename
 *
 * Example:
 * To translate read myfile.xpt and write it to myfile-clean.xpt
 *    sas xport2xport.sas -sysparm myfile.xpt
 ********************************************************************/

%macro readsysparm;

  %global inputfile outputfile dataset;

  %if %sysevalf(%superq(sysparm)=,boolean) %then %do;
    * If run without parameters;
    %put ERROR: missing required sysparm;
  %end;
  %else %do;
    %let inputfile      = %scan(&sysparm,1,' ');

    * outputfile is inputfile without the ".xpt" extension;
    %let inputfilelen   = %sysfunc(length(&inputfile));
    %let extensionlen   = %sysfunc(length(.xpt));
    %let extensionindex = %sysevalf(&inputfilelen - &extensionlen);
    %let outputfile     = %sysfunc(substr(&inputfile, 1, &extensionindex));
  %end;

%mend;
%readsysparm

* Read the XPORT file to an XPORT file;
libname xportin  xport "&inputfile";
libname xportout xport "&outputfile\-clean\.xpt";
proc copy in=xportin out=xportout memtype=data;
run;

