/**
 * descriptor/NybbleTupleDescriptor.java
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
import com.avail.descriptor.ByteTupleDescriptor;
import com.avail.descriptor.IntegerDescriptor;
import com.avail.descriptor.IntegerRangeTypeDescriptor;
import com.avail.descriptor.NybbleTupleDescriptor;
import com.avail.descriptor.TupleDescriptor;
import com.avail.descriptor.TypeDescriptor.Types;
import com.avail.descriptor.VoidDescriptor;
import java.util.List;
import static com.avail.descriptor.AvailObject.*;
import static java.lang.Math.*;

@IntegerSlots({
	"hashOrZero", 
	"rawQuadAt#"
})
public class NybbleTupleDescriptor extends TupleDescriptor
{
	int unusedNybblesOfLastWord;


	// GENERATED accessors

	int ObjectRawQuadAt (
			final AvailObject object, 
			final int index)
	{
		//  GENERATED getter method (indexed).

		return object.integerSlotAtByteIndex(((index * 4) + 4));
	}

	void ObjectRawQuadAtPut (
			final AvailObject object, 
			final int index, 
			final int value)
	{
		//  GENERATED setter method (indexed).

		object.integerSlotAtByteIndexPut(((index * 4) + 4), value);
	}



	// java printing

	void printObjectOnAvoidingIndent (
			final AvailObject object, 
			final StringBuilder aStream, 
			final List<AvailObject> recursionList, 
			final int indent)
	{
		if ((object.tupleSize() == 0))
		{
			aStream.append("<>");
			return;
		}
		if (isMutable())
		{
			aStream.append("(mut)");
		}
		aStream.append("NybbleTuple with: #[");
		int rowSize = max ((60 - ((indent * 3) / 2)), 8);
		//  How many equal (shorter by at least 1 on the last) rows are needed?
		final int rows = ((object.tupleSize() + rowSize) / rowSize);
		//  How many on each row for that layout?
		rowSize = ((object.tupleSize() + rows) / rows);
		//  Round up to a multiple of eight per row.
		rowSize = (((rowSize + 7) / 8) * 8);
		int rowStart = 1;
		while ((rowStart <= object.tupleSize())) {
			aStream.append('\n');
			for (int _count1 = 1; _count1 <= indent; _count1++)
			{
				aStream.append('\t');
			}
			for (int i = rowStart, _end2 = min (((rowStart + rowSize) - 1), object.tupleSize()); i <= _end2; i++)
			{
				final byte val = object.extractNybbleFromTupleAt(i);
				assert 0 <= val && val <= 15;
				aStream.append(Integer.toHexString(val));
				if (((i % 8) == 0))
				{
					aStream.append(' ');
				}
			}
			rowStart += rowSize;
		}
		aStream.append(']');
	}



	// operations

	boolean ObjectCompareFromToWithStartingAt (
			final AvailObject object, 
			final int startIndex1, 
			final int endIndex1, 
			final AvailObject anotherObject, 
			final int startIndex2)
	{
		//  Compare sections of two tuples.

		return anotherObject.compareFromToWithNybbleTupleStartingAt(
			startIndex2,
			((startIndex2 + endIndex1) - startIndex1),
			object,
			startIndex1);
	}

	boolean ObjectCompareFromToWithNybbleTupleStartingAt (
			final AvailObject object, 
			final int startIndex1, 
			final int endIndex1, 
			final AvailObject aNybbleTuple, 
			final int startIndex2)
	{
		//  Compare sections of two nybble tuples.

		if ((object.sameAddressAs(aNybbleTuple) && (startIndex1 == startIndex2)))
		{
			return true;
		}
		if ((endIndex1 < startIndex1))
		{
			return true;
		}
		//  Compare actual nybbles.
		int index2 = startIndex2;
		for (int i = startIndex1; i <= endIndex1; i++)
		{
			if (! (object.rawNybbleAt(i) == aNybbleTuple.rawNybbleAt(index2)))
			{
				return false;
			}
			++index2;
		}
		return true;
	}

	boolean ObjectEquals (
			final AvailObject object, 
			final AvailObject another)
	{
		return another.equalsNybbleTuple(object);
	}

	boolean ObjectEqualsNybbleTuple (
			final AvailObject object, 
			final AvailObject aNybbleTuple)
	{
		//  First, check for object-structure (address) identity.

		if (object.sameAddressAs(aNybbleTuple))
		{
			return true;
		}
		if (! (object.tupleSize() == aNybbleTuple.tupleSize()))
		{
			return false;
		}
		if (! (object.hash() == aNybbleTuple.hash()))
		{
			return false;
		}
		if (! object.compareFromToWithNybbleTupleStartingAt(
			1,
			object.tupleSize(),
			aNybbleTuple,
			1))
		{
			return false;
		}
		//  They're equal (but occupy disjoint storage).  Replace one with an indirection to the other
		//  to reduce storage costs and the frequency of nybble-wise comparisons.
		object.becomeIndirectionTo(aNybbleTuple);
		aNybbleTuple.makeImmutable();
		//  Now that there are at least two references to it
		return true;
	}

	boolean ObjectIsBetterRepresentationThan (
			final AvailObject object, 
			final AvailObject anotherObject)
	{
		//  Given two objects that are known to be equal, is the first one in a better form (more
		//  compact, more efficient, older generation) than the second one?

		//  Currently there is no more desirable representation than a nybble tuple
		return true;
	}

	boolean ObjectIsInstanceOfSubtypeOf (
			final AvailObject object, 
			final AvailObject aType)
	{
		//  Answer whether object is an instance of a subtype of aType.  Don't generate
		//  an approximate type and do the comparison, because the approximate type
		//  will just send this message recursively.

		if (aType.equals(Types.voidType.object()))
		{
			return true;
		}
		if (aType.equals(Types.all.object()))
		{
			return true;
		}
		if (! aType.isTupleType())
		{
			return false;
		}
		//  See if it's an acceptable size...
		final AvailObject size = IntegerDescriptor.objectFromInt(object.tupleSize());
		if (! size.isInstanceOfSubtypeOf(aType.sizeRange()))
		{
			return false;
		}
		//  tuple's size is out of range.
		final AvailObject typeTuple = aType.typeTuple();
		final int breakIndex = min (object.tupleSize(), typeTuple.tupleSize());
		for (int i = 1; i <= breakIndex; i++)
		{
			if (! object.tupleAt(i).isInstanceOfSubtypeOf(aType.typeAtIndex(i)))
			{
				return false;
			}
		}
		final AvailObject defaultTypeObject = aType.defaultType();
		if (IntegerRangeTypeDescriptor.nybbles().isSubtypeOf(defaultTypeObject))
		{
			return true;
		}
		for (int i = (breakIndex + 1), _end1 = object.tupleSize(); i <= _end1; i++)
		{
			if (! object.tupleAt(i).isInstanceOfSubtypeOf(defaultTypeObject))
			{
				return false;
			}
		}
		return true;
	}

	void ObjectRawNybbleAtPut (
			final AvailObject object, 
			final int index, 
			final byte aNybble)
	{
		//  Set the nybble at the given index.  Use big Endian.

		int byteIndex = numberOfFixedIntegerSlots * 4 + (index + 7) / 2;
		int leftShift = (index & 1) * 4;
		short theByte = object.byteSlotAtByteIndex(byteIndex);
		theByte = (short)((theByte & ~(15 << leftShift)) + (aNybble << leftShift));
		object.byteSlotAtByteIndexPut(byteIndex, theByte);
	}

	AvailObject ObjectMakeImmutable (
			final AvailObject object)
	{
		//  Make the object immutable so it can be shared safely.

		if (isMutable)
		{
			object.descriptor(NybbleTupleDescriptor.isMutableSize(false, object.tupleSize()));
			object.makeSubobjectsImmutable();
		}
		return object;
	}



	// operations-tuples

	byte ObjectExtractNybbleFromTupleAt (
			final AvailObject object, 
			final int index)
	{
		//  Get the element at the given index in the tuple object, and extract a nybble from it.
		//  Fail if it's not a nybble.
		//
		//  WARNING - optimized to take header information into account for fast access.

		int byteIndex = (index + 15) / 2;      // 15 = self numberOfFixedSlots * 4 * 2 + 7
		return (byte)((object.byteSlotAtByteIndex(byteIndex) >> ((index & 1) * 4)) & 15);
	}

	short ObjectRawByteAt (
			final AvailObject object, 
			final int index)
	{
		//  Answer the byte at the given byte-index.  This is actually two nybbles packed together.

		return object.byteSlotAtByteIndex((((numberOfFixedIntegerSlots() * 4) + index) + 3));
	}

	void ObjectRawByteAtPut (
			final AvailObject object, 
			final int index, 
			final short anInteger)
	{
		//  Set the byte at the given byte-index.  This is actually two nybbles packed together.
		//  Use big endian.

		object.byteSlotAtByteIndexPut((((numberOfFixedIntegerSlots() * 4) + index) + 3), anInteger);
	}

	byte ObjectRawNybbleAt (
			final AvailObject object, 
			final int index)
	{
		//  Answer the nybble at the given index in the nybble tuple object.

		int byteIndex = numberOfFixedIntegerSlots * 4 + (index + 7) / 2;
		int rightShift = (index & 1) * 4;
		return (byte)((object.byteSlotAtByteIndex(byteIndex) >> rightShift) & 15);
	}

	AvailObject ObjectTupleAt (
			final AvailObject object, 
			final int index)
	{
		//  Answer the element at the given index in the nybble tuple object.

		return IntegerDescriptor.objectFromByte(object.rawNybbleAt(index));
	}

	void ObjectTupleAtPut (
			final AvailObject object, 
			final int index, 
			final AvailObject aNybbleObject)
	{
		//  Set the nybble at the given index to the given object (which should be an AvailObject that's an integer 0<=n<=15).

		object.rawNybbleAtPut(index, aNybbleObject.extractNybble());
	}

	AvailObject ObjectTupleAtPuttingCanDestroy (
			final AvailObject object, 
			final int index, 
			final AvailObject newValueObject, 
			final boolean canDestroy)
	{
		//  Answer a tuple with all the elements of object except at the given index we should
		//  have newValueObject.  This may destroy the original tuple if canDestroy is true.

		assert ((index >= 1) && (index <= object.tupleSize()));
		if (! newValueObject.isNybble())
		{
			if (newValueObject.isByte())
			{
				return copyAsMutableByteTuple(object).tupleAtPuttingCanDestroy(
					index,
					newValueObject,
					true);
			}
			return object.copyAsMutableObjectTuple().tupleAtPuttingCanDestroy(
				index,
				newValueObject,
				true);
		}
		if (! (canDestroy & isMutable))
		{
			return copyAsMutableByteTuple(object).tupleAtPuttingCanDestroy(
				index,
				newValueObject,
				true);
		}
		//  Ok, clobber the object in place...
		object.rawNybbleAtPut(index, newValueObject.extractNybble());
		object.hashOrZero(0);
		//  ...invalidate the hash value.  Probably cheaper than computing the difference or even testing for an actual change.
		return object;
	}

	int ObjectTupleIntAt (
			final AvailObject object, 
			final int index)
	{
		//  Answer the integer element at the given index in the nybble tuple object.

		return object.rawNybbleAt(index);
	}

	int ObjectTupleSize (
			final AvailObject object)
	{
		//  Answer the number of elements in the object (as a Smalltalk Integer).

		return (((object.integerSlotsCount() - numberOfFixedIntegerSlots()) * 8) - unusedNybblesOfLastWord);
	}



	// private-accessing

	int ObjectBitsPerEntry (
			final AvailObject object)
	{
		//  Answer approximately how many bits per entry are taken up by this object.

		return 4;
	}

	void unusedNybblesOfLastWord (
			final int anInteger)
	{
		//  Set unusedNybblesOfLastWord in this descriptor instance.

		unusedNybblesOfLastWord = anInteger;
	}



	// private-computation

	int ObjectComputeHashFromTo (
			final AvailObject object, 
			final int start, 
			final int end)
	{
		//  See comment in superclass.  This method must produce the same value.
		//  This could eventually be rewritten to do byte at a time (table lookup) and to
		//  use the square of the current multiplier.

		int hash = 0;
		for (int nybbleIndex = end; nybbleIndex >= start; nybbleIndex--)
		{
			final int itemHash = (IntegerDescriptor.hashOfByte(object.rawNybbleAt(nybbleIndex)) ^ PreToggle);
			hash = ((TupleDescriptor.multiplierTimes(hash) + itemHash) & HashMask);
		}
		return (TupleDescriptor.multiplierTimes(hash) & HashMask);
	}



	// private-copying

	AvailObject copyAsMutableByteTuple (
			final AvailObject object)
	{
		//  Answer a mutable copy of object that holds bytes, as opposed to just nybbles.

		final AvailObject result = AvailObject.newIndexedDescriptor(((object.tupleSize() + 3) / 4), ByteTupleDescriptor.isMutableSize(true, object.tupleSize()));
		//  Transfer the leader information (the stuff before the tuple's first element)...
		result.hashOrZero(object.hashOrZero());
		for (int i = 1, _end1 = result.tupleSize(); i <= _end1; i++)
		{
			result.rawByteAtPut(i, object.rawNybbleAt(i));
		}
		return result;
	}



	// private-initialization

	AvailObject mutableObjectOfSize (
			final int size)
	{
		//  Build a new object instance with room for size elements.

		if (! isMutable)
		{
			error("This descriptor should be mutable");
			return VoidDescriptor.voidObject();
		}
		assert (((size + unusedNybblesOfLastWord) & 7) == 0);
		final AvailObject result = AvailObject.newIndexedDescriptor(((size + 7) / 8), this);
		return result;
	}

	/* Descriptor lookup */
	public static NybbleTupleDescriptor isMutableSize(boolean flag, int size)
	{
		int delta = (flag ? 0 : 1);
		return (NybbleTupleDescriptor) allDescriptors [112 + delta + ((size & 7) * 2)];
	}
	
	/**
	 * Construct a new {@link NybbleTupleDescriptor}.
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
	 * @param unusedNybblesOfLastWord
	 *        The number of unused nybbles of the last word.
	 */
	protected NybbleTupleDescriptor (
		final int myId,
		final boolean isMutable,
		final int numberOfFixedObjectSlots,
		final int numberOfFixedIntegerSlots,
		final boolean hasVariableObjectSlots,
		final boolean hasVariableIntegerSlots,
		final int unusedNybblesOfLastWord)
	{
		super(
			myId,
			isMutable,
			numberOfFixedObjectSlots,
			numberOfFixedIntegerSlots,
			hasVariableObjectSlots,
			hasVariableIntegerSlots);
		this.unusedNybblesOfLastWord = unusedNybblesOfLastWord;
	}
}
