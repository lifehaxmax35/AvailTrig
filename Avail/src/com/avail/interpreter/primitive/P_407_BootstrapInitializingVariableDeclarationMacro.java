/**
 * P_407_BootstrapInitializingVariableDeclarationMacro.java
 * Copyright © 1993-2012, Mark van Gulik and Todd L Smith.
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

package com.avail.interpreter.primitive;

import static com.avail.descriptor.TypeDescriptor.Types.*;
import static com.avail.descriptor.ParseNodeTypeDescriptor.ParseNodeKind.*;
import static com.avail.interpreter.Primitive.Flag.*;
import java.util.*;
import com.avail.compiler.AvailRejectedParseException;
import com.avail.descriptor.*;
import com.avail.interpreter.*;

/**
 * The {@code P_407_BootstrapInitializingVariableDeclarationMacro} primitive is
 * used for bootstrapping declaration of a {@link #LOCAL_VARIABLE_NODE local
 * variable} (without an initializing expression).
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public class P_407_BootstrapInitializingVariableDeclarationMacro extends Primitive
{
	/**
	 * The sole instance of this primitive class.  Accessed through reflection.
	 */
	public final static Primitive instance =
		new P_407_BootstrapInitializingVariableDeclarationMacro().init(
			3, CannotFail, Bootstrap);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter)
	{
		assert args.size() == 3;
		final AvailObject variableNameLiteral = args.get(0);
		final AvailObject typeLiteral = args.get(1);
		final AvailObject initializingExpression = args.get(2);

		final AvailObject type = typeLiteral.token().literal();
		if (type.equals(TOP.o()) || type.equals(BottomTypeDescriptor.bottom()))
		{
			throw new AvailRejectedParseException(
				StringDescriptor.from(
					"variable's declared type to be something other than "
					+ type));
		}
		if (!initializingExpression.expressionType().isSubtypeOf(type))
		{
			throw new AvailRejectedParseException(
				StringDescriptor.from(
					String.format(
						"initializing assignment expression's type (%s) "
						+ "to match variable type (%s)",
						initializingExpression.expressionType(),
						type)));
		}
		final AvailObject variableDeclaration =
			DeclarationNodeDescriptor.newVariable(
				variableNameLiteral.token().literal(), // contains another token
				type,
				initializingExpression);
		variableDeclaration.makeImmutable();
		return interpreter.primitiveSuccess(variableDeclaration);
	}

	@Override
	protected AvailObject privateBlockTypeRestriction ()
	{
		return FunctionTypeDescriptor.create(
			TupleDescriptor.from(
				/* Variable name token */
				LITERAL_NODE.create(TOKEN.o()),
				/* Variable type */
				LITERAL_NODE.create(InstanceMetaDescriptor.anyMeta()),
				/* Initialization expression */
				EXPRESSION_NODE.mostGeneralType()),
			LOCAL_VARIABLE_NODE.mostGeneralType());
	}
}