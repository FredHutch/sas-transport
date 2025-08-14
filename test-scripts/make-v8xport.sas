/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS program that was used to generate
 *   src/test/res/v8xport.xpt
 ********************************************************************/

* create sample data;
data report(label='Sample Data Set' type='SALES');

   input Item $ 1-6 Material $ 8-14 Cost 16-22 Profit 24-31;
   format Item
          Material $upcase9.
          Cost
          Profit   dollar15.2;

   /* V5 40 character limit is here -----------------------V */
   label      Item=The item of clothing that was sold;
   label  Material=What the item is made of;
   label      Cost=Dollars to create item/material;
   label    Profit=Dollars of profit for item/material;

   informat
          Item     $char.
          Material $9.
          Cost     comma10.2
          Profit   dollar10.2;

datalines;
shirts cotton  2256354 83952175
ties   silk    498678  2349615
suits  silk    9482146 69839563
belts  leather 7693    14893
shoes  leather 7936712 22964
;
run;

***********************************************************************************************/
* create a XPORT file from a SAS dataset using Proc Copy;
%loc2xpt(libref=work, filespec='v8xport.xpt', format=V8);
run;

