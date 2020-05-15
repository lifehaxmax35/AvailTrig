/*
 * SetDescriptor.kt
 * Copyright © 1993-2020, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  * Neither the name of the copyright holder nor the names of the contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
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
package com.avail.descriptor.sets

import com.avail.annotations.AvailMethod
import com.avail.annotations.ThreadSafe
import com.avail.descriptor.Descriptor
import com.avail.descriptor.character.A_Character.Companion.codePoint
import com.avail.descriptor.representation.*
import com.avail.descriptor.representation.NilDescriptor.Companion.nil
import com.avail.descriptor.sets.LinearSetBinDescriptor.Companion.createLinearSetBinPair
import com.avail.descriptor.sets.LinearSetBinDescriptor.Companion.emptyLinearSetBin
import com.avail.descriptor.sets.SetBinDescriptor.Companion.generateSetBinFrom
import com.avail.descriptor.sets.SetDescriptor.ObjectSlots.ROOT_BIN
import com.avail.descriptor.tuples.A_Tuple
import com.avail.descriptor.tuples.ObjectTupleDescriptor.generateObjectTupleFrom
import com.avail.descriptor.tuples.TupleDescriptor.emptyTuple
import com.avail.descriptor.types.A_Type
import com.avail.descriptor.types.AbstractEnumerationTypeDescriptor.enumerationWith
import com.avail.descriptor.types.IntegerRangeTypeDescriptor.singleInt
import com.avail.descriptor.types.SetTypeDescriptor.setTypeForSizesContentType
import com.avail.descriptor.types.TypeDescriptor.Types
import com.avail.descriptor.types.TypeTag
import com.avail.exceptions.AvailErrorCode
import com.avail.optimizer.jvm.CheckedMethod
import com.avail.optimizer.jvm.CheckedMethod.staticMethod
import com.avail.optimizer.jvm.ReferencedInGeneratedCode
import com.avail.serialization.SerializerOperation
import com.avail.utility.IteratorNotNull
import com.avail.utility.json.JSONWriter
import java.util.*

/**
 * An Avail [set][SetDescriptor] refers to the root of a Bagwell Ideal Hash
 * Tree. If the set is empty, the root is [nil][NilDescriptor], which may not be
 * a member of a set. If the set has one element, the root is the element
 * itself. If the set has two or more elements then a [bin][SetBinDescriptor]
 * must be used.  There are two types of bin, the
 * [linear&#32;bin][LinearSetBinDescriptor] and the
 * [hashed&#32;bin][HashedSetBinDescriptor]. The linear bin is used below a
 * threshold size, after which a hashed bin is substituted. The linear bin
 * simply contains an arbitrarily ordered list of elements, which are examined
 * exhaustively when testing set membership. The hashed bin uses up to five bits
 * of an element's [hash][AvailObject.hash] value to partition the set. The
 * depth of the bin in the hash tree determines which hash bits are used.
 *
 * @constructor
 *
 * @param mutability
 *   The [mutability][Mutability] of the new descriptor.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
class SetDescriptor
/**
 * Construct a new `SetDescriptor`.
 *
 */
private constructor(
	mutability: Mutability
) : Descriptor(
	mutability, TypeTag.SET_TAG, ObjectSlots::class.java, null
) {
	/**
	 * The layout of object slots for my instances.
	 */
	enum class ObjectSlots : ObjectSlotsEnum {
		/**
		 * The topmost bin of this set.  If it's [nil][NilDescriptor], the set
		 * is empty.  If it's a [set&#32;bin][SetBinDescriptor] then the bin
		 * contains the elements.  Otherwise the set contains one element, the
		 * object in this field.
		 */
		ROOT_BIN
	}

	override fun printObjectOnAvoidingIndent(
		self: AvailObject,
		aStream: StringBuilder,
		recursionMap: IdentityHashMap<A_BasicObject, Void>,
		indent: Int
	): Unit = with(aStream) {
		when {
			self.setSize() == 0 -> append('∅')
			self.setElementsAreAllInstancesOfKind(Types.CHARACTER.o()) -> {
				append("¢[")
				val codePointsSet: SortedSet<Int> = TreeSet()
				self.mapTo(codePointsSet) { it.codePoint() }
				val iterator: Iterator<Int> = codePointsSet.iterator()
				var runStart = iterator.next()
				do {
					var runEnd = runStart
					var next = -1
					while (iterator.hasNext()) {
						next = iterator.next()
						if (next != runEnd + 1) {
							break
						}
						runEnd++
						next = -1
					}
					writeRangeElement(aStream, runStart)
					if (runEnd != runStart) {
						// Skip the dash if the start and end are consecutive.
						if (runEnd != runStart + 1) {
							appendCodePoint('-'.toInt())
						}
						writeRangeElement(aStream, runEnd)
					}
					runStart = next
				} while (runStart != -1)
				append("]")
			}
			else -> {
				val tuple = self.asTuple()
				append('{')
				var first = true
				for (element in tuple) {
					if (!first) {
						append(", ")
					}
					element.printOnAvoidingIndent(
						aStream, recursionMap, indent + 1)
					first = false
				}
				append('}')
			}
		}
	}

	override fun o_NameForDebugger(self: AvailObject): String =
		"${super.o_NameForDebugger(self)}: setSize=${self.setSize()}"

	/**
	 * Synthetic slots to display.
	 */
	internal enum class FakeSetSlots : ObjectSlotsEnum {
		/**
		 * A fake slot to present in the debugging view for each of the elements
		 * of this set.
		 */
		ELEMENT_
	}

	/**
	 * Use the [SetIterator] to build the list of values (in arbitrary order).
	 * Hide the bin structure.
	 */
	override fun o_DescribeForDebugger(
		self: AvailObject
	): Array<AvailObjectFieldHelper> = self.mapIndexed { i, element ->
		AvailObjectFieldHelper(self, FakeSetSlots.ELEMENT_, i + 1, element)
	}.toTypedArray()

	@AvailMethod
	override fun o_Equals(self: AvailObject, another: A_BasicObject): Boolean =
		another.equalsSet(self)

	@AvailMethod
	override fun o_EqualsSet(self: AvailObject, aSet: A_Set): Boolean = when {
		self.sameAddressAs(aSet) -> true
		rootBin(self).sameAddressAs(rootBin(aSet as AvailObject)) -> true
		self.setSize() != aSet.setSize() -> false
		self.hash() != aSet.hash() -> false
		!rootBin(self).isBinSubsetOf(aSet) -> false
		// They're equal.
		!isShared -> {
			aSet.makeImmutable()
			self.becomeIndirectionTo(aSet)
			true
		}
		!aSet.descriptor().isShared -> {
			self.makeImmutable()
			aSet.becomeIndirectionTo(self)
			true
		}
		else -> {
			// They're both shared, so we can't make one an indirection.
			// Substitute one of the bins for the other to speed up
			// subsequent equality checks.
			self.writeBackSlot(ROOT_BIN, 1, rootBin(aSet))
			true
		}
	}

	@AvailMethod
	override fun o_HasElement(
		self: AvailObject,
		elementObject: A_BasicObject
	): Boolean = rootBin(self).binHasElementWithHash(
		elementObject, elementObject.hash())

	/**
	 * A set's hash is a simple function of its rootBin's setBinHash, which is
	 * always the sum of its elements' hashes.
	 */
	@AvailMethod
	override fun o_Hash(self: AvailObject): Int =
		rootBin(self).setBinHash() xor 0xCD9EFC6

	@AvailMethod
	override fun o_IsInstanceOfKind(
		self: AvailObject,
		aTypeObject: A_Type
	): Boolean = when {
		aTypeObject.isSupertypeOfPrimitiveTypeEnum(Types.NONTYPE) -> true
		!aTypeObject.isSetType -> false
		// See if it's an acceptable size...
		!aTypeObject.sizeRange().rangeIncludesInt(self.setSize()) -> false
		else -> {
			val expectedContentType = aTypeObject.contentType()
			when {
				expectedContentType.isEnumeration ->
					// Check the complete membership.
					self.all {
						expectedContentType.enumerationIncludesInstance(it)
					}
				else -> rootBin(self).binElementsAreAllInstancesOfKind(
					expectedContentType)
			}
		}
	}

	@AvailMethod
	override fun o_IsSet(self: AvailObject) = true

	@AvailMethod
	override fun o_IsSubsetOf(self: AvailObject, another: A_Set): Boolean =
		(self.setSize() <= another.setSize()
			&& rootBin(self).isBinSubsetOf(another))

	@AvailMethod
	override fun o_Kind(self: AvailObject): A_Type =
		setTypeForSizesContentType(
			singleInt(self.setSize()),
			enumerationWith(self))

	/**
	 * Answer whether all my elements are instances of the specified kind.
	 */
	@AvailMethod
	override fun o_SetElementsAreAllInstancesOfKind(
		self: AvailObject,
		kind: AvailObject
	): Boolean = rootBin(self).binElementsAreAllInstancesOfKind(kind)

	/**
	 * Compute the intersection of two sets (a ∩ b).  May destroy one of them if
	 * it's mutable and canDestroy is true.
	 */
	@AvailMethod
	override fun o_SetIntersectionCanDestroy(
		self: AvailObject,
		otherSet: A_Set,
		canDestroy: Boolean
	): A_Set {
		val (smaller, larger) = when {
			self.setSize() <= otherSet.setSize() -> self to otherSet.traversed()
			else -> otherSet.traversed() to self
		}
		var result: A_Set = smaller.makeImmutable()
		smaller.forEach {
			if (!larger.hasElement(it)) {
				result = result.setWithoutElementCanDestroy(it, true)
			}
		}
		return result
	}

	/**
	 * Compute whether the intersection of two sets is non-empty (a ∩ b ≠ ∅).
	 */
	override fun o_SetIntersects(
		self: AvailObject,
		otherSet: A_Set
	): Boolean {
		val (smaller, larger) = when {
			self.setSize() <= otherSet.setSize() -> self to otherSet.traversed()
			else -> otherSet.traversed() to self
		}
		return smaller.any { larger.hasElement(it) }
	}

	/**
	 * Compute the asymmetric difference of two sets (a \ b).  May destroy one
	 * of them if it's mutable and canDestroy is true.
	 */
	@AvailMethod
	override fun o_SetMinusCanDestroy(
		self: AvailObject,
		otherSet: A_Set,
		canDestroy: Boolean
	): A_Set = self.makeImmutable().fold(self as A_Set) { result, element ->
		when {
			!otherSet.hasElement(element) -> result
			else -> result.setWithoutElementCanDestroy(element, true)
		}
	}

	/**
	 * Compute the union of two sets (a ∪ b).  May destroy one of them if it's
	 * mutable and canDestroy is true.
	 */
	@AvailMethod
	override fun o_SetUnionCanDestroy(
		self: AvailObject,
		otherSet: A_Set,
		canDestroy: Boolean
	): A_Set {
		// Compute the union of two sets. May destroy one of them if it's
		// mutable and canDestroy is true.
		val (smaller, larger) = when {
			self.setSize() <= otherSet.setSize() -> self to otherSet.traversed()
			else -> otherSet.traversed() to self
		}
		if (!canDestroy && larger.descriptor().isMutable) {
			larger.makeImmutable()
		}
		if (smaller.setSize() == 0) {
			return larger
		}
		return smaller.fold(larger) { result: A_Set, element ->
			result.setWithElementCanDestroy(element.makeImmutable(), true)
		}
	}

	@AvailMethod
	override fun o_SetWithElementCanDestroy(
		self: AvailObject,
		newElementObject: A_BasicObject,
		canDestroy: Boolean
	): A_Set {
		// Ensure newElementObject is in the set, adding it if necessary. May
		// destroy the set if it's mutable and canDestroy is true.
		val elementHash = newElementObject.hash()
		val root = rootBin(self)
		val oldSize = root.setBinSize()
		val newRootBin = root.setBinAddingElementHashLevelCanDestroy(
			newElementObject,
			elementHash,
			0,
			canDestroy && isMutable)
		if (newRootBin.setBinSize() == oldSize) {
			if (!canDestroy) {
				self.makeImmutable()
			}
			return self
		}
		val result = if (canDestroy && isMutable) self else mutable().create()
		setRootBin(result, newRootBin)
		return result
	}

	@AvailMethod
	override fun o_SetWithoutElementCanDestroy(
		self: AvailObject,
		elementObjectToExclude: A_BasicObject,
		canDestroy: Boolean
	): A_Set {
		// Ensure elementObjectToExclude is not in the set, removing it if
		// necessary. May destroy the set if it's mutable and canDestroy is
		// true.
		val root = rootBin(self)
		val oldSize = root.setBinSize()
		val newRootBin = root.binRemoveElementHashLevelCanDestroy(
			elementObjectToExclude,
			elementObjectToExclude.hash(),
			0,
			canDestroy && isMutable)
		if (newRootBin.setBinSize() == oldSize) {
			if (!canDestroy) {
				self.makeImmutable()
			}
			return self
		}
		val result = if (canDestroy && isMutable) self else mutable().create()
		setRootBin(result, newRootBin)
		return result
	}

	override fun o_ShowValueInNameForDebugger(self: AvailObject) = false

	/**
	 * A [SetIterator] is returned when a [set][SetDescriptor] is asked for its
	 * [AvailObject.iterator].  Among other uses, this is useful when combined
	 * with Kotlin's "forEach" control structure.
	 */
	abstract class SetIterator : IteratorNotNull<AvailObject> {
		override fun remove() {
			throw UnsupportedOperationException()
		}
	}

	@AvailMethod
	override fun o_Iterator(self: AvailObject): SetIterator =
		rootBin(self).setBinIterator()

	@AvailMethod
	override fun o_AsTuple(self: AvailObject): A_Tuple {
		val size = self.setSize()
		if (size == 0) {
			return emptyTuple()
		}
		val iterator = self.iterator()
		return generateObjectTupleFrom(self.setSize()) {
			iterator.next().makeImmutable()
		}
	}

	@AvailMethod
	override fun o_SetSize(self: AvailObject): Int = rootBin(self).setBinSize()

	@AvailMethod
	@ThreadSafe
	override fun o_SerializerOperation(self: AvailObject): SerializerOperation =
		SerializerOperation.SET

	override fun o_WriteTo(self: AvailObject, writer: JSONWriter) {
		writer.startObject()
		writer.write("kind")
		writer.write("set")
		writer.write("elements")
		writer.startArray()
		for (o in self) {
			o.writeTo(writer)
		}
		writer.endArray()
		writer.endObject()
	}

	override fun o_WriteSummaryTo(self: AvailObject, writer: JSONWriter) {
		writer.startObject()
		writer.write("kind")
		writer.write("set")
		writer.write("elements")
		writer.startArray()
		for (o in self) {
			o.writeSummaryTo(writer)
		}
		writer.endArray()
		writer.endObject()
	}

	override fun mutable(): SetDescriptor = mutable

	override fun immutable(): SetDescriptor = immutable

	override fun shared(): SetDescriptor = shared

	companion object {
		/**
		 * Extract the root [bin][SetBinDescriptor] from the [set][A_Set].  The
		 * set must be known to have a [SetDescriptor] as its descriptor.
		 *
		 * @param self
		 *   The set from which to extract the root bin.
		 * @return
		 *   The set's bin.
		 */
		private fun rootBin(self: AvailObject): AvailObject =
			self.slot(ROOT_BIN)

		/**
		 * Replace the [set][A_Set]'s root [bin][SetBinDescriptor]. The
		 * replacement may be [nil] to indicate an empty map.
		 *
		 * @param set
		 *   The set (must not be an indirection).
		 * @param bin
		 *   The root bin for the set, or nil.
		 */
		private fun setRootBin(
			set: AvailObject,
			bin: A_BasicObject
		) = set.setSlot(ROOT_BIN, bin)

		/**
		 * Write a Unicode code point that's either the start or end of a range,
		 * or a single value.
		 *
		 * @param builder
		 *   Where to write the possibly encoded code point.
		 * @param codePoint
		 *   The code point (an [Int] in the range 0..1,114,111) to write.
		 */
		private fun writeRangeElement(
			builder: StringBuilder,
			codePoint: Int
		) {
			var escaped = -1
			when (codePoint) {
				' '.toInt() -> {
					builder.appendCodePoint(' '.toInt())
					return
				}
				'-'.toInt(), '['.toInt(), ']'.toInt() -> {
					builder.append(String.format("\\(%x)", codePoint))
					return
				}
				'\n'.toInt() -> escaped = 'n'.toInt()
				'\r'.toInt() -> escaped = 'r'.toInt()
				'\t'.toInt() -> escaped = 't'.toInt()
				'\\'.toInt() -> escaped = '\\'.toInt()
				'"'.toInt() -> escaped = '"'.toInt()
			}
			if (escaped != -1) {
				builder.appendCodePoint('\\'.toInt())
				builder.appendCodePoint(escaped)
				return
			}
			when (Character.getType(codePoint)) {
				Character.COMBINING_SPACING_MARK.toInt(),
				Character.CONTROL.toInt(),
				Character.ENCLOSING_MARK.toInt(),
				Character.FORMAT.toInt(),
				Character.NON_SPACING_MARK.toInt(),
				Character.PARAGRAPH_SEPARATOR.toInt(),
				Character.PRIVATE_USE.toInt(),
				Character.SPACE_SEPARATOR.toInt(),
				Character.SURROGATE.toInt(),
				Character.UNASSIGNED.toInt() ->
					builder.append(String.format("\\(%X)", codePoint))
				else -> builder.appendCodePoint(codePoint)
			}
		}

		/**
		 * Construct a new [set][SetDescriptor] from the specified
		 * [collection][Collection] of [objects][AvailObject]. Neither the
		 * elements nor the resultant set are made immutable.
		 *
		 * @param collection
		 *   A collection of [A_BasicObject]s.
		 * @return
		 *   A new mutable set containing the elements of the collection.
		 */
		@JvmStatic
		fun setFromCollection(collection: Collection<A_BasicObject>): A_Set {
			val iterator = collection.iterator()
			return generateSetFrom(collection.size) { iterator.next() }
		}

		/**
		 * Construct a Java [Set] from the specified Avail [A_Set].  The
		 * elements are not made immutable.
		 *
		 * @param set
		 *   An Avail set.
		 * @return
		 *   The corresponding Java [Set] of [AvailObject]s.
		 */
		@JvmStatic
		fun toSet(set: A_Set): Set<AvailObject> =
			set.mapTo(mutableSetOf()) { it }

		/**
		 * Create an Avail set with the specified elements. The elements are not
		 * made immutable first, nor is the new set.
		 *
		 * @param elements
		 *   The array of Avail values from which to construct a set.
		 * @return
		 *   The new mutable set.
		 */
		@JvmStatic
		fun set(vararg elements: A_BasicObject): A_Set =
			generateSetFrom(elements.size) { elements[it - 1] }

		/**
		 * Create an Avail set with the numeric values of the specified [ ]s.
		 * The numeric codes (Avail integers) are not made immutable first, nor
		 * is the new set.
		 *
		 * @param errorCodeElements
		 *   The array of AvailErrorCodes from which to construct a set.
		 * @return
		 *   The new mutable set.
		 */
		@JvmStatic
		fun set(vararg errorCodeElements: AvailErrorCode): A_Set =
			generateSetFrom(errorCodeElements) { it.numericCode() }

		/** The mutable [SetDescriptor].  */
		private val mutable = SetDescriptor(Mutability.MUTABLE)

		/** The immutable [SetDescriptor].  */
		private val immutable = SetDescriptor(Mutability.IMMUTABLE)

		/** The shared [SetDescriptor].  */
		private val shared = SetDescriptor(Mutability.SHARED)

		/** The empty set.  */
		private var theEmptySet: A_Set = mutable.create().apply {
			setRootBin(this, emptyLinearSetBin(0))
			hash()
			makeShared()
		}

		/**
		 * Answer the empty set.
		 *
		 * @return
		 *   The empty set.
		 */
		@ReferencedInGeneratedCode
		@JvmStatic
		fun emptySet(): A_Set = theEmptySet

		/**
		 * The [CheckedMethod] for [emptySet].
		 */
		@JvmField
		val emptySetMethod: CheckedMethod = staticMethod(
			SetDescriptor::class.java,
			::emptySet.name,
			A_Set::class.java)

		/**
		 * Create an Avail set with exactly one element.  The element is not
		 * made immutable first, nor is the new set.
		 *
		 * @param element
		 *   The sole element of the set.
		 * @return
		 *   The new mutable set.
		 */
		@JvmStatic
		fun singletonSet(element: A_BasicObject): A_Set =
			mutable.create().apply {
				setRootBin(this, element)
				hash()
			}

		/**
		 * Create an Avail set with exactly two elements.  The elements are not
		 * made immutable first, nor is the new set.
		 *
		 * @param element1
		 *   The first element of the set.
		 * @param element2
		 *   The second element of the set.
		 * @return The new mutable set.
		 */
		private fun twoElementSet(
			element1: A_BasicObject,
			element2: A_BasicObject
		): A_Set {
			assert(!element1.equals(element2))
			return mutable.create().apply {
				setRootBin(
					this,
					createLinearSetBinPair(0, element1, element2))
				hash()
			}
		}

		/**
		 * Create an [A_Set], then run the generator the specified number of
		 * times to produce elements to add.  Deduplicate the elements.  Answer
		 * the resulting set.
		 *
		 * @param size
		 *   The number of values to extract from the generator.
		 * @param generator
		 *   A generator to provide [AvailObject]s to store.
		 * @return
		 *   The new set.
		 */
		@JvmStatic
		fun generateSetFrom(
			size: Int,
			generator: (Int) -> A_BasicObject
		): A_Set = when (size) {
			0 -> emptySet()
			1 -> singletonSet(generator(1))
			2 -> {
				val element1 = generator(1)
				val element2 = generator(2)
				when {
					element1.equals(element2) -> singletonSet(element1)
					else -> twoElementSet(element1, element2)
				}
			}
			else -> mutable.create().apply {
				setRootBin(
					this, generateSetBinFrom(0, size, generator))
			}
		}

		/**
		 * Create an [A_Set], then run the iterator the specified number of
		 * times to produce elements to add.  Deduplicate the elements.  Answer
		 * the resulting set.
		 *
		 * @param size
		 *   The number of values to extract from the iterator.
		 * @param iterator
		 *   An iterator to provide [AvailObject]s to store.
		 * @return
		 *   The new set.
		 */
		@JvmStatic
		fun generateSetFrom(
			size: Int,
			iterator: Iterator<A_BasicObject>
		): A_Set = generateSetFrom(size) { iterator.next() }

		/**
		 * Create an [A_Set], then run the iterator the specified number of
		 * times to produce elements to add.  Deduplicate the elements.  Answer
		 * the resulting set.
		 *
		 * @param A
		 *   The type of value produced by the iterator and consumed by the
		 *   transformer.
		 * @param size
		 *   The number of values to extract from the iterator.
		 * @param iterator
		 *   An iterator to provide [AvailObject]s to store.
		 * @param transformer
		 *   A function to transform iterator elements.
		 * @return
		 *   The new set.
		 */
		@JvmStatic
		fun <A> generateSetFrom(
			size: Int,
			iterator: Iterator<A>,
			transformer: (A)->A_BasicObject
		): A_Set = generateSetFrom(size) { transformer(iterator.next()) }

		/**
		 * Create an [A_Set], then iterate over the collection, invoking the
		 * transformer to produce elements to add.  Deduplicate the elements.
		 * Answer the resulting set.
		 *
		 * @param A
		 *   The type of value found in the collection and consumed by the
		 *   transformer.
		 * @param collection
		 *   A collection containing values to be transformed.
		 * @param transformer
		 *   A function to transform collection elements to [A_BasicObject]s.
		 * @return
		 *   The new set.
		 */
		@JvmStatic
		fun <A> generateSetFrom(
			collection: Collection<A>,
			transformer: (A)->A_BasicObject
		): A_Set = generateSetFrom(
			collection.size, collection.iterator(), transformer)

		/**
		 * Create an [A_Set], then iterate over the array, invoking the
		 * transformer to produce elements to add.  Deduplicate the elements.
		 * Answer the resulting set.
		 *
		 * @param A
		 *   The type of value found in the array and consumed by the
		 *   transformer.
		 * @param array
		 *   An array containing values to be transformed.
		 * @param transformer
		 *   A function to transform array elements to [A_BasicObject]s.
		 * @return
		 *   The new set.
		 */
		@JvmStatic
		fun <A> generateSetFrom(
			array: Array<A>,
			transformer: (A)->A_BasicObject
		): A_Set = generateSetFrom(array.size) { transformer(array[it - 1]) }

		/**
		 * Create an [A_Set], then iterate over the [A_Tuple], invoking the
		 * transformer to produce elements to add.  Deduplicate the elements.
		 * Answer the resulting set.
		 *
		 * @param tuple
		 *   A tuple containing values to be transformed.
		 * @param transformer
		 *   A function to transform tuple elements to [A_BasicObject]s.
		 * @return
		 *   The new set.
		 */
		@JvmStatic
		fun generateSetFrom(
			tuple: A_Tuple,
			transformer: (AvailObject)->A_BasicObject
		): A_Set = generateSetFrom(
			tuple.tupleSize(), tuple.iterator(), transformer)
	}
}