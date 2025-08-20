///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * An interface for mapping the two-digit years which is stored in SAS format to a real year (usually four digits).
 */
@FunctionalInterface
interface YearMapper {
    /**
     * Maps a two-digit year, like 99, to a real year, like 1999.
     *
     * @param twoDigitYear
     *     A two-digit year. The caller guarantees that this is a number between 0 and 99, inclusive.
     *
     * @return A year on the Gregorian calendar which corresponds to {@code twoDigitYear}.
     */
    int twoDigitYearToRealYear(int twoDigitYear);
}
