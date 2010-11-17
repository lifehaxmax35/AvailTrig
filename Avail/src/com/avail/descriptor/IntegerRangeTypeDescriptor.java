/**
 * descriptor/IntegerRangeTypeDescriptor.java
 * Copyright (c) 2010, Mark van Gulik.
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

import com.avail.descriptor.AvailObject;
import com.avail.descriptor.InfinityDescriptor;
import com.avail.descriptor.IntegerDescriptor;
import com.avail.descriptor.IntegerRangeTypeDescriptor;
import com.avail.descriptor.TypeDescriptor;
import java.util.List;
import static com.avail.descriptor.AvailObject.*;

@IntegerSlots("inclusiveFlags")
@ObjectSlots({
	"lowerBound", 
	"upperBound"
})
public class IntegerRangeTypeDescriptor extends TypeDescriptor
{


	// GENERATED accessors

	void ObjectInclusiveFlags (
			final AvailObject object, 
			final int value)
	{
		//  GENERATED setter method.

		object.integerSlotAtByteIndexPut(4, value);
	}

	void ObjectLowerBound (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED setter method.

		object.objectSlotAtByteIndexPut(-4, value);
	}

	void ObjectUpperBound (
			final AvailObject object, 
			final AvailObject value)
	{
		//  GENERATED setter method.

		object.objectSlotAtByteIndexPut(-8, value);
	}

	int ObjectInclusiveFlags (
			final AvailObject object)
	{
		//  GENERATED getter method.

		return object.integerSlotAtByteIndex(4);
	}

	AvailObject ObjectLowerBound (
			final AvailObject object)
	{
		//  GENERATED getter method.

		return object.objectSlotAtByteIndex(-4);
	}

	AvailObject ObjectUpperBound (
			final AvailObject object)
	{
		//  GENERATED getter method.

		return object.objectSlotAtByteIndex(-8);
	}



	// java printing

	void printObjectOnAvoidingIndent (
			final AvailObject object, 
			final StringBuilder aStream, 
			final List<AvailObject> recursionList, 
			final int indent)
	{
		aStream.append(object.lowerInclusive() ? '[' : '(');
		object.lowerBound().printOnAvoidingIndent(aStream, recursionList, indent + 1);
		aStream.append("..");
		object.upperBound().printOnAvoidingIndent(aStream, recursionList, indent + 1);
		aStream.append(object.upperInclusive() ? ']' : ')');		
	}



	// operations

	boolean ObjectEquals (
			final AvailObject object, 
			final AvailObject another)
	{
		return another.equalsIntegerRangeType(object);
	}

	boolean ObjectEqualsIntegerRangeType (
			final AvailObject object, 
			final AvailObject another)
	{
		//  Integer range types compare for equality by comparing their minima and maxima.

		if (! object.lowerBound().equals(another.lowerBound()))
		{
			return false;
		}
		if (! object.upperBound().equals(another.upperBound()))
		{
			return false;
		}
		if (! (object.lowerInclusive() == another.lowerInclusive()))
		{
			return false;
		}
		if (! (object.upperInclusive() == another.upperInclusive()))
		{
			return false;
		}
		return true;
	}

	AvailObject ObjectExactType (
			final AvailObject object)
	{
		//  Answer the object's type.

		return Types.integerType.object();
	}

	int ObjectHash (
			final AvailObject object)
	{
		//  Answer the object's hash value.  Be careful, as the range (10..20) is the same type
		//  as the range [11..19], so they should hash the same.  Actually, this is taken
		//  care of during instance creation - if an exclusive bound is finite, it is converted
		//  to its inclusive equivalent.  Otherwise asking for one of the bounds will yield a value
		//  which is either inside or outside depending on something that should not be
		//  observable (because it serves to distinguish two representations of equal objects).

		return IntegerRangeTypeDescriptor.computeHashFromLowerBoundHashUpperBoundHashLowerInclusiveUpperInclusive(
			object.lowerBound().hash(),
			object.upperBound().hash(),
			object.lowerInclusive(),
			object.upperInclusive());
	}

	AvailObject ObjectType (
			final AvailObject object)
	{
		//  Answer the object's type.

		return Types.integerType.object();
	}



	// operations-integer range

	void ObjectLowerInclusiveUpperInclusive (
			final AvailObject object, 
			final boolean lowInc, 
			final boolean highInc)
	{
		//  Set the lower inclusive and upper inclusive flags.

		object.inclusiveFlags(((lowInc ? 1 : 0) + (highInc ? 256 : 0)));
	}

	boolean ObjectLowerInclusive (
			final AvailObject object)
	{
		return ((object.inclusiveFlags() & 1) == 1);
	}

	boolean ObjectUpperInclusive (
			final AvailObject object)
	{
		return ((object.inclusiveFlags() & 256) == 256);
	}



	// operations-types

	boolean ObjectIsSubtypeOf (
			final AvailObject object, 
			final AvailObject aType)
	{
		//  Check if object (a type) is a subtype of aType (should also be a type).

		return aType.isSupertypeOfIntegerRangeType(object);
	}

	boolean ObjectIsSupertypeOfIntegerRangeType (
			final AvailObject object, 
			final AvailObject possibleSub)
	{
		//  Integer range types compare like the subsets they represent.  The only elements that
		//  matter in the comparisons are within one unit of the four boundary conditions (because
		//  these are the only places where the type memberships can change), so just use these.
		//  In particular, use the value just inside and the value just outside each boundary.  If
		//  the subtype's constraints don't logically imply the supertype's constraints then the
		//  subtype is not actually a subtype.  Make use of the fact that integer range types have
		//  their bounds canonized into inclusive form, if finite, at range type creation time.

		final AvailObject subMinObject = possibleSub.lowerBound();
		final AvailObject superMinObject = object.lowerBound();
		if (subMinObject.lessThan(superMinObject))
		{
			return false;
		}
		if ((subMinObject.equals(superMinObject) && (possibleSub.lowerInclusive() && (! object.lowerInclusive()))))
		{
			return false;
		}
		final AvailObject subMaxObject = possibleSub.upperBound();
		final AvailObject superMaxObject = object.upperBound();
		if (superMaxObject.lessThan(subMaxObject))
		{
			return false;
		}
		if ((superMaxObject.equals(subMaxObject) && (possibleSub.upperInclusive() && (! object.upperInclusive()))))
		{
			return false;
		}
		return true;
	}

	AvailObject ObjectTypeIntersection (
			final AvailObject object, 
			final AvailObject another)
	{
		//  Answer the most general type that is still at least as specific as these.

		if (object.isSubtypeOf(another))
		{
			return object;
		}
		if (another.isSubtypeOf(object))
		{
			return another;
		}
		return another.typeIntersectionOfIntegerRangeType(object);
	}

	AvailObject ObjectTypeIntersectionOfIntegerRangeType (
			final AvailObject object, 
			final AvailObject another)
	{
		//  Answer the most specific type that is still at least as general as these.

		AvailObject minObject = object.lowerBound();
		boolean isMinInc = object.lowerInclusive();
		if (another.lowerBound().equals(minObject))
		{
			isMinInc = (isMinInc && another.lowerInclusive());
		}
		else if (minObject.lessThan(another.lowerBound()))
		{
			minObject = another.lowerBound();
			isMinInc = another.lowerInclusive();
		}
		AvailObject maxObject = object.upperBound();
		boolean isMaxInc = object.upperInclusive();
		if (another.upperBound().equals(maxObject))
		{
			isMaxInc = (isMaxInc && another.upperInclusive());
		}
		else if (another.upperBound().lessThan(maxObject))
		{
			maxObject = another.upperBound();
			isMaxInc = another.upperInclusive();
		}
		//  at least two references now.
		//
		//  at least two references now.
		return IntegerRangeTypeDescriptor.lowerBoundInclusiveUpperBoundInclusive(
			minObject.makeImmutable(),
			isMinInc,
			maxObject.makeImmutable(),
			isMaxInc);
	}

	AvailObject ObjectTypeUnion (
			final AvailObject object, 
			final AvailObject another)
	{
		//  Answer the most specific type that is still at least as general as these.

		if (object.isSubtypeOf(another))
		{
			return another;
		}
		if (another.isSubtypeOf(object))
		{
			return object;
		}
		return another.typeUnionOfIntegerRangeType(object);
	}

	AvailObject ObjectTypeUnionOfIntegerRangeType (
			final AvailObject object, 
			final AvailObject another)
	{
		//  Answer the most specific type that is still at least as general as these.

		AvailObject minObject = object.lowerBound();
		boolean isMinInc = object.lowerInclusive();
		if (another.lowerBound().equals(minObject))
		{
			isMinInc = (isMinInc || another.lowerInclusive());
		}
		else if (another.lowerBound().lessThan(minObject))
		{
			minObject = another.lowerBound();
			isMinInc = another.lowerInclusive();
		}
		AvailObject maxObject = object.upperBound();
		boolean isMaxInc = object.upperInclusive();
		if (another.upperBound().equals(maxObject))
		{
			isMaxInc = (isMaxInc || another.upperInclusive());
		}
		else if (maxObject.lessThan(another.upperBound()))
		{
			maxObject = another.upperBound();
			isMaxInc = another.upperInclusive();
		}
		return IntegerRangeTypeDescriptor.lowerBoundInclusiveUpperBoundInclusive(
			minObject,
			isMinInc,
			maxObject,
			isMaxInc);
	}

	boolean ObjectIsIntegerRangeType (
			final AvailObject object)
	{
		return true;
	}




	// Startup/shutdown

	static AvailObject NaturalNumbers;


	static AvailObject Nybbles;


	static AvailObject Characters;


	static AvailObject Bytes;


	static AvailObject ExtendedIntegers;


	static AvailObject Integers;


	static AvailObject WholeNumbers;

	static void createWellKnownObjects ()
	{
		ExtendedIntegers = lowerBoundInclusiveUpperBoundInclusive(
			InfinityDescriptor.negativeInfinity(),
			true,
			InfinityDescriptor.positiveInfinity(),
			true);
		Integers = lowerBoundInclusiveUpperBoundInclusive(
			InfinityDescriptor.negativeInfinity(),
			false,
			InfinityDescriptor.positiveInfinity(),
			false);
		NaturalNumbers = lowerBoundInclusiveUpperBoundInclusive(
			IntegerDescriptor.one(),
			true,
			InfinityDescriptor.positiveInfinity(),
			false);
		WholeNumbers = lowerBoundInclusiveUpperBoundInclusive(
			IntegerDescriptor.zero(),
			true,
			InfinityDescriptor.positiveInfinity(),
			false);
		Bytes = lowerBoundInclusiveUpperBoundInclusive(
			IntegerDescriptor.zero(),
			true,
			IntegerDescriptor.objectFromByte(((short)(255))),
			true);
		Nybbles = lowerBoundInclusiveUpperBoundInclusive(
			IntegerDescriptor.zero(),
			true,
			IntegerDescriptor.objectFromByte(((short)(15))),
			true);
		Characters = lowerBoundInclusiveUpperBoundInclusive(
			IntegerDescriptor.zero(),
			true,
			IntegerDescriptor.objectFromInt(0xFFFF),
			true);
	}

	static void clearWellKnownObjects ()
	{
		//  Default implementation - subclasses may need more variations.

		ExtendedIntegers = null;
		Integers = null;
		NaturalNumbers = null;
		WholeNumbers = null;
		Bytes = null;
		Nybbles = null;
		Characters = null;
	}



	/* Hashing */
	static int computeHashFromLowerBoundHashUpperBoundHashLowerInclusiveUpperInclusive (
			int lowerBoundHash,
			int upperBoundHash,
			boolean lowerInclusive,
			boolean upperInclusive)
	{
		final int flagsHash =
			lowerInclusive
				? (upperInclusive ? 0x1503045E : 0x053A6C17)
				: (upperInclusive ? 0x1DB2D751 : 0x1130427D);
		return ((lowerBoundHash * 29) ^ flagsHash ^ upperBoundHash) & HashMask;
	}


	/* Object creation */

	public static AvailObject extendedIntegers ()
	{
		return ExtendedIntegers;
	}

	public static AvailObject integers ()
	{
		return Integers;
	}

	public static AvailObject wholeNumbers ()
	{
		return WholeNumbers;
	}

	public static AvailObject bytes ()
	{
		return Bytes;
	}

	public static AvailObject nybbles ()
	{
		return Nybbles;
	}

	public static AvailObject singleInteger (AvailObject integerObject)
	{
		integerObject.makeImmutable();
		return IntegerRangeTypeDescriptor.lowerBoundInclusiveUpperBoundInclusive(
			integerObject, true, integerObject, true);
	}


	public static AvailObject lowerBoundInclusiveUpperBoundInclusive (
			AvailObject lowerBound,
			boolean lowerInclusive,
			AvailObject upperBound,
			boolean upperInclusive)
	{
		if (lowerBound.sameAddressAs(upperBound))
			if (lowerBound.descriptor().isMutable())
				error("Don't plug a mutable object in as two distinct construction parameters");
		AvailObject low = lowerBound;
		boolean lowInc = lowerInclusive;
		if (! lowInc)
		{
			//  Try to rewrite (if possible) as inclusive boundary.
			if (low.isFinite())
			{
				low = low.plusCanDestroy(IntegerDescriptor.one(), false);
				lowInc = true;
			}
		}
		AvailObject high = upperBound;
		boolean highInc = upperInclusive;
		if (! highInc)
		{
			//  Try to rewrite (if possible) as inclusive boundary.
			if (high.isFinite())
			{
				high = high.minusCanDestroy(IntegerDescriptor.one(), false);
				highInc = true;
			}
		}
		if (high.lessThan(low))
		{
			return Types.terminates.object();
		}
		if (high.equals(low) && ((! highInc) || (! lowInc)))
		{
			//  Unusual cases such as [INF..INF) give preference to exclusion over inclusion.
			return Types.terminates.object();
		}
		AvailObject result = AvailObject.newIndexedDescriptor(0, IntegerRangeTypeDescriptor.mutableDescriptor());
		result.lowerBound(low);
		result.upperBound(high);
		result.lowerInclusiveUpperInclusive(lowInc, highInc);
		return result;
	}

	/**
	 * Construct a new {@link IntegerRangeTypeDescriptor}.
	 *
	 * @param myId The id of the {@linkplain Descriptor descriptor}.
	 * @param isMutable
	 *        Does the {@linkplain Descriptor descriptor} represent a mutable
	 *        object?
	 * @param numberOfFixedObjectSlots
	 *        The number of fixed {@linkplain AvailObject object} slots.
	 * @param numberOfFixedIntegerSlots The number of fixed integer slots.
	 * @param hasVariableObjectSlots
	 *        Does an {@linkplain AvailObject object} using this {@linkplain
	 *        Descriptor} have any variable object slots?
	 * @param hasVariableIntegerSlots
	 *        Does an {@linkplain AvailObject object} using this {@linkplain
	 *        Descriptor} have any variable integer slots?
	 */
	protected IntegerRangeTypeDescriptor (
		final int myId,
		final boolean isMutable,
		final int numberOfFixedObjectSlots,
		final int numberOfFixedIntegerSlots,
		final boolean hasVariableObjectSlots,
		final boolean hasVariableIntegerSlots)
	{
		super(
			myId,
			isMutable,
			numberOfFixedObjectSlots,
			numberOfFixedIntegerSlots,
			hasVariableObjectSlots,
			hasVariableIntegerSlots);
	}
	
	public static IntegerRangeTypeDescriptor mutableDescriptor()
	{
		return (IntegerRangeTypeDescriptor) allDescriptors [80];
	}
	
	public static IntegerRangeTypeDescriptor immutableDescriptor()
	{
		return (IntegerRangeTypeDescriptor) allDescriptors [81];
	}
}
