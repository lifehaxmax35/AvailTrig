/**
 * descriptor/TupleDescriptor.java
 * Copyright (c) 2010, Mark van Gulik.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
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
import static java.lang.Math.min;
import static java.util.Collections.max;
import java.util.*;
import com.avail.annotations.*;

/**
 * {@code TupleDescriptor} is an abstract descriptor class under which all tuple
 * representations are defined (not counting {@linkplain BottomTypeDescriptor
 * bottom} and {@linkplain IndirectionDescriptor transparent indirections}).  It
 * defines a {@link IntegerSlots#HASH_OR_ZERO HASH_OR_ZERO} integer slot which
 * must be defined in all subclasses.
 *
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 */
public abstract class TupleDescriptor
extends Descriptor
{
	/**
	 * The layout of integer slots for my instances.
	 */
	public enum IntegerSlots implements IntegerSlotsEnum
	{
		/**
		 * A slot to hold the cached hash value of a tuple.  If zero, then the
		 * hash value must be computed upon request.  Note that in the very rare
		 * case that the hash value actually equals zero, the hash value has to
		 * be computed every time it is requested.
		 */
		HASH_OR_ZERO
	}

	@Override @AvailMethod
	void o_HashOrZero (final AvailObject object, final int value)
	{
		object.integerSlotPut(IntegerSlots.HASH_OR_ZERO, value);
	}

	@Override @AvailMethod
	int o_HashOrZero (final AvailObject object)
	{
		return object.integerSlot(IntegerSlots.HASH_OR_ZERO);
	}

	@Override
	public boolean allowsImmutableToMutableReferenceInField (
		final AbstractSlotsEnum e)
	{
		return e == IntegerSlots.HASH_OR_ZERO;
	}

	@Override
	public void printObjectOnAvoidingIndent (
		final @NotNull AvailObject object,
		final StringBuilder aStream,
		final List<AvailObject> recursionList,
		final int indent)
	{
		final List<String> strings = new ArrayList<String>(object.tupleSize());
		int totalChars = 0;
		boolean anyBreaks = false;
		for (final AvailObject element : object)
		{
			final StringBuilder localBuilder = new StringBuilder();
			element.printOnAvoidingIndent(
				localBuilder,
				recursionList,
				indent + 1);
			totalChars += localBuilder.length();
			if (!anyBreaks)
			{
				anyBreaks = localBuilder.indexOf("\n") >= 0;
			}
			strings.add(localBuilder.toString());
		}
		aStream.append('<');
		final boolean breakElements = strings.size() > 1
				&& (anyBreaks || totalChars > 60);
		for (int i = 0; i < strings.size(); i++)
		{
			if (i > 0)
			{
				aStream.append(",");
				if (!breakElements)
				{
					aStream.append(" ");
				}
			}
			if (breakElements)
			{
				aStream.append("\n");
				for (int j = indent; j > 0; j--)
				{
					aStream.append("\t");
				}
			}
			aStream.append(strings.get(i));
		}
		aStream.append('>');
	}

	@Override @AvailMethod
	boolean o_EqualsAnyTuple (
		final @NotNull AvailObject object,
		final AvailObject aTuple)
	{
		// Compare this arbitrary Tuple and the given arbitrary tuple.

		if (object.sameAddressAs(aTuple))
		{
			return true;
		}
		// Compare sizes...
		final int size = object.tupleSize();
		if (size != aTuple.tupleSize())
		{
			return false;
		}
		if (o_Hash(object) != aTuple.hash())
		{
			return false;
		}
		for (int i = 1; i <= size; i++)
		{
			if (!o_TupleAt(object, i).equals(aTuple.tupleAt(i)))
			{
				return false;
			}
		}
		if (object.isBetterRepresentationThan(aTuple))
		{
			aTuple.becomeIndirectionTo(object);
			object.makeImmutable();
		}
		else
		{
			object.becomeIndirectionTo(aTuple);
			// Now that there are at least two references to it...
			aTuple.makeImmutable();
		}
		return true;
	}

	@Override @AvailMethod
	boolean o_EqualsByteString (
		final @NotNull AvailObject object,
		final AvailObject aByteString)
	{
		// Default to generic tuple comparison.

		return o_EqualsAnyTuple(object, aByteString);
	}

	@Override @AvailMethod
	boolean o_EqualsByteTuple (
		final @NotNull AvailObject object,
		final AvailObject aTuple)
	{
		// Default to generic tuple comparison.

		return o_EqualsAnyTuple(object, aTuple);
	}

	@Override @AvailMethod
	boolean o_EqualsNybbleTuple (
		final @NotNull AvailObject object,
		final AvailObject aTuple)
	{
		// Default to generic comparison.

		return o_EqualsAnyTuple(object, aTuple);
	}

	@Override @AvailMethod
	boolean o_EqualsObjectTuple (
		final @NotNull AvailObject object,
		final AvailObject aTuple)
	{
		// Default to generic comparison.

		return o_EqualsAnyTuple(object, aTuple);
	}

	@Override @AvailMethod
	boolean o_EqualsTwoByteString (
		final @NotNull AvailObject object,
		final AvailObject aTwoByteString)
	{
		// Default to generic tuple comparison.

		return o_EqualsAnyTuple(object, aTwoByteString);
	}

	@Override @AvailMethod
	boolean o_IsBetterRepresentationThan (
		final @NotNull AvailObject object,
		final AvailObject anotherObject)
	{
		// Given two objects that are known to be equal, is the first one in a
		// better form (more compact, more efficient, older generation) than
		// the second one?

		return object.bitsPerEntry() < anotherObject.bitsPerEntry();
	}

	@Override @AvailMethod
	boolean o_IsInstanceOfKind (
		final @NotNull AvailObject object,
		final AvailObject aTypeObject)
	{
		// Answer whether object is an instance of a subtype of aTypeObject.
		// Don't generate an approximate type and do the comparison, because the
		// approximate type will defer to this very method.

		if (aTypeObject.equals(TOP.o()))
		{
			return true;
		}
		if (aTypeObject.equals(ANY.o()))
		{
			return true;
		}
		if (!aTypeObject.isTupleType())
		{
			return false;
		}
		// See if it's an acceptable size...
		final int tupleSize = object.tupleSize();
		final AvailObject sizeObject = IntegerDescriptor.fromInt(tupleSize);
		if (!sizeObject.isInstanceOf(aTypeObject.sizeRange()))
		{
			return false;
		}
		// The tuple's size is out of range.
		final AvailObject typeTuple = aTypeObject.typeTuple();
		final int breakIndex = min(tupleSize, typeTuple.tupleSize());
		for (int i = 1; i <= breakIndex; i++)
		{
			if (!object.tupleAt(i).isInstanceOf(
				aTypeObject.typeAtIndex(i)))
			{
				return false;
			}
		}
		final AvailObject defaultTypeObject = aTypeObject.defaultType();
		if (!ANY.o().isSubtypeOf(defaultTypeObject))
		{
			for (int i = breakIndex + 1; i <= tupleSize; i++)
			{
				if (!object.tupleAt(i).isInstanceOf(defaultTypeObject))
				{
					return false;
				}
			}
		}
		return true;
	}

	@Override @AvailMethod
	int o_Hash (final AvailObject object)
	{
		// The hash value is stored raw in the object's hashOrZero slot if it
		// has been computed, otherwise that slot is zero. If a zero is
		// detected, compute the hash and store it in hashOrZero. Note that the
		// hash can (extremely rarely) be zero, in which case the hash has to be
		// computed each time.

		int hash = object.hashOrZero();
		if (hash == 0)
		{
			hash = computeHashForObject(object);
			object.hashOrZero(hash);
		}
		return hash;
	}

	@Override @AvailMethod
	@NotNull AvailObject o_Kind (final AvailObject object)
	{
		final AvailObject tupleOfTypes = object.copyAsMutableObjectTuple();
		final int tupleSize = object.tupleSize();
		for (int i = 1; i <= tupleSize; i++)
		{
			tupleOfTypes.tupleAtPuttingCanDestroy(
				i,
				InstanceTypeDescriptor.on(object.tupleAt(i)),
				true);
		}
		return TupleTypeDescriptor.tupleTypeForSizesTypesDefaultType(
			IntegerDescriptor.fromInt(object.tupleSize()).kind(),
			tupleOfTypes,
			BottomTypeDescriptor.bottom());
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithStartingAt (
		final @NotNull AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final AvailObject anotherObject,
		final int startIndex2)
	{
		error(
			"Subclass responsibility: Object:compareFrom:to:with:startingAt: in Avail.TupleDescriptor",
			object);
		return false;
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithAnyTupleStartingAt (
		final @NotNull AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final AvailObject aTuple,
		final int startIndex2)
	{
		// Compare sections of two tuples. Default generic comparison.

		int index2 = startIndex2;
		for (int index1 = startIndex1; index1 <= endIndex1; index1++)
		{
			if (!object.tupleAt(index1).equals(aTuple.tupleAt(index2)))
			{
				return false;
			}
			index2++;
		}
		return true;
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithByteStringStartingAt (
		final @NotNull AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final AvailObject aByteString,
		final int startIndex2)
	{
		// Compare sections of two tuples. Default to generic comparison.

		return o_CompareFromToWithAnyTupleStartingAt(
			object,
			startIndex1,
			endIndex1,
			aByteString,
			startIndex2);
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithByteTupleStartingAt (
		final @NotNull AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final AvailObject aByteTuple,
		final int startIndex2)
	{
		// Compare sections of two tuples. Default to generic comparison.

		return o_CompareFromToWithAnyTupleStartingAt(
			object,
			startIndex1,
			endIndex1,
			aByteTuple,
			startIndex2);
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithNybbleTupleStartingAt (
		final @NotNull AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final AvailObject aNybbleTuple,
		final int startIndex2)
	{
		// Compare sections of two tuples. Default to generic comparison.

		return o_CompareFromToWithAnyTupleStartingAt(
			object,
			startIndex1,
			endIndex1,
			aNybbleTuple,
			startIndex2);
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithObjectTupleStartingAt (
		final @NotNull AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final AvailObject anObjectTuple,
		final int startIndex2)
	{
		// Compare sections of two tuples. Default to generic comparison.

		return o_CompareFromToWithAnyTupleStartingAt(
			object,
			startIndex1,
			endIndex1,
			anObjectTuple,
			startIndex2);
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithTwoByteStringStartingAt (
		final @NotNull AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final AvailObject aTwoByteString,
		final int startIndex2)
	{
		// Compare sections of two tuples. Default to generic comparison.

		return o_CompareFromToWithAnyTupleStartingAt(
			object,
			startIndex1,
			endIndex1,
			aTwoByteString,
			startIndex2);
	}

	@Override @AvailMethod
	@NotNull AvailObject o_ConcatenateTuplesCanDestroy (
		final @NotNull AvailObject object,
		final boolean canDestroy)
	{
		// Take a tuple of tuples and answer one big tuple constructed by
		// concatenating the subtuples together. Optimized so that the resulting
		// splice tuple's zones are not themselves splice tuples.

		int zones = 0;
		int newSize = 0;
		for (int i = 1, end = object.tupleSize(); i <= end; i++)
		{
			final AvailObject sub = object.tupleAt(i);
			final int subZones = sub.tupleSize() == 0
				? 0
				: sub.traversed().isSplice()
					? sub.numberOfZones()
					: 1;
			// Empty zones are not allowed in splice tuples.
			zones += subZones;
			newSize += sub.tupleSize();
		}
		if (newSize == 0)
		{
			return TupleDescriptor.empty();
		}
		// Now we know how many zones will be in the final tuple. We must
		// allocate room for it. In the Java translation, the garbage collector
		// will never increase the number of zones, so there will always be
		// room in the allocated chunk (maybe more than enough) for all the
		// zones.

		final AvailObject result =
			AvailObject.newObjectIndexedIntegerIndexedDescriptor(
				zones,
				zones * 2,
				SpliceTupleDescriptor.mutable());
		int majorIndex = 0;
		int zone = 1;
		for (int i = 1, end = object.tupleSize(); i <= end; i++)
		{
			final AvailObject sub = object.tupleAt(i).traversed();
			if (sub.isSplice())
			{
				assert sub.tupleSize() > 0;
				for (
					int originalZone = 1, end2 = sub.numberOfZones();
					originalZone <= end2;
					originalZone++)
				{
					majorIndex += sub.sizeOfZone(originalZone);
					result.forZoneSetSubtupleStartSubtupleIndexEndOfZone(
						zone,
						sub.subtupleForZone(originalZone),
						sub.startSubtupleIndexInZone(originalZone),
						majorIndex);
					if (canDestroy && sub.descriptor().isMutable())
					{
						sub.setSubtupleForZoneTo(
							originalZone,
							NullDescriptor.nullObject());
					}
					zone++;
				}
			}
			else
			{
				if (sub.tupleSize() != 0)
				{
					majorIndex += sub.tupleSize();
					result.forZoneSetSubtupleStartSubtupleIndexEndOfZone(
						zone,
						sub,
						1,
						majorIndex);
					zone++;
				}
				if (canDestroy && isMutable())
				{
					object.tupleAtPut(i, NullDescriptor.nullObject());
				}
			}
		}
		assert zone == zones + 1 : "Wrong number of zones";
		assert majorIndex == newSize : "Wrong resulting tuple size";
		result.hashOrZero(result.computeHashFromTo(1, majorIndex));
		if (canDestroy && isMutable)
		{
			object.assertObjectUnreachableIfMutable();
		}
		result.verify();
		return result;
	}

	@Override @AvailMethod
	@NotNull AvailObject o_CopyTupleFromToCanDestroy (
		final @NotNull AvailObject object,
		final int start,
		final int end,
		final boolean canDestroy)
	{
		// Make a tuple that only contains the given range of elements of the
		// given tuple.  Overridden in ObjectTupleDescriptor so that if
		// isMutable and canDestroy are true then the parts of the tuple outside
		// the subrange will have their reference counts decremented (i.e.,
		// destroyed if mutable) and those tuple slots will be set to top.

		assert 1 <= start && start <= end + 1;
		assert 0 <= end && end <= object.tupleSize();
		if (start - 1 == end)
		{
			if (isMutable && canDestroy)
			{
				object.assertObjectUnreachableIfMutable();
			}
			return TupleDescriptor.empty();
		}
		if (isMutable && canDestroy && (start == 1 || end - start < 20))
		{
			if (start != 1)
			{
				for (int i = 1; i <= end - start + 1; i++)
				{
					object.tupleAtPut(i, object.tupleAt(start + i - 1));
				}
			}
			object.truncateTo(end - start + 1);
			return object;
		}
		final AvailObject result =
			AvailObject.newObjectIndexedIntegerIndexedDescriptor(
				1,
				2,
				SpliceTupleDescriptor.mutable());
		result.hashOrZero(object.computeHashFromTo(start, end));
		result.forZoneSetSubtupleStartSubtupleIndexEndOfZone(
			1,
			object,
			start,
			end - start + 1);
		result.verify();
		return result;
	}

	@Override @AvailMethod
	byte o_ExtractNybbleFromTupleAt (
		final @NotNull AvailObject object,
		final int index)
	{
		// Get the element at the given index in the tuple object, and extract a
		// nybble from it.  Fail if it's not a nybble. Obviously overridden for
		// speed in NybbleTupleDescriptor.

		final int nyb = object.tupleIntAt(index);
		if (!(nyb >= 0 && nyb <= 15))
		{
			error("nybble is out of range", object);
			return 0;
		}
		return (byte) nyb;
	}

	@Override @AvailMethod
	int o_HashFromTo (
		final @NotNull AvailObject object,
		final int startIndex,
		final int endIndex)
	{
		// Compute object's hash value over the given range.

		if (startIndex == 1 && endIndex == object.tupleSize())
		{
			return object.hash();
		}
		return object.computeHashFromTo(startIndex, endIndex);
	}

	@Override @AvailMethod
	@NotNull AvailObject o_TupleAt (final AvailObject object, final int index)
	{
		// Answer the element at the given index in the tuple object.

		error(
			"Subclass responsibility: Object:tupleAt: in Avail.TupleDescriptor",
			object);
		return NullDescriptor.nullObject();
	}

	@Override @AvailMethod
	@NotNull AvailObject o_TupleAtPuttingCanDestroy (
		final @NotNull AvailObject object,
		final int index,
		final AvailObject newValueObject,
		final boolean canDestroy)
	{
		// Answer a tuple with all the elements of object, except at the given
		// index we should have newValueObject. This may destroy the original
		// tuple if canDestroy is true.

		error(
			"Subclass responsibility: Object:tupleAt:putting:canDestroy: in Avail.TupleDescriptor",
			object);
		return NullDescriptor.nullObject();
	}

	@Override @AvailMethod
	int o_TupleIntAt (final AvailObject object, final int index)
	{
		// Answer the integer element at the given index in the tuple object.

		error(
			"Subclass responsibility: Object:tupleIntAt: in Avail.TupleDescriptor",
			object);
		return 0;
	}

	@Override @AvailMethod
	@NotNull AvailObject o_AsSet (final AvailObject object)
	{
		// Convert the tuple to a set.

		AvailObject result = SetDescriptor.empty();
		for (int i = 1, end = object.tupleSize(); i <= end; i++)
		{
			result = result.setWithElementCanDestroy(object.tupleAt(i), true);
		}
		return result;
	}

	@Override @AvailMethod
	boolean o_IsTuple (final AvailObject object)
	{
		return true;
	}

	@Override @AvailMethod
	boolean o_IsByteTuple (final AvailObject object)
	{
		for (int i = object.tupleSize(); i >= 1; i--)
		{
			if (!object.tupleAt(i).isByte())
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
	 */
	@Override @AvailMethod
	boolean o_IsString (final @NotNull AvailObject object)
	{
		for (int i = object.tupleSize(); i >= 1; i--)
		{
			if (!object.tupleAt(i).isCharacter())
			{
				return false;
			}
		}

		return true;
	}

	@Override @AvailMethod
	int o_TupleSize (final AvailObject object)
	{
		// Answer the number of elements in the object (as a Smalltalk Integer).

		error(
			"Subclass responsibility: o_TupleSize: in Avail.TupleDescriptor",
			object);
		return 0;
	}

	@Override @AvailMethod
	boolean o_IsSplice (final AvailObject object)
	{
		return false;
	}

	/**
	 * Compute the object's hash value.
	 *
	 * @param object The object to hash.
	 * @return The hash value.
	 */
	int computeHashForObject (final AvailObject object)
	{
		return object.computeHashFromTo(1, object.tupleSize());
	}

	@Override @AvailMethod
	int o_ComputeHashFromTo (
		final @NotNull AvailObject object,
		final int start,
		final int end)
	{
		// Compute the hash value from the object's data. The result should be
		// a Smalltalk Integer between 16r00000000 and 16rFFFFFFFF inclusive.
		// To keep the rehashing cost down for concatenated tuples, we use a
		// non-commutative hash function. If the tuple has elements with hash
		// values h[1] through h[n], we use the formula...
		// H=h[1]a^1 + h[2]a^2 + h[3]a^3... + h[n]a^n
		// This can be rewritten as sum(i=1..n)(a^i * h[i]). The constant 'a' is
		// chosen as a primitive element of (Z[2^32],*), specifically 1664525,
		// as taken from Knuth, The Art of Computer Programming, Vol. 2, 2nd
		// ed., page 102, row 26. See also pages 19, 20, theorems B and C. The
		// period of this cycle is 2^30. The element hash values are xored with
		// a random constant (16r9EE570A6) before being used, to help prevent
		// similar nested tuples from producing equal hashes.

		int hash = 0;
		for (int index = end; index >= start; index--)
		{
			final int itemHash = object.tupleAt(index).hash() ^ PreToggle;
			hash = hash * Multiplier + itemHash;
		}
		return hash * Multiplier;
	}

	@Override @AvailMethod
	String o_AsNativeString (
		final @NotNull AvailObject object)
	{
		// Only applicable to tuples that contain characters.

		final int size = object.tupleSize();
		final StringBuilder builder = new StringBuilder(size);
		for (int i = 1; i <= size; i++)
		{
			builder.appendCodePoint(object.tupleAt(i).codePoint());
		}
		return builder.toString();
	}

	/**
	 * Answer a mutable copy of object that holds arbitrary objects.
	 */
	@Override @AvailMethod
	@NotNull AvailObject o_CopyAsMutableObjectTuple (
		final @NotNull AvailObject object)
	{
		final int size = object.tupleSize();
		final AvailObject result = ObjectTupleDescriptor.mutable().create(size);
		result.hashOrZero(object.hashOrZero());
		for (int i = 1; i <= size; i++)
		{
			result.tupleAtPut(i, object.tupleAt(i));
		}
		return result;
	}

	/**
	 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
	 */
	@Override
	public @NotNull
	Iterator<AvailObject> o_Iterator (
		final @NotNull AvailObject object)
	{
		final AvailObject selfSnapshot =
			isMutable
				? object.copyAsMutableObjectTuple()
				: object;
		final int size = selfSnapshot.tupleSize();
		return new Iterator<AvailObject>()
		{
			/**
			 * The index of the next {@linkplain AvailObject element}.
			 */
			int index = 1;

			@Override
			public boolean hasNext ()
			{
				return index <= size;
			}

			@Override
			public @NotNull AvailObject next ()
			{
				if (index > size)
				{
					throw new NoSuchElementException();
				}

				return selfSnapshot.tupleAt(index++);
			}

			@Override
			public void remove ()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * The empty tuple.
	 */
	static AvailObject EmptyTuple;

	/**
	 * Create my cached empty tuple and various well known strings.
	 */
	static void createWellKnownObjects ()
	{
		EmptyTuple = NybbleTupleDescriptor.mutableObjectOfSize(0);
		EmptyTuple.hashOrZero(0);
		EmptyTuple.makeImmutable();
}

	/**
	 * Clear my cached empty tuple and various well known strings.
	 */
	static void clearWellKnownObjects ()
	{
		EmptyTuple = null;
	}

	/**
	 * Create a tuple with the specified elements.  The elements are not made
	 * immutable first, nor is the new tuple.
	 *
	 * @param elements
	 *            The array of AvailObjects from which to construct a tuple.
	 * @return
	 *            The new mutable tuple.
	 */
	public static AvailObject from (
		final AvailObject ... elements)
	{
		AvailObject tuple;
		final int size = elements.length;
		tuple = ObjectTupleDescriptor.mutable().create(size);
		for (int i = 1; i <= size; i++)
		{
			tuple.tupleAtPut(i, elements[i - 1]);
		}
		return tuple;
	}

	/**
	 * Construct a new tuple of arbitrary {@linkplain AvailObject Avail objects}
	 * passed in a list.  The elements are not made immutable first, nor is the
	 * new tuple.
	 *
	 * @param collection
	 *        The list of {@linkplain AvailObject Avail objects} from which
	 *        to construct a tuple.
	 * @return The new mutable tuple of objects.
	 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
	 */
	public static AvailObject fromCollection (
		final List<AvailObject> collection)
	{
		final AvailObject tuple = ObjectTupleDescriptor.mutable().create(
			collection.size());
		int i = 1;
		for (final AvailObject element : collection)
		{
			tuple.tupleAtPut(i, element);
			i++;
		}
		return tuple;
	}

	/**
	 * Construct a new tuple of arbitrary {@linkplain AvailObject Avail objects}
	 * based on the given tuple, but with an additional element appended.  The
	 * elements may end up being shared between the original and the copy, so
	 * the client must ensure that either the elements are marked immutable, or
	 * one of the copies is not kept after the call.
	 *
	 * @param originalTuple
	 *            The original tuple of {@linkplain AvailObject Avail objects}
	 *            on which to base the new tuple.
	 * @param newElement
	 *            The new element that should be at the end of the new tuple.
	 * @return
	 *            The new mutable tuple of objects including all elements of the
	 *            passed tuple plus the new element.
	 */
	public static @NotNull AvailObject append (
		final @NotNull AvailObject originalTuple,
		final @NotNull AvailObject newElement)
	{
		final int originalSize = originalTuple.tupleSize();
		final AvailObject newTuple = ObjectTupleDescriptor.mutable().create(
			originalSize + 1);
		for (int i = 1; i <= originalSize; i++)
		{
			newTuple.tupleAtPut(i, originalTuple.tupleAt(i));
		}
		newTuple.tupleAtPut(originalSize + 1, newElement);
		return newTuple;
	}

	/**
	 * Construct a new tuple of arbitrary {@linkplain AvailObject Avail objects}
	 * based on the given tuple, but with an occurrence of the specified element
	 * missing, if it was present at all.  The elements may end up being shared
	 * between the original and the copy, so the client must ensure that either
	 * the elements are marked immutable, or one of the copies is not kept after
	 * the call.  If the element is not found, then answer the original tuple.
	 *
	 * @param originalTuple
	 *            The original tuple of {@linkplain AvailObject Avail objects}
	 *            on which to base the new tuple.
	 * @param elementToExclude
	 *            The element that should should have an occurrence excluded
	 *            from the new tuple, if it was present.
	 * @return
	 *            The new tuple.
	 */
	public static @NotNull AvailObject without (
		final @NotNull AvailObject originalTuple,
		final @NotNull AvailObject elementToExclude)
	{
		final int originalSize = originalTuple.tupleSize();
		for (int seekIndex = 1; seekIndex <= originalSize; seekIndex++)
		{
			if (originalTuple.tupleAt(seekIndex).equals(elementToExclude))
			{
				final AvailObject newTuple =
					ObjectTupleDescriptor.mutable().create(originalSize - 1);
				for (int i = 1; i < seekIndex; i++)
				{
					newTuple.tupleAtPut(i, originalTuple.tupleAt(i));
				}
				for (int i = seekIndex + 1; i <= originalSize; i++)
				{
					newTuple.tupleAtPut(i - 1, originalTuple.tupleAt(i));
				}
				return newTuple;
			}
		}
		return originalTuple;
	}

	/**
	 * Construct a new tuple of integers.  Use the most compact representation
	 * that can still represent each supplied {@link Integer}.
	 *
	 * @param list
	 *            The list of Java {@linkplain Integer}s to assemble in a tuple.
	 * @return
	 *            A new mutable tuple of integers.
	 */
	public static AvailObject fromIntegerList (
		final List<Integer> list)
	{
		AvailObject tuple;
		final int maxValue = list.size() == 0 ? 0 : max(list);
		if (maxValue <= 15)
		{
			tuple = NybbleTupleDescriptor.mutableObjectOfSize(list.size());
			for (int i = 1; i <= list.size(); i++)
			{
				tuple.rawNybbleAtPut(i, list.get(i - 1).byteValue());
			}
		}
		else if (maxValue <= 255)
		{
			tuple = ByteTupleDescriptor.mutableObjectOfSize(list.size());
			for (int i = 1; i <= list.size(); i++)
			{
				tuple.rawByteAtPut(i, list.get(i - 1).shortValue());
			}
		}
		else
		{
			tuple = ObjectTupleDescriptor.mutable().create(list.size());
			for (int i = 1; i <= list.size(); i++)
			{
				tuple.tupleAtPut(
					i,
					IntegerDescriptor.fromInt(list.get(i - 1).intValue()));
			}
		}
		return tuple;
	}

	/**
	 * Compute {@link #Multiplier} raised to the specified power, truncated to
	 * an int.
	 *
	 * @param anInteger
	 *            The exponent by which to raise the base {@link #Multiplier}.
	 * @return
	 *            {@link #Multiplier} raised to the specified power.
	 */
	static int multiplierRaisedTo (final int anInteger)
	{
		int result = 1;
		int power = Multiplier;
		int residue = anInteger;
		while (residue != 0)
		{
			if ((residue & 1) != 0)
			{
				result *= power;
			}
			power *= power;
			residue >>>= 1;
		}
		return result;
	}


	/**
	 * Return the empty {@linkplain TupleDescriptor tuple}.  Other empty tuples
	 * can be created, but if you know the tuple is empty you can save time and
	 * space by returning this one.
	 *
	 * @return The tuple of size zero.
	 */
	public static AvailObject empty ()
	{
		return EmptyTuple;
	}

	/**
	 * The constant by which each element's hash should be XORed prior to
	 * combining them.  This reduces the chance of systematic collisions due to
	 * using the same elements in different patterns of nested tuples.
	 */
	static final int PreToggle = 0x71E570A6;

	/**
	 * Construct a new {@link TupleDescriptor}.
	 *
	 * @param isMutable
	 *            Does the {@linkplain Descriptor descriptor} represent a
	 *            mutable object?
	 */
	protected TupleDescriptor (final boolean isMutable)
	{
		super(isMutable);
	}
}
