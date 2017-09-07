/**
 * LiteralTokenDescriptor.java
 * Copyright © 1993-2017, The Avail Foundation, LLC.
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

import static com.avail.descriptor.AbstractEnumerationTypeDescriptor
	.withInstance;
import static com.avail.descriptor.LiteralTokenDescriptor.IntegerSlots.*;
import static com.avail.descriptor.LiteralTokenDescriptor.ObjectSlots.*;
import static com.avail.descriptor.LiteralTokenTypeDescriptor.literalTokenType;
import static com.avail.descriptor.TypeDescriptor.Types.*;

import com.avail.annotations.AvailMethod;
import com.avail.annotations.EnumField;
import com.avail.annotations.HideFieldInDebugger;
import com.avail.compiler.CompilationContext;
import com.avail.compiler.scanning.LexingState;
import com.avail.serialization.SerializerOperation;
import com.avail.utility.json.JSONWriter;

/**
 * I represent a token that's a literal representation of some object.
 *
 * <p>In addition to the state inherited from {@link TokenDescriptor}, I add a
 * field to hold the literal value itself.</p>
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public final class LiteralTokenDescriptor
extends TokenDescriptor
{
	/**
	 * My class's slots of type int.
	 */
	public enum IntegerSlots
	implements IntegerSlotsEnum
	{
		/**
		 * {@link BitField}s for the token type code, the starting byte
		 * position, and the line number.
		 */
		TOKEN_TYPE_AND_START_AND_LINE;

		/**
		 * The {@link Enum#ordinal() ordinal} of the {@link TokenType} that
		 * indicates what basic kind of token this is.  Currently four bits are
		 * reserved for this purpose.
		 */
		@EnumField(describedBy = TokenType.class)
		static final BitField TOKEN_TYPE_CODE =
			bitField(TOKEN_TYPE_AND_START_AND_LINE, 0, 4);

		/**
		 * The line number in the source file. Currently signed 28 bits, which
		 * should be plenty.
		 */
		static final BitField LINE_NUMBER =
			bitField(TOKEN_TYPE_AND_START_AND_LINE, 4, 28);

		/**
		 * The starting position in the source file. Currently signed 32 bits,
		 * but this may change at some point -- not that we really need to parse
		 * 2GB of <em>Avail</em> source in one file, due to its deeply flexible
		 * syntax.
		 */
		static final BitField START =
			bitField(TOKEN_TYPE_AND_START_AND_LINE, 32, 32);

		static
		{
			assert TokenDescriptor.IntegerSlots.TOKEN_TYPE_AND_START_AND_LINE
					.ordinal()
				== TOKEN_TYPE_AND_START_AND_LINE.ordinal();
			assert TokenDescriptor.IntegerSlots.TOKEN_TYPE_CODE.isSamePlaceAs(
				TOKEN_TYPE_CODE);
			assert TokenDescriptor.IntegerSlots.START.isSamePlaceAs(
				START);
			assert TokenDescriptor.IntegerSlots.LINE_NUMBER.isSamePlaceAs(
				LINE_NUMBER);
		}
	}

	/**
	 * My slots of type {@link AvailObject}. Note that they have to start the
	 * same as in my superclass {@link TokenDescriptor}.
	 */
	public enum ObjectSlots
	implements ObjectSlotsEnum
	{
		/**
		 * The {@linkplain StringDescriptor string}, exactly as I appeared in
		 * the source.
		 */
		STRING,

		/**
		 * The lower case {@linkplain StringDescriptor string}, cached as an
		 * optimization for case insensitive parsing.
		 */
		@HideFieldInDebugger
		LOWER_CASE_STRING,

		/** The {@linkplain A_String leading whitespace}. */
		@HideFieldInDebugger
		LEADING_WHITESPACE,

		/** The {@linkplain A_String trailing whitespace}. */
		@HideFieldInDebugger
		TRAILING_WHITESPACE,

		/**
		 * A {@link RawPojoDescriptor raw pojo} holding the {@link LexingState}
		 * after this token.
		 *
		 * <p>The field is typically {@link NilDescriptor#nil() nil}, to
		 * indicate the {@link LexingState} should be looked up by position (and
		 * line number) via {@link CompilationContext#lexingStateAt(int, int)}.
		 * </p>
		 */
		NEXT_LEXING_STATE_POJO,

		/** The actual {@link AvailObject} wrapped by this token. */
		LITERAL;

		static
		{
			assert TokenDescriptor.ObjectSlots.STRING.ordinal()
				== STRING.ordinal();
			assert TokenDescriptor.ObjectSlots.LOWER_CASE_STRING.ordinal()
				== LOWER_CASE_STRING.ordinal();
			assert TokenDescriptor.ObjectSlots.LEADING_WHITESPACE.ordinal()
				== LEADING_WHITESPACE.ordinal();
			assert TokenDescriptor.ObjectSlots.TRAILING_WHITESPACE.ordinal()
				== TRAILING_WHITESPACE.ordinal();
			assert TokenDescriptor.ObjectSlots.NEXT_LEXING_STATE_POJO.ordinal()
				== NEXT_LEXING_STATE_POJO.ordinal();
		}
	}

	@Override
	boolean allowsImmutableToMutableReferenceInField (final AbstractSlotsEnum e)
	{
		return e == LOWER_CASE_STRING
			|| e == TRAILING_WHITESPACE
			|| e == NEXT_LEXING_STATE_POJO
			|| super.allowsImmutableToMutableReferenceInField(e);
	}

	@Override @AvailMethod
	AvailObject o_Literal (final AvailObject object)
	{
		return object.slot(LITERAL);
	}

	@Override @AvailMethod
	A_Type o_Kind (final AvailObject object)
	{
		return literalTokenType(withInstance(object));
	}

	@Override @AvailMethod
	boolean o_IsInstanceOfKind (
		final AvailObject object,
		final A_Type aTypeObject)
	{
		if (aTypeObject.isSupertypeOfPrimitiveTypeEnum(TOKEN))
		{
			return true;
		}
		if (!aTypeObject.isLiteralTokenType())
		{
			return false;
		}
		return object.slot(LITERAL).isInstanceOf(aTypeObject.literalType());
	}

	@Override
	boolean o_IsLiteralToken (final AvailObject object)
	{
		return true;
	}

	@Override
	SerializerOperation o_SerializerOperation (final AvailObject object)
	{
		return SerializerOperation.LITERAL_TOKEN;
	}

	@Override
	void o_WriteTo (final AvailObject object, final JSONWriter writer)
	{
		writer.startObject();
		writer.write("kind");
		writer.write("token");
		writer.write("start");
		writer.write(object.slot(START));
		writer.write("line number");
		writer.write(object.slot(LINE_NUMBER));
		writer.write("lexeme");
		object.slot(STRING).writeTo(writer);
		writer.write("leading whitespace");
		object.slot(LEADING_WHITESPACE).writeTo(writer);
		writer.write("trailing whitespace");
		object.slot(TRAILING_WHITESPACE).writeTo(writer);
		writer.write("literal");
		object.slot(LITERAL).writeTo(writer);
		writer.endObject();
	}

	@Override
	void o_WriteSummaryTo (final AvailObject object, final JSONWriter writer)
	{
		writer.startObject();
		writer.write("kind");
		writer.write("token");
		writer.write("start");
		writer.write(object.slot(START));
		writer.write("line number");
		writer.write(object.slot(LINE_NUMBER));
		writer.write("lexeme");
		object.slot(STRING).writeTo(writer);
		writer.write("leading whitespace");
		object.slot(LEADING_WHITESPACE).writeTo(writer);
		writer.write("trailing whitespace");
		object.slot(TRAILING_WHITESPACE).writeTo(writer);
		writer.write("literal");
		object.slot(LITERAL).writeSummaryTo(writer);
		writer.endObject();
	}

	/**
	 * Create and initialize a new {@linkplain LiteralTokenDescriptor literal
	 * token}.
	 *
	 * @param string
	 *        The token text.
	 * @param leadingWhitespace
	 *        The leading whitespace.
	 * @param trailingWhitespace
	 *        The trailing whitespace.
	 * @param start
	 *        The token's starting character position in the file.
	 * @param lineNumber
	 *        The line number on which the token occurred.
	 * @param tokenType
	 *        The type of token to create.
	 * @param literal The literal value.
	 * @return The new literal token.
	 */
	public static AvailObject create (
		final A_String string,
		final A_String leadingWhitespace,
		final A_String trailingWhitespace,
		final int start,
		final int lineNumber,
		final TokenType tokenType,
		final A_BasicObject literal)
	{
		final AvailObject instance = mutable.create();
		instance.setSlot(STRING, string);
		instance.setSlot(LEADING_WHITESPACE, leadingWhitespace);
		instance.setSlot(TRAILING_WHITESPACE, trailingWhitespace);
		instance.setSlot(LOWER_CASE_STRING, NilDescriptor.nil());
		instance.setSlot(START, start);
		instance.setSlot(LINE_NUMBER, lineNumber);
		instance.setSlot(TOKEN_TYPE_CODE, tokenType.ordinal());
		instance.setSlot(LITERAL, literal);
		instance.setSlot(NEXT_LEXING_STATE_POJO, NilDescriptor.nil());
		return instance;
	}

	/**
	 * Construct a new {@link LiteralTokenDescriptor}.
	 *
	 * @param mutability
	 *        The {@linkplain Mutability mutability} of the new descriptor.
	 */
	private LiteralTokenDescriptor (final Mutability mutability)
	{
		super(
			mutability,
			TypeTag.LITERAL_TOKEN_TAG,
			ObjectSlots.class,
			IntegerSlots.class);
	}

	/** The mutable {@link LiteralTokenDescriptor}. */
	private static final LiteralTokenDescriptor mutable =
		new LiteralTokenDescriptor(Mutability.MUTABLE);

	@Override
	LiteralTokenDescriptor mutable ()
	{
		return mutable;
	}

	/** The shared {@link LiteralTokenDescriptor}. */
	private static final LiteralTokenDescriptor shared =
		new LiteralTokenDescriptor(Mutability.SHARED);

	@Override
	LiteralTokenDescriptor immutable ()
	{
		// Answer the shared descriptor, since there isn't an immutable one.
		return shared;
	}

	@Override
	LiteralTokenDescriptor shared ()
	{
		return shared;
	}
}
