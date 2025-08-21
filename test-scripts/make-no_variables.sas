/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/no_variables.xpt
 *********************************************************************/

* A dataset with no variables;
libname xportout xport 'no_variables.xpt';
data xportout.blank(label='A dataset with no variables');
datalines;
;
run;

