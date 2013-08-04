/**
 * IntegerIntervalTupleDescriptor.java
 * Copyright © 1993-2013, Mark van Gulik and Todd L Smith.
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

import static com.avail.descriptor.IntegerIntervalTupleDescriptor.IntegerSlots.*;
import static com.avail.descriptor.IntegerIntervalTupleDescriptor.ObjectSlots.*;
import java.util.ArrayList;
import java.util.List;
import com.avail.annotations.AvailMethod;
import com.avail.annotations.HideFieldInDebugger;

/**
 * {@code IntegerIntervalTupleDescriptor} represents an ordered tuple of
 * integers that each differ from their predecessor by DELTA, an integer value.
 *
 * @author Leslie Schultz &lt;leslie@availlang.org&gt;
 */
public class IntegerIntervalTupleDescriptor
extends TupleDescriptor
{
	/**
	 * The layout of integer slots for my instances.
	 */
	public enum IntegerSlots
	implements IntegerSlotsEnum
	{
		/**
		 * A slot to hold the cached hash value of a tuple. If zero, then the
		 * hash value must be computed upon request. Note that in the very rare
		 * case that the hash value actually equals zero, the hash value has to
		 * be computed every time it is requested.
		 */
		@HideFieldInDebugger
		HASH_OR_ZERO,

		/**
		 * The number of elements in the tuple.
		 *
		 * The API's {@link AvailObject#tupleSize() tuple size accessor}
		 * currently returns a Java integer, because there wasn't much of a
		 * problem limiting manually-constructed tuples to two billion elements.
		 * This restriction will eventually be removed.
		 */
		SIZE;

		static
		{
			assert TupleDescriptor.IntegerSlots.HASH_OR_ZERO.ordinal()
				== HASH_OR_ZERO.ordinal();
		}
	}

	/**
	 * The layout of object slots for my instances.
	 */
	public enum ObjectSlots
	implements ObjectSlotsEnum
	{
		/** The first value in the tuple, inclusive. */
		START,

		/**
		 * The last value in the tuple, inclusive. Within the constructor,
		 * the supplied END is normalized to the actual last value.
		 */
		END,

		/**
		 * The difference between a value and its subsequent neighbor in the
		 * tuple.
		 */
		DELTA
	}

	/**
	 * The minimum size for integer interval tuple creation. All tuples
	 * requested below this size will be created as standard tuples or the empty
	 * tuple.
	 */
	private static int minimumSize = 4;

	@Override @AvailMethod
	public boolean o_IsIntegerIntervalTuple(final AvailObject object)
	{
		return true;
	}

	@Override @AvailMethod
	A_Tuple o_CopyTupleFromToCanDestroy (
		final AvailObject object,
		final int start,
		final int end,
		final boolean canDestroy)
	{
		// Ensure parameters are in bounds
		assert 1 <= start && start <= end + 1;
		final int oldSize = object.slot(SIZE);
		final int newSize = end - start + 1;
		assert 0 <= end && end <= oldSize;

		// If the requested copy is actually a subrange, make it.
		if (newSize != oldSize)
		{
			final AvailObject traversed = object.traversed();
			final A_Number delta = traversed.slot(DELTA);

			// Calculate the new start value from the given start index.
			A_Number newStartValue;
			final A_Number oldStartValue = traversed.slot(START);
			if (start == 1)
			{
				newStartValue = oldStartValue;
			}
			else
			{
				final A_Number newStartIndex =
					IntegerDescriptor.fromInt(start);
				newStartValue = oldStartValue.plusCanDestroy(
					delta.timesCanDestroy(newStartIndex, false),
					false);
			}

			// Calculate the new end value from the given end index.
			A_Number newEndValue;
			final A_Number oldEndValue = traversed.slot(END);
			if (end == oldSize)
			{
				newEndValue = oldEndValue;
			}
			else
			{
				final A_Number newEndIndex = IntegerDescriptor.fromInt(end);
				newEndValue = oldStartValue.plusCanDestroy(
					delta.timesCanDestroy(newEndIndex, false),
					false);
			}

			// Create the new tuple, set its size, and return it.
			if (isMutable() && canDestroy)
			{
				// Reuse object's slots
				if (oldStartValue != newStartValue)
				{
					object.setSlot(START, newStartValue);
				}
				if (oldEndValue != newEndValue)
				{
					// Don't need to worry about normalizing since this was
					// calculated from an index.
					object.setSlot(END, newEndValue);
				}
				// Step will be the same in a copy within the source
				return object;
			}
			final AvailObject copy =
				(AvailObject) createInterval(newStartValue, newEndValue, delta);
			copy.setSlot(SIZE, newSize);
			return copy;
		}

		// Otherwise, this method is requesting a full copy of the original.
		if (isMutable() && !canDestroy)
		{
			object.makeImmutable();
		}
		return object;
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithStartingAt (
		final AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final A_Tuple anotherObject,
		final int startIndex2)
	{
		return anotherObject.compareFromToWithIntegerIntervalTupleStartingAt(
			startIndex2,
			startIndex2 + endIndex1 - startIndex1,
			object,
			startIndex1);
	}

	@Override @AvailMethod
	boolean o_CompareFromToWithIntegerIntervalTupleStartingAt (
		final AvailObject object,
		final int startIndex1,
		final int endIndex1,
		final A_Tuple anIntegerIntervalTuple,
		final int startIndex2)
	{
		// If the objects refer to the same memory, and the indices match
		// up, the subranges are the same.
		if (object.sameAddressAs(anIntegerIntervalTuple) &&
			startIndex1 == startIndex2)
		{
			return true;
		}

		// If the objects do not refer to the same memory but the tuples are
		// identical,
		if (object.equals(anIntegerIntervalTuple))
		{
			// indirect one to the other if it is not shared.
			if (!isShared())
			{
				anIntegerIntervalTuple.makeImmutable();
				object.becomeIndirectionTo(anIntegerIntervalTuple);
			}
			else if (!anIntegerIntervalTuple.descriptor().isShared())
			{
				object.makeImmutable();
				anIntegerIntervalTuple.becomeIndirectionTo(object);
			}

			// If the subranges start at the same place, they are the same.
			if (startIndex1 == startIndex2)
			{
				return true;
			}
			return false;
		}

		// Finally, check the subranges.
		final A_Tuple first = object.copyTupleFromToCanDestroy(
			startIndex1,
			endIndex1,
			false);
		final A_Tuple second = anIntegerIntervalTuple.copyTupleFromToCanDestroy(
			startIndex2,
			startIndex2 + endIndex1 - startIndex1,
			false);
		return first.equals(second);
	}


	@Override
	A_Tuple o_ConcatenateWith (
		final AvailObject object,
		final A_Tuple otherTuple,
		final boolean canDestroy)
	{
		if (!canDestroy)
		{
			object.makeImmutable();
			otherTuple.makeImmutable();
		}

		// Assess the possibility that the concatenation will still be an
		// integer interval tuple.
		if (otherTuple.isIntegerIntervalTuple())
		{
			final AvailObject otherDirect = otherTuple.traversed();
			final AvailObject delta = object.slot(DELTA);

			// If the other's delta is the same as mine,
			if (delta.equals(otherDirect.slot(DELTA)))
			{
				// and the other's start is one delta away from my end,
				if (object.slot(END).plusCanDestroy(delta, false)
					.equals(otherDirect.slot(START)))
				{
					// then we're adjacent.
					final int newSize = object.slot(SIZE) +
						otherDirect.slot(SIZE);

					// If we can do replacement in place,
					// use me for the return value.
					if (isMutable())
					{
						object.setSlot(END, otherDirect.slot(END));
						object.setSlot(SIZE, newSize);
						object.hashOrZero(0);
						return object;
					}
					// Or the other one.
					if (otherTuple.descriptor().isMutable())
					{
						otherDirect.setSlot(START, object.slot(START));
						otherDirect.setSlot(SIZE, newSize);
						otherDirect.hashOrZero(0);
						return otherDirect;
					}

					// Otherwise, create a new interval.
					return createInterval(
						object.slot(START),
						otherDirect.slot(END),
						delta);
				}
			}
		}
		if (otherTuple.treeTupleLevel() == 0)
		{
			return TreeTupleDescriptor.createPair(object, otherTuple, 1, 0);
		}
		return TreeTupleDescriptor.concatenateAtLeastOneTree(
			object,
			otherTuple,
			true);
	}

	@Override @AvailMethod
	boolean o_Equals (final AvailObject object, final A_BasicObject another)
	{
		return another.equalsIntegerIntervalTuple(object);
	}

	@Override @AvailMethod
	boolean o_EqualsIntegerIntervalTuple (
		final AvailObject object,
		final A_Tuple anIntegerIntervalTuple)
	{
		// First, check for object-structure (address) identity.
		if (object.sameAddressAs(anIntegerIntervalTuple))
		{
			return true;
		}

		// If the objects do not refer to the same memory, check if the tuples
		// are identical.
		final AvailObject firstTraversed = object.traversed();
		final AvailObject secondTraversed = anIntegerIntervalTuple.traversed();

		// Check that the slots match.
		final int firstHash = firstTraversed.slot(HASH_OR_ZERO);
		final int secondHash = secondTraversed.slot(HASH_OR_ZERO);
		if (firstHash != 0 && secondHash != 0 && firstHash != secondHash)
		{
			return false;
		}
		// Since we have SIZE as int, it's cheaper to check it than END.
		if (firstTraversed.slot(SIZE) != secondTraversed.slot(SIZE))
		{
			return false;
		}
		if (!firstTraversed.slot(DELTA).equals(secondTraversed.slot(DELTA)))
		{
			return false;
		}
		if (!firstTraversed.slot(START).equals(secondTraversed.slot(START)))
		{
			return false;
		}

		// All the slots match. Indirect one to the other if it is not shared.
		if (!isShared())
		{
			anIntegerIntervalTuple.makeImmutable();
			object.becomeIndirectionTo(anIntegerIntervalTuple);
		}
		else if (!anIntegerIntervalTuple.descriptor().isShared())
		{
			object.makeImmutable();
			anIntegerIntervalTuple.becomeIndirectionTo(object);
		}
		return true;

	}

	@Override
	int o_BitsPerEntry (final AvailObject object)
	{
		// Consider a billion element tuple. Since an interval tuple requires
		// only O(1) storage, irrespective of its size, the average bits per
		// entry is 0.
		return 0;
	}

	@Override @AvailMethod
	AvailObject o_TupleAt (final AvailObject object, final int index)
	{
		// Answer the value at the given index in the tuple object.
		// START + (index-1) × DELTA
		assert index >= 1 && index <= object.tupleSize();
		A_Number temp = IntegerDescriptor.fromInt(index - 1);
		temp = temp.timesCanDestroy(object.slot(DELTA), false);
		temp = temp.plusCanDestroy(object.slot(START), false);
		return (AvailObject) temp;
	}

	@Override @AvailMethod
	A_Tuple o_TupleAtPuttingCanDestroy (
		final AvailObject object,
		final int index,
		final A_BasicObject newValueObject,
		final boolean canDestroy)
	{
		// Answer a tuple with all the elements of object except at the given
		// index we should have newValueObject. This may destroy the original
		// tuple if canDestroy is true.
		assert index >= 1 && index <= object.tupleSize();
		if (!canDestroy || !isMutable())
		{
			/* TODO: [LAS] Later - Create nybble or byte tuples if appropriate. */
			return object.copyAsMutableObjectTuple().tupleAtPuttingCanDestroy(
				index,
				newValueObject,
				true);
		}
		object.objectTupleAtPut(index, newValueObject);
		// Invalidate the hash value.
		object.hashOrZero(0);
		return object;
	}

	@Override @AvailMethod
	int o_TupleIntAt (final AvailObject object, final int index)
	{
		// Answer the value at the given index in the tuple object.
		return object.tupleAt(index).extractInt();
	}

	@Override @AvailMethod
	int o_TupleSize (final AvailObject object)
	{
		return object.slot(SIZE);
	}

	/** The mutable {@link IntegerIntervalTupleDescriptor}. */
	public static final IntegerIntervalTupleDescriptor mutable =
		new IntegerIntervalTupleDescriptor(Mutability.MUTABLE);

	@Override
	IntegerIntervalTupleDescriptor mutable ()
	{
		return mutable;
	}

	/** The immutable {@link IntegerIntervalTupleDescriptor}. */
	private static final IntegerIntervalTupleDescriptor immutable =
		new IntegerIntervalTupleDescriptor(Mutability.IMMUTABLE);

	@Override
	IntegerIntervalTupleDescriptor immutable ()
	{
		return immutable;
	}

	/** The shared {@link IntegerIntervalTupleDescriptor}. */
	private static final IntegerIntervalTupleDescriptor shared =
		new IntegerIntervalTupleDescriptor(Mutability.SHARED);

	@Override
	IntegerIntervalTupleDescriptor shared ()
	{
		return shared;
	}

	/**
	 * Construct a new {@link IntegerIntervalTupleDescriptor}.
	 *
	 * @param mutability
	 */
	public IntegerIntervalTupleDescriptor (final Mutability mutability)
	{
		super(mutability);
	}

	/**
	 * Create a new interval according to the parameters.
	 *
	 * @param start The first integer in the interval.
	 * @param end The last allowable integer in the interval.
	 * @param delta The difference between an integer and its subsequent
	 *              neighbor in the interval. Delta is nonzero.
	 * @return The new interval.
	 */
	public static A_Tuple createInterval (
		final A_Number start,
		final A_Number end,
		final A_Number delta)
	{
		assert !delta.equals(IntegerDescriptor.zero());

		final A_Number difference = end.minusCanDestroy(start, false);
		final A_Number zero = IntegerDescriptor.zero();

		// If there is only one member in the range, return that integer in
		// its own tuple.
		if (difference.equals(zero))
		{
			return TupleDescriptor.from(start);
		}

		// If the progression is in a different direction than the delta, there
		// are no members of this interval, so return the empty tuple.
		if (difference.greaterThan(zero) != delta.greaterThan(zero))
		{
			return TupleDescriptor.empty();
		}

		// If there are fewer than minimumSize members in this interval, create
		// a normal tuple with them in it instead of an interval tuple.
		final int size;
//		// TODO: [LAS] Remove when Mark fixes -a/-b rounding towards +∞.
//		if (difference.lessThan(zero))
//		{
//			final A_Number negativeOne = IntegerDescriptor.fromInt(-1);
//			final A_Number tempDifference =
//				difference.timesCanDestroy(negativeOne, false);
//			final A_Number tempDelta =
//				delta.timesCanDestroy(negativeOne, false);
//			size = 1 +
//				tempDifference.divideCanDestroy(tempDelta, false).extractInt();
//		}
//		else
//		{

			size = 1 + difference.divideCanDestroy(delta, false).extractInt();
//		}
		if (size < minimumSize)
		{
			final List<A_Number> members = new ArrayList<A_Number>(size);
			A_Number newMember = start;
			for (int i = 0; i < size; i++)
			{
				members.add(newMember);
				newMember = newMember.addToIntegerCanDestroy(delta, false);
			}
			return TupleDescriptor.fromList(members);
		}

		// If the slot contents are small enough, create a
		// SmallIntegerIntervalTuple.
		if (SmallIntegerIntervalTupleDescriptor.isCandidate(
			start, end, delta))
		{
			return SmallIntegerIntervalTupleDescriptor.createInterval(
				start.extractInt(),
				end.extractInt(),
				delta.extractInt());
		}

		// No other efficiency shortcuts. Normalize end, and create a range.
		// overshot = (end - start) mod delta
		final A_Number overshot = difference.minusCanDestroy(
			delta.timesCanDestroy(
				difference.divideCanDestroy(delta, false), false), false);
		final A_Number normalizedEnd = end.minusCanDestroy(overshot, false);
		return forceCreate(start, normalizedEnd, delta, size);
	}

	/**
	 * Create a new IntegerIntervalTuple using the supplied arguments,
	 * regardless of the suitability of other representations.
	 *
	 * @param start The first integer in the interval.
	 * @param normalizedEnd The last integer in the interval.
	 * @param delta The difference between an integer and its subsequent
	 *              neighbor in the interval. Delta is nonzero.
	 * @param size The size of the interval, in number of elements.
	 * @return The new interval.
	 */
	static A_Tuple forceCreate (
		final A_Number start,
		final A_Number normalizedEnd,
		final A_Number delta,
		final int size)
	{
		final AvailObject interval = mutable.create();
		interval.setSlot(HASH_OR_ZERO, 0);
		interval.setSlot(START, start.makeImmutable());
		interval.setSlot(END, normalizedEnd.makeImmutable());
		interval.setSlot(DELTA, delta.makeImmutable());
		interval.setSlot(SIZE, size);
		return interval;
	}
}
