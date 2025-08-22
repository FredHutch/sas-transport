Purpose
============

The primary purpose of this library is to be able to generate SAS transport datasets for submission to the FDA.

A secondary purpose is to accept as data as input from SAS programmers where CSV is not practical.

Requirements
------------

1. MUST: Create XPORT suitable for FDA submission
2. MUST: Read XPORT to get data as richly as from a CSV
3. MUST: Scale to support large datasets (5 GB)
4. SHOULD: Warn caller when data is truncated or doesn't "fit" into XPORT
5. SHOULD: Warn caller when XPORT cannot be understood completely
6. SHOULD: Use reflection to read/write observations as POJO
7. NICE IF: Supported CPORT format (compressed XPORT). There is no public spec for this.
8. NICE IF: Supported XPORT for v8 SAS programs (FDA does not accept this).
9. NICE IF: Library is posted to GitHub
10. DONT CARE: A validation plan is provided.

Design Principles
-----------------

**Natural in Java**

- JavaDoc on all public types and methods.
- Strong type system
- Follows standard Java conventions and Effective Java

**Prefer readability to performance:**
For code that processes data used for FDA submission, clarity of intent is a feature.
When deciding between performance or clarity, prefer clarity.
This includes not precomputing values and using strings instead of bytes array.

**Try for symmetry:**
Try for API symmetry between the reader and the writer.
This reduces the complexity of using the API.

**Easy stuff should be easy:**
The API should support whatever is needed, but doing easy things should remain easy.
For example, reading an XPORT file should be 10 lines of code if you don't care about details how to interpret dates.

Companion Scripts
-----------------

- `xportview.groovy`: A Groovy script that dumps a textual representation of an XPORT file.
  This exercises the library and is a useful debugging tool.

- `xport2csv.groovy`: A Groovy script that converts an XPORT file to a CSV.
  This is an API design test for how difficult it is to create a
  useful tool with the library. This shouldn't be difficult to write
  (if it is, then API should be fixed).

- `make-xport.sas`: A sample SAS program for writing an XPORT file.
  This is used to help Java programmers generate test data.

- `xport2csv.sas`: A SAS program that converts an XPORT file into a CSV.
  This is a debugging tool to show how SAS interprets an XPORT (that is, the correct way to view the XPORT).

API Anti-symmetry problems
---------------------------

**StandardFormat and user formats.**
Formats can be specified with enum StandardFormat for CHAR, UPCASE, etc.
When reading, if we get a non-standard, it would be USER_DEFINED.
This means we need a way to get the enum and the String form.

**Years are two-digits.**
Create or Modify time are stored as strings, giving 2 digits for dates.

The writer takes a Date (naturally), but the reader cannot set a Date
without caller intervention to supply the base year.
Most caller won't even care about this until the year 2060.

**Internal "Header" records.**
There are some C structs documented in TS-140 that need to be serialized and unserialized.
I created package-private intermediate classes for these, but the truth is that the writing
is so different from the reading that there is no overlap in their use.

**You can write a null value, but you read it as MissingValue**
Reading a missing numeric value results in the corresponding MissingValue being returned,
but writing allows "null" be interpreted as MissingValue.STANDARD.

The rationale is that the data may be coming from JSON, YAML, or a database.
These storage formats all support "null" as a possible value but not MissingValue.
Rather than require developers to convert null to a missing value, we do it for them.
This is only for numeric values, because SAS represents a missing character value as a blank string.

**createExporter() is an instance method, createImporter() is static.**
In one case, you have a library object to export.
In the other case, you want to construct the library from a file.


Design Decisions
----------------

**The naming convention will follow Java, then TS-140.**

The target audience is assumed to be first a Java programmer with some SAS familiarity.

The library should first and foremost feel like a natural Java language.

Where further conventions are arbitrary, they will follow TS-140.
For example, the term "label" is used instead of "tag" or "description".

**There will be a streaming interface to support very large datasets.**

The FDA says it supports datasets up to 5 GB.

This follows from the requirement to support any FDA-accepted dataset.

**There will be separate exceptions for invalid XPORT and unsupported.**

