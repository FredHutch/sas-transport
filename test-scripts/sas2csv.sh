#!/usr/bin/env bash
##################################################################
# Copyright (c) 2025 Fred Hutch Cancer Center
# Licensed under the MIT License - see LICENSE file for details
##################################################################

# Check command-line arguments
if [ "$#" -ne 2 ]; then
  echo "Usage: $0 XPORTBASE DATASET"    >&2
  echo ""                               >&2
  echo "Example:"                       >&2
  echo "  $0 160_byte_observation DATA" >&2
  exit 1
fi

DATASET=$2
SASFILE="make-$1.sas"
XPTFILE="$1.xpt"
CSVFILE="$1.csv"

if [ ! -e "$SASFILE" ]; then
   echo "ERROR: $SASFILE does not exist"
   exit 1
fi

# Remove the XPORT file, if it exists.
rm --force "$XPTFILE"

# Run the SAS program to create the XPORT.
sas "$SASFILE"

if [ ! -e "$XPTFILE" ]; then
   # The XPORT file wasn't generated.  Show the log.
   echo "ERROR: $XPTFILE was not created"
   grep -iw ERROR "make-$1.log"
   exit 1
fi

# The XPORT was created.  Generate the CSV.
rm --force "$CSVFILE"
sas ../misc-scripts/xport2csv.sas -sysparm "$XPTFILE $DATASET"
if [ ! -e "$CSVFILE" ]; then
   # The CSV wasn't generated.  Show the errors.
   echo "$CSVFILE was not created"
   grep -iw ERROR xport2csv.log
   exit 1
fi

# Show the CSV
cat "$CSVFILE"

