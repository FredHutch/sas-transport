#!/usr/bin/env bash
##################################################################
# Copyright (c) 2025 Fred Hutch Cancer Center
# Licensed under the MIT License - see LICENSE file for details
##################################################################

if cmp "$1" "$2" ; then
  # The files are equal.  do nothing
  echo files are equal
else
  # The files are different.  Show the diffs
  diff <(hexdump -Cv "$1") <(hexdump -Cv "$2")
fi