Both will derive from IOException.

**There will be types for SAS concepts, however trivial.**
For example, "enum VariableType", even though boolean isNumeric would suffice.

This is to support a strong typing system.

**Will support a conforming JVM in any locale**

Following from naturalness in Java...this will support JVMs configured in any locale or character sets.
Mostly, this means not relying on the default character set or calendar formatting for dates.

**Can't export Date, Instant, Timestamp, or Time values**

SAS's dates, times, and timestamp are all in "local time", in that they are offsets from an Epoch
that doesn't have a timestamp. Any Java type with an implicit or explicit time zone can't be converted
to "local time" without knowing the time zone. Guessing the wrong time zone would lead to silent data loss.

It's better to make the programmer provide the time zone by converting it to local time than to
silently corrupt the data.

**There will be a "strict" mode when reading**

There will be different parsing modes to let the reader decide how lenient the API should be.
For example, if data would be truncated when writing to XPORT, if data doesn't conform to FDA guidelines, or
if non-essential values are corrupt when reading.

**We target JRE 8**

The Java 8 features that I plan to use are:

- AutoCloseable on reader/writer (caller can use try with resources)
- Path (NIO) (caller can use Path to read/write)
- java.time.* types

**No validation plan is provided on GitHub**

Even though the only people who might use this library would care have to
validate their software, we will not provide a validation plan.

This is because we don't validate at the library level, and so we have no need to create our own plan.
Since a posting the library on GitHub merely speculates that there would be community interest, a validation plan may
not actually be useful to anyone.

Therefore, we will not expend the effort for this.

**Support for compressed transport files is "nice-to-have"**

The FDA guidelines state:

> SAS Transport files processed by the SAS CPORT cannot be reviewed, processed, or archived by FDA.
> Sponsors can find the record layout for SAS XPORT transport files through SAS technical document TS-140.
> All SAS XPORT transport files should use .xpt as the file extension.
> There should be one dataset per XPORT file and the files should not be compressed.

Furthermore, the compressed XPORT format is not publicly documented.

As a result, we may be able to support it and any support we do have would be ad hoc based and reverse-engineered.

If it cannot be supported, it compressed transport files should be *recognized* so
that it can be rejected with a clear diagnostic message.

**Test Plan**

Search for historical .XPT files looking for XPORT.
These will not be committed as part of the product but used to test the processing.
Ones created on different operating systems or SAS versions are good for variations,
but even within an OS/version, there variety can be obtained with respect to alignment or data types.

Run viewxport.groovy on these files (to test the library).

Unit tests will create files using the library and compare to an expected value (binary comparison against file on
disk).
This file will have previously been loaded by SAS.

Unit tests will include loading files that are malformed in interesting ways.
These should have predictable exceptions thrown when reading.

Conversion for all floating point values from the TS-140 cnxptiee() sample code will be used in unit tests.

Reading of VSX/VMS-sized records (136 instead of 140, for some reason).
These may need to be constructed explicitly.

API design questions
--------------------

**StrictnessMode or API object with callbacks**

The writer can reasonably take a "strictness" flag, but the reader may want finer control.

- Malformed SAS transport
- Metadata Loss (truncation of label)
- Data Loss
- Non-ASCII characters in data
- Data truncation
- Unknown format

**Builder pattern to support immutable objects**
In order for SasLibraryDescription and SasDatasetDescription to be immutable, I expect to provide a builder.
I haven't made a builder anything with a class hierarchy this deep
and I probably wouldn't if it were within a single app.

**Since immutable should I implement hashCode(), equals()?**
I suppose this follows from trying to create an API that fits with Effective Java.

**How should observations passed back on the reader?**
It could use List<Object>, a list of variants, or fill in a POJO by reflection.

For anything more than implementing middleware where the observations
are opaque, a POJO would be preferable.

**Should SASTransportImporter implement an iterator() method?**
If we have multiple ways of reading it, does it need multiple iterator() methods?

**String VariableFormat.format(Object value)**
This would be a lot of work, implementing the formatter for each StandardFormat.
However, it's needed to support a CSV export.
