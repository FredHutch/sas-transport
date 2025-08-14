/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS Program that was used to generate
 *  src/test/res/numeric_variations.xpt
 *********************************************************************/

* Data set with variations on numeric data including;
*   All kinds of missing values;
*;
*   -10;
*   0;
*   10;
*;
*   a number slightly LARGER than the smallest possible POSITIVE number;
*   the smallest possible POSITIVE number;
*   a number slightly SMALLER than the smallest possible POSITIVE number;
*   a number MUCH SMALLER than the smallest possible POSITIVE number;
*;
*   a number slightly LARGER than the smallest possible NEGATIVE number;
*   the smallest possible NEGATIVE number;
*   a number slightly SMALLER than the smallest possible NEGATIVE number;
*   a number MUCH SMALLER than the smallest possible NEGATIVE number;
*;
*   a number SLIGHTLY SMALLER than the largest possible POSITIVE number;
*   the largest possible POSITIVE number;
*   a number slightly larger than the largest possible POSITIVE number;
*   a number MUCH LARGER than the largest possible POSITIVE number;
*;
*   a number SLIGHTLY SMALLER than the largest possible NEGATIVE number;
*   the largest possible NEGATIVE number;
*   a number slightly larger than the largest possible NEGATIVE number;
*   a number MUCH LARGER than the largest possible NEGATIVE number;
*;
*   a sequence of numbers with rising exponents across the full range;
data numbers(label='Variations of numeric data');
   input  NUMBER;
   format NUMBER E32.;
   label  NUMBER='A number';
datalines;
.
._
.A
.B
.C
.D
.E
.F
.G
.H
.I
.J
.K
.L
.M
.N
.O
.P
.Q
.R
.S
.T
.U
.V
.W
.X
.Y
.Z
-10
0
10
5.3976053469341111111111111E-79
5.3976053469340000000000000E-79
5.3976053469339000000000000E-79
1E-80
-5.3976053469341111111111111E-79
-5.3976053469340000000000000E-79
-5.3976053469339000000000000E-79
-1E-80
7.2370055773322100000000000E+75
7.2370055773323200000000000E+75
7.2370055773323000000000000E+75
1E76
-7.2370055773322100000000000E+75
-7.2370055773323200000000000E+75
-7.2370055773323000000000000E+75
-1E76
1.23E-79
1.23E-78
1.23E-77
1.23E-76
1.23E-75
1.23E-74
1.23E-73
1.23E-72
1.23E-71
1.23E-70
1.23E-69
1.23E-68
1.23E-67
1.23E-66
1.23E-65
1.23E-64
1.23E-63
1.23E-62
1.23E-61
1.23E-60
1.23E-59
1.23E-58
1.23E-57
1.23E-56
1.23E-55
1.23E-54
1.23E-53
1.23E-52
1.23E-51
1.23E-50
1.23E-49
1.23E-48
1.23E-47
1.23E-46
1.23E-45
1.23E-44
1.23E-43
1.23E-42
1.23E-41
1.23E-40
1.23E-39
1.23E-38
1.23E-37
1.23E-36
1.23E-35
1.23E-34
1.23E-33
1.23E-32
1.23E-31
1.23E-30
1.23E-29
1.23E-28
1.23E-27
1.23E-26
1.23E-25
1.23E-24
1.23E-23
1.23E-22
1.23E-21
1.23E-20
1.23E-19
1.23E-18
1.23E-17
1.23E-16
1.23E-15
1.23E-14
1.23E-13
1.23E-12
1.23E-11
1.23E-10
1.23E-09
1.23E-08
1.23E-07
1.23E-06
1.23E-05
1.23E-04
1.23E-03
1.23E-02
1.23E-01
1.23
1.23E+01
1.23E+02
1.23E+03
1.23E+04
1.23E+05
1.23E+06
1.23E+07
1.23E+08
1.23E+09
1.23E+10
1.23E+11
1.23E+12
1.23E+13
1.23E+14
1.23E+15
1.23E+16
1.23E+17
1.23E+18
1.23E+19
1.23E+20
1.23E+21
1.23E+22
1.23E+23
1.23E+24
1.23E+25
1.23E+26
1.23E+27
1.23E+28
1.23E+29
1.23E+30
1.23E+31
1.23E+32
1.23E+33
1.23E+34
1.23E+35
1.23E+36
1.23E+37
1.23E+38
1.23E+39
1.23E+40
1.23E+41
1.23E+42
1.23E+43
1.23E+44
1.23E+45
1.23E+46
1.23E+47
1.23E+48
1.23E+49
1.23E+50
1.23E+51
1.23E+52
1.23E+53
1.23E+54
1.23E+55
1.23E+56
1.23E+57
1.23E+58
1.23E+59
1.23E+60
1.23E+61
1.23E+62
1.23E+63
1.23E+64
1.23E+65
1.23E+66
1.23E+67
1.23E+68
1.23E+69
1.23E+70
1.23E+71
1.23E+72
1.23E+73
1.23E+74
1.23E+75
;

* Export the data set into an V5 XPORT file;
%loc2xpt(libref=work, filespec='numeric_variations.xpt', format=V5);
run;
