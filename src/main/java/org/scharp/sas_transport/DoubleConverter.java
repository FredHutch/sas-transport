///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2025 Fred Hutch Cancer Center
// Licensed under the MIT License - see LICENSE file for details
///////////////////////////////////////////////////////////////////////////////
package org.scharp.sas_transport;

/**
 * This class contains some utility methods for converting to and from the binary XPORT double format.
 *
 * <p>
 * According to 4.2.3 of the Java specification, the "double" type is a double-precision 64-bit format IEEE 754 values
 * and operations as specified in IEEE Standard for Binary Floating-Point Arithmetic, ANSI/IEEE Standard 754-1985.
 * </p>
 *
 * <p>
 * According to the INTRODUCTION of TS-140, the XPORT double is in "IBM-style double". This format is not as well
 * documented.
 * </p>
 *
 * <p>
 * TS-140 contains a reference implementation of conversion functions, but they are written in C for machines where the
 * maximum word size was 32-bits. Furthermore, the formatting whitespace is garbled in the document, which makes it more
 * difficult to read.
 * </p>
 *
 * <p>
 * There is a Python library called "xport" that is more readable but non-normative.
 * </p>
 */
abstract class DoubleConverter {

    // Prevent ArgumentUtil from being instantiated.
    private DoubleConverter() {
    }

    //private static final long XPORT_MIN_POSITIVE = 0x0010000000000000L;
    //private static final long XPORT_MAX_POSITIVE = 0x7FFFFFFFFFFFFFFFL;

    /**
     * An implementation of {@code xpt2ieee()} from TS-140 that uses the JVM double as the native double.
     *
     * @param data
     *     The binary data that was copied from the XPORT. This is an IBM-style double.
     *
     * @return Either a Double or a MissingValue that corresponds to the value represented by data.
     *
     * @throws IllegalArgumentException
     *     if the byte array is a malformed bit pattern for a double
     */
    static Object xportToDouble(byte[] data) {
        assert data.length == 8;

        // Convert the byte array into a long
        long xportLong = //
            (Byte.toUnsignedLong(data[0]) << 56) | //
                (Byte.toUnsignedLong(data[1]) << 48) | //
                (Byte.toUnsignedLong(data[2]) << 40) | //
                (Byte.toUnsignedLong(data[3]) << 32) | //
                (Byte.toUnsignedLong(data[4]) << 24) | //
                (Byte.toUnsignedLong(data[5]) << 16) | //
                (Byte.toUnsignedLong(data[6]) << 8) | //
                (Byte.toUnsignedLong(data[7]) << 0);
        if (xportLong == 0) {
            return 0D; // optimization for common case, return 0.
        }

        // XPORT: 1-bit sign, 7 exponent (excess of 64, base 16), 56 mantissa
        // JVM: 1-bit sign, 11 exponent (excess of 1023, base 2), 52 mantissa
        long sign = (xportLong & 0x8000000000000000L);
        long xportExponent = (xportLong >> 56) & 0x7F;
        long xportMantissa = (xportLong & 0x00FFFFFFFFFFFFFFL);

        if (xportMantissa == 0) {
            // We already know this is not 0, but it could be an encoding of a missing value.
            MissingValue missingValue = MissingValue.fromXportByteRepresentation(data[0]);
            if (missingValue != null) {
                return missingValue;
            }

            // This is neither 0 nor a documented missing value.
            // The reference implementation in TS_140 returns a double constructed from
            //   {0xFF, 0xFF, ~data[0], 0, 0, 0, 0, 0}
            // Which is a form of NaN that can be used to recreate the code without information loss
            // in the inverse function.
            // Such a NaN is unlikely to be interpreted correctly by the caller, so it's clearer
            // to throw an exception.
            // This pattern was copied from Python library.
            throw new IllegalArgumentException("mantissa is zero but value is not 0 or a MissingValue");
        }

        // The XPORT exponent is base 16, which is pow(2,3), but the Java exponent is base 2.
        // This means that each XPORT exponent is missing 3 bits of information from the
        // final number.
        // We take the highest order bits from the mantissa, which correspond to the
        // power of two that would be multiplied into to the final number.
        int shift;
        if (0 != (xportMantissa & 0x0080000000000000L)) {
            shift = 3; // divide mantissa by 8, add 3 to exponent
        } else if (0 != (xportMantissa & 0x0040000000000000L)) {
            shift = 2; // divide mantissa by 4, add 2 to exponent
        } else if (0 != (xportMantissa & 0x0020000000000000L)) {
            shift = 1; // divide mantissa by 2, add 1 to exponent
        } else {
            shift = 0; // divide mantissa by 1, add 0 to exponent (do nothing)
        }

        // Remove the parts from the mantissa that we will add to the exponent.
        long javaMantissa = xportMantissa >> shift;

        // The bit to the left of the binary point is always implied to be 1 in Java,
        // but not stored as such, so we clear it.
        // The shift above leaves the highest bit of the mantissa set for any
        // XPORT number that was set to maximize precision (if not, the top three
        // bits were 0, in which case the mantissa should have been shifted left
        // and the exponent incremented by one.
        // TODO: There was nothing in the reference code for this, but if the high bit
        // is zero, then we'd be changing the number by not clearing the 1.
        assert (javaMantissa & 0x0010000000000000L) != 0;
        javaMantissa &= ~(0x0010000000000000L);

        // XPORT exponent is excess 64.
        xportExponent -= 64;

        // Because we masked off the highest bit of the mantissa, we effectively
        // divided the number by two.  To account for this, we increment the exponent.
        xportExponent--;

        // XPORT exponent is base 16 and Java uses base 2.
        // So for every 1 the XPORT exponent goes up, the Java exponent goes up 4.
        // Therefore, to convert from XPORT exponent to Java exponent, we multiply by 4.
        long javaExponent = xportExponent * 4;

        // The Java exponent is defined to have a bias of 1023.
        // That is, the true exponent is what's in excess of 1023.
        javaExponent += 1023;

        // Finally, the power-of-two that we removed from the mantissa above
        // can now be added into the exponent.
        javaExponent += shift;

        // There should be no excess bits.
        assert (sign & ~(0x1 << 63)) == 0;
        assert (javaExponent & ~0x7FFL) == 0;
        assert (javaMantissa & (0xFFFL << 52)) == 0;

        // Put it all together
        long javaLong = sign | (javaExponent << 52) | javaMantissa;

        return Double.longBitsToDouble(javaLong);
    }

