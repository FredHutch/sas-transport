#!/usr/bin/env bash
##################################################################
# Copyright (c) 2025 Fred Hutch Cancer Center
# Licensed under the MIT License - see LICENSE file for details
##################################################################

# Runs all SAS programs to generate the test XPT files.

for sas_program in *.sas; do
   xpt_file="$(basename --suffix=.sas $sas_program).xpt"

   # Remove any file that may exist.
   rm -f $xpt_file

   # Run SAS to generate the XPORT file.
   echo Running $sas_program
   sas $sas_program
   if [ $? -eq 2 ]; then
       # The program failed. Show the errors.
       echo "$sas_program failed"
       grep -iw ERROR "$(basename --suffix=.sas $sas_program).log"
       exit 1
   fi
done;

# Copy the test data to the correct location.
cp *.xpt ../src/test/resources/org/scharp/sas_transport/
