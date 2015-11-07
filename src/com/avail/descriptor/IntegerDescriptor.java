/**
 * IntegerDescriptor.java
 * Copyright © 1993-2015, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.avail.descriptor;

import static com.avail.descriptor.AvailObject.*;
import static com.avail.descriptor.TypeDescriptor.Types.*;
import static java.lang.Math.*;
import static com.avail.descriptor.AbstractNumberDescriptor.Order.*;
import static com.avail.descriptor.IntegerDescriptor.IntegerSlots.*;
import static com.avail.descriptor.Mutability.*;
import java.math.BigInteger;
import java.util.*;
import com.avail.annotations.*;
import com.avail.exceptions.*;
import com.avail.exceptions.ArithmeticException;
import com.avail.serialization.SerializerOperation;
import com.avail.utility.json.JSONWriter;

/**
 * An Avail {@linkplain IntegerDescriptor integer} is represented by a little
 * endian series of {@code int} slots.  The slots are really treated as
 * unsigned, except for the final slot which is considered signed.  The high bit
 * of the final slot (i.e., its sign bit) is the sign bit of the entire object.
 *
 * <p>
 * Avail integers should always occupy the fewest number of slots to
 * unambiguously indicate the represented integer.  A zero integer is
 * represented by a single int slot containing a zero {@code int}.  Any {@code
 * int} can be converted to an Avail integer by using a single slot, and any
 * {@code long} can be represented with at most two slots.
 * </p>
 *
 * <p>
 * Since Avail will soon (2015.09.28) require 8-byte alignment, its
 * representation has been updated to use 64-bit longs.  Rather than rewrite the
 * painstakingly difficult operations to uee 64-bit longs directly, we fetch and
 * update 32-bit ints using {@link AvailObject#intSlot(IntegerSlotsEnum, int)},
 * a temporary compatibility mechanism to make this global refactoring
 * tractable.  However, we also introduce the {@link #unusedIntsOfLastLong}
 * field in the descriptor to maintain the invariant that the number of occupied
 * 32-bit int fields is always minimized.
 * </p>
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public class IntegerDescriptor
extends ExtendedIntegerDescriptor
{
	/**
	 * The layout of integer slots for my instances.
	 */
	public enum IntegerSlots
	implements IntegerSlotsEnum
	{
		/**
		 * <p>
		 * Avail integers should always occupy the fewest number of slots
		 * to unambiguously indicate the represented integer.  A zero integer is
		 * represented by a single slot containing a zero {@code int}.  Any
		 * {@code int} can be converted to an Avail integer by using a single
		 * slot, and any {@code long} can be represented with at most two slots.
		 * </p>
		 *
		 * <p>
		 * Thus, if the top slot is zero ({@code 0}), the second-from-top slot
		 * must have its upper bit set (fall in the range
		 * <nobr>{@code -0x80000000..-1}</nobr>), otherwise the last slot would
		 * be redundant.  Likewise, if the top slot is minus one ({code -1}),
		 * the second-from-top slot must have its upper bit clear (fall in the
		 * range <nobr>{@code 0..0x7FFFFFFF}</nobr>).
		 * </p>
		 */
		RAW_LONG_SLOTS_;
	}

	/**
	 * The number of ints of the last {@code long} that do not participate in
	 * the representation of the {@linkplain IntegerDescriptor integer}.
	 * Must be 0 or 1.
	 */
	private final byte unusedIntsOfLastLong;

	/**
	 * Answer the number of int slots in the passed integer object (it must not
	 * be an indirection).
	 *
	 * @param object The integer object.
	 * @return The number of occupied int slots in the object.
	 */
	public static int intCount (final A_Number object)
	{
		final IntegerDescriptor descriptor =
			(IntegerDescriptor)object.descriptor();
		return (object.integerSlotsCount() << 1)
			- descriptor.unusedIntsOfLastLong;
	}

	@Override
	public void printObjectOnAvoidingIndent (
		final AvailObject object,
		final StringBuilder aStream,
		final IdentityHashMap<A_BasicObject, Void> recursionMap,
		final int indent)
	{
		if (object.isLong())
		{
			// The *vast* majority of uses, extends a bit beyond 9 quintillion.
			aStream.append(object.extractLong());
		}
		else
		{
			// A slower approach that deals with huge numbers.  Collect groups
			// of 18 digits at a time by repeatedly dividing by a quintillion
			// and recording the moduli.  Avoid making the original object
			// immutable unnecessarily, as printing is performed by the Java
			// debugger, and printing a mutable integer can cause subsequent
			// initializing/updating writes to fail.
			A_Number residue = object.descriptor().isMutable()
				? newLike(mutable(), object, 0, 0).makeImmutable()
				: object;
			if (object.lessThan(zero()))
			{
				aStream.append('-');
				residue = zero().minusCanDestroy(residue, false);
			}
			// Make room for a little more than is needed.
			final long[] digitGroups = new long[intCount(object)];
			int digitGroupSubscript = 0;
			do
			{
				final A_Number quotient = residue.divideCanDestroy(
					quintillionInteger, false);
				final A_Number modulus = residue.minusCanDestroy(
					quotient.timesCanDestroy(quintillionInteger, false),
					false);
				assert modulus.isLong();
				digitGroups[digitGroupSubscript++] = modulus.extractLong();
				residue = quotient;
			} while (residue.greaterThan(zero()));
			// Write the first big digit (up to 18 actual digits).
			aStream.append(digitGroups[--digitGroupSubscript]);
			while (--digitGroupSubscript >= 0)
			{
				// We add a quintillion to force otherwise-leading zeroes to be
				// output, then skip the bogus leading 1.  It's still several
				// quintillion from overflowing a long.
				final String paddedString = Long.toString(
					quintillionLong + digitGroups[digitGroupSubscript]);
				assert paddedString.length() == 19;
				aStream.append(paddedString, 1, 19);
			}
		}
	}

	@Override @AvailMethod
	int o_RawSignedIntegerAt (final AvailObject object, final int subscript)
	{
		return object.intSlot(RAW_LONG_SLOTS_, subscript);
	}

	@Override @AvailMethod
	void o_RawSignedIntegerAtPut (
		final AvailObject object,
		final int subscript,
		final int value)
	{
		object.setIntSlot(RAW_LONG_SLOTS_, subscript, value);
	}

	@Override @AvailMethod
	boolean o_Equals (final AvailObject object, final A_BasicObject another)
	{
		return another.equalsInteger(object);
	}

	/**
	 * Compare two integers for equality.
	 */
	@Override @AvailMethod
	boolean o_EqualsInteger (
		final AvailObject object,
		final A_Number anAvailInteger)
	{
		final int slotsCount = intCount(object);
		if (slotsCount != intCount(anAvailInteger))
		{
			// Assume integers being compared are always normalized (trimmed).
			return false;
		}
		for (int i = 1; i <= slotsCount; i++)
		{
			final int a = object.intSlot(RAW_LONG_SLOTS_, i);
			final int b = anAvailInteger.rawSignedIntegerAt(i);
			if (a != b)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if this is an integer whose value equals the given int.
	 */
	@Override @AvailMethod
	boolean o_EqualsInt (
		final AvailObject object,
		final int theInt)
	{
		if (intCount(object) != 1)
		{
			// Assume it's normalized (trimmed).
			return false;
		}
		return object.intSlot(RAW_LONG_SLOTS_, 1) == theInt;
	}

	@Override @AvailMethod
	boolean o_IsInstanceOfKind (
		final AvailObject object,
		final A_Type aType)
	{
		if (aType.isSupertypeOfPrimitiveTypeEnum(NUMBER))
		{
			return true;
		}
		else if (!aType.isIntegerRangeType())
		{
			return false;
		}
		if (aType.upperInclusive())
		{
			if (!object.lessOrEqual(aType.upperBound()))
			{
				return false;
			}
		}
		else if (!object.lessThan(aType.upperBound()))
		{
			return false;
		}

		if (aType.lowerInclusive())
		{
			if (!aType.lowerBound().lessOrEqual(object))
			{
				return false;
			}
		}
		else if (!aType.lowerBound().lessThan(object))
		{
			return false;
		}
		return true;
	}

	@Override @AvailMethod
	Order o_NumericCompare (
		final AvailObject object,
		final A_Number another)
	{
		return another.numericCompareToInteger(object).reverse();
	}

	@Override @AvailMethod
	int o_Hash (final AvailObject object)
	{
		if (object.isUnsignedByte())
		{
			return hashOfUnsignedByte(object.extractUnsignedByte());
		}
		return computeHashOfIntegerObject(object);
	}

	@Override @AvailMethod
	boolean o_IsFinite (final AvailObject object)
	{
		return true;
	}

	@Override @AvailMethod
	A_Type o_Kind (final AvailObject object)
	{
		object.makeImmutable();
		return IntegerRangeTypeDescriptor.singleInteger(object);
	}

	@Override @AvailMethod
	A_Number o_DivideCanDestroy (
		final AvailObject object,
		final A_Number aNumber,
		final boolean canDestroy)
	{
		return aNumber.divideIntoIntegerCanDestroy(object, canDestroy);
	}

	@Override @AvailMethod
	A_Number o_MinusCanDestroy (
		final AvailObject object,
		final A_Number aNumber,
		final boolean canDestroy)
	{
		return aNumber.subtractFromIntegerCanDestroy(object, canDestroy);
	}

	@Override @AvailMethod
	A_Number o_PlusCanDestroy (
		final AvailObject object,
		final A_Number aNumber,
		final boolean canDestroy)
	{
		return aNumber.addToIntegerCanDestroy(object, canDestroy);
	}

	@Override @AvailMethod
	A_Number o_TimesCanDestroy (
		final AvailObject object,
		final A_Number aNumber,
		final boolean canDestroy)
	{
		return aNumber.multiplyByIntegerCanDestroy(object, canDestroy);
	}

	@Override @AvailMethod
	boolean o_IsNybble (final AvailObject object)
	{
		if (intCount(object) > 1)
		{
			return false;
		}
		final int value = object.extractInt();
		return (value & 15) == value;
	}

	@Override @AvailMethod
	boolean o_IsSignedByte (final AvailObject object)
	{
		if (intCount(object) > 1)
		{
			return false;
		}
		final int value = object.extractInt();
		return value == (byte) value;
	}

	@Override @AvailMethod
	boolean o_IsUnsignedByte (final AvailObject object)
	{
		if (intCount(object) > 1)
		{
			return false;
		}
		final int value = object.extractInt();
		return (value & 255) == value;
	}

	@Override @AvailMethod
	boolean o_IsSignedShort (final AvailObject object)
	{
		if (intCount(object) > 1)
		{
			return false;
		}
		final int value = object.extractInt();
		return value == (short) value;
	}

	@Override @AvailMethod
	boolean o_IsUnsignedShort (final AvailObject object)
	{
		if (intCount(object) > 1)
		{
			return false;
		}
		final int value = object.extractInt();
		return (value & 65535) == value;
	}

	@Override @AvailMethod
	boolean o_IsInt (final AvailObject object)
	{
		return intCount(object) == 1;
	}

	@Override @AvailMethod
	boolean o_IsLong (final AvailObject object)
	{
		return intCount(object) <= 2;
	}

	@Override @AvailMethod
	byte o_ExtractNybble (final AvailObject object)
	{
		assert intCount(object) == 1;
		final int value = object.rawSignedIntegerAt(1);
		assert value == (value & 15) : "Value is out of range for a nybble";
		return (byte)value;
	}

	@Override @AvailMethod
	byte o_ExtractSignedByte (final AvailObject object)
	{
		assert intCount(object) == 1;
		final int value = object.rawSignedIntegerAt(1);
		assert value == (byte) value : "Value is out of range for a byte";
		return (byte) value;
	}

	@Override @AvailMethod
	short o_ExtractUnsignedByte (final AvailObject object)
	{
		assert intCount(object) == 1;
		final int value = object.rawSignedIntegerAt(1);
		assert value == (value & 255) : "Value is out of range for a byte";
		return (short)value;
	}

	@Override @AvailMethod
	short o_ExtractSignedShort (final AvailObject object)
	{
		assert intCount(object) == 1;
		final int value = object.rawSignedIntegerAt(1);
		assert value == (short) value : "Value is out of range for a short";
		return (short) value;
	}

	@Override @AvailMethod
	int o_ExtractUnsignedShort (final AvailObject object)
	{
		assert intCount(object) == 1;
		final int value = object.rawSignedIntegerAt(1);
		assert value == (value & 65535) : "Value is out of range for a short";
		return value;
	}

	@Override @AvailMethod
	int o_ExtractInt (final AvailObject object)
	{
		assert intCount(object) == 1 : "Integer value out of bounds";
		return object.rawSignedIntegerAt(1);
	}

	@Override @AvailMethod
	long o_ExtractLong (final AvailObject object)
	{
		assert
			intCount(object) >= 1 && intCount(object) <= 2
			: "Integer value out of bounds";

		if (intCount(object) == 1)
		{
			return object.rawSignedIntegerAt(1);
		}

		long value = object.rawSignedIntegerAt(1) & 0xffffffffL;
		value |= ((long) object.rawSignedIntegerAt(2)) << 32L;
		return value;
	}

	@Override @AvailMethod
	float o_ExtractFloat (final AvailObject object)
	{
		return (float) extractDoubleScaled(object, 0);
	}

	@Override @AvailMethod
	double o_ExtractDouble (final AvailObject object)
	{
		return extractDoubleScaled(object, 0);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Manually constructed accessor method.  Access the quad-byte using the
	 * native byte-ordering, but using little endian between quad-bytes (i.e.,
	 * least significant quad comes first).
	 * </p>
	 */
	@Override @AvailMethod
	long o_RawUnsignedIntegerAt (
		final AvailObject object,
		final int subscript)
	{
		final int signedInt = object.intSlot(RAW_LONG_SLOTS_, subscript);
		return signedInt & 0xFFFFFFFFL;
	}

	/**
	 * Manually constructed accessor method.  Overwrite the quad-byte using the
	 * native byte-ordering, but using little endian between quad-bytes (i.e.,
	 * least significant quad comes first).
	 */
	@Override @AvailMethod
	void o_RawUnsignedIntegerAtPut (
		final AvailObject object,
		final int subscript,
		final int value)
	{
		object.setIntSlot(RAW_LONG_SLOTS_, subscript, value);
	}

	@Override @AvailMethod
	void o_TrimExcessInts (final AvailObject object)
	{
		// Remove any redundant ints from my end.  Since I'm stored in Little
		// Endian representation, I can simply be truncated with no need to
		// shift data around.
		assert isMutable();
		int size = intCount(object);
		if (size > 1)
		{
			if (object.intSlot(RAW_LONG_SLOTS_, size) >= 0)
			{
				while (size > 1
						&& object.intSlot(RAW_LONG_SLOTS_, size) == 0
						&& object.intSlot(RAW_LONG_SLOTS_, size - 1) >= 0)
				{
					size--;
					if ((size & 1) == 0)
					{
						// Remove an entire long.
						object.truncateWithFillerForNewIntegerSlotsCount(
							(size + 1) >> 1);
					}
					else
					{
						// Safety: Zero the bytes if the size is now odd.
						object.setIntSlot(RAW_LONG_SLOTS_, size + 1, 0);
					}
				}
			}
			else
			{
				while (size > 1
						&& object.intSlot(RAW_LONG_SLOTS_, size) == -1
						&& object.intSlot(RAW_LONG_SLOTS_, size - 1) < 0)
				{
					size--;
					if ((size & 1) == 0)
					{
						// Remove an entire long.
						object.truncateWithFillerForNewIntegerSlotsCount(
							(size + 1) >> 1);
					}
					else
					{
						// Safety: Zero the bytes if the size is now odd.
						object.setIntSlot(RAW_LONG_SLOTS_, size + 1, 0);
					}
				}
			}
			object.descriptor = descriptorFor(MUTABLE, size);
		}
	}

	@Override @AvailMethod
	A_Number o_AddToInfinityCanDestroy (
		final AvailObject object,
		final Sign sign,
		final boolean canDestroy)
	{
		return sign == Sign.POSITIVE
			? InfinityDescriptor.positiveInfinity()
			: InfinityDescriptor.negativeInfinity();
	}

	/**
	 * Choose the most spacious mutable {@linkplain AvailObject argument}.
	 *
	 * @param object
	 *        An {@linkplain IntegerDescriptor integer}.
	 * @param another
	 *        An integer.
	 * @return One of the arguments, or {@code null} if neither argument was
	 *         suitable.
	 */
	private @Nullable AvailObject largerMutableOf (
		final AvailObject object,
		final AvailObject another)
	{
		final int objectSize = intCount(object);
		final int anIntegerSize = intCount(another);
		AvailObject output = null;
		if (objectSize == anIntegerSize)
		{
			output =
				isMutable()
					? object
					: another.descriptor().isMutable()
						? another
						: null;
		}
		else if (objectSize > anIntegerSize)
		{
			output = isMutable() ? object : null;
		}
		else
		{
			output = another.descriptor().isMutable() ? another : null;
		}
		return output;
	}

	@Override @AvailMethod
	A_Number o_AddToIntegerCanDestroy (
		final AvailObject object,
		final A_Number anInteger,
		final boolean canDestroy)
	{
		// This routine would be much quicker with access to machine carry
		// flags, but Java doesn't let us actually go down to the metal (nor do
		// C and C++). Our best recourse without reverting to assembly language
		// is to use 64-bit.
		final int objectSize = intCount(object);
		final int anIntegerSize = intCount(anInteger);
		AvailObject output = canDestroy
			? largerMutableOf(object, (AvailObject)anInteger)
			: null;
		if (objectSize == 1 && anIntegerSize == 1)
		{
			// See if the (signed) sum will fit in 32 bits, the most common case
			// by far.
			final long sum = object.extractLong() + anInteger.extractLong();
			if (sum == (int) sum)
			{
				// Yes, it fits.  Clobber one of the inputs, or create a new
				// object if they were both immutable...
				if (output == null)
				{
					output = createUninitialized(1);
				}
				assert intCount(output) == 1;
				output.rawSignedIntegerAtPut(1, (int)sum);
				return output;
			}
			// Doesn't fit in 32 bits; use two 32-bit words.
			return fromLong(sum);
		}
		// Set estimatedSize to the max of the input sizes. There will only
		// rarely be an overflow and at most by one cell. Underflows should also
		// be pretty rare, and they're handled by output.trimExcessInts().
		if (output == null)
		{
			output = createUninitialized(max(objectSize, anIntegerSize));
		}
		final int outputSize = intCount(output);
		final long extendedObject =
			object.rawSignedIntegerAt(objectSize) >> 31 & 0xFFFFFFFFL;
		final long extendedAnInteger =
			anInteger.rawSignedIntegerAt(anIntegerSize) >> 31 & 0xFFFFFFFFL;
		long partial = 0;
		int lastInt = 0;
		// The object is always big enough to store the max of the number of
		// quads from each input, so after the loop we can check partial and the
		// upper bit of the result to see if another quad needs to be appended.
		for (int i = 1; i <= outputSize; i++)
		{
			partial += i > objectSize
				? extendedObject
				: object.rawUnsignedIntegerAt(i);
			partial += i > anIntegerSize
				? extendedAnInteger
				: anInteger.rawUnsignedIntegerAt(i);
			lastInt = (int) partial;
			output.rawSignedIntegerAtPut(i, lastInt);
			partial >>>= 32;
		}
		partial += extendedObject + extendedAnInteger;
		if (lastInt >> 31 != (int) partial)
		{
			// Top bit of last word no longer agrees with sign of result. Extend
			// it.
			final AvailObject newOutput = createUninitialized(outputSize + 1);
			for (int i = 1; i <= outputSize; i++)
			{
				newOutput.setIntSlot(
					RAW_LONG_SLOTS_,
					i,
					output.intSlot(RAW_LONG_SLOTS_, i));
			}
			newOutput.rawSignedIntegerAtPut(outputSize + 1, (int) partial);
			// No need to truncate it in this case.
			return newOutput;
		}
		output.trimExcessInts();
		return output;
	}

	@Override
	A_Number o_AddToDoubleCanDestroy (
		final AvailObject object,
		final A_Number doubleObject,
		final boolean canDestroy)
	{
		final double d = DoubleDescriptor.addDoubleAndIntegerCanDestroy(
			doubleObject.extractDouble(),
			object,
			canDestroy);
		return DoubleDescriptor.objectFromDoubleRecycling(
			d,
			doubleObject,
			canDestroy);
	}

	@Override
	A_Number o_AddToFloatCanDestroy (
		final AvailObject object,
		final A_Number floatObject,
		final boolean canDestroy)
	{
		final double d = DoubleDescriptor.addDoubleAndIntegerCanDestroy(
			floatObject.extractDouble(),
			object,
			canDestroy);
		return FloatDescriptor.objectFromFloatRecycling(
			(float)d,
			floatObject,
			canDestroy);
	}

	@Override @AvailMethod
	A_Number o_DivideIntoInfinityCanDestroy (
		final AvailObject object,
		final Sign sign,
		final boolean canDestroy)
	{
		if (object.equals(zero()))
		{
			throw new ArithmeticException(
				AvailErrorCode.E_CANNOT_DIVIDE_BY_ZERO);
		}
		return object.greaterThan(zero()) ^ (sign == Sign.POSITIVE)
			? InfinityDescriptor.negativeInfinity()
			: InfinityDescriptor.positiveInfinity();
	}

	/**
	 * Choose a mutable {@linkplain AvailObject argument}.
	 *
	 * @param object
	 *        An {@link IntegerDescriptor integer}.
	 * @param another
	 *        An integer.
	 * @return One of the arguments, or {@code null} if neither argument is
	 *         mutable.
	 */
	private @Nullable A_Number mutableOf (
		final A_Number object,
		final A_Number another)
	{
		@Nullable A_Number output = null;
		if (isMutable())
		{
			output = object;
		}
		else if (another.descriptor().isMutable())
		{
			output = another;
		}
		return output;
	}

	@Override @AvailMethod
	A_Number o_DivideIntoIntegerCanDestroy (
		final AvailObject object,
		final A_Number anInteger,
		final boolean canDestroy)
	{
		// Compute anInteger / object. Round towards negative infinity.
		if (object.equals(zero()))
		{
			throw new ArithmeticException(
				AvailErrorCode.E_CANNOT_DIVIDE_BY_ZERO);
		}
		if (anInteger.equals(zero()))
		{
			return anInteger;
		}
		if (object.lessThan(zero()))
		{
			// a/o for o<0:  use (-a/-o)
			return object.subtractFromIntegerCanDestroy(zero(), canDestroy)
				.divideIntoIntegerCanDestroy(
					anInteger.subtractFromIntegerCanDestroy(zero(), canDestroy),
					canDestroy);
		}
		if (anInteger.lessThan(zero()))
		{
			// a/o for a<0, o>0:  use -1-(-1-a)/o
			// e.g., -9/5  = -1-(-1+9)/5  = -1-8/5 = -2
			// e.g., -10/5 = -1-(-1+10)/5 = -1-9/5 = -2
			// e.g., -11/5 = -1-(-1+11)/5 = -1-10/5 = -3
			final A_Number minusOneMinusA =
				negativeOne().minusCanDestroy(anInteger, false);
			final A_Number quotient =
				minusOneMinusA.divideCanDestroy(object, true);
			return negativeOne().minusCanDestroy(quotient, true);
		}
		if (object.isInt() && anInteger.isInt())
		{
			final long quotient = ((long)anInteger.extractInt())
				/ ((long)object.extractInt());
			// NOTE:  This test can ONLY fail for -2^31/-1 (which is a *long*).
			if (quotient == (int)quotient)
			{
				// Yes, it fits.  Clobber one of the inputs, or create a new
				// int-sized object if they were both immutable...
				A_Number output = canDestroy
					? mutableOf(object, anInteger)
					: null;
				if (output == null)
				{
					output = createUninitialized(1);
				}
				assert intCount(output) == 1;
				output.rawSignedIntegerAtPut(1, (int)quotient);
				return output;
			}
			// Doesn't fit.  Worst case: -2^31 / -1 = 2^31, which easily fits in
			// 64 bits, even with the sign.
			return fromLong(quotient);
		}
		// Both integers are now positive, and the divisor is not zero. That
		// simplifies things quite a bit. Ok, we need to keep estimating the
		// quotient and reverse multiplying until our remainder is in
		// [0..divisor - 1]. Each pass through the loop we estimate
		// partialQuotient = remainder / object. This should be accurate to
		// about 50 bits. We then set remainder = remainder - (partialQuotient *
		// object). If remainder goes negative (due to overestimation), we
		// toggle a flag (saying whether it represents a positive or negative
		// quantity), then negate it to make it a positive integer. Also, we
		// either add partialQuotient to the fullQuotient or subtract it,
		// depending on the setting of this flag. At the end, we adjust the
		// remainder (in case it represents a negative value) and quotient
		// (accordingly). Note that we're using double precision floating point
		// math to do the estimation. Not all processors will do this well, so a
		// 32-bit fixed-point division estimation might be a more 'portable' way
		// to do this. The rest of the algorithm would stay the same, however.

		A_Number remainder = anInteger;
		boolean remainderIsReallyNegative = false;
		A_Number fullQuotient = zero();
		A_Number partialQuotient = zero();

		final int divisorSlotsCount = intCount(object);
		// Power of two by which to scale doubleDivisor to get actual value
		final long divisorScale = (divisorSlotsCount - 1) * 32L;
		final long divisorHigh = object.rawUnsignedIntegerAt(divisorSlotsCount);
		final long divisorMedium = divisorSlotsCount > 1
			? object.rawUnsignedIntegerAt(divisorSlotsCount - 1)
			: 0;
		final long divisorLow = divisorSlotsCount > 2
			? object.rawUnsignedIntegerAt(divisorSlotsCount - 2)
			: 0;
		final double doubleDivisor =
			scalb((double)divisorLow, -64) +
			scalb((double)divisorMedium, -32) +
			divisorHigh;

		while (remainder.greaterOrEqual(object))
		{
			// Estimate partialQuotient = remainder / object, using the
			// uppermost 3 words of each. Allow a slightly negative remainder
			// due to rounding.  Compensate at the bottom of the loop.
			final int dividendSlotsCount = intCount(remainder);
		   // Power of two by which to scale doubleDividend to get actual value
			final long dividendScale = (dividendSlotsCount - 1) * 32L;
			final long dividendHigh =
				remainder.rawUnsignedIntegerAt(dividendSlotsCount);
			final long dividendMedium = dividendSlotsCount > 1
				? remainder.rawUnsignedIntegerAt(dividendSlotsCount - 1)
				: 0;
			final long dividendLow = dividendSlotsCount > 2
				? remainder.rawUnsignedIntegerAt(dividendSlotsCount - 2)
				: 0;
			final double doubleDividend =
				scalb((double)dividendLow, -64) +
				scalb((double)dividendMedium, -32) +
				dividendHigh;

			// Divide the doubles to estimate remainder / object. The estimate
			// should be very good since we extracted 96 bits of data, only
			// about 33 of which could be leading zero bits. The mantissas are
			// basically filled with as many bits as they can hold, so the
			// division should produce about as many bits of useful output.
			// After suitable truncation and conversion to an integer, this
			// quotient should produce about 50 bits of the final result.
			//
			// It's not obvious that it always converges, but here's my
			// reasoning. The first pass produces 50 accurate bits of quotient
			// (or completes by producing a small enough remainder). The
			// remainder from this pass is used as the dividend in the next
			// pass, and this is always at least 50 bits smaller than the
			// previous dividend. Eventually this leads to a remainder within a
			// factor of two of the dividend.
			//
			// Now say we have a very large divisor and that the first 50+ bits
			// of the divisor and (remaining) dividend agree exactly. In that
			// case the estimated division will still make progress, because it
			// will produce exactly 1.0d as the quotient, which causes the
			// remainder to decrease to <= the divisor (because we already got
			// it to within a factor of two above), thus terminating the loop.
			// Note that the quotient can't be <1.0d (when quotientScale is also
			// zero), since that can only happen when the remainder is truly
			// less than the divisor, which would have caused an exit after the
			// previous iteration. If it's >1.0d (or =1.0d) then we are making
			// progress each step, eliminating 50 actual bits, except on the
			// final iteration which must converge in at most one more step.
			// Note that we could have used just a few bits in the floating
			// point division and still always converged in time proportional to
			// the difference in bit lengths divided by the number of bits of
			// accuracy in the floating point quotient.
			final double doubleQuotient = doubleDividend / doubleDivisor;
			final long quotientScale = dividendScale - divisorScale;
			assert quotientScale >= 0L;

			// Include room for sign bit plus safety margin.
			partialQuotient = createUninitialized(
				(int)((quotientScale + 2 >> 5) + 1));

			final long bitShift = quotientScale
				- (((long) intCount(partialQuotient) - 1) << 5L);
			assert -100L < bitShift && bitShift < 100L;
			double scaledDoubleQuotient = scalb(doubleQuotient, (int)bitShift);
			for (int i = intCount(partialQuotient); i >= 1; --i)
			{
				long word = 0;
				if (scaledDoubleQuotient != 0.0d)
				{
					word = (long)scaledDoubleQuotient;
					assert word >= 0 && word <= 0xFFFFFFFFL;
					scaledDoubleQuotient -= word;
					scaledDoubleQuotient = scalb(scaledDoubleQuotient, 32);
				}
				partialQuotient.rawSignedIntegerAtPut(i, (int)word);
			}
			partialQuotient.trimExcessInts();

			if (remainderIsReallyNegative)
			{
				fullQuotient =
					partialQuotient.subtractFromIntegerCanDestroy(
						fullQuotient, false);
			}
			else
			{
				fullQuotient =
					partialQuotient.addToIntegerCanDestroy(fullQuotient, false);
			}
			remainder = remainder.noFailMinusCanDestroy(
				partialQuotient.noFailTimesCanDestroy(object, false),
				false);
			if (remainder.lessThan(zero()))
			{
				// Oops, we overestimated the partial quotient by a little bit.
				// I would guess this never gets much more than one part in
				// 2^50. Because of the way we've done the math, when the
				// problem is small enough the estimated division is always
				// correct. So we don't need to worry about this case near the
				// end, just when there are plenty of digits of accuracy
				// produced by the estimated division. So instead of correcting
				// the partial quotient and remainder, we simply toggle a flag
				// indicating whether the remainder we're actually dealing with
				// is positive or negative, and then negate the remainder to
				// keep it positive.
				remainder = remainder.subtractFromIntegerCanDestroy(
					zero(),
					false);
				remainderIsReallyNegative = !remainderIsReallyNegative;
			}
		}
		// At this point, we really have a remainder in [-object+1..object-1].
		// If the remainder is less than zero, adjust it to make it positive.
		// This just involves adding the divisor to it while decrementing the
		// quotient because the divisor doesn't quite go into the dividend as
		// many times as we thought.
		if (remainderIsReallyNegative && remainder.greaterThan(zero()))
		{
			// We fix the sign of remainder then add object all in one fell
			// swoop.
			remainder = remainder.subtractFromIntegerCanDestroy(object, false);
			fullQuotient =
				one().subtractFromIntegerCanDestroy(fullQuotient, false);
		}
		assert remainder.greaterOrEqual(zero()) && remainder.lessThan(object);
		return fullQuotient;
	}

	@Override
	public A_Number o_DivideIntoDoubleCanDestroy (
		final AvailObject object,
		final A_Number doubleObject,
		final boolean canDestroy)
	{
		// This one is tricky.  The integer might be bigger than the maximum
		// double, but the product with a very small double may produce a value
		// that is still in range.  Avoid the overflow in that case by working
		// with a scaled down version of the integer: target a "significand"
		// below about 2^96.
		final int scale = Math.max(intCount(object) - 4, 0) * 32;
		final double scaledIntAsDouble = extractDoubleScaled(object, scale);
		assert !Double.isInfinite(scaledIntAsDouble);
		final double scaledQuotient =
			doubleObject.extractDouble() / scaledIntAsDouble;
		final double quotient = Math.scalb(scaledQuotient, scale);
		return DoubleDescriptor.objectFromDoubleRecycling(
			quotient,
			doubleObject,
			canDestroy);
	}

	@Override
	public A_Number o_DivideIntoFloatCanDestroy (
		final AvailObject object,
		final A_Number floatObject,
		final boolean canDestroy)
	{
		// This one is tricky.  The integer might be bigger than the maximum
		// double, but the product with a very small double may produce a value
		// that is still in range of a float.  Actually, I doubt this is
		// possible, but it's easier to just make it match the double case.
		// Avoid the overflow by working with a scaled down version of the
		// integer: target a "significand" below about 2^96.
		final int scale = Math.max(intCount(object) - 4, 0) * 32;
		final double scaledIntAsDouble = extractDoubleScaled(object, scale);
		assert !Double.isInfinite(scaledIntAsDouble);
		final double scaledQuotient =
			floatObject.extractDouble() / scaledIntAsDouble;
		final double quotient = Math.scalb(scaledQuotient, scale);
		return FloatDescriptor.objectFromFloatRecycling(
			(float)quotient,
			floatObject,
			canDestroy);
	}

	@Override @AvailMethod
	A_Number o_MultiplyByInfinityCanDestroy (
		final AvailObject object,
		final Sign sign,
		final boolean canDestroy)
	{
		if (object.equals(zero()))
		{
			throw new ArithmeticException(
				AvailErrorCode.E_CANNOT_MULTIPLY_ZERO_AND_INFINITY);
		}
		return object.greaterThan(zero()) ^ (sign == Sign.POSITIVE)
			? InfinityDescriptor.negativeInfinity()
			: InfinityDescriptor.positiveInfinity();
	}

	@Override @AvailMethod
	A_Number o_MultiplyByIntegerCanDestroy (
		final AvailObject object,
		final A_Number anInteger,
		final boolean canDestroy)
	{
		A_Number output = null;
		if (object.isInt() && anInteger.isInt())
		{
			// See if the (signed) product will fit in 32 bits, the most common
			// case by far.
			final long prod = ((long)object.extractInt())
				* ((long)anInteger.extractInt());
			if (prod == (int)prod)
			{
				// Yes, it fits.  Clobber one of the inputs, or create a new
				// int-sized object if they were both immutable...
				output = canDestroy ? mutableOf(object, anInteger) : null;
				if (output == null)
				{
					output = createUninitialized(1);
				}
				assert intCount(output) == 1;
				output.rawSignedIntegerAtPut(1, (int)prod);
				return output;
			}
			// Doesn't fit.  Worst case: -2^31 * -2^31 = +2^62, which fits in 64
			// bits, even with the sign.
			return fromLong(prod);
		}
		final int size1 = intCount(object);
		final int size2 = intCount(anInteger);
		// The following is a safe upper bound.  See below.
		final int targetSize = size1 + size2;
		output = createUninitialized(targetSize);
		final long extension1 =
			(object.rawSignedIntegerAt(size1) >> 31) & 0xFFFFFFFFL;
		final long extension2 =
			(anInteger.rawSignedIntegerAt(size2) >> 31) & 0xFFFFFFFFL;

		// We can't recycle storage quite as easily here as we did for addition
		// and subtraction, because the intermediate sum would be clobbering one
		// of the multiplicands that we (may) need to scan again.  So always
		// allocate the new object.  The product will always fit in N+M cells if
		// the multiplicands fit in sizes N and M cells.  For proof, consider
		// the worst case (using bytes for the example).  In hex,
		// -80 * -80 = +4000, which fits.  Also, 7F*7F = 3F01, and
		// 7F*-80 = -3F80.  So no additional padding is necessary.  The scheme
		// we will use is to compute each word of the result, low to high, using
		// a carry of two words.  All quantities are treated as unsigned, but
		// the multiplicands are sign-extended as needed.  Multiplying two
		// one-word multiplicands yields a two word result, so we need to use
		// three words to properly hold the carry (it would take more than four
		// billion words to overflow this, and only one billion words are
		// addressable on a 32-bit machine).  The three-word intermediate value
		// is handled as two two-word accumulators, A and B.  B is considered
		// shifted by a word (to the left).  The high word of A is added to the
		// low word of B (with carry to the high word of B), and then the high
		// word of A is cleared.  This "shifts the load" to B for holding big
		// numbers without affecting their sum.  When a new two-word value needs
		// to be added in, this trick is employed, followed by directly adding
		// the two-word value to A, as long as we can ensure the *addition*
		// won't overflow, which is the case if the two-word value is an
		// unsigned product of two one-word values.  Since FF*FF=FE01, we can
		// safely add this to 00FF (getting FF00) without overflow.  Pretty
		// slick, huh?

		long low = 0;
		long high = 0;
		for (int i = 1; i <= targetSize; i++)
		{
			for (int k = 1, m = i; k <= i; k++, m--)
			{
				final long multiplicand1 = k > size1
					? extension1
					: object.rawUnsignedIntegerAt(k);
				final long multiplicand2 = m > size2
					? extension2
					: anInteger.rawUnsignedIntegerAt(m);
				low += multiplicand1 * multiplicand2;
				// Add upper of low to high.
				high += low >>> 32;
				// Subtract the same amount from low (clear upper word).
				low &= 0xFFFFFFFFL;
			}
			output.rawSignedIntegerAtPut(i, (int)low);
			low = high & 0xFFFFFFFFL;
			high >>>= 32;
		}
		// We can safely ignore any remaining bits from the partial products.
		output.trimExcessInts();
		return output;
	}

	@Override
	public A_Number o_MultiplyByDoubleCanDestroy (
		final AvailObject object,
		final A_Number doubleObject,
		final boolean canDestroy)
	{
		// This one is tricky.  The integer might be bigger than the maximum
		// double, but the product with a very small double may produce a value
		// that is still in range.  Avoid the overflow in that case by working
		// with a scaled down version of the integer: target a "significand"
		// below about 2^96.
		final int scale = Math.max(intCount(object) - 4, 0) * 32;
		final double scaledIntAsDouble = extractDoubleScaled(object, scale);
		assert !Double.isInfinite(scaledIntAsDouble);
		final double scaledProduct =
			doubleObject.extractDouble() * scaledIntAsDouble;
		final double product = Math.scalb(scaledProduct, scale);
		return DoubleDescriptor.objectFromDoubleRecycling(
			product,
			doubleObject,
			canDestroy);
	}

	@Override
	public A_Number o_MultiplyByFloatCanDestroy (
		final AvailObject object,
		final A_Number floatObject,
		final boolean canDestroy)
	{
		// This one is tricky.  The integer might be bigger than the maximum
		// double, but the product with a very small double may produce a value
		// that is still in range of a float.  Actually, I doubt this is
		// possible, but it's easier to just make it match the double case.
		// Avoid the overflow by working with a scaled down version of the
		// integer: target a "significand" below about 2^96.
		final int scale = Math.max(intCount(object) - 4, 0) * 32;
		final double scaledIntAsDouble = extractDoubleScaled(object, scale);
		assert !Double.isInfinite(scaledIntAsDouble);
		final double scaledProduct =
			floatObject.extractDouble() * scaledIntAsDouble;
		final double product = Math.scalb(scaledProduct, scale);
		return FloatDescriptor.objectFromFloatRecycling(
			(float)product,
			floatObject,
			canDestroy);
	}

	@Override @AvailMethod
	A_Number o_SubtractFromInfinityCanDestroy (
		final AvailObject object,
		final Sign sign,
		final boolean canDestroy)
	{
		return sign == Sign.POSITIVE
			? InfinityDescriptor.positiveInfinity()
			: InfinityDescriptor.negativeInfinity();
	}

	@Override @AvailMethod
	A_Number o_SubtractFromIntegerCanDestroy (
		final AvailObject object,
		final A_Number anInteger,
		final boolean canDestroy)
	{
		// This routine would be much quicker with access to machine carry
		// flags, but Java doesn't let us actually go down to the metal (nor do
		// C and C++). Our best recourse without reverting to assembly language
		// is to use 64-bit arithmetic.
		final int objectSize = intCount(object);
		final int anIntegerSize = intCount(anInteger);
		AvailObject output = canDestroy
			? largerMutableOf(object, (AvailObject)anInteger)
			: null;
		if (objectSize == 1 && anIntegerSize == 1)
		{
			// See if the (signed) difference will fit in 32 bits, the most
			// common case by far.
			final long diff = anInteger.extractLong() - object.extractLong();
			if (diff == (int) diff)
			{
				// Yes, it fits. Clobber one of the inputs, or create a new
				// object if they were both immutable...
				if (output == null)
				{
					output = createUninitialized(1);
				}
				assert intCount(output) == 1;
				output.rawSignedIntegerAtPut(1, (int)diff);
				return output;
			}
			// Doesn't fit in 32 bits; use two 32-bit words.
			output = createUninitialized(2);
			output.rawSignedIntegerAtPut(1, (int)diff);
			output.rawSignedIntegerAtPut(2, (int)(diff>>32));
			return output;
		}
		// Set estimatedSize to the max of the input sizes. There will only
		// rarely be an overflow and at most by one cell. Underflows should also
		// be pretty rare, and they're handled by output.trimExcessInts().
		if (output == null)
		{
			output = createUninitialized(max(objectSize, anIntegerSize));
		}
		final int outputSize = intCount(output);
		final long extendedObject =
			(object.rawSignedIntegerAt(objectSize) >> 31) & 0xFFFFFFFFL;
		final long extendedAnInteger =
			(anInteger.rawSignedIntegerAt(anIntegerSize) >> 31) & 0xFFFFFFFFL;
		long partial = 1;
		int lastInt = 0;
		// The object is always big enough to store the max of the number of
		// quads from each input, so after the loop we can check partial and the
		// upper bit of the result to see if another quad needs to be appended.
		for (int i = 1; i <= outputSize; i++)
		{
			partial += i > anIntegerSize
				? extendedAnInteger
				: anInteger.rawUnsignedIntegerAt(i);
			partial += (i > objectSize
				? extendedObject
				: object.rawUnsignedIntegerAt(i)) ^ 0xFFFFFFFFL;
			lastInt = (int) partial;
			output.rawSignedIntegerAtPut(i, lastInt);
			partial >>>= 32;
		}
		partial += extendedAnInteger + (extendedObject ^ 0xFFFFFFFFL);
		if (lastInt >> 31 != (int) partial)
		{
			// Top bit of last word no longer agrees with sign of result.
			// Extend it.
			final AvailObject newOutput = createUninitialized(outputSize + 1);
			for (int i = 1; i <= outputSize; i++)
			{
				newOutput.rawSignedIntegerAtPut(
					i, output.rawSignedIntegerAt(i));
			}
			newOutput.rawSignedIntegerAtPut(outputSize + 1, (int) partial);
			// No need to truncate it in this case.
			return newOutput;
		}
		output.trimExcessInts();
		return output;
	}

	@Override
	public A_Number o_SubtractFromDoubleCanDestroy (
		final AvailObject object,
		final A_Number doubleObject,
		final boolean canDestroy)
	{
		// Compute the negative (i.e., int-double)
		final double d = DoubleDescriptor.addDoubleAndIntegerCanDestroy(
			-doubleObject.extractDouble(),
			object,
			canDestroy);
		// Negate it to produce (double-int).
		return DoubleDescriptor.objectFromDoubleRecycling(
			-d,
			doubleObject,
			canDestroy);
	}

	@Override
	public A_Number o_SubtractFromFloatCanDestroy (
		final AvailObject object,
		final A_Number floatObject,
		final boolean canDestroy)
	{
		// Compute the negative (i.e., int-float)
		final double d = DoubleDescriptor.addDoubleAndIntegerCanDestroy(
			-floatObject.extractDouble(),
			object,
			canDestroy);
		// Negate it to produce (float-int).
		return FloatDescriptor.objectFromFloatRecycling(
			(float)-d,
			floatObject,
			canDestroy);
	}

	@Override @AvailMethod
	Order o_NumericCompareToInteger (
		final AvailObject object,
		final A_Number anInteger)
	{
		final int size1 = intCount(object);
		final int size2 = intCount(anInteger);
		final int high1 = object.intSlot(RAW_LONG_SLOTS_, size1);
		final int high2 = anInteger.rawSignedIntegerAt(size2);
		final int composite1 = high1 >= 0 ? size1 : -size1;
		final int composite2 = high2 >= 0 ? size2 : -size2;
		if (composite1 != composite2)
		{
			return composite1 < composite2 ? LESS : MORE;
		}
		// The sizes and signs are the same.
		assert size1 == size2;
		assert high1 >= 0 == high2 >= 0;
		if (high1 != high2)
		{
			return high1 < high2 ? LESS : MORE;
		}
		for (int i = size1 - 1; i >= 1; i--)
		{
			final int a = object.intSlot(RAW_LONG_SLOTS_, i);
			final int b = anInteger.rawSignedIntegerAt(i);
			if (a != b)
			{
				return (a & 0xFFFFFFFFL) < (b & 0xFFFFFFFFL) ? LESS : MORE;
			}
		}
		return EQUAL;
	}

	@Override @AvailMethod
	Order o_NumericCompareToInfinity (
		final AvailObject object,
		final Sign sign)
	{
		return sign == Sign.POSITIVE ? LESS : MORE;
	}

	@Override @AvailMethod
	Order o_NumericCompareToDouble (
		final AvailObject object,
		final double aDouble)
	{
		return
			DoubleDescriptor.compareDoubleAndInteger(aDouble, object).reverse();
	}

	@Override @AvailMethod
	A_Number o_BitwiseAnd (
		final AvailObject object,
		final A_Number anInteger,
		final boolean canDestroy)
	{
		final int objectSize = intCount(object);
		final int anIntegerSize = intCount(anInteger);
		AvailObject output = canDestroy
			? largerMutableOf(object, (AvailObject)anInteger)
			: null;
		// Both integers are 32 bits. This is by far the most common case.
		if (objectSize == 1 && anIntegerSize == 1)
		{
			final int result = object.rawSignedIntegerAt(1)
				& anInteger.rawSignedIntegerAt(1);
			if (output == null)
			{
				output = createUninitialized(1);
			}
			output.rawSignedIntegerAtPut(1, result);
			return output;
		}
		// If neither of the inputs were suitable for destruction, then allocate
		// a new one whose size is that of the larger input.
		if (output == null)
		{
			output = createUninitialized(max(objectSize, anIntegerSize));
		}
		// Handle larger integers.
		final int outputSize = intCount(output);
		final int extendedObject = object.rawSignedIntegerAt(objectSize) >> 31;
		final int extendedAnInteger =
			anInteger.rawSignedIntegerAt(anIntegerSize) >> 31;
		for (int i = 1; i <= outputSize; i++)
		{
			final int objectWord = i > objectSize
				? extendedObject
				: object.rawSignedIntegerAt(i);
			final int anIntegerWord = i > anIntegerSize
				? extendedAnInteger
				: anInteger.rawSignedIntegerAt(i);
			final int result = objectWord & anIntegerWord;
			output.rawSignedIntegerAtPut(i, result);
		}
		output.trimExcessInts();
		return output;
	}

	@Override @AvailMethod
	A_Number o_BitwiseOr (
		final AvailObject object,
		final A_Number anInteger,
		final boolean canDestroy)
	{
		final int objectSize = intCount(object);
		final int anIntegerSize = intCount(anInteger);
		AvailObject output = canDestroy
			? largerMutableOf(object, (AvailObject)anInteger)
			: null;
		// Both integers are 32 bits. This is by far the most common case.
		if (objectSize == 1 && anIntegerSize == 1)
		{
			final int result = object.rawSignedIntegerAt(1)
				| anInteger.rawSignedIntegerAt(1);
			if (output == null)
			{
				output = createUninitialized(1);
			}
			output.rawSignedIntegerAtPut(1, result);
			return output;
		}
		// If neither of the inputs were suitable for destruction, then allocate
		// a new one whose size is that of the larger input.
		if (output == null)
		{
			output = createUninitialized(max(objectSize, anIntegerSize));
		}
		// Handle larger integers.
		final int outputSize = intCount(output);
		final int extendedObject = object.rawSignedIntegerAt(objectSize) >> 31;
		final int extendedAnInteger =
			anInteger.rawSignedIntegerAt(anIntegerSize) >> 31;
		for (int i = 1; i <= outputSize; i++)
		{
			final int objectWord = i > objectSize
				? extendedObject
				: object.rawSignedIntegerAt(i);
			final int anIntegerWord = i > anIntegerSize
				? extendedAnInteger
				: anInteger.rawSignedIntegerAt(i);
			final int result = objectWord | anIntegerWord;
			output.rawSignedIntegerAtPut(i, result);
		}
		output.trimExcessInts();
		return output;
	}

	@Override @AvailMethod
	A_Number o_BitwiseXor (
		final AvailObject object,
		final A_Number anInteger,
		final boolean canDestroy)
	{
		final int objectSize = intCount(object);
		final int anIntegerSize = intCount(anInteger);
		AvailObject output = canDestroy
			? largerMutableOf(object, (AvailObject)anInteger)
			: null;
		// Both integers are 32 bits. This is by far the most common case.
		if (objectSize == 1 && anIntegerSize == 1)
		{
			final int result = object.rawSignedIntegerAt(1)
				^ anInteger.rawSignedIntegerAt(1);
			if (output == null)
			{
				output = createUninitialized(1);
			}
			output.rawSignedIntegerAtPut(1, result);
			return output;
		}
		// If neither of the inputs were suitable for destruction, then allocate
		// a new one whose size is that of the larger input.
		if (output == null)
		{
			output = createUninitialized(max(objectSize, anIntegerSize));
		}
		// Handle larger integers.
		final int outputSize = intCount(output);
		final int extendedObject = object.rawSignedIntegerAt(objectSize) >> 31;
		final int extendedAnInteger =
			anInteger.rawSignedIntegerAt(anIntegerSize) >> 31;
		for (int i = 1; i <= outputSize; i++)
		{
			final int objectWord = i > objectSize
				? extendedObject
				: object.rawSignedIntegerAt(i);
			final int anIntegerWord = i > anIntegerSize
				? extendedAnInteger
				: anInteger.rawSignedIntegerAt(i);
			final int result = objectWord ^ anIntegerWord;
			output.rawSignedIntegerAtPut(i, result);
		}
		output.trimExcessInts();
		return output;
	}

	/**
	 * Shift the given positive number to the left by the specified shift factor
	 * (number of bits), then truncate the representation to force bits above
	 * the specified position to be zeroed.  The shift factor may be
	 * negative, indicating a right shift by the corresponding positive amount,
	 * in which case truncation will still happen.
	 *
	 * <p>
	 * For example, shifting the binary number 1011<sub>2</sub> to the left by 2
	 * positions will produce 101100<sub>2</sub>, then truncating it to, say 5
	 * bits, would produce 01100<sub>2</sub>.  For a second example, the
	 * positive number 110101 can be shifted left by -2 positions (which is a
	 * right shift of 2) to get 1101, and a subsequent truncation to 10 bits
	 * would leave it unaffected.
	 * </p>
	 *
	 * @param object
	 *            The non-negative integer to shift and mask.
	 * @param shiftFactor
	 *            How much to shift left (may be negative to indicate a right
	 *            shift).
	 * @param truncationBits
	 *            A positive integer indicating how many low-order bits of the
	 *            shifted value should be preserved.
	 * @param canDestroy
	 *            Whether it is permitted to alter the original object if it
	 *            happens to be mutable.
	 * @return (object × 2<sup>shiftFactor</sup>) mod 2<sup>truncationBits</sup>
	 */
	@Override @AvailMethod
	A_Number o_BitShiftLeftTruncatingToBits (
		final AvailObject object,
		final A_Number shiftFactor,
		final A_Number truncationBits,
		final boolean canDestroy)
	{
		if (!truncationBits.isInt())
		{
			throw new ArithmeticException(
				AvailErrorCode.E_SHIFT_AND_TRUNCATE_REQUIRES_NON_NEGATIVE);
		}
		final int truncationInt = truncationBits.extractInt();
		if (truncationInt < 0)
		{
			throw new ArithmeticException(
				AvailErrorCode.E_SHIFT_AND_TRUNCATE_REQUIRES_NON_NEGATIVE);
		}
		final Order sign = object.numericCompareToInteger(zero());
		if (sign == LESS)
		{
			throw new ArithmeticException(
				AvailErrorCode.E_SHIFT_AND_TRUNCATE_REQUIRES_NON_NEGATIVE);
		}
		if (sign == EQUAL)
		{
			if (!canDestroy & isMutable())
			{
				object.makeImmutable();
			}
			// 0*2^n = 0
			return object;
		}
		if (!shiftFactor.isInt())
		{
			// e.g., 123 >> 999999999999999999 is 0
			// also 123 << 999999999999999999 truncated to N bits (N<2^31) is 0.
			return zero();
		}
		final int shiftInt = shiftFactor.extractInt();
		if (object.isLong())
		{
			final long baseLong = object.extractLong();
			final long shiftedLong = bitShift(baseLong, shiftInt);
			if (shiftInt < 0
				|| truncationInt < 64
				|| bitShift(shiftedLong, -shiftInt) == baseLong)
			{
				// Either a right shift, or a left shift that didn't lose bits,
				// or a left shift that will fit in a long after the truncation.
				// In these cases the result will still be a long.
				long resultLong = shiftedLong;
				if (truncationInt < 64)
				{
					resultLong &= (1 << truncationInt) - 1;
				}
				if (canDestroy && isMutable())
				{
					if (resultLong == (int)resultLong)
					{
						// Fits in an int.  Try to recycle.
						if (intCount(object) == 1)
						{
							object.rawSignedIntegerAtPut(1, (int)resultLong);
							return object;
						}
					}
					else
					{
						// *Fills* a long.  Try to recycle.
						if (intCount(object) == 2)
						{
							object.rawSignedIntegerAtPut(
								1,
								(int)resultLong);
							object.rawSignedIntegerAtPut(
								2,
								(int)(resultLong >> 32L));
							return object;
						}
					}
				}
				// Fall back and create a new integer object.
				return fromLong(resultLong);
			}
		}
		// Answer doesn't (necessarily) fit in a long.
		final int sourceSlots = intCount(object);
		int estimatedBits = (sourceSlots << 5) + shiftInt;
		estimatedBits = min(estimatedBits, truncationInt + 1);
		estimatedBits = max(estimatedBits, 1);
		final int slotCount = (estimatedBits + 31) >> 5;
		final A_Number result = createUninitialized(slotCount);
		final int shortShift = shiftInt & 31;
		int sourceIndex = slotCount - (shiftInt >> 5);
		long accumulator = 0xDEADCAFEBABEBEEFL;
		// We range from slotCount+1 to 1 to pre-load the accumulator.
		for (int destIndex = slotCount + 1; destIndex >= 1; destIndex--)
		{
			final int nextWord =
				(1 <= sourceIndex && sourceIndex <= sourceSlots)
					? object.rawSignedIntegerAt(sourceIndex)
					: 0;
			accumulator <<= 32;
			accumulator |= nextWord << shortShift;
			if (destIndex <= slotCount)
			{
				result.rawSignedIntegerAtPut(
					destIndex,
					(int)(accumulator >> 32));
			}
			sourceIndex--;
		}
		// Mask it if necessary to truncate some upper bits.
		int mask = (1 << (truncationInt & 31)) - 1;
		for (
			int destIndex = truncationInt >> 5;
			destIndex <= slotCount;
			destIndex++)
		{
			result.rawSignedIntegerAtPut(
				destIndex,
				result.rawSignedIntegerAt(destIndex) & mask);
			// Completely wipe any higher ints.
			mask = 0;
		}
		result.trimExcessInts();
		return result;
	}

	/**
	 * Shift the given integer to the left by the specified shift factor
	 * (number of bits).  The shift factor may be negative, indicating a right
	 * shift by the corresponding positive amount.
	 *
	 * @param object
	 *            The integer to shift.
	 * @param shiftFactor
	 *            How much to shift left (may be negative to indicate a right
	 *            shift).
	 * @param canDestroy
	 *            Whether it is permitted to alter the original object if it
	 *            happens to be mutable.
	 * @return ⎣object × 2<sup>shiftFactor</sup>⎦
	 */
	@Override @AvailMethod
	A_Number o_BitShift (
		final AvailObject object,
		final A_Number shiftFactor,
		final boolean canDestroy)
	{
		if (object.equals(zero()))
		{
			if (!canDestroy & isMutable())
			{
				object.makeImmutable();
			}
			// 0*2^n = 0
			return object;
		}
		if (!shiftFactor.isInt())
		{
			if (shiftFactor.numericCompareToInteger(zero()) == MORE)
			{
				// e.g., 123 << 999999999999999999 is too big
				throw new ArithmeticException(
					AvailErrorCode.E_TOO_LARGE_TO_REPRESENT);
			}
			if (object.numericCompareToInteger(zero()) == MORE)
			{
				// e.g., 123 >> 999999999999999999 is 0
				return zero();
			}
			// e.g., -123 >> 999999999999999999 is -1
			return negativeOne();
		}
		final int shiftInt = shiftFactor.extractInt();
		if (object.isLong())
		{
			final long baseLong = object.extractLong();
			final long shiftedLong = arithmeticBitShift(baseLong, shiftInt);
			if (shiftInt < 0
				|| arithmeticBitShift(shiftedLong, -shiftInt) == baseLong)
			{
				// Either a right shift, or a left shift that didn't lose bits.
				// In these cases the result will still be a long.
				final long resultLong = shiftedLong;
				if (canDestroy && isMutable())
				{
					if (resultLong == (int)resultLong)
					{
						// Fits in an int.  Try to recycle.
						if (intCount(object) == 1)
						{
							object.rawSignedIntegerAtPut(1, (int)resultLong);
							return object;
						}
					}
					else
					{
						// *Fills* a long.  Try to recycle.
						if (intCount(object) == 2)
						{
							object.rawSignedIntegerAtPut(
								1,
								(int)resultLong);
							object.rawSignedIntegerAtPut(
								2,
								(int)(resultLong >> 32L));
							return object;
						}
					}
				}
				// Fall back and create a new integer object.
				return fromLong(resultLong);
			}
		}
		// Answer doesn't (necessarily) fit in a long.
		final int sourceSlots = intCount(object);
		int estimatedBits = (sourceSlots << 5) + shiftInt;
		estimatedBits = max(estimatedBits, 1);
		final int intSlotCount = (estimatedBits + 31) >> 5;
		final A_Number result = createUninitialized(intSlotCount);
		final int shortShift = shiftInt & 31;
		int sourceIndex = intSlotCount - (shiftInt >> 5);
		long accumulator = 0xDEADCAFEBABEBEEFL;
		final int signExtension =
			object.numericCompareToInteger(zero()) == LESS
				? -1
				: 0;
		// We range from slotCount+1 to 1 to pre-load the accumulator.
		for (int destIndex = intSlotCount + 1; destIndex >= 1; destIndex--)
		{
			final int nextWord =
				sourceIndex < 1
					? 0
					: sourceIndex > sourceSlots
						? signExtension
						: object.rawSignedIntegerAt(sourceIndex);
			accumulator <<= 32;
			accumulator |= (nextWord & 0xFFFFFFFFL) << shortShift;
			if (destIndex <= intSlotCount)
			{
				result.rawSignedIntegerAtPut(
					destIndex,
					(int)(accumulator >> 32));
			}
			sourceIndex--;
		}
		result.trimExcessInts();
		return result;
	}

	@Override @AvailMethod
	SerializerOperation o_SerializerOperation (
		final AvailObject object)
	{
		if (object.isInt())
		{
			final int intValue = object.extractInt();
			if (0 <= intValue && intValue <= 10)
			{
				switch (intValue)
				{
					case 0: return SerializerOperation.ZERO_INTEGER;
					case 1: return SerializerOperation.ONE_INTEGER;
					case 2: return SerializerOperation.TWO_INTEGER;
					case 3: return SerializerOperation.THREE_INTEGER;
					case 4: return SerializerOperation.FOUR_INTEGER;
					case 5: return SerializerOperation.FIVE_INTEGER;
					case 6: return SerializerOperation.SIX_INTEGER;
					case 7: return SerializerOperation.SEVEN_INTEGER;
					case 8: return SerializerOperation.EIGHT_INTEGER;
					case 9: return SerializerOperation.NINE_INTEGER;
					case 10: return SerializerOperation.TEN_INTEGER;
				}
			}
			if ((intValue & 0xFF) == intValue)
			{
				return SerializerOperation.BYTE_INTEGER;
			}
			if ((intValue & 0xFFFF) == intValue)
			{
				return SerializerOperation.SHORT_INTEGER;
			}
			return SerializerOperation.INT_INTEGER;
		}
		return SerializerOperation.BIG_INTEGER;
	}

	@Override
	@Nullable Object o_MarshalToJava (
		final AvailObject object,
		final @Nullable Class<?> classHint)
	{
		// Force marshaling to java.math.BigInteger.
		if (BigInteger.class.equals(classHint))
		{
			return object.asBigInteger();
		}
		// Force marshaling to Java's primitive long type.
		else if (Long.TYPE.equals(classHint))
		{
			if (!object.isLong())
			{
				throw new MarshalingException();
			}
			return Long.valueOf(object.extractLong());
		}
		// Force marshaling to Java's primitive int type.
		else if (Integer.TYPE.equals(classHint))
		{
			if (!object.isInt())
			{
				throw new MarshalingException();
			}
			return Integer.valueOf(object.extractInt());
		}
		// Force marshaling to Java's primitive short type.
		else if (Short.TYPE.equals(classHint))
		{
			if (!object.isSignedShort())
			{
				throw new MarshalingException();
			}
			return Short.valueOf(object.extractSignedShort());
		}
		// Force marshaling to Java's primitive byte type.
		else if (Byte.TYPE.equals(classHint))
		{
			if (!object.isSignedByte())
			{
				throw new MarshalingException();
			}
			return Byte.valueOf(object.extractSignedByte());
		}
		// No useful hint was provided, so marshal to the smallest primitive
		// integral type able to express object's value.
		if (object.isLong())
		{
			if (object.isInt())
			{
				if (object.isSignedShort())
				{
					if (object.isSignedByte())
					{
						return Byte.valueOf(object.extractSignedByte());
					}
					return Short.valueOf(object.extractSignedShort());
				}
				return Integer.valueOf(object.extractInt());
			}
			return Long.valueOf(object.extractLong());
		}
		return object.asBigInteger();
	}

	/**
	 * Answer a {@link BigInteger} that is the numerical equivalent of the given
	 * object, which is an {@linkplain IntegerDescriptor Avail integer}.
	 */
	@Override @AvailMethod
	public BigInteger o_AsBigInteger (final AvailObject object)
	{
		final int integerCount = intCount(object);
		if (integerCount <= 2)
		{
			return BigInteger.valueOf(object.extractLong());
		}
		final byte[] bytes = new byte[integerCount << 2];
		for (int i = integerCount, b = 0; i > 0; i--)
		{
			final int integer = object.intSlot(RAW_LONG_SLOTS_, i);
			bytes[b++] = (byte) (integer >> 24);
			bytes[b++] = (byte) (integer >> 16);
			bytes[b++] = (byte) (integer >> 8);
			bytes[b++] = (byte) integer;
		}
		return new BigInteger(bytes);
	}

	@Override
	boolean o_IsNumericallyIntegral (final AvailObject object)
	{
		return true;
	}

	@Override
	void o_WriteTo (final AvailObject object, final JSONWriter writer)
	{
		if (object.isLong())
		{
			writer.write(object.extractLong());
		}
		else
		{
			writer.write(object.asBigInteger());
		}
	}

	/**
	 * Convert the specified Java {@code long} into an Avail {@linkplain
	 * IntegerDescriptor integer}.
	 *
	 * @param aLong A Java {@code long}.
	 * @return An {@link AvailObject}.
	 */
	public static A_Number fromLong (final long aLong)
	{
		if (aLong == (aLong & 255))
		{
			return immutableByteObjects[(int) aLong];
		}
		if (aLong == (int)aLong)
		{
			final AvailObject result = createUninitialized(1);
			result.setIntSlot(RAW_LONG_SLOTS_, 1, (int) aLong);
			return result;
		}
		final AvailObject result = createUninitialized(2);
		result.setIntSlot(RAW_LONG_SLOTS_, 1, (int) aLong);
		result.setIntSlot(RAW_LONG_SLOTS_, 2, (int) (aLong >> 32L));
		return result;
	}

	/**
	 * Create an {@linkplain IntegerDescriptor Avail integer} that is the
	 * numerical equivalent of the given Java {@link BigInteger}.
	 *
	 * @param bigInteger The BigInteger to convert.
	 * @return An Avail integer representing the same number as the argument.
	 */
	public static A_Number fromBigInteger (final BigInteger bigInteger)
	{
		final byte[] bytes = bigInteger.toByteArray();
		if (bytes.length < 8)
		{
			return fromLong(bigInteger.longValue());
		}
		final int signByte = ((bytes[0] >> 7) & 1) * 255;
		final int intCount = (bytes.length + 4) >> 2;
		final AvailObject result = createUninitialized(intCount);
		// Start with the least significant bits.
		int byteIndex = bytes.length - 1;
		for (int destIndex = 1; destIndex <= intCount; destIndex++)
		{
			int intValue;
			if (byteIndex >= 3)
			{
				intValue = (bytes[byteIndex] & 255);
				intValue += (bytes[byteIndex - 1] & 255) << 8;
				intValue += (bytes[byteIndex - 2] & 255) << 16;
				intValue += (bytes[byteIndex - 3] & 255) << 24;
			}
			else
			{
				// Do the highest order int specially, low to high bytes.
				intValue = (byteIndex >= 0 ? bytes[byteIndex] : signByte) & 255;
				intValue += ((byteIndex >= 1 ? bytes[byteIndex - 1] : signByte) & 255) << 8;
				intValue += ((byteIndex >= 2 ? bytes[byteIndex - 2] : signByte) & 255) << 16;
				intValue += ((byteIndex >= 3 ? bytes[byteIndex - 3] : signByte) & 255) << 24;
			}
			result.setIntSlot(RAW_LONG_SLOTS_, destIndex, intValue);
			byteIndex -= 4;
		}
		result.trimExcessInts();
		return result;
	}

	/**
	 * Answer an Avail {@linkplain IntegerDescriptor integer} that holds the
	 * truncation of the {@code double} argument, rounded towards zero.
	 *
	 * @param aDouble
	 *            The object whose truncation should be encoded as an Avail
	 *            integer.
	 * @return An Avail integer.
	 */
	public static A_Number truncatedFromDouble (final double aDouble)
	{
		// Extract the top three 32-bit sections.  That guarantees 65 bits
		// of mantissa, which is more than a double actually captures.
		double truncated = aDouble;
		if (truncated >= Long.MIN_VALUE && truncated <= Long.MAX_VALUE)
		{
			// Common case -- it fits in a long.
			return IntegerDescriptor.fromLong((long)truncated);
		}
		final boolean neg = truncated < 0.0d;
		truncated = abs(truncated);
		final int exponent = getExponent(truncated);
		final int slots = exponent + 31 / 32;  // probably needs work
		final AvailObject out = createUninitialized(slots);
		truncated = scalb(truncated, (1 - slots) * 32);
		for (int i = slots; i >= 1; --i)
		{
			final long intSlice = (int) truncated;
			out.setIntSlot(RAW_LONG_SLOTS_, i, (int)intSlice);
			truncated -= intSlice;
			truncated = scalb(truncated, 32);
		}
		out.trimExcessInts();
		if (neg)
		{
			return zero().noFailMinusCanDestroy(out, true);
		}
		return out;
	}


	/**
	 * Convert the specified Java {@code int} into an Avail {@linkplain
	 * IntegerDescriptor integer}.
	 *
	 * @param anInteger A Java {@code int}.
	 * @return An {@link AvailObject}.
	 */
	public static A_Number fromInt (final int anInteger)
	{
		if (anInteger == (anInteger & 255))
		{
			return immutableByteObjects[anInteger];
		}
		final AvailObject result = createUninitialized(1);
		result.setIntSlot(RAW_LONG_SLOTS_, 1, anInteger);
		return result;
	}

	/**
	 * Convert the specified byte-valued Java {@code short} into an Avail
	 * {@linkplain IntegerDescriptor integer}.
	 *
	 * @param anInteger A Java {@code int}.
	 * @return An {@link AvailObject}.
	 */
	public static A_Number fromUnsignedByte (final short anInteger)
	{
		assert anInteger >= 0 && anInteger <= 255;
		return immutableByteObjects[anInteger];
	}

	/**
	 * Extract a {@code double} from this integer, but scale it down by the
	 * specified power of two in the process.  If the integer is beyond the
	 * scale of a double but the scale would bring it in range, don't overflow.
	 *
	 * @param object
	 *            The integer to convert to a double.
	 * @param exponentBias
	 *            The scale by which the result's exponent should be adjusted
	 *            (a positive number scales the result downward).
	 * @return
	 *            The integer times 2^-exponentBias, as a double.
	 */
	public static double extractDoubleScaled (
		final AvailObject object,
		final int exponentBias)
	{
		// Extract and scale the top three ints from anInteger, if available.
		// This guarantees at least 64 correct upper bits were extracted (if
		// available), which is better than a double can represent.
		final int slotsCount = intCount(object);
		final long high = object.rawSignedIntegerAt(slotsCount);
		double d = Math.scalb(
			(double)high,
			((slotsCount - 1) << 5) - exponentBias);
		if (slotsCount > 1)
		{
			final long med = (high & ~0xFFFFFFFFL)
				+ object.rawUnsignedIntegerAt(slotsCount - 1);
			d += Math.scalb(
				(double)med,
				((slotsCount - 2) << 5) - exponentBias);
			if (slotsCount > 2)
			{
				final long low = (high & ~0xFFFFFFFFL)
					+ object.rawUnsignedIntegerAt(slotsCount - 2);
				d += Math.scalb(
					(double)low,
					((slotsCount - 3) << 5) - exponentBias);
			}
		}
		return d;
	}

	/**
	 * Answer an {@link AvailObject} representing the {@linkplain
	 * IntegerDescriptor integer} negative one (-1).
	 *
	 * @return The Avail integer negative one (-1).
	 */
	public static A_Number negativeOne ()
	{
		final A_Number neg = negativeOne;
		assert neg != null;
		return neg;
	}

	/**
	 * Answer the {@code int} hash value of the given {@code short} in the range
	 * 0..255 inclusive.
	 *
	 * @param anInteger
	 *        The {@code short} to be hashed.  It must be in the range 0..255
	 *        inclusive.
	 * @return The hash of the passed unsigned byte.
	 */
	static int hashOfUnsignedByte (final short anInteger)
	{
		final int[] hashes = hashesOfUnsignedBytes;
		assert hashes != null;
		return hashes[anInteger];
	}

	/** The initialization value for computing the hash of an integer. */
	static final int initialHashValue = 0x13592884;

	/**
	 * The value to xor with after multiplying by the {@link #multiplier} for
	 * each int of the integer.
	 */
	static final int postMultiplyHashToggle = 0x95ffb59f;

	/**
	 * The value to add after performing a final extra multiply by {@link
	 * #multiplier}.
	 */
	static final int finalHashAddend = 0x5127ee66;


	/**
	 * Hash the passed {@code int}.  Note that it must have the same value as
	 * what {@link #computeHashOfIntegerObject(A_Number)} would return, given
	 * an encoding of the {@code int} as an Avail {@linkplain IntegerDescriptor
	 * integer}.
	 *
	 * @param anInt The {@code int} to hash.
	 * @return The hash of the given {@code int}.
	 */
	static int computeHashOfInt (final int anInt)
	{
		int h = initialHashValue + anInt;
		h *= multiplier;
		h ^= postMultiplyHashToggle;
		h *= multiplier;
		h += finalHashAddend;
		return h;
	}

	/**
	 * Compute the hash of the given Avail {@linkplain IntegerDescriptor
	 * integer} object.  Note that if the input is within the range of an {@code
	 * int}, it should produce the same value as the equivalent invocation of
	 * {@link #computeHashOfInt(int)}.
	 *
	 * @param anIntegerObject
	 *        An Avail {@linkplain IntegerDescriptor integer} to be hashed.
	 * @return The hash of the given Avail {@linkplain IntegerDescriptor
	 *         integer}.
	 */
	static int computeHashOfIntegerObject (final A_Number anIntegerObject)
	{
		int output = initialHashValue;
		for (int i = intCount(anIntegerObject); i > 0; i--)
		{
			output += anIntegerObject.rawSignedIntegerAt(i);
			output *= multiplier;
			output ^= postMultiplyHashToggle;
		}
		output *= multiplier;
		output += finalHashAddend;
		return output;
	}

	/**
	 * Create a mutable {@linkplain IntegerDescriptor integer} with the
	 * specified number of uninitialized int slots.
	 *
	 * @param size The number of int slots to have in the result.
	 * @return An uninitialized, mutable integer.
	 */
	public static AvailObject createUninitialized (final int size)
	{
		final IntegerDescriptor descriptor = descriptorFor(MUTABLE, size);
		return descriptor.create((size + 1) >> 1);
	}

	/**
	 * Construct a new {@link IntegerDescriptor}.
	 *
	 * @param mutability
	 *        The {@linkplain Mutability mutability} of the new descriptor.
	 * @param unusedIntsOfLastLong
	 *        The number of unused {@code int}s in the last {@code long}.  Must
	 *        be 0 or 1.
	 */
	private IntegerDescriptor (
		final Mutability mutability,
		final byte unusedIntsOfLastLong)
	{
		super(mutability, null, IntegerSlots.class);
		assert (unusedIntsOfLastLong & ~1) == 0;
		this.unusedIntsOfLastLong = unusedIntsOfLastLong;
	}

	/**
	 * The static list of descriptors of this kind, organized in such a way that
	 * they can be found by mutability and the number of unused ints in the last
	 * long.
	 */
	private static final IntegerDescriptor[] descriptors =
		new IntegerDescriptor[2 * 3];

	static {
		int i = 0;
		for (final byte excess : new byte[] {0,1})
		{
			for (final Mutability mut : Mutability.values())
			{
				descriptors[i++] = new IntegerDescriptor(mut, excess);
			}
		}
	}

	/**
	 * Answer the descriptor that has the specified mutability flag and is
	 * suitable to describe a tuple with the given number of elements.
	 *
	 * @param flag
	 *            Whether the requested descriptor should be mutable.
	 * @param size
	 *            How many elements are in a tuple to be represented by the
	 *            descriptor.
	 * @return
	 *            A {@link TwoByteStringDescriptor} suitable for representing a
	 *            two-byte string of the given mutability and {@link
	 *            AvailObject#tupleSize() size}.
	 */
	private static IntegerDescriptor descriptorFor (
		final Mutability flag,
		final int size)
	{
		return descriptors[(size & 1) * 3 + flag.ordinal()];
	}

	@Override
	IntegerDescriptor mutable ()
	{
		return descriptors[
			(unusedIntsOfLastLong & 1) * 3 + MUTABLE.ordinal()];
	}

	@Override
	IntegerDescriptor immutable ()
	{
		return descriptors[
			(unusedIntsOfLastLong & 1) * 3 + IMMUTABLE.ordinal()];
	}

	@Override
	IntegerDescriptor shared ()
	{
		return descriptors[
			(unusedIntsOfLastLong & 1) * 3 + SHARED.ordinal()];
	}

	/**
	 * An array of 256 {@code int}s, corresponding to the hashes of the values
	 * 0..255 inclusive.
	 */
	private static final int[] hashesOfUnsignedBytes;

	static
	{
		final int[] hashes = new int [256];
		for (int i = 0; i <= 255; i++)
		{
			hashes[i] = computeHashOfInt(i);
		}
		hashesOfUnsignedBytes = hashes;
	}

	/**
	 * An array of 256 immutable {@linkplain IntegerDescriptor integers},
	 * corresponding with the indices 0..255 inclusive.  These make many kinds
	 * of calculations much more efficient than naively constructing a fresh
	 * {@link AvailObject} unconditionally.
	 */
	private static final A_Number[] immutableByteObjects;

	static
	{
		final AvailObject[] bytes = new AvailObject [256];
		for (int i = 0; i <= 255; i++)
		{
			final AvailObject object = createUninitialized(1);
			object.rawSignedIntegerAtPut(1, i);
			bytes[i] = object.makeShared();
		}
		immutableByteObjects = bytes;
	}

	/** An Avail integer representing zero (0). */
	private static A_Number zero = immutableByteObjects[0];

	/** An Avail integer representing one (1). */
	private static A_Number one = immutableByteObjects[1];

	/** An Avail integer representing two (2). */
	private static A_Number two = immutableByteObjects[2];

	/** An Avail integer representing ten (10). */
	private static A_Number ten = immutableByteObjects[10];

	/** The Avail integer negative one (-1). */
	private static final A_Number negativeOne;

	static
	{
		final AvailObject neg = createUninitialized(1);
		neg.rawSignedIntegerAtPut(1, -1);
		negativeOne = neg.makeShared();
	}

	/**
	 * Answer an {@link AvailObject} representing the {@linkplain
	 * IntegerDescriptor integer} zero (0).
	 *
	 * @return The Avail integer zero.
	 */
	public static A_Number zero ()
	{
		return zero;
	}

	/**
	 * Answer an {@link AvailObject} representing the {@linkplain
	 * IntegerDescriptor integer} one (1).
	 *
	 * @return The Avail integer one.
	 */
	public static A_Number one ()
	{
		return one;
	}

	/**
	 * Answer an {@link AvailObject} representing the {@linkplain
	 * IntegerDescriptor integer} two (2).
	 *
	 * @return The Avail integer two.
	 */
	public static A_Number two ()
	{
		return two;
	}

	/**
	 * Answer an {@link AvailObject} representing the {@linkplain
	 * IntegerDescriptor integer} ten (10).
	 *
	 * @return The Avail integer ten.
	 */
	public static A_Number ten ()
	{
		return ten;
	}

	/**
	 * One (U.S.) quintillion, which is 10^18.  This is the largest power of ten
	 * representable as a signed long.
	 */
	private static final long quintillionLong = 1_000_000_000_000_000_000L;

	/**
	 * One (U.S.) quintillion, which is 10^18.  This is the largest power of ten
	 * for which {@link #o_IsLong(AvailObject)} returns true.
	 */
	private static final A_Number quintillionInteger =
		IntegerDescriptor.fromLong(quintillionLong);
}