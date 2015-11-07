/**
 * SequenceNodeDescriptor.java
 * Copyright © 1993-2015, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * modification, are permitted provided that the following conditions are met:
 * Redistribution and use in source and binary forms, with or without
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

import static com.avail.descriptor.TypeDescriptor.Types.*;
import static com.avail.descriptor.ParseNodeTypeDescriptor.ParseNodeKind.*;
import static com.avail.descriptor.SequenceNodeDescriptor.ObjectSlots.*;
import com.avail.annotations.*;
import com.avail.compiler.AvailCodeGenerator;
import com.avail.descriptor.ParseNodeTypeDescriptor.ParseNodeKind;
import com.avail.utility.evaluation.*;
import com.avail.utility.json.JSONWriter;
import java.util.List;

/**
 * My instances represent a sequence of {@linkplain ParseNodeDescriptor parse
 * nodes} to be treated as statements, except possibly the last one.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public final class SequenceNodeDescriptor
extends ParseNodeDescriptor
{
	/**
	 * My slots of type {@link AvailObject}.
	 */
	public enum ObjectSlots
	implements ObjectSlotsEnum
	{
		/**
		 * The {@linkplain ParseNodeDescriptor statements} that should be
		 * considered to execute sequentially, discarding each result except
		 * possibly for that of the last statement.
		 */
		STATEMENTS
	}

	@Override @AvailMethod
	A_Tuple o_Statements (final AvailObject object)
	{
		return object.slot(STATEMENTS);
	}

	@Override @AvailMethod
	A_Type o_ExpressionType (final AvailObject object)
	{
		final A_Tuple statements = object.slot(STATEMENTS);
		return statements.tupleSize() > 0
			? statements.tupleAt(statements.tupleSize()).expressionType()
			: TOP.o();
	}

	@Override @AvailMethod
	int o_Hash (final AvailObject object)
	{
		return object.slot(STATEMENTS).hash() + 0xE38140CA;
	}

	@Override @AvailMethod
	boolean o_EqualsParseNode (
		final AvailObject object,
		final A_Phrase aParseNode)
	{
		return !aParseNode.isMacroSubstitutionNode()
			&& object.parseNodeKind().equals(aParseNode.parseNodeKind())
			&& object.slot(STATEMENTS).equals(aParseNode.statements());
	}

	@Override @AvailMethod
	void o_EmitValueOn (
		final AvailObject object,
		final AvailCodeGenerator codeGenerator)
	{
		final A_Tuple statements = object.slot(STATEMENTS);
		final int statementsCount = statements.tupleSize();
		if (statements.tupleSize() > 0)
		{
			for (int i = 1; i < statementsCount; i++)
			{
				statements.tupleAt(i).emitEffectOn(codeGenerator);
			}
			statements.tupleAt(statementsCount).emitValueOn(codeGenerator);
		}
	}

	@Override @AvailMethod
	void o_EmitEffectOn (
		final AvailObject object,
		final AvailCodeGenerator codeGenerator)
	{
		for (final A_Phrase statement : object.slot(STATEMENTS))
		{
			statement.emitEffectOn(codeGenerator);
		}
	}

	@Override @AvailMethod
	void o_ChildrenMap (
		final AvailObject object,
		final Transformer1<A_Phrase, A_Phrase> aBlock)
	{
		A_Tuple statements = object.slot(STATEMENTS);
		for (int i = 1; i <= statements.tupleSize(); i++)
		{
			statements = statements.tupleAtPuttingCanDestroy(
				i,
				aBlock.valueNotNull(statements.tupleAt(i)),
				true);
		}
		object.setSlot(STATEMENTS, statements);
	}

	@Override @AvailMethod
	void o_ChildrenDo (
		final AvailObject object,
		final Continuation1<A_Phrase> aBlock)
	{
		for (final AvailObject statement : object.slot(STATEMENTS))
		{
			aBlock.value(statement);
		}
	}

	@Override
	void o_StatementsDo (
		final AvailObject object,
		final Continuation1<A_Phrase> continuation)
	{
		for (final AvailObject statement : object.slot(STATEMENTS))
		{
			continuation.value(statement);
		}
	}

	@Override @AvailMethod
	void o_ValidateLocally (
		final AvailObject object,
		final @Nullable A_Phrase parent)
	{
		// Do nothing.
	}

	@Override @AvailMethod
	void o_FlattenStatementsInto (
		final AvailObject object,
		final List<A_Phrase> accumulatedStatements)
	{
		for (final A_Phrase statement : object.slot(STATEMENTS))
		{
			statement.flattenStatementsInto(accumulatedStatements);
		}
	}

	@Override
	ParseNodeKind o_ParseNodeKind (final AvailObject object)
	{
		return SEQUENCE_NODE;
	}

	@Override
	void o_WriteTo (final AvailObject object, final JSONWriter writer)
	{
		writer.startObject();
		writer.write("kind");
		writer.write("sequence phrase");
		writer.write("statements");
		object.slot(STATEMENTS).writeTo(writer);
		writer.endObject();
	}

	@Override
	void o_WriteSummaryTo (final AvailObject object, final JSONWriter writer)
	{
		writer.startObject();
		writer.write("kind");
		writer.write("sequence phrase");
		writer.write("statements");
		object.slot(STATEMENTS).writeSummaryTo(writer);
		writer.endObject();
	}

	/**
	 * Create a new {@linkplain SequenceNodeDescriptor sequence node} from the
	 * given {@linkplain TupleDescriptor tuple} of {@linkplain
	 * ParseNodeDescriptor statements}.
	 *
	 * @param statements
	 *        The expressions to assemble into a {@linkplain
	 *        SequenceNodeDescriptor sequence node}.
	 * @return The resulting sequence node.
	 */
	public static A_Phrase newStatements (final A_Tuple statements)
	{
		final AvailObject instance = mutable.create();
		instance.setSlot(STATEMENTS, statements);
		instance.makeShared();
		return instance;
	}

	/**
	 * Construct a new {@link SequenceNodeDescriptor}.
	 *
	 * @param mutability
	 *        The {@linkplain Mutability mutability} of the new descriptor.
	 */
	private SequenceNodeDescriptor (final Mutability mutability)
	{
		super(mutability, ObjectSlots.class, null);
	}

	/** The mutable {@link SequenceNodeDescriptor}. */
	private static final SequenceNodeDescriptor mutable =
		new SequenceNodeDescriptor(Mutability.MUTABLE);

	@Override
	SequenceNodeDescriptor mutable ()
	{
		return mutable;
	}

	/** The shared {@link SequenceNodeDescriptor}. */
	private static final SequenceNodeDescriptor shared =
		new SequenceNodeDescriptor(Mutability.SHARED);

	@Override
	SequenceNodeDescriptor shared ()
	{
		return shared;
	}
}