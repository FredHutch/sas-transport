Introduction
============

This file contains a list of interesting things that I learned during implementation.

Resources
-----------------------

[SAS Transport V6](https://support.sas.com/techsup/technote/ts140.pdf) -
This is the normative definition of SAS Transport V6 file format.

[SAS Transport V8](https://support.sas.com/techsup/technote/ts140_2.pdf) -
This is the normative definition of SAS Transport V8 file format.

[Study Data Technical Conformance Guide](https://www.fda.gov/media/88173/download) -
FDA Submission Guide. Section 3.3 includes further restrictions on SAS Transport files.


Highest Priority Issues
-----------------------

- The character encoding is part of file format, values spec'd to be ASCII.
- XPORT is a standard that not even SAS follows.
  The %loc2xpt macro generates files that cannot be read by XPORT engine.
  To support both XPORT engine and %loc2xpt xpt files, we could try to be clever.
- In V5 XPORT, values are limited to 200 bytes (SAS typically allows 32767)
- In V5 XPORT, variable names are limited to 8 bytes (SAS typically allows 32)
- In V5 XPORT, Labels are limited to 40 bytes (SAS typically allows 256)

High number of bugs during development
--------------------------------------

I found many more bugs during development than I had expected.

Many of these stemmed from not understanding how to do bitwise manipulation in Java.
When Java promotes from byte to int, large byte values like 0xFF become negative
and are not suitable for bit shifting without being further masked.

Also, when using `>>`, it can shift in a 1 bit if the value was negative.
`>>` is good for dividing by 2, but `>>>` should be used for bitwise manipulation.

Another source of bugs was caused by having to redesign the reading engine as
I learned more about how SAS behaves.

Because of the number of bugs that I started with, I have more confidence in the
tests than the implementation.

File format ambiguities
-----------------------

TS-140 is vague and under-documented.
It is insufficient to provide a correct implementation.
To resolve this ambiguity, I referred to the de facto standard (SAS).

**Whitespace not included in TS-140.**
There is significant whitespace within the header markers, but whitespace is conflated in TS-140.
For example, there are two spaces between "MEMBER" and "HEADER" in
"HEADER RECORD*******MEMBER HEADER RECORD!!!!!!!", but it's documented as one space.

**Can have multiple data sets per XPORT.**
This is not mentioned in TS-140, but XPORT files can have multiple data sets.
In this case, the observation records are padded to the 80 byte boundary and then
followed by the "HEADER RECORD*******MEMBER HEADER RECORD!!!!!!!"

**Final padding vs missing data.**
TS-140 states that the last observation in the file should be padded to fill
out an 80-byte record. However, the "missing value" bit pattern and the
padding bit pattern are identical, so when the observation size is small,
it's ambiguous whether it's an observation with missing values or padding.

**No mention of variable length limit.**
TS-140 implies that character variable has a limit of 32787, based on the type of the "nlng" field (short).
In fact, character variables have a length limit of 200.
This is mentioned as a "difference between version 6 and version 8" in the specification for the V8 format XPORT:
TS-140_2.

> The data portion can have character values that exceed 200 characters.

Interestingly, the SAS XPORT engine truncates values to 200 characters when
exporting, but can import lengths up to 32767.

Multiple ways to create XPORT in SAS
------------------------------------
SAS provides two ways to create an XPORT file.

The first is with the "XPORT Engine" and the second is with the `%loc2xpt` macro
(read as "local format to XPORT macro").
The `%loc2xpt` macro was added in the second maintenance release of SAS 9.4.
It has the nice feature of inspecting the data that you're trying to export and,
if it has characteristics that would be lost if exporting to V5 XPORT (such as long labels or names),
then the extended V8 XPORT format is used instead.

You might think that `%loc2xpt` uses the XPORT Engine, but you'd be wrong.
Instead, it's an entirely separate implementation that's completely written in SAS.
It also has an entirely separate set of bugs:

- The dataset OS is positioned with an off-by-one error, bleeding into the following field  
  (which is supposed to be ASCII spaces)
- Numbers that are slightly larger than the maximum number supported by the format are represented incorrectly  
  (there's some overflow in the bitwise manipulation) and end up being persisted as a value that is smaller than
  numbers that are smaller.
  After a certain size, the number is capped to the maximum number (the correct behavior).
- The "offset in observation" field is written incorrectly, which completely
  garbles data if you write with %loc2xpt and read with XPORT engine.
  (write with `test-scripts/make-v5_loc2xpt.sas` and read with `test-scripts/xport2csv.sas`).

So, it's important that this library be able to read files generated with
the %loc2xpt macro, but when deciding on which behavior is normative, the
XPORT Engine should be used.

I was not able to find any user discussion on which to prefer, only that the
V5 XPORT is the standard accepted by the FDA and that %loc2xpt must be used
if you want the extra features of the V8 format.

SAS Bugs
--------

**SAS and Java differ on dates in the distant future/past**

SAS is documented as supporting years between 1572 and 19,900.

https://support.sas.com/documentation/cdl/en/lrcon/62955/HTML/default/viewer.htm#a002200738.htm

However, SAS cannot format the year 20,000, since its formatting is limited to 4-digit years.
Even limiting the year range 1572-9999 A.D, SAS has a slightly different formatting of the date than Java.

In Java, 9999-12-29T23:59:59 is rendered by SAS as 9999-12-31T23:59:59.

In Java, 1582-01-01T00:00:00 is rendered by SAS as 1582-10-11T00:00:00.

That is, SAS says for the far-future date, SAS says the date is two days later than Java.
For the past date, SAS says the date is 10 days later than Java.

I expect that Java is correct and that SAS has two bugs:

1) For far-future dates, SAS is not handling the century-leap-year exception correctly.
2) For far-past date, SAS's documentation is incorrect; it just doesn't handle 1582.

**XPORT vs CSV in SAS**

SAS has multiple ways to read a CSV. One is to create the data set with the
shape (names and formats) hard-coded and copy the data from the CSV into your data set.
The other is to PROC IMPORT the CSV which takes the variable names from the first line of
the CSV and infers the type and formats from the content (you can configure how many
lines to read, in case some of the values are missing).

If you're using the first option (PROC COPY or INFILE the data), then there
are advantages and disadvantages of using XPORT instead of CSV.

Pros:

- Don't have to set the INFORMAT for hard-to-read data.
- Correctly handles newlines embedded in data.
- File size should be smaller (and should be faster to read)

Cons:

- Potentially some loss of precision for numeric data.
- Does not support non-ASCII characters.  (The specification states that all
  character data is ASCII, but character data is stored as bytes, so
  technically it's possible to use any encoding that you like, but the
  character encoding would become higher-level convention that is not
  evident within the file).
- Variable lengths are limited to 200 characters.
  (The specification does not mention this limit, but the specification for the V8 format implies that
  the limit for the V5 version is 200 characters.
  The XPORT engine does not export more than 200 characters, but it can import lengths up to 32767).

If you're used to using the second method (PROC IMPORT), then switching to
an XPORT has of the pros/cons as listed above, plus:

Pros:

- Names, types, and formats are exactly set with no guessing.
- Don't need to fuss with setting the delimiters
- The A-Z missing values are handled correctly.

Cons:

- Names are truncated to 8 characters, instead of the usual 32.
- Labels are truncated to 40.

When writing an XPORT out, there you have roughly the same advantages and disadvantages (truncation).
However, if you're writing an XPORT out so to be read in later by SAS, then XPORT has a slight advantage
in that it can preserve some additional metadata (formats, labels, and dataset name).

Survey of open-source software for reading/writing SAS files
------------------------------------------------------------

There is more interest in reading SAS7BDAT files than XPORT files.

For sas7bdat, a spec was reverse-engineered to create an R library for reading (but not writing) the files.

https://github.com/BioStatMatt/sas7bdat

The specification was later updated by me https://github.com/FredHutch/sas7bdat-specification

This R library was then ported to Python, which was later completely re-written, although no functionality for writing
was added.

https://pypi.org/project/sas7bdat/

The Python library has become a standard that has been ported to other languages, including JavaScript.

The Pandas Data Frame library can write sas, but it is partly based on the sas7bdat library.

https://pandas.pydata.org/pandas-docs/stable/reference/api/pandas.read_sas.html
https://github.com/pandas-dev/pandas/tree/master/pandas/io/sas

Pandas supports reading SAS7BDAT and XPORT.

In Java, the main implementation for reading SAS7BDAT is epam's parso library:

https://lifescience.opensource.epam.com/parso.html
https://github.com/epam/parso

This is mature and used by several tools.
It does not provide functionality for writing SAS7BDAT.

The only source code I found for writing SAS files was in C:

https://github.com/WizardMac/ReadStat/tree/master/src/sas

However, its README (ad of 2025-08-14) says:

> The produced SAS7BDAT files still cannot be read by SAS, but feel free to contribute your binary-format expertise
> here.

A useful starting point for searching for code: https://github.com/topics/sas7bdat

**Parso Library**

When trying to write SAS7BDAT, I used Parso as a tool to determine the well-formedness of the file.
On some malformed files, I got an IndexOutOfBoundsException when parsing it.

src/test/res/malformed_missing_column_type.sas7bdat

The parso library was able to handle non-ASCII variable names that were encoded in UTF-8.

The Parso library has been dormant since 2021. It has
a [pull request to read XPORT](https://github.com/epam/parso/pull/85). 