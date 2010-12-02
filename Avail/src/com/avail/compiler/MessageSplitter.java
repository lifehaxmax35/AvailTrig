/**
 * com.avail.compiler/MessageSplitter.java
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

package com.avail.compiler;

import static com.avail.descriptor.AvailObject.error;
import java.util.ArrayList;
import java.util.List;
import com.avail.compiler.scanner.AvailScanner;
import com.avail.descriptor.AvailObject;
import com.avail.descriptor.ByteStringDescriptor;
import com.avail.descriptor.IntegerDescriptor;
import com.avail.descriptor.MethodSignatureDescriptor;
import com.avail.descriptor.TupleDescriptor;
import com.avail.descriptor.TupleTypeDescriptor;
import com.avail.descriptor.TypeDescriptor;

/**
 * This class is used to split Avail message names into a sequence of
 * pseudo-instructions that can be used directly for parsing.  The
 * pseudo-instructions are of the form:
 * 
 * <p><ul>
 * <li>0     - parseArgument</li>
 * <li>1     - pushEmptyList</li>
 * <li>2     - append (pop A, append A to list on top of stack)</li>
 * <li>3     - (reserved)</li>
 * <li>4*N   - branch to instruction N (attempt to continue parsing at both
 *             the next instruction and N)</li>
 * <li>4*N+1 - jump to instruction N (do not attempt to continue at the next
 *             instruction)</li>
 * <li>4*N+2 - parseKeyword at part N</li>
 * <li>4*N+3 - (reserved)</li>
 * </ul></p>
 *
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 */
public class MessageSplitter
{
	/**
	 * The Avail string to be parsed. 
	 */
	final AvailObject messageName;

	/**
	 * The individual tokens (strings) constituting the message.  Alphanumerics
	 * are in runs, separated from other alphanumerics by a single space.
	 * Operator characters are never beside spaces, and are always parsed as
	 * individual tokens.  Open chevron («), double dagger (‡), and close
	 * chevron (») are used to indicate repeated/optional substructures.  The
	 * backquote (`) can precede any operator character to ensure it is not
	 * used in a special way like chevrons and double dagger.  A backquote may
	 * also escape another backquote.
	 */
	final List<AvailObject> messageParts = new ArrayList<AvailObject>(10);
	
	/**
	 * The current one-based parsing position in the list of tokens.
	 */
	int messagePartPosition;
	
	/**
	 * A list of integers representing parsing instructions.  These instructions
	 * can parse a specific keyword, recursively parse an argument, branch for
	 * backtracking, and manipulate a stack of parse nodes.
	 */
	final List<Integer> instructions = new ArrayList<Integer>(10);

	/**
	 * The top-most {@link Group}.
	 */
	private Group rootGroup;

	
	/**
	 * An {@linkplain Expression} represents a structural view of part of the
	 * message name.
	 */
	abstract class Expression
	{

		/**
		 * Write instructions for parsing me to the given list.
		 * @param list The list of integers {@link MessageSplitter encoding}
		 * parsing instructions.
		 */
		void emitOn (List<Integer> list)
		{
			// Do nothing.
		}
		
		
		/**
		 * Answer whether or not this an argument or group.
		 * @return True iff this is an argument or group.
		 */
		boolean isArgumentOrGroup ()
		{
			return false;
		}
		

		/**
		 * Check that the given type signature is appropriate for this message
		 * expression.  If not, throw a suitable exception.
		 * <p>
		 * This is also called recursively on subcomponents, and it checks that
		 * group arguments have the correct structure for what will be parsed.
		 * The method may reject parses based on the number of repetitions of a
		 * group at a call site, but not the number of arguments actually
		 * delivered by each repetition.  For example, the message "«_:_‡,»" can
		 * limit the number of _:_ pairs to at most 5 by declaring the tuple
		 * type's size to be [5..5].  However, the message "«_:_‡[_]»" will
		 * always produce a tuple of 3-tuples followed by a 2-tuple (if any
		 * elements at all occur).  Attempting to add a method implementation
		 * for this message that only accepted a tuple of 7-tuples would be
		 * inappropriate (and ineffective).  Instead, it should be required to
		 * accept a tuple whose size is in the range [2..3].
		 * <p>
		 * Note that the outermost (pseudo)group represents the entire message,
		 * so the caller should synthesize a fixed-length {@link
		 * TupleTypeDescriptor tuple type} for the outermost check.
		 * 
		 * @param argumentType A {@link TupleTypeDescriptor tuple type}
		 *                     describing the types of arguments that a method
		 *                     being added will accept.
		 */
		public void checkType (AvailObject argumentType)
		{
			return;
		}
		
		
		@Override
		public String toString ()
		{
			return getClass().getSimpleName();
		}
	};

	
	/**
	 * A {@linkplain Simple} is an {@linkplain Expression} that represents a
	 * single token, except for the double-dagger character.
	 */
	final class Simple extends Expression
	{
		/**
		 * The one-based index of this token within the {@link
		 * MessageSplitter#messageParts message parts}. 
		 */
		final int tokenIndex;

		
		/**
		 * Construct a new {@link Simple} expression representing a specific
		 * token expected in the input.
		 *
		 * @param tokenIndex The one-based index of the token within the {@link
		 * MessageSplitter#messageParts message parts}.
		 */
		Simple (int tokenIndex)
		{
			this.tokenIndex = tokenIndex;
		}

		
		@Override
		void emitOn (List<Integer> list)
		{
			super.emitOn(list);
			// Parse the specific keyword.
			list.add(tokenIndex * 4 + 2);
		}
		
		
		@Override
		public String toString ()
		{
			StringBuilder builder = new StringBuilder();
			builder.append(getClass().getSimpleName());
			builder.append("(");
			builder.append(messageParts.get(tokenIndex - 1).asNativeString());
			return builder.toString();
		}

		
		@Override
		public void checkType (AvailObject argumentType)
		{
			assert false : "checkType() should not be called for Simple" +
					" expressions";
		}

	}

	
	/**
	 * An {@linkplain Argument} is an occurrence of "_" in a message name.  It
	 * indicates where an argument is expected.
	 */
	final class Argument extends Expression
	{
		@Override
		boolean isArgumentOrGroup ()
		{
			return true;
		}

		
		@Override
		void emitOn (List<Integer> list)
		{
			super.emitOn(list);
			list.add(0);  // parseArgument
		}

		
		/**
		 * A simple underscore can be arbitrarily restricted, other than when
		 * it is restricted to the uninstantiable type {@link
		 * TypeDescriptor.Types#terminates terminates}.
		 */
		@Override
		public void checkType (AvailObject argumentType)
		{
			if (argumentType.equals(TypeDescriptor.Types.terminates.object()))
			{
				error("Method argument type should not be \"terminates\".");
			}
			return;
		}
	}


	/**
	 * A {@linkplain Group} is delimited by the open chevron ("«") and close
	 * chevron ("»") characters, and may contain subgroups and an occurrence of
	 * a double dagger ("‡").  If no dagger or subgroup is present, the sequence
	 * of message parts between the chevrons are allowed to occur zero or more
	 * times at a call site (i.e., a send of this message).  When the number of
	 * underscores ("_") plus the number of subgroups is exactly one, the
	 * argument (or subgroup) values are assembled into a list.  Otherwise the
	 * arguments and/or subgroups are assembled into a list of fixed-sized
	 * lists, each containing one entry for each argument or subgroup.
	 * <p>
	 * When a dagger occurs in a group, the parts to the left of the dagger
	 * can occur zero or more times, but separated by the parts to the right.
	 * For example, "«_‡,»" is how to specify a comma-separated list of
	 * arguments.  This pattern contains a single underscore and no subgroups,
	 * so parsing "1,2,3" would simply produce the list <1,2,3>.  The pattern
	 * "«_=_;»" will parse "1=2;3=4;5=6;" into <<1,2>,<3,4>,<5,6>> because it
	 * has two underscores.
	 * <p>
	 * The message "«A_‡x_»" parses zero or more occurrences in the text of the
	 * keyword "A" followed by an argument, separated by the keyword "x" and an
	 * argument.  "A 1 x 2 A 3 x 4 A 5" is such an expression (and "A 1 x 2" is
	 * not).  In this case, the arguments will be grouped in such a way that the
	 * final element of the list, if any, is missing the post-dagger elements:
	 * <<1,2>,<3,4>,<5>>.
	 */
	final class Group extends Expression
	{
		/**
		 * Whether a double dagger (‡) has been encountered in the tokens for
		 * this group.
		 */
		boolean hasDagger = false;
		
		/**
		 * How many argument tokens (_) were specified prior to the double
		 * dagger (or the end of the group if no double dagger is present).
		 */
		int argumentsBeforeDagger = 0;
		
		/**
		 * How many argument tokens (_) appeared after the double dagger, or
		 * zero if there was no double dagger. 
		 */
		int argumentsAfterDagger = 0;
		
		/**
		 * The expressions that appeared before the double dagger, or in the
		 * entire subexpression if no double dagger is present. 
		 */
		List<Expression> expressionsBeforeDagger = new ArrayList<Expression>();
		
		/**
		 * The expressions that appeared after the double dagger, or an empty
		 * list if no double dagger is present. 
		 */
		List<Expression> expressionsAfterDagger = new ArrayList<Expression>();
		
		/**
		 * The one-based position in the instruction stream to branch to in
		 * order to parse zero occurrences of this group.  Set during the first
		 * pass of code generation.
		 */
		int loopSkip = -1;
		
		/**
		 * The one-based position in the instruction stream to branch to from
		 * the dagger's position within the loop for this group.  Depending on
		 * the number of arguments and subgroups specified within this group,
		 * this may or may not equal {@link #loopSkip}.  If not equal, the only
		 * intervening instruction will be an append to add the final partial
		 * group to the list.
		 */
		int loopExit = -1;

		/**
		 * Add an expression to the group, either before or after the dagger,
		 * depending on whether hasDagger has been set.
		 * 
		 * @param e The expression to add.
		 */
		final void addExpression (Expression e)
		{
			if (!hasDagger)
			{
				expressionsBeforeDagger.add(e);
				if (e.isArgumentOrGroup())
				{
					argumentsBeforeDagger++;
				}
			}
			else
			{
				expressionsAfterDagger.add(e);
				if (e.isArgumentOrGroup())
				{
					argumentsAfterDagger++;
				}
			}
		}
		
		
		@Override
		boolean isArgumentOrGroup ()
		{
			return true;
		}
		
		
		/**
		 * Determine if this group should generate a tuple of plain arguments
		 * or a tuple of fixed-length tuples of plain arguments.
		 * 
		 * @return True if this group will generate a tuple of fixed-length
		 *         tuples, otherwise false (and the group will generate a tuple
		 *         of individual arguments or subgroups).
		 */
		boolean needsDoubleWrapping ()
		{
			return argumentsBeforeDagger != 1 || argumentsAfterDagger != 0; 
		}

		
		@Override
		void emitOn (List<Integer> list)
		{
			super.emitOn(list);
			if (!needsDoubleWrapping())
			{
				/* Special case -- one argument case produces a list of
				 * expressions rather than a list of fixed-length lists of
				 * expressions.  The generated instructions should look like:
				 * 
				 * push empty list
				 * branch to @loopSkip
				 * @loopStart:
				 * ...Stuff before dagger.
				 * append  (add solution)
				 * branch to @loopExit (even if no dagger)
				 * ...Stuff after dagger, nothing if dagger is omitted.  Must
				 * ...follow argument or subgroup with "append" instruction.
				 * jump to @loopStart
				 * @loopExit:
				 * @loopSkip:
				 */
				list.add(1);  // push empty list
				list.add(4 * loopSkip);  // branch to @loopSkip
				int loopStart = list.size() + 1;
				for (Expression expression : expressionsBeforeDagger)
				{
					expression.emitOn(list);
				}
				list.add(2);  // append
				list.add(4 * loopExit);  // branch to @loopExit
				for (Expression expression : expressionsAfterDagger)
				{
					assert !expression.isArgumentOrGroup();
					expression.emitOn(list);
				}
				list.add(loopStart * 4 + 1);  // jump to @loopStart
				loopExit = list.size() + 1;
				loopSkip = list.size() + 1;
			}
			else
			{
				/* General case -- the individual arguments need to be wrapped
				 * with "append" as for the special case above, but the start
				 * of each loop has to push an empty tuple, the dagger has to
				 * branch to a special @loopExit that closes the last (partial)
				 * group, and the backward jump should be preceded by an append
				 * to capture a solution.  Here's the code:
				 * 
				 * push empty list (the list of solutions)
				 * branch to @loopSkip
				 * @loopStart:
				 * push empty list (a compound solution)
				 * ...Stuff before dagger, where arguments and subgroups must
				 * ...be followed by "append" instruction.
				 * branch to @loopExit
				 * ...Stuff after dagger, nothing if dagger is omitted.  Must
				 * ...follow argument or subgroup with "append" instruction.
				 * append  (add complete solution)
				 * jump @loopStart
				 * @loopExit:
				 * append  (add partial solution up to dagger)
				 * @loopSkip:
				 */
				list.add(1);  // push empty list
				list.add(4 * loopSkip);  // branch to @loopSkip
				int loopStart = list.size() + 1;
				list.add(1);  // inner list
				for (Expression expression : expressionsBeforeDagger)
				{
					expression.emitOn(list);
					if (expression.isArgumentOrGroup())
					{
						list.add(2);  // append
					}
				}
				list.add(4 * loopExit);  // branch to @loopExit
				for (Expression expression : expressionsAfterDagger)
				{
					expression.emitOn(list);
					if (expression.isArgumentOrGroup())
					{
						list.add(2);  // append
					}
				}
				list.add(2);  // add inner list to outer
				list.add(loopStart * 4 + 1);  // jump to @loopStart
				loopExit = list.size() + 1;
				list.add(2);  // append partial tuple, up to dagger
				loopSkip = list.size() + 1;
			}
		}

		@Override
		public String toString ()
		{
			List<String> strings = new ArrayList<String>();
			for (Expression e : expressionsBeforeDagger)
			{
				strings.add(e.toString());
			}
			if (hasDagger)
			{
				strings.add("‡");
				for (Expression e : expressionsAfterDagger)
				{
					strings.add(e.toString());
				}
			}

			StringBuilder builder = new StringBuilder();
			builder.append("Group(");
			boolean first = true;
			for (String s : strings)
			{
				if (first)
				{
					first = false;
				}
				else
				{
					builder.append(", ");
				}
				builder.append(s);
			}
			builder.append(")");
			return builder.toString();
		}

	
		/**
		 * Check if the given type is suitable for holding values generated by
		 * this group.
		 */
		@Override
		public void checkType (AvailObject argumentType)
		{
			// Always expect a tuple of solutions here.
			if (argumentType.equals(TypeDescriptor.Types.terminates.object()))
			{
				error("Method argument type should not be \"terminates\".");
			}

			if (!argumentType.isTupleType())
			{
				error(
					"The repeated group \"" + toString() +
					"\" must accept a tuple, not \"" +
					argumentType.toString() + "\".");
			}
			
			if (!needsDoubleWrapping())
			{
				// Expect a tuple of individual values.  No further checks are
				// needed.
			}
			else
			{
				// Expect a tuple of tuples of values, where the inner tuple
				// size ranges from the number of arguments left of the dagger
				// up to that plus the number of arguments right of the dagger.
				assert argumentType.isTupleType();
				AvailObject expectedLower = IntegerDescriptor.objectFromInt(
					argumentsBeforeDagger);
				AvailObject expectedUpper = IntegerDescriptor.objectFromInt(
					argumentsBeforeDagger + argumentsAfterDagger);
				for (int i = 1, limit = argumentType.typeTuple().tupleSize();
					i <= limit;
					i++)
				{
					AvailObject solutionType = argumentType.typeAtIndex(i);
					if (!solutionType.isTupleType())
					{
						error(
							"The declared type for the subexpression \"" +
							toString() +
							"\" is expected to be a tuple type whose " +
							Integer.toString(i) +
							"-th element accepts a tuple.");
					}
					// Check that the solution that will reside at the current
					// index accepts either a full group or a group up to the
					// dagger.
					AvailObject solutionTypeSizes = solutionType.sizeRange();
					AvailObject lower = solutionTypeSizes.lowerBound();
					AvailObject upper = solutionTypeSizes.upperBound();
					if ((lower != expectedLower && lower != expectedUpper)
						|| (upper != expectedLower && upper != expectedUpper))
					{
						error(
							"The complex group \"" + toString() +
							"\" should have elements whose types are " +
							"tuples restricted to have lower and upper " +
							"bounds equal to the pre-dagger cardinality (" +
							expectedLower.toString() +
							") or the total cardinality (" +
							expectedUpper.toString() +
							").  Instead, the " + Integer.toString(i) +
							"-th element of the outer list has type " +
							solutionType.toString() + ".");
					}
					int j = 1;
					for (Expression e : expressionsBeforeDagger)
					{
						if (e.isArgumentOrGroup())
						{
							e.checkType(solutionType.typeAtIndex(j));
							j++;
						}
					}
					for (Expression e : expressionsAfterDagger)
					{
						if (e.isArgumentOrGroup())
						{
							e.checkType(solutionType.typeAtIndex(j));
							j++;
						}
					}
				}
			}
			return;
		}

	}
	
	
	/**
	 * Construct a new {@link MessageSplitter}, parsing the provided message
	 * into token strings and generating parsing instructions for parsing
	 * occurrences of this message.
	 *
	 * @param messageName An Avail {@link ByteStringDescriptor string} specifying the keywords and arguments
	 * of some message being defined.
	 */
	public MessageSplitter (AvailObject messageName)
	{
		this.messageName = messageName;
		messageName.makeImmutable();
		splitMessage();
		messagePartPosition = 1;
		rootGroup = parseGroup();
		if (rootGroup.hasDagger)
		{
			error("Dagger is not allowed outside chevrons");
		}
		if (messagePartPosition != messageParts.size() + 1)
		{
			error("Imbalanced chevrons in message: "
				+ messageName.asNativeString());
		}
		// Emit it twice -- once to calculate the branch positions, and then
		// again to output using the correct branches.
		for (int i = 1; i <= 2; i++)
		{
			instructions.clear();
			for (Expression expression : rootGroup.expressionsBeforeDagger)
			{
				expression.emitOn(instructions);
			}
		}
		assert rootGroup.expressionsAfterDagger.isEmpty();

		// dumpForDebug();
	}
	
	
	/**
	 * Dump debugging information about this {@linkplain MessageSplitter} to
	 * System.out.
	 */
	public void dumpForDebug ()
	{
		List<String> partsList = new ArrayList<String>(messageParts.size());
		for (AvailObject part : messageParts)
		{
			partsList.add(part.asNativeString());
		}
		AvailObject instructionsTuple = instructionsTuple();
		List<Integer> instructionsList = new ArrayList<Integer>();
		for (AvailObject instruction : instructionsTuple)
		{
			instructionsList.add(instruction.extractInt());
		}
		System.out.printf(
			"%s  ->  %s  ->  %s%n",
			messageName.asNativeString(),
			partsList.toString(),
			instructionsList.toString());
	}
	
	
	/**
	 * Answer a {@linkplain TupleDescriptor tuple} of Avail {@linkplain
	 * ByteStringDescriptor strings} comprising this message.
	 * 
	 * @return A tuple of strings.
	 */
	public AvailObject messageParts ()
	{
		AvailObject tuple = TupleDescriptor.mutableObjectFromArray(
			messageParts);
		tuple.makeImmutable();
		return tuple;
	}
	
	
	/**
	 * Answer a {@linkplain TupleDescriptor tuple} of Avail {@linkplain
	 * IntegerDescriptor integers} describing how to parse this message.
	 * See {@link MessageSplitter} for a description of the parse instructions.
	 * 
	 * @return The tuple of integers encoding parse instructions for this
	 *         message.
	 */
	public AvailObject instructionsTuple ()
	{
		AvailObject tuple = TupleDescriptor.mutableCompressedFromIntegerArray(
			instructions);
		tuple.makeImmutable();
		return tuple;
	}
	
	
	/**
	 * Decompose the message name into its constituent token strings.  These
	 * can be subsequently parsed to generate the actual parse instructions.
	 * Do not do any semantic analysis here, not even backquote processing --
	 * that would lead to confusion over whether an operator was supposed to be
	 * treated as a special token like open-chevron («) rather than like a
	 * backquote-escaped token).
	 */
	private void splitMessage ()
	{
		if (messageName.tupleSize() == 0)
		{
			return;
		}
		int position = 1;
		while (position <= messageName.tupleSize()) {
			final char ch = (char) messageName.tupleAt(position).codePoint();
			if (ch == ' ')
			{
				if (messageParts.size() == 0
					|| isCharacterAnUnderscoreOrSpaceOrOperator(
						(char) messageName.tupleAt(position - 1).codePoint()))
				{
					error(
						"Illegally canonized method name"
						+ " (problem before space)");
				}
				//  Skip the space.
				position++;
				if (position > messageName.tupleSize()
						|| isCharacterAnUnderscoreOrSpaceOrOperator(
							(char) messageName.tupleAt(position).codePoint()))
				{
					error(
						"Illegally canonized method name"
						+ " (problem after space)");
				}
			}
			else if (ch == '_' || AvailScanner.isOperatorCharacter(ch))
			{
				messageParts.add(messageName.copyTupleFromToCanDestroy(
					position,
					position,
					false));
				position++;
			}
			else
			{
				final int start = position;
				while (position <= messageName.tupleSize()
						&& !isCharacterAnUnderscoreOrSpaceOrOperator(
							(char) messageName.tupleAt(position).codePoint()))
				{
					position++;
				}
				messageParts.add(messageName.copyTupleFromToCanDestroy(
					start,
					position - 1,
					false));
			}
		}
	}
	
	
	/**
	 * Create a group from the series of tokens describing it.  This is also
	 * used to construct the outermost sequence of expressions, with the
	 * restriction that an occurrence of a double-dagger in the outermost
	 * pseudo-group is an error.  Expect the {@linkplain #messagePartPosition}
	 * to point (via a one-based offset) to the first token of the group, or
	 * just past the end if the group is empty.  Leave the messagePartPosition
	 * pointing just past the last token of the group.
	 * <p>
	 * The caller is responsible for identifying and skipping an open chevron
	 * prior to this group, and for consuming the close chevron after parsing
	 * the group.  The outermost caller is also responsible for ensuring the
	 * entire input was exactly consumed.
	 * 
	 * @return A {@link Group} expression parsed from the {@link #messageParts}.
	 */
	Group parseGroup ()
	{
		Group group = new Group();
		while (true)
		{
			if (messagePartPosition > messageParts.size())
			{
				return group;
			}
			AvailObject token = messageParts.get(messagePartPosition - 1);
			if (token.equals(TupleDescriptor.closeChevronTuple()))
			{
				return group;
			}
			else if (token.equals(TupleDescriptor.underscoreTuple()))
			{
				group.addExpression(new Argument());
				messagePartPosition++;
			}
			else if (token.equals(TupleDescriptor.doubleDaggerTuple()))
			{
				if (group.hasDagger)
				{
					error("Two daggers encountered in group");
				}
				group.hasDagger = true;
				messagePartPosition++;
			}
			else if (token.equals(TupleDescriptor.openChevronTuple()))
			{
				// Eat the open chevron, parse a subgroup, eat the (mandatory)
				// close chevron, and add the group.
				messagePartPosition++;
				Group subgroup = parseGroup();
				if (messagePartPosition <= messageParts.size())
				{
					token = messageParts.get(messagePartPosition - 1);
				}
				// Otherwise token stays an open chevron, hence not a close...
				if (!token.equals(TupleDescriptor.closeChevronTuple()))
				{
					error(
						"Expected matching close chevron in method name",
						messageName);
				}
				messagePartPosition++;
				group.addExpression(subgroup);
			}
			else
			{
				// Parse a backquote or regular keyword or operator
				if (token.equals(TupleDescriptor.backQuoteTuple()))
				{
					messagePartPosition++;  // eat the backquote
					if (messagePartPosition > messageParts.size())
					{
						error(
							"Expected operator character after backquote, "
								+ "not end of message name",
							messageName);
					}
					token = messageParts.get(messagePartPosition - 1);
					if (token.tupleSize() != 1
						|| !isCharacterAnUnderscoreOrSpaceOrOperator(
							(char)token.tupleAt(1).codePoint()))
					{
						error(
							"Expecting operator character after backquote",
							messageName);
					}
				}
				// Parse a regular keyword or operator
				group.addExpression(new Simple(messagePartPosition));
				messagePartPosition++;
			}
		}
	}


	/**
	 * Return the number of arguments a {@link MethodSignatureDescriptor method}
	 * implementing this name would accept.
	 * 
	 * @return The number of arguments this message takes. 
	 */
	public int numberOfArguments ()
	{
		return rootGroup.argumentsBeforeDagger + rootGroup.argumentsAfterDagger;
	}
	
	/**
	 * Answer whether the specified character is an operator character, space,
	 * or underscore.
	 *
	 * @param aCharacter A Java {@code char}.
	 * @return {@code true} if the specified character is an operator character,
	 *          space, or underscore; or {@code false} otherwise.
	 */
	private static boolean isCharacterAnUnderscoreOrSpaceOrOperator (
		final char aCharacter)
	{
		return aCharacter == '_'
			|| aCharacter == ' '
			|| AvailScanner.isOperatorCharacter(aCharacter);
	}

}