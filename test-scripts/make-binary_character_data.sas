/*********************************************************************
 * Copyright (c) 2025 Fred Hutch Cancer Center
 * Licensed under the MIT License - see LICENSE file for details
 *********************************************************************
 * A SAS program that was used to create
 *  src/test/res/binary_character_data.xpt
 *********************************************************************/

* This file contains character data with possible bytes, from 0 to 255.;
* Each observation has one variable that is 16 bytes long.;
* The observations are written as datalines using a base16 encoding.;
* The byte are given in numerical order.;

libname xportout xport 'binary_character_data.xpt';
data xportout.testdata(label='Data set of binary data (all bytes)');
   input  BINARY $HEX32.;
   format BINARY $BASE64X24.;
   label  BINARY=16 bytes of binary data;
datalines;
000102030405060708090A0B0C0D0E0F
101112131415161718191A1B1C1D1E1F
202122232425262728292A2B2C2D2E2F
303132333435363738393A3B3C3D3E3F
404142434445464748494A4B4C4D4E4F
505152535455565758595A5B5C5D5E5F
606162636465666768696A6B6C6D6E6F
707172737475767778797A7B7C7D7E7F
808182838485868788898A8B8C8D8E8F
909192939495969798999A9B9C9D9E9F
A0A1A2A3A4A5A6A7A8A9AAABACADAEAF
B0B1B2B3B4B5B6B7B8B9BABBBCBDBEBF
C0C1C2C3C4C5C6C7C8C9CACBCCCDCECF
D0D1D2D3D4D5D6D7D8D9DADBDCDDDEDF
E0E1E2E3E4E5E6E7E8E9EAEBECEDEEEF
F0F1F2F3F4F5F6F7F8F9FAFBFCFDFEFF
;
run;

