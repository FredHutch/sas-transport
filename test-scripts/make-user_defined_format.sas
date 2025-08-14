/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/user_defined_format.xpt
 *********************************************************************/

* Create some user-defined formats;
proc format;
  VALUE $SEX
    "M" = "Male"
    "F" = "Female";

  VALUE INCOME
    LOW   -< 20000 = "Low"
    20000 -< 60000 = "Middle"
    60000 - HIGH   = "High";

  VALUE SMOKER
    0     = 'Nonsmoker'
    1     = 'Past smoker'
    2     = 'Current smoker'
    OTHER = 'Unknown';
run;

* create a XPORT file from a SAS dataset using the XPORT engine;
libname xportout xport 'user_defined_format.xpt';
data xportout.report(label='Data Set with User-Defined Formats');

   input ID $ Sex $ Income Smoker;
   format ID     $UPCASE9.
          Sex    $SEX.
          Income INCOME.
          Smoker SMOKER.;

   /* V5 40 character limit is here -----------------------V */
   label        ID=A unique identifier for the person;
   label       Sex=The gender at birth;
   label    Income=Annual income;
   label    Smoker=Code for cigarette smoking status;

datalines;
31ABC M  45000  0
21JKL F  200000 1
382JI F  131000 2
MRX12 X  6000   3
;
run;

