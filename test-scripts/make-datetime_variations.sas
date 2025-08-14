/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/datetime_variations.xpt
 *********************************************************************/

* Data set with variations on datetimes including:
*   All kinds of missing values;
*   one second before the Epoch;
*   the Epoch;
*   one second after the Epoch;
*   one millisecond before the Epoch (fractional value);
*   one millisecond after the Epoch (fractional value);
*   the smallest supported date;
*   the largest supported date;
*;
libname xportout xport 'datetime_variations.xpt';
data xportout.datetime(label='Variations of datetime data');
   input    DATETIME DATE TIME;
   INformat DATETIME DATETIME22.3 DATE DATE11. TIME TIME12.3;
   format   DATETIME DATETIME22.3 DATE DATE11. TIME TIME12.3;
   label
       DATETIME='A datetime'
           DATE='A date (no time)'
           TIME='A time (no date)'
   ;
datalines;
.                         .                   .
._                        ._                  ._
.A                        .A                  .A
.B                        .B                  .B
.C                        .C                  .C
.D                        .D                  .D
.E                        .E                  .E
.F                        .F                  .F
.G                        .G                  .G
.H                        .H                  .H
.I                        .I                  .I
.J                        .J                  .J
.K                        .K                  .K
.L                        .L                  .L
.M                        .M                  .M
.N                        .N                  .N
.O                        .O                  .O
.P                        .P                  .P
.Q                        .Q                  .Q
.R                        .R                  .R
.S                        .S                  .S
.T                        .T                  .T
.U                        .U                  .U
.V                        .V                  .V
.W                        .W                  .W
.X                        .X                  .X
.Y                        .Y                  .Y
.Z                        .Z                  .Z
31DEC1959:23:59:59.000    31-DEC-1959         23:59:59.000
01JAN1960:00:00:00.000    01-JAN-1960         00:00:00.000
01JAN1960:00:00:01.000    02-JAN-1960         00:00:01.000
31DEC1959:23:59:59.999    31-DEC-1959         23:59:59.999
01JAN1960:00:00:00.001    01-JAN-1960         00:00:00.001
01JAN1582:00:00:00.000    01-JAN-1582         00:00:00
31DEC9999:23:59:59.000    31-DEC-9999         00:00:00
;