    private static byte[] longBitsToByteArray(long xportDoubleLongBits) {
        return new byte[] { //
            (byte) (0xFF & (xportDoubleLongBits >> 56)), //
            (byte) (0xFF & (xportDoubleLongBits >> 48)), //
            (byte) (0xFF & (xportDoubleLongBits >> 40)), //
            (byte) (0xFF & (xportDoubleLongBits >> 32)), //
            (byte) (0xFF & (xportDoubleLongBits >> 24)), //
            (byte) (0xFF & (xportDoubleLongBits >> 16)), //
            (byte) (0xFF & (xportDoubleLongBits >> 8)), //
            (byte) (0xFF & (xportDoubleLongBits >> 0)), //
        };
    }

    /**
     * An implementation of {@code ieee2xpt()} from TS-140 which uses the JVM double as the native double.
     *
     * @param javaDouble
     *     The number to convert.
     *
     * @return The serialized form of {@code javaDouble}, in XPORT format.
     */
    static byte[] doubleToXport(double javaDouble) {

        // JAVA : 1-bit sign, 11-bit exponent (excess of 1023, base 2), 52-bit mantissa
        // XPORT: 1-bit sign,  7-bit exponent (excess of 64, base 16),  56-bit mantissa

        // Since the sign bit remains the same, this code essentially solves the equation
        // 2**(J-EXP - 1023) * ((1 + J-MANT) * 2**-52) = 16**(X-EXP - 64) * (X-MANT) * 2**-56

        long javaLong = Double.doubleToRawLongBits(javaDouble);
        if (javaLong == 0) {
            return new byte[8]; // special case for 0: all zero bytes
        }

        long signBit = (javaLong & 0x8000000000000000L);
        long javaExponent = ((javaLong >> 52) & 0x7FF) - 1023;
        long javaMantissa = (javaLong & 0x00FFFFFFFFFFFFFL);

        if (javaExponent == 1024 && javaMantissa != 0) {
            // This is a NaN.
            // (All exponent bits set, with fractional part as non-zero).
            //
            // In ieee2xpt(ieee,xport) in TS-140, non-canonical NaN was translated into
            // missing values.  It looks like this was intended to be paired with some logic
            // in xpt2ieee, which encoded missing values as non-canonical NaN in a way that the exact
            // missing value could be reconstructed.  However, since Java has no special semantics to
            // preserve non-canonical NaN and since this library has a district structured form for
            // missing values, there's no reason for a caller to intentionally provide a non-canonical
            // NaN that encodes a missing value with the expectation that it would be written
            // as the missing value.
            //
            // Instead, we treat all NaN as an error.
            throw new IllegalArgumentException("XPORT format has no representation for NaN");
        }

        // If the Java exponent is <-260 or >248, then it cannot fit into the XPORT exponent,
        // as the XPORT exponent is only 7 bits: from (0 - 65)*4 to (127 - 65)*4.
        // (The -65 comes from the 64 bias + the 1 implicit).
        // (The *4 comes from the remapping from Java's base 2 exponent to XPORT's base 16 exponent).
        //
        // These special cases are not present the reference code in TS-140,
        // which returns 0x7FFFFFFFFFFFFFFFL and 0xFFFFFFFFFFFFFFFFL, but they exist in
        // TS-140_2, and it's the way SAS 9.1 handles overflow.
        if (javaExponent > 248) {
            throw new IllegalArgumentException("XPORT format cannot store numbers larger than pow(2, 248)");
            // TODO: in a non-strict mode
            // return longBitsToByteArray(signBit | XPORT_MAX_POSITIVE);
        }
        if (javaExponent < -260) {
            throw new IllegalArgumentException("XPORT format cannot store numbers smaller than pow(2, -260)");
            // TODO: in a non-strict mode
            //return longBitsToByteArray(signBit | XPORT_MIN_POSITIVE);
        }

        // Java exponents are for base 2   : mantissa * pow( 2, exponent)
        // XPORT exponents are for base 16 : mantissa * pow(16, exponent)

        // The Java format has an implicit 1 to the left of the mantissa.
        javaMantissa |= (1L << 52);

        // We save the low order 2 bits of the Java exponent since this would get lost
        // when we divide the exponent by 4 (right shift by 2) and we will have to shift
        // the fraction by the appropriate number of bits to keep the proper magnitude.

        // To convert from Java exponent to XPORT export, divide by 4 (right shift 2).
        //   pow(16, exponent / 4) == pow(2, exponent)
        long xportExponent = javaExponent >>> 2;

        // The above division by four truncates, which loses 2 bits of information.
        // For example: 64=pow(2,6)
        // becomes      pow(16,6/4) = pow(16,1) = 16
        //
        // Since the mantissa is a fraction that gets multiplied into pow(16, xportExponent),
        // we correct for this truncation by multiplying the java mantissa by pow(2, remainder).
        long remainder = javaExponent & 0x3; // javaExponent MOD 4
        long xportMantissa = javaMantissa << remainder; // mantissa * pow(2, remainder)

        // The XPORT mantissa is 56 bits whereas the Java mantissa is 52 bits.
        // To account for the extra four bits, we increment the exponent by 1.
        // This has the effect of shifting the 52-bit Java mantissa into the 56-bit
        // space, leaving the bottom four bits as 0.
        xportExponent++;

        // The XPORT exponents are given as the excess of 64.
        xportExponent += 64;

        // XPORT has 1-bit sign, 7-bits exponent, and 56-bits mantissa.
        //  We must shift the sign and exponent into their places.
        return longBitsToByteArray(signBit | (xportExponent << 56) | xportMantissa);
    }
}