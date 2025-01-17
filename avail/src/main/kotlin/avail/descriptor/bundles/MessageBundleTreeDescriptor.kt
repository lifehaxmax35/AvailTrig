/*
 * MessageBundleTreeDescriptor.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
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
package avail.descriptor.bundles

import avail.AvailRuntimeSupport
import avail.AvailRuntimeSupport.nextNonzeroHash
import avail.compiler.ParsingOperation
import avail.compiler.ParsingOperation.BRANCH_FORWARD
import avail.compiler.ParsingOperation.CHECK_ARGUMENT
import avail.compiler.ParsingOperation.Companion.decode
import avail.compiler.ParsingOperation.Companion.operand
import avail.compiler.ParsingOperation.JUMP_BACKWARD
import avail.compiler.ParsingOperation.JUMP_FORWARD
import avail.compiler.ParsingOperation.PARSE_PART
import avail.compiler.ParsingOperation.PARSE_PART_CASE_INSENSITIVELY
import avail.compiler.ParsingOperation.PREPARE_TO_RUN_PREFIX_FUNCTION
import avail.compiler.ParsingOperation.TYPE_CHECK_ARGUMENT
import avail.compiler.splitter.MessageSplitter
import avail.compiler.splitter.MessageSplitter.Companion.constantForIndex
import avail.descriptor.bundles.A_Bundle.Companion.grammaticalRestrictions
import avail.descriptor.bundles.A_Bundle.Companion.messagePart
import avail.descriptor.bundles.A_BundleTree.Companion.addPlanInProgress
import avail.descriptor.bundles.A_BundleTree.Companion.allParsingPlansInProgress
import avail.descriptor.bundles.A_BundleTree.Companion.expand
import avail.descriptor.bundles.A_BundleTree.Companion.hasBackwardJump
import avail.descriptor.bundles.A_BundleTree.Companion.isSourceOfCycle
import avail.descriptor.bundles.A_BundleTree.Companion.latestBackwardJump
import avail.descriptor.bundles.A_BundleTree.Companion.lazyActions
import avail.descriptor.bundles.MessageBundleTreeDescriptor.IntegerSlots.Companion.HASH
import avail.descriptor.bundles.MessageBundleTreeDescriptor.IntegerSlots.Companion.HAS_BACKWARD_JUMP_INSTRUCTION
import avail.descriptor.bundles.MessageBundleTreeDescriptor.IntegerSlots.Companion.IS_SOURCE_OF_CYCLE
import avail.descriptor.maps.A_Map
import avail.descriptor.maps.A_Map.Companion.forEach
import avail.descriptor.maps.A_Map.Companion.hasKey
import avail.descriptor.maps.A_Map.Companion.keysAsSet
import avail.descriptor.maps.A_Map.Companion.mapAt
import avail.descriptor.maps.A_Map.Companion.mapAtOrNull
import avail.descriptor.maps.A_Map.Companion.mapAtPuttingCanDestroy
import avail.descriptor.maps.A_Map.Companion.mapAtReplacingCanDestroy
import avail.descriptor.maps.A_Map.Companion.mapSize
import avail.descriptor.maps.A_Map.Companion.mapWithoutKeyCanDestroy
import avail.descriptor.maps.MapDescriptor
import avail.descriptor.maps.MapDescriptor.Companion.emptyMap
import avail.descriptor.methods.A_Definition
import avail.descriptor.methods.A_GrammaticalRestriction
import avail.descriptor.methods.A_Macro
import avail.descriptor.methods.A_Sendable
import avail.descriptor.module.A_Module
import avail.descriptor.module.A_Module.Companion.hasAncestor
import avail.descriptor.numbers.A_Number
import avail.descriptor.numbers.A_Number.Companion.extractInt
import avail.descriptor.numbers.IntegerDescriptor.Companion.fromInt
import avail.descriptor.parsing.A_DefinitionParsingPlan
import avail.descriptor.parsing.A_DefinitionParsingPlan.Companion.bundle
import avail.descriptor.parsing.A_DefinitionParsingPlan.Companion.definition
import avail.descriptor.parsing.A_DefinitionParsingPlan.Companion.parsingInstructions
import avail.descriptor.parsing.A_ParsingPlanInProgress
import avail.descriptor.parsing.A_ParsingPlanInProgress.Companion.isBackwardJump
import avail.descriptor.parsing.A_ParsingPlanInProgress.Companion.nameHighlightingPc
import avail.descriptor.parsing.A_ParsingPlanInProgress.Companion.parsingPc
import avail.descriptor.parsing.A_ParsingPlanInProgress.Companion.parsingPlan
import avail.descriptor.parsing.ParsingPlanInProgressDescriptor.Companion.newPlanInProgress
import avail.descriptor.representation.A_BasicObject
import avail.descriptor.representation.AbstractSlotsEnum
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.AvailObjectFieldHelper
import avail.descriptor.representation.BitField
import avail.descriptor.representation.Descriptor
import avail.descriptor.representation.IntegerSlotsEnum
import avail.descriptor.representation.Mutability
import avail.descriptor.representation.Mutability.SHARED
import avail.descriptor.representation.NilDescriptor.Companion.nil
import avail.descriptor.sets.A_Set
import avail.descriptor.sets.A_Set.Companion.hasElement
import avail.descriptor.sets.A_Set.Companion.setSize
import avail.descriptor.sets.A_Set.Companion.setUnionCanDestroy
import avail.descriptor.sets.A_Set.Companion.setWithElementCanDestroy
import avail.descriptor.sets.A_Set.Companion.setWithoutElementCanDestroy
import avail.descriptor.sets.SetDescriptor.Companion.emptySet
import avail.descriptor.tuples.A_String
import avail.descriptor.tuples.A_Tuple
import avail.descriptor.tuples.A_Tuple.Companion.appendCanDestroy
import avail.descriptor.tuples.A_Tuple.Companion.component1
import avail.descriptor.tuples.A_Tuple.Companion.component2
import avail.descriptor.tuples.A_Tuple.Companion.tupleAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleIntAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.tuples.TupleDescriptor.Companion.toList
import avail.descriptor.types.A_Type
import avail.descriptor.types.PhraseTypeDescriptor
import avail.descriptor.types.PhraseTypeDescriptor.PhraseKind.PARSE_PHRASE
import avail.descriptor.types.PrimitiveTypeDescriptor.Types
import avail.descriptor.types.TupleTypeDescriptor.Companion.tupleTypeForTypes
import avail.descriptor.types.TypeTag
import avail.dispatch.LeafLookupTree
import avail.dispatch.LookupTree
import avail.dispatch.LookupTreeAdaptor
import avail.dispatch.TypeComparison.Companion.compareForParsing
import avail.interpreter.execution.Interpreter
import avail.interpreter.levelTwo.operand.TypeRestriction
import avail.interpreter.levelTwo.operand.TypeRestriction.Companion.restrictionForType
import avail.interpreter.levelTwo.operand.TypeRestriction.RestrictionFlagEncoding.BOXED_FLAG
import avail.performance.Statistic
import avail.performance.StatisticReport.EXPANDING_PARSING_INSTRUCTIONS
import avail.utility.Strings.newlineTab
import avail.utility.safeWrite
import java.util.ArrayDeque
import java.util.Collections.sort
import java.util.Deque
import java.util.IdentityHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.concurrent.read

/**
 * A [message&#32;bundle&#32;tree][MessageBundleTreeDescriptor] is used by the
 * Avail parser.  Since the Avail syntax is so flexible, we make up for that
 * simplicity with a complementary complexity in the parsing mechanism.  A
 * message bundle tree is used to keep track of how far along the parser has
 * gotten in the parsing of a method invocation.  More powerfully, it does this
 * for multiple methods simultaneously, at least up to the point that the method
 * names diverge.
 *
 * For example, assume the methods "_foo_bar" and "_foo_baz" are both visible in
 * the current module.  After parsing an argument, the "foo" keyword, and
 * another argument, the next thing to look for is either the "bar" keyword or
 * the "baz" keyword.  Depending which keyword comes next, we will have parsed
 * an invocation of either the first or the second method.  Both possibilities
 * have been parsed together (i.e., only once) up to this point, and the next
 * keyword encountered decides which (if either) method call is being invoked.
 *
 * [MessageSplitter] is used to generate a sequence of parsing instructions for
 * a method name.  These parsing instructions determine how long multiple
 * potential method invocations can be parsed together and when they must
 * diverge.
 *
 * @property latestBackwardJump
 *   This is the most recently encountered backward jump in the ancestry of this
 *   bundle tree, or nil if none were encountered in the ancestry. There could
 *   be multiple competing parsing plans in that target bundle tree, some of
 *   which had a backward jump and some of which didn't, but we only require
 *   that at least one had a backward jump.
 *
 *   Since every loop has a backward jump, and since the target has a pointer to
 *   its own preceding backward jump, we can trace this back through every
 *   backward jump in the ancestry (some of which will contain the same
 *   parsing-plans-in-progress).  When we expand a node that's a backward jump,
 *   we chase these pointers to determine if there's an equivalent node in the
 *   ancestry – one with the same set of parsing-plans-in-progress.  If so, we
 *   use its expansion rather than creating yet another duplicate copy.  This
 *   saves space and time in the case that there are repeated arguments to a
 *   method, and at least one encountered invocation of that method has a large
 *   number of repetitions.  An example is the literal set notation "{«_‡,»}",
 *   which can be used to specify a set with thousands of elements.  Without
 *   this optimization, that would be tens of thousands of additional bundle
 *   trees to maintain.
 *
 * @constructor
 *
 * @param mutability
 *   The [Mutability] of the new descriptor.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
class MessageBundleTreeDescriptor private constructor(
	mutability: Mutability,
	var latestBackwardJump: A_BundleTree
) : Descriptor(
	mutability,
	TypeTag.BUNDLE_TREE_TAG,
	null,
	IntegerSlots::class.java)
{
	/**
	 * A [ReentrantReadWriteLock] used for coordinating accessing and updating
	 * the fields.
	 */
	private val lock = ReentrantReadWriteLock()

	/**
	 * A [map][MapDescriptor] from [A_Bundle]s to maps, which are themselves
	 * from [definitions][A_Definition] to [sets][A_Set] of
	 * [plans-in-progress][A_ParsingPlanInProgress] for that
	 * definition/bundle.  Note that the inner maps may be empty in the case
	 * that a grammatical restriction has been defined before any visible
	 * definitions.  This doesn't affect parsing, but makes the logic easier
	 * about deciding which grammatical restrictions are visible when adding
	 * a definition later.
	 */
	private var allPlansInProgress = emptyMap

	/**
	 * An [A_Map] from visible [A_Bundle]s to maps from [A_Definition] to the
	 * [A_Set]s of [plans-in-progress][A_ParsingPlanInProgress] for that
	 * definition/bundle. It has the same content as [allPlansInProgress] until
	 * these items have been categorized as complete, incomplete, action, or
	 * prefilter. They are categorized if and when this message bundle tree is
	 * reached during parsing.
	 */
	@Volatile
	private var unclassified = emptyMap

	/**
	 * An [A_Map] from an [A_Bundle] (being parsed via this bundle tree) to an
	 * [A_Set] of [A_Sendable] definitions, which includes both method
	 * [A_Definition]s and [A_Macro]s.  In particular, these are the definitions
	 * which have just been successfully parsed at this point in the tree.
	 */
	private var lazyComplete = emptyMap

	/**
	 * An [A_Map] from [A_String] to an [A_Map] from keyword index to the
	 * successor [A_BundleTree]s.  The intermediate map is only present to
	 * ensure the correct keyword index can be captured along with the parsed
	 * token along each path.
	 *
	 * During parsing, if the next token is a key of this map then consume that
	 * token, look it up in this map to get another map, and for each keyword
	 * index (key) in the next map, capture the (token, keyword index) pair
	 * before continuing parsing in the associated message bundle tree.
	 * Otherwise record a suitable parsing failure message for this position in
	 * the source stream, in case this ends up being the rightmost parse
	 * position to be reached.
	 *
	 * [A_Bundle]s only get added to this map if their current instruction is
	 * the [ParsingOperation.PARSE_PART] instruction. There may be other
	 * instructions current for other message bundles, but they will be
	 * represented in the [lazyActions] map, or the [lazyPrefilterMap] if their
	 * instruction is a [ParsingOperation.CHECK_ARGUMENT].
	 */
	private var lazyIncomplete = emptyMap

	/**
	 * An [A_Map] from lower-case [A_String] to an [A_Map] from keyword index
	 * to the successor [A_BundleTree]s.  The intermediate map is only present
	 * to ensure the correct keyword index can be capture along with the parsed
	 * token along each path.
	 *
	 * During parsing, if the next token, following conversion to lower case, is
	 * a key of this map, then consume that token, look it up in this map to get
	 * another map, and for each keyword index (key) in the next map, capture
	 * the (token, keyword index) pair before continuing parsing in the
	 * associated message bundle tree. Otherwise record a suitable parsing
	 * failure message for this position in the source stream, in case this ends
	 * up being the rightmost parse position to be reached.
	 *
	 * [A_Bundle]s only get added to this map if their current instruction is
	 * the [ParsingOperation.PARSE_PART_CASE_INSENSITIVELY] instruction. There
	 * may be other instructions current for other message bundles, but they
	 * will be represented in the [lazyActions] map, or the [lazyPrefilterMap]
	 * if their instruction is a [ParsingOperation.CHECK_ARGUMENT].
	 */
	private var lazyIncompleteCaseInsensitive = emptyMap

	/**
	 * This is a map from an encoded [ParsingOperation] (an [Integer]) to an
	 * [A_Tuple] of [A_BundleTree]s to attempt if the instruction succeeds.
	 *
	 * Note that the [ParsingOperation.PARSE_PART] and
	 * [ParsingOperation.PARSE_PART_CASE_INSENSITIVELY] instructions are treated
	 * specially, as only one keyword can be next in the source stream (so
	 * there's no value in checking whether it's an X, whether it's a Y, whether
	 * it's a Z, etc. Instead, the [lazyIncomplete] and
	 * [lazyIncompleteCaseInsensitive] maps take care of dealing with this
	 * efficiently with a single lookup.
	 *
	 * Similarly, the [ParsingOperation.CHECK_ARGUMENT] instruction is treated
	 * specially. When it is encountered and the argument that was just parsed
	 * is a send phrase, that send phrase is looked up in the
	 * [lazyPrefilterMap], yielding the next message bundle tree. If it's not
	 * present as a key (or the argument isn't a send), then the instruction is
	 * looked up normally in the lazy actions map.
	 */
	private var lazyActions = emptyMap

	/**
	 * If we wait until all tokens and arguments of a potential method send have
	 * been parsed before checking that all the arguments have the right types
	 * and precedence then we may spend a *lot* of extra effort parsing
	 * unnecessary expressions. For example, the "_×_" operation might not allow
	 * a "_+_" call for its left or right arguments, so parsing "1+2×…" as
	 * "(1+2)×…" is wasted effort.
	 *
	 * This is especially expensive for operations with many arguments that
	 * could otherwise be culled by the shapes and types of early arguments,
	 * such as for "«_‡++»", which forbids arguments being invocations of the
	 * same message, keeping a call with many arguments flat. I haven't worked
	 * out the complete recurrence relations for this, but it's probably
	 * exponential (it certainly grows *much* faster than linearly without this
	 * optimization).
	 *
	 * To accomplish this culling we have to filter out any inconsistent
	 * [message&#32;bundles][MessageBundleDescriptor] as we parse. Since we
	 * already do this in general while parsing expressions, all that remains is
	 * to check right after an argument has been parsed (or replayed due to
	 * memoization). The check for now is simple and doesn't consider argument
	 * types, simply excluding methods based on the grammatical restrictions.
	 * Type culling is more expensive, and is postponed in the
	 * [ParsingOperation] sequence until after any easy tests have passed, like
	 * consuming additional fixed tokens.
	 *
	 * When a message bundle's next instruction is
	 * [ParsingOperation.CHECK_ARGUMENT] (which must be all or nothing within an
	 * [A_BundleTree], this lazy prefilter map is populated. It maps from
	 * interesting [A_Bundle]s that might occur as an argument to an
	 * appropriately reduced message bundle tree (i.e., a message bundle tree
	 * containing precisely those method bundles that allow that argument. The
	 * only keys that occur are ones for which at least one restriction exists
	 * in at least one of the still possible [A_Bundle]s. When [unclassified] is
	 * empty, *all* such restricted argument message bundles occur in this map.
	 * Note that some of the resulting message bundle trees may be completely
	 * empty. Also note that some of the trees may be shared, so be careful to
	 * discard them rather than maintaining them when new method bundles or
	 * grammatical restrictions are added.
	 *
	 * When an argument is a message that is not restricted for any of the
	 * message bundles in this message bundle tree (i.e., it does not occur as a
	 * key in this map), then the sole entry in [lazyIncomplete] is used. The
	 * key is always the [ParsingOperation.CHECK_ARGUMENT], which all message
	 * bundles in this message bundle tree must have.
	 */
	private var lazyPrefilterMap = emptyMap

	/**
	 * An [A_Tuple] of pairs (2-tuples) where the first element is a
	 * [phrase&#32;type][PhraseTypeDescriptor] and the second element is an
	 * [A_ParsingPlanInProgress]. These should stay synchronized with
	 * [lazyTypeFilterTree].
	 */
	private var lazyTypeFilterPairsTuple: A_Tuple = emptyTuple

	/**
	 * The [type-testing&#32;tree][LookupTree] for handling the case that at
	 * least one [A_ParsingPlanInProgress] in this message bundle tree is at
	 * a [parsingPc][A_ParsingPlanInProgress.parsingPc] pointing to a
	 * [ParsingOperation.TYPE_CHECK_ARGUMENT] operation. This allows
	 * relatively efficient elimination of inappropriately typed arguments.
	 *
	 * Since navigating this tree while performing the indicated type tests
	 * is slower than other parsing operations, the [MessageSplitter]
	 * produces an [A_DefinitionParsingPlan] that usually postpones the type
	 * check of the latest parsed argument until after any successive fixed
	 * tokens have been consumed.
	 */
	private var lazyTypeFilterTree: LookupTree<A_Tuple, A_BundleTree>? = null

	/**
	 * The layout of integer slots for my instances.
	 */
	enum class IntegerSlots : IntegerSlotsEnum {
		/**
		 * [BitField]s for the hash and the parsing pc.  See below.
		 */
		HASH_AND_MORE;

		companion object {
			/**
			 * The hash, selected randomly at creation.
			 */
			val HASH = BitField(HASH_AND_MORE, 0, 32) { null }

			/**
			 * This flag is set when this bundle tree contains at least one
			 * parsing-plan-in-progress at a [ParsingOperation.JUMP_BACKWARD]
			 * instruction.
			 */
			val HAS_BACKWARD_JUMP_INSTRUCTION = BitField(HASH_AND_MORE, 32, 1) {
				(it != 0).toString()
			}

			/**
			 * This flag is set when a bundle tree is redirected to an
			 * equivalent ancestor bundle tree.  The current bundle tree's
			 * [latestBackwardJump] is set directly to the target of the cycle
			 * when this flag is set.
			 */
			val IS_SOURCE_OF_CYCLE = BitField(HASH_AND_MORE, 33, 1) {
				(it != 0).toString()
			}
		}
	}

	override fun allowsImmutableToMutableReferenceInField(
		e: AbstractSlotsEnum
	) = e === IntegerSlots.HASH_AND_MORE

	override fun printObjectOnAvoidingIndent(
		self: AvailObject,
		builder: StringBuilder,
		recursionMap: IdentityHashMap<A_BasicObject, Void>,
		indent: Int
	): Unit = with(builder) {
		append("BundleTree(")
		val plansInProgress = allPlansInProgress
		val bundleCount = plansInProgress.mapSize
		if (bundleCount <= 15) {
			val strings = mutableMapOf<String, Int>()
			plansInProgress.forEach { _, value: A_Map ->
				value.forEach { _, plansInProgress: A_Set ->
					plansInProgress.forEach { planInProgress ->
						val string = planInProgress.nameHighlightingPc
						strings[string] = strings.getOrDefault(string, 0) + 1
					}
				}
			}
			val sorted = mutableListOf<String>()
			for ((key, count) in strings) {
				sorted.add(
					if (count == 1) key else "$key(×$count)")
			}
			sort(sorted)
			if (bundleCount <= 3) {
				sorted.joinTo(builder, ", ")
			}
			else
			{
				val pre = buildString { newlineTab(indent) }
				sorted.joinTo(builder, pre, pre)
			}
		}
		else
		{
			append("$bundleCount entries")
		}
		append(")")
	}

	override fun o_DescribeForDebugger(
		self: AvailObject
	): Array<AvailObjectFieldHelper>
	{
		val fields = when
		{
			self.isSourceOfCycle || self.hasBackwardJump ->
				super.o_DescribeForDebugger(self).toMutableList()
			else -> mutableListOf()
		}
		if (unclassified.mapSize > 0)
		{
			fields.add(
				AvailObjectFieldHelper(
					self,
					DebuggerObjectSlots.DUMMY_DEBUGGER_SLOT,
					-1,
					unclassified,
					slotName = "*** unclassified",
					forcedName = "*** unclassified"))
		}
		if (latestBackwardJump.notNil)
		{
			fields.add(
				AvailObjectFieldHelper(
					self,
					DebuggerObjectSlots.DUMMY_DEBUGGER_SLOT,
					-1,
					latestBackwardJump,
					slotName = "latestBackwardJump",
					forcedName = "latestBackwardJump"))
		}
		if (lazyComplete.mapSize > 0)
		{
			fields.add(
				AvailObjectFieldHelper(
					self,
					DebuggerObjectSlots.DUMMY_DEBUGGER_SLOT,
					-1,
					lazyComplete,
					slotName = "lazyComplete",
					forcedName = "lazyComplete"))
		}
		if (lazyIncomplete.mapSize > 0)
		{
			fields.add(
				AvailObjectFieldHelper(
					self,
					DebuggerObjectSlots.DUMMY_DEBUGGER_SLOT,
					-1,
					lazyIncomplete,
					slotName = "lazyIncomplete",
					forcedName = "lazyIncomplete"))
		}
		if (lazyIncompleteCaseInsensitive.mapSize > 0)
		{
			fields.add(
				AvailObjectFieldHelper(
					self,
					DebuggerObjectSlots.DUMMY_DEBUGGER_SLOT,
					-1,
					lazyIncompleteCaseInsensitive,
					slotName = "lazyIncompleteCaseInsensitive",
					forcedName = "lazyIncompleteCaseInsensitive"))
		}
		val actions = self.lazyActions
		if (actions.mapSize > 0)
		{
			val actionEntries = actions.keysAsSet
				.sortedBy { it.extractInt }
				.map { key ->
					val keyInt = key.extractInt
					val operation = decode(keyInt)
					val operand = operand(keyInt)
					val description = operation.describe(operand)
					AvailObjectFieldHelper(
						self,
						DebuggerObjectSlots.DUMMY_DEBUGGER_SLOT,
						-1,
						actions.mapAt(key).toList().toTypedArray(),
						slotName = description,
						forcedName = description)
				}
			fields.add(
				AvailObjectFieldHelper(
					self,
					DebuggerObjectSlots.DUMMY_DEBUGGER_SLOT,
					-1,
					actionEntries.toTypedArray(),
					slotName = "lazyActions",
					forcedName = "lazyActions"))
		}
		if (lazyPrefilterMap.mapSize > 0)
		{
			fields.add(
				AvailObjectFieldHelper(
					self,
					DebuggerObjectSlots.DUMMY_DEBUGGER_SLOT,
					-1,
					lazyPrefilterMap,
					slotName = "lazyPrefilterMap",
					forcedName = "lazyPrefilterMap"))
		}
		if (lazyTypeFilterTree !== null)
		{
			fields.add(
				AvailObjectFieldHelper(
					self,
					DebuggerObjectSlots.DUMMY_DEBUGGER_SLOT,
					-1,
					lazyTypeFilterTree,
					slotName = "lazyTypeFilterTree",
					forcedName = "lazyTypeFilterTree"))
		}
		return fields.toTypedArray()
	}

	/**
	 * Add the plan to this bundle tree.  Use `self` as a monitor for mutual
	 * exclusion to ensure *changes* from multiple fibers won't interfere, *not*
	 * to ensure the mutual safety of [A_BundleTree.expand].
	 */
	override fun o_AddPlanInProgress(
		self: AvailObject,
		planInProgress: A_ParsingPlanInProgress
	) = lock.safeWrite {
		allPlansInProgress =
			layeredMapWithPlan(allPlansInProgress, planInProgress)
		unclassified = layeredMapWithPlan(unclassified, planInProgress)
		if (planInProgress.isBackwardJump)
		{
			self[HAS_BACKWARD_JUMP_INSTRUCTION] = 1
		}
	}

	override fun o_AllParsingPlansInProgress(self: AvailObject) =
		lock.read { allPlansInProgress }

	override fun o_Equals(self: AvailObject, another: A_BasicObject) =
		another.traversed().sameAddressAs(self)

	/**
	 * Expand the bundle tree if there's anything unclassified in it.
	 */
	override fun o_Expand(
		self: AvailObject,
		module: A_Module)
	{
		if (lock.read { unclassified.mapSize == 0 }) return
		lock.safeWrite {
			val theUnclassified = unclassified
			if (theUnclassified.mapSize == 0)
			{
				// Someone else expanded it since we checked with only the read
				// lock, above.
				return
			}
			val oldTypeFilterSize = lazyTypeFilterPairsTuple.tupleSize
			// Figure out what the latestBackwardJump will be for any successor
			// bundle trees that need to be created.
			val latestBack: A_BundleTree
			if (self[HAS_BACKWARD_JUMP_INSTRUCTION] != 0)
			{
				// New descendants will point to me as a potential target.
				if (self[IS_SOURCE_OF_CYCLE] != 0)
				{
					// It was already the source of a backward link.  We don't
					// need to create any more descendants here.
					return
				}
				// It's not already the source of a cycle.  See if we can
				// find an equivalent ancestor to cycle back to.
				var ancestor: A_BundleTree = latestBackwardJump
				while (ancestor.notNil) {
					if (ancestor.allParsingPlansInProgress.equals(
							allPlansInProgress))
					{
						// This ancestor is equivalent to me, so mark me as a
						// backward cyclic link and plug that exact ancestor
						// into the LATEST_BACKWARD_JUMP slot.
						self[IS_SOURCE_OF_CYCLE] = 1
						latestBackwardJump = ancestor
						// The caller will deal with fully expanding the
						// ancestor.
						return
					}
					ancestor = ancestor.latestBackwardJump
				}
				// We didn't find a usable ancestor to cycle back to. New
				// successors should link back to me.
				latestBack = self
			}
			else
			{
				// This bundle tree doesn't have a backward jump, so any new
				// descendants should use the same LATEST_BACKWARD_JUMP as me.
				latestBack = latestBackwardJump
			}

			// Update my components.
			theUnclassified.forEach { bundle, defToPlansInProgress: A_Map ->
				defToPlansInProgress.forEach { def, plansInProgress: A_Set ->
					plansInProgress.forEach { planInProgress ->
						val pc = planInProgress.parsingPc
						val plan = planInProgress.parsingPlan
						val instructions = plan.parsingInstructions
						if (pc == instructions.tupleSize + 1) {
							// Just reached the end of these instructions.
							// It's past the end of the parsing instructions.
							lazyComplete =
								lazyComplete.mapAtReplacingCanDestroy(
									bundle, emptySet, true
								) { _, defs ->
									defs.setWithElementCanDestroy(def, true)
								}
						}
						else
						{
							val timeBefore = AvailRuntimeSupport.captureNanos()
							val instruction = instructions.tupleIntAt(pc)
							val op = decode(instruction)
							updateForPlan(self, plan, pc, module)
							val timeAfter = AvailRuntimeSupport.captureNanos()
							op.expandingStatisticInNanoseconds.record(
								timeAfter - timeBefore,
								Interpreter.currentIndexOrZero())
						}
					}
				}
			}
			// Make the updates shared.
			lazyComplete = lazyComplete.makeShared()
			lazyIncomplete = lazyIncomplete.makeShared()
			lazyIncompleteCaseInsensitive =
				lazyIncompleteCaseInsensitive.makeShared()
			lazyActions = lazyActions.makeShared()
			lazyPrefilterMap = lazyPrefilterMap.makeShared()
			lazyTypeFilterPairsTuple = lazyTypeFilterPairsTuple.makeShared()

			if (lazyTypeFilterPairsTuple.tupleSize != oldTypeFilterSize)
			{
				// Rebuild the type-checking lookup tree.
				lazyTypeFilterTree = ParserTypeChecker.createRoot(
					toList(lazyTypeFilterPairsTuple),
					listOf(
						restrictionForType(
							PARSE_PHRASE.mostGeneralType, BOXED_FLAG)),
					latestBack)
			}
			// Do this volatile write last for correctness.
			unclassified = emptyMap
		}
	}

	/**
	 * An [A_GrammaticalRestriction] has been added.  Update this bundle tree
	 * and any relevant successors related to the given
	 * [A_ParsingPlanInProgress] to agree with the new restriction.
	 */
	override fun o_UpdateForNewGrammaticalRestriction(
		self: AvailObject,
		planInProgress: A_ParsingPlanInProgress,
		treesToVisit: Deque<Pair<A_BundleTree, A_ParsingPlanInProgress>>
	) = lock.safeWrite {
		val plan = planInProgress.parsingPlan
		if (unclassified.hasKey(plan.bundle))
		{
			// The plan (or another plan with the same bundle) is still
			// unclassified, so do nothing.
			return
		}
		val instructions = plan.parsingInstructions
		val pcsToVisit: Deque<Int> = ArrayDeque()
		pcsToVisit.add(planInProgress.parsingPc)
		while (!pcsToVisit.isEmpty())
		{
			val pc = pcsToVisit.removeLast()
			if (pc == instructions.tupleSize + 1)
			{
				// We've reached an end-point for parsing this plan.  The
				// grammatical restriction has no remaining effect.
				return
			}
			val instruction = instructions.tupleIntAt(pc)
			when (val op = decode(instruction))
			{
				JUMP_BACKWARD, JUMP_FORWARD, BRANCH_FORWARD ->
				{
					// These should have bubbled out of the bundle tree.
					// Loop to get to the affected successor trees.
					pcsToVisit.addAll(op.successorPcs(instruction, pc))
				}
				CHECK_ARGUMENT, TYPE_CHECK_ARGUMENT ->
				{
					// Keep it simple and invalidate this entire bundle
					// tree.
					invalidate(self)
				}
				PARSE_PART ->
				{
					// Look it up in LAZY_INCOMPLETE.
					val keywordIndex = op.keywordIndex(instruction)
					val keyword: A_String =
						plan.bundle.messagePart(keywordIndex)
					val submap: A_Map = lazyIncomplete.mapAt(keyword)
					val successor: A_BundleTree =
						submap.mapAt(fromInt(keywordIndex))
					treesToVisit.add(
						successor to newPlanInProgress(plan, pc + 1))
				}
				PARSE_PART_CASE_INSENSITIVELY ->
				{
					// Look it up in LAZY_INCOMPLETE_CASE_INSENSITIVE.
					val keywordIndex = op.keywordIndex(instruction)
					val keyword: A_String =
						plan.bundle.messagePart(keywordIndex)
					val submap: A_Map =
						lazyIncompleteCaseInsensitive.mapAt(keyword)
					val successor: A_BundleTree =
						submap.mapAt(fromInt(keywordIndex))
					treesToVisit.add(
						successor to newPlanInProgress(plan, pc + 1))
				}
				else ->
				{
					// It's an ordinary action.  Each JUMP and BRANCH was
					// already dealt with in a previous case.
					val successors: A_Tuple =
						lazyActions.mapAt(instructions.tupleAt(pc))
					for (successor in successors) {
						treesToVisit.add(
							successor to newPlanInProgress(plan, pc + 1))
					}
				}
			}
		}
	}

	override fun o_Hash(self: AvailObject) = self[HASH]

	override fun o_Kind(self: AvailObject) = Types.MESSAGE_BUNDLE_TREE.o

	override fun o_LazyActions(self: AvailObject) = lock.read { lazyActions }

	override fun o_LazyComplete(self: AvailObject): A_Map =
		lock.read { lazyComplete }

	override fun o_LazyIncomplete(self: AvailObject) =
		lock.read { lazyIncomplete }

	override fun o_LazyIncompleteCaseInsensitive(self: AvailObject) =
		lock.read { lazyIncompleteCaseInsensitive }

	override fun o_LazyPrefilterMap(self: AvailObject) =
		lock.read { lazyPrefilterMap }

	//	lazyTypeFilterPairs
	override fun o_LazyTypeFilterTree(self: AvailObject) =
		lock.read { lazyTypeFilterTree }

	/**
	 * Remove the plan from this bundle tree.  We don't need to remove the
	 * bundle itself if this is the last plan for that bundle, since this can
	 * only be called when satisfying a forward declaration – by adding another
	 * definition.
	 */
	override fun o_RemovePlanInProgress(
		self: AvailObject,
		planInProgress: A_ParsingPlanInProgress
	) = lock.safeWrite {
		allPlansInProgress =
			layeredMapWithoutPlan(allPlansInProgress, planInProgress)
		unclassified = layeredMapWithoutPlan(unclassified, planInProgress)
	}

	/**
	 * Answer the nearest ancestor that was known at some time to have a
	 * backward jump in at least one of its parsing-plans-in-progress.
	 *
	 * @param self
	 *   The bundle tree.
	 * @return
	 *   A predecessor bundle tree or nil.
	 */
	override fun o_LatestBackwardJump(self: AvailObject): A_BundleTree =
		lock.read { latestBackwardJump }

	override fun o_HasBackwardJump(self: AvailObject): Boolean =
		self[HAS_BACKWARD_JUMP_INSTRUCTION] != 0

	override fun o_IsSourceOfCycle(self: AvailObject): Boolean =
		self[IS_SOURCE_OF_CYCLE] != 0

	override fun o_IsSourceOfCycle(
		self: AvailObject,
		isSourceOfCycle: Boolean)
	{
		self[IS_SOURCE_OF_CYCLE] = if (isSourceOfCycle) 1 else 0
	}

	override fun mutable() = unsupported

	override fun immutable() = unsupported

	override fun shared(): MessageBundleTreeDescriptor
	{
		assert(isShared)
		return this
	}

	/**
	 * Invalidate the internal expansion of the given bundle tree.  Note that
	 * this should only happen when we're changing the grammar in some way,
	 * which happens mutually exclusive of parsing, so we don't really need to
	 * use a lock.
	 *
	 * @param self
	 *   Which [A_BundleTree] to invalidate.
	 */
	private fun invalidate(
		@Suppress("UNUSED_PARAMETER")
		self: A_BundleTree
	) = invalidationsStat.record {
		lock.safeWrite {
			lazyComplete = emptyMap
			lazyIncomplete = emptyMap
			lazyIncompleteCaseInsensitive = emptyMap
			lazyActions = emptyMap
			lazyPrefilterMap = emptyMap
			lazyTypeFilterPairsTuple = emptyTuple
			lazyTypeFilterTree = null
			unclassified = allPlansInProgress
		}
	}

	/**
	 * Categorize a single parsing plan in progress.
	 *
	 * @param bundleTree
	 *   The [A_BundleTree] that we're updating.  The state is passed
	 *   separately in arguments, to be written back after all the mutable
	 *   arguments have been updated for all parsing-plans-in-progress.
	 * @param plan
	 *   The [A_DefinitionParsingPlan] to categorize.
	 * @param pc
	 *   The one-based program counter that indexes each applicable
	 *   [plan][A_DefinitionParsingPlan]'s
	 *   [instructions][A_DefinitionParsingPlan.parsingInstructions]. Note
	 *   that this value can be one past the end of the instructions,
	 *   indicating parsing is complete.
	 * @param module
	 *   The [A_Module] that limits the scope of information used for
	 *   parsing.  This is used to restrict the visibility of semantic and
	 *   grammatical restrictions, as well as which method and macro
	 *   [A_Definition]s can be parsed.
	 */
	private fun updateForPlan(
		bundleTree: AvailObject,
		plan: A_DefinitionParsingPlan,
		pc: Int,
		module: A_Module)
	{
		val hasBackwardJump =
			bundleTree[HAS_BACKWARD_JUMP_INSTRUCTION] != 0
		val latestBackward: A_BundleTree =
			if (hasBackwardJump) bundleTree
			else bundleTree.latestBackwardJump
		val instructions = plan.parsingInstructions
		if (pc == instructions.tupleSize + 1)
		{
			lazyComplete = lazyComplete.mapAtReplacingCanDestroy(
				plan.bundle, emptySet, true
			) { _, defs ->
				defs.setWithElementCanDestroy(plan.definition, true)
			}
			return
		}
		val instruction = plan.parsingInstructions.tupleIntAt(pc)
		val op = decode(instruction)
		when (op) {
			JUMP_FORWARD, BRANCH_FORWARD, JUMP_BACKWARD ->
			{
				if (op == JUMP_BACKWARD && !hasBackwardJump)
				{
					// We just discovered the first backward jump in any
					// parsing-plan-in-progress at this node.
					bundleTree[HAS_BACKWARD_JUMP_INSTRUCTION] = 1
				}
				// Bubble control flow right out of the bundle trees. There
				// should never be a JUMP or BRANCH in an actionMap. Rather, the
				// successor instructions (recursively in the case of jumps to
				// other jumps) are directly exploded into the current bundle
				// tree.  Not only does this save the cost of dispatching these
				// control flow operations, but it also allows more potential
				// matches for the next token to be undertaken in a single
				// lookup. We can safely recurse here, because plans cannot have
				// any empty loops due to progress check instructions.
				for (nextPc in op.successorPcs(instruction, pc))
				{
					updateForPlan(bundleTree, plan, nextPc, module)
				}
				return
			}
			PARSE_PART ->
			{
				// Parse a specific keyword.
				val keywordIndex = op.keywordIndex(instruction)
				val keyword = plan.bundle.messagePart(keywordIndex)
				lazyIncomplete = lazyIncomplete.mapAtReplacingCanDestroy(
					keyword, emptyMap, true
				) { _, submap ->
					submap.mapAtReplacingCanDestroy(
						fromInt(keywordIndex), nil, true
					) { _, originalSubtree ->
						val subtree: A_BundleTree = originalSubtree.ifNil {
							newBundleTree(latestBackward)
						}
						subtree.addPlanInProgress(
							newPlanInProgress(plan, pc + 1))
						subtree
					}
				}
				return
			}
			PARSE_PART_CASE_INSENSITIVELY ->
			{
				// Parse a specific case-insensitive keyword.
				val keywordIndex = op.keywordIndex(instruction)
				val keyword = plan.bundle.messagePart(keywordIndex)
				lazyIncompleteCaseInsensitive =
					lazyIncompleteCaseInsensitive.mapAtReplacingCanDestroy(
						keyword, emptyMap, true
					) { _, submap ->
						submap.mapAtReplacingCanDestroy(
							fromInt(keywordIndex), nil, true
						) { _, originalSubtree ->
							val subtree: A_BundleTree = originalSubtree.ifNil {
								newBundleTree(latestBackward)
							}
							subtree.addPlanInProgress(
								newPlanInProgress(plan, pc + 1))
							subtree
						}
					}
				return
			}
			PREPARE_TO_RUN_PREFIX_FUNCTION ->
			{
				// Each macro definition has its own prefix functions, so for
				// each plan create a separate successor message bundle tree.
				// After a complete macro has been parsed, we will eliminate
				// paths that used a macro definition that wasn't the most
				// specific for the actual arguments that were parsed.
				lazyActions = lazyActions.mapAtReplacingCanDestroy(
					fromInt(instruction), emptyTuple, true
				) { _, successors ->
					val newTarget: A_BundleTree = newBundleTree(latestBackward)
					newTarget.addPlanInProgress(newPlanInProgress(plan, pc + 1))
					successors.appendCanDestroy(newTarget, true)
				}
				// We added it to the actions, so don't fall through.
				return
			}
			TYPE_CHECK_ARGUMENT ->
			{
				// An argument was just parsed and passed its grammatical
				// restriction check.  Now it needs to do a type check with a
				// type-dispatch tree.
				val typeIndex = op.typeCheckArgumentIndex(instruction)
				val phraseType: A_Type = constantForIndex(typeIndex)
				val planInProgress = newPlanInProgress(plan, pc + 1)
				val pair = tuple(phraseType, planInProgress)
				lazyTypeFilterPairsTuple = lazyTypeFilterPairsTuple
					.appendCanDestroy(pair, true)
				// The new tuple size will be detected after all the updates.
				return
			}
			CHECK_ARGUMENT ->
			{
				// It's a checkArgument instruction.
				val checkArgumentIndex = op.checkArgumentIndex(instruction)
				// Add it to the action map.
				val instructionObject: A_Number = fromInt(instruction)
				val successors: A_Tuple? =
					lazyActions.mapAtOrNull(instructionObject)
				val successor: A_BundleTree = when (successors)
				{
					null -> newBundleTree(latestBackward).also { tree ->
						lazyActions = lazyActions.mapAtPuttingCanDestroy(
							instructionObject, tuple(tree), true)
					}
					else -> {
						assert(successors.tupleSize == 1)
						successors.tupleAt(1)
					}
				}
				var forbiddenBundles = emptySet
				plan.bundle.grammaticalRestrictions.forEach { restriction ->
					// Exclude grammatical restrictions that aren't defined in
					// an ancestor module.
					val definitionModule = restriction.definitionModule()
					if (definitionModule.isNil
						|| module.hasAncestor(definitionModule))
					{
						val bundles: A_Set =
							restriction.argumentRestrictionSets().tupleAt(
								checkArgumentIndex)
						forbiddenBundles =
							forbiddenBundles.setUnionCanDestroy(bundles, true)
					}
				}
				val planInProgress = newPlanInProgress(plan, pc + 1)
				// Add it to every existing branch where it's permitted.
				lazyPrefilterMap.forEach { bundle, prefilterSuccessor ->
					if (!forbiddenBundles.hasElement(bundle))
					{
						prefilterSuccessor.addPlanInProgress(planInProgress)
					}
				}
				// Add branches for any new restrictions.  Pre-populate with
				// every bundle present thus far, since none of them had this
				// restriction.
				forbiddenBundles.forEach { restrictedBundle ->
					if (!lazyPrefilterMap.hasKey(restrictedBundle)) {
						val newTarget = newBundleTree(latestBackward)
						// Be careful.  We can't add ALL_BUNDLES, since it may
						// contain some bundles that are still UNCLASSIFIED.
						// Instead, use ALL_BUNDLES of the successor found under
						// this instruction, since it *has* been kept up to date
						// as the bundles have gotten classified.
						successor.allParsingPlansInProgress.forEach {
								_, defMap ->
							defMap.forEach { _, plans ->
								plans.forEach { inProgress ->
									newTarget.addPlanInProgress(inProgress)
								}
							}
						}
						lazyPrefilterMap =
							lazyPrefilterMap.mapAtPuttingCanDestroy(
								restrictedBundle, newTarget, true)
					}
				}
				// Finally, add it to the action map.  This had to be postponed,
				// since we didn't want to add it under any new restrictions,
				// and the actionMap is what gets visited to populate new
				// restrictions.
				successor.addPlanInProgress(planInProgress)
				// Note:  Fall out of the when{} here, since the action also has
				// to be added to the actionMap (to deal with the case that a
				// subexpression is a non-send, or a send that is not forbidden
				// in this position by *any* potential parent sends).
			}
			else ->
			{
				// Fall out of when{} for all other operations.
			}
		}
		// It's not a keyword parsing instruction or a type-check or
		// preparation for a prefix function, so it's an ordinary parsing
		// instruction.  It might be a CHECK_ARGUMENT that has already
		// updated the prefilterMap and fallen out. Control flow
		// instructions should have been dealt with in a prior case.
		val nextPcs = op.successorPcs(instruction, pc)
		assert(nextPcs.size == 1 && nextPcs[0] == pc + 1)
		val instructionObject: A_Number = fromInt(instruction)
		val successors: A_Tuple? = lazyActions.mapAtOrNull(instructionObject)
		val successor: A_BundleTree = if (successors !== null)
		{
			assert(successors.tupleSize == 1)
			successors.tupleAt(1)
		}
		else
		{
			newBundleTree(latestBackward).also { tree ->
				lazyActions = lazyActions.mapAtPuttingCanDestroy(
					instructionObject, tuple(tree), true)
			}
		}
		successor.addPlanInProgress(newPlanInProgress(plan, pc + 1))
	}

	companion object {
		/**
		 * This is the [LookupTreeAdaptor] for building and navigating the
		 * [lazyTypeFilterTree]s.  It gets built from 2-tuples containing a
		 * [phrase&#32;type][PhraseTypeDescriptor] and a corresponding
		 * [A_ParsingPlanInProgress].  The type is used to perform type
		 * filtering after parsing each leaf argument, and the phrase type is
		 * the expected type of that latest argument.
		 */
		object ParserTypeChecker :
			LookupTreeAdaptor<A_Tuple, A_BundleTree, A_BundleTree>()
		{
			override val emptyLeaf by lazy {
				LeafLookupTree<A_Tuple, A_BundleTree>(newBundleTree(nil))
			}

			// Extract the phrase type from the pair, and use it directly as
			// the signature type for the tree.
			override fun extractSignature(element: A_Tuple): A_Type =
				tupleTypeForTypes(element.tupleAt(1))

			override fun constructResult(
				elements: List<A_Tuple>,
				memento: A_BundleTree
			) = newBundleTree(latestBackwardJump = memento).apply {
				elements.forEach { (_, planInProgress) ->
					addPlanInProgress(planInProgress)
				}
			}

			override fun compareTypes(
				argumentRestrictions: List<TypeRestriction>,
				signatureType: A_Type
			) = compareForParsing(argumentRestrictions, signatureType)

			override fun testsArgumentPositions() = false

			override fun subtypesHideSupertypes() = false
		}

		/**
		 * Add the [A_ParsingPlanInProgress] to the given [A_Map].  The map is
		 * from [A_Bundle] to a submap, which is from [A_Definition] to a set of
		 * [plans-in-progress][A_ParsingPlanInProgress] for that
		 * bundle/definition pair.
		 *
		 * @param outerMap
		 *   {bundle → {definition → {plan-in-progress |}|}|}
		 * @param planInProgress
		 *   The [A_ParsingPlanInProgress] to add.
		 * @return
		 *   The map of maps of plans-in-progress.
		 */
		private fun layeredMapWithPlan(
			outerMap: A_Map,
			planInProgress: A_ParsingPlanInProgress
		): A_Map
		{
			val plan = planInProgress.parsingPlan
			return outerMap.mapAtReplacingCanDestroy(
				plan.bundle, emptyMap, true
			) { _, submap ->
				submap.mapAtReplacingCanDestroy(
					plan.definition, emptySet, true
				) { _, inProgressSet ->
					inProgressSet.setWithElementCanDestroy(planInProgress, true)
				}
			}.makeShared()
		}

		/** A [Statistic] for tracking bundle tree invalidations. */
		private val invalidationsStat = Statistic(
			EXPANDING_PARSING_INSTRUCTIONS, "(invalidations)")

		/**
		 * Remove the [A_ParsingPlanInProgress] from the given map.  The map is
		 * from [A_Bundle] to a submap, which is from [A_Definition] to a set of
		 * [plans-in-progress][A_ParsingPlanInProgress] for that
		 * bundle/definition pair.
		 *
		 * @param outerMap
		 *   {bundle → {definition → {plan-in-progress |}|}|}
		 * @param planInProgress
		 *   The [A_ParsingPlanInProgress] to remove.
		 * @return
		 *   The new map of maps, with the plan-in-progress removed.
		 */
		private fun layeredMapWithoutPlan(
			outerMap: A_Map,
			planInProgress: A_ParsingPlanInProgress
		): A_Map
		{
			val plan = planInProgress.parsingPlan
			val bundle = plan.bundle
			val definition = plan.definition
			var submap: A_Map = outerMap.mapAtOrNull(bundle) ?: return outerMap
			var inProgressSet: A_Set =
				submap.mapAtOrNull(definition) ?: return outerMap
			if (!inProgressSet.hasElement(planInProgress)) {
				return outerMap
			}
			inProgressSet = inProgressSet.setWithoutElementCanDestroy(
				planInProgress, true)
			submap =
				if (inProgressSet.setSize > 0)
				{
					submap.mapAtPuttingCanDestroy(
						definition, inProgressSet, true)
				}
				else
				{
					submap.mapWithoutKeyCanDestroy(definition, true)
				}
			val newOuterMap =
				if (submap.mapSize > 0)
				{
					outerMap.mapAtPuttingCanDestroy(bundle, submap, true)
				}
				else outerMap.mapWithoutKeyCanDestroy(bundle, true)
			return newOuterMap.makeShared()
		}

		/**
		 * Create a new empty [A_BundleTree].
		 *
		 * @param latestBackwardJump
		 *   The nearest ancestor bundle tree that was known at some point to
		 *   contain a backward jump instruction, or nil if there were no such
		 *   ancestors.
		 * @return
		 *   A new empty message bundle tree.
		 */
		fun newBundleTree(
			latestBackwardJump: A_BundleTree
		): A_BundleTree
		{
			val descriptor = MessageBundleTreeDescriptor(
				SHARED, latestBackwardJump)
			return AvailObject.newIndexedDescriptor(0, descriptor).apply {
				setSlot(HASH, nextNonzeroHash())
			}
		}
	}
}
