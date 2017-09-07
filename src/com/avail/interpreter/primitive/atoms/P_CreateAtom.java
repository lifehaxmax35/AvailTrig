/**
 * P_CreateAtom.java
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
package com.avail.interpreter.primitive.atoms;

import com.avail.descriptor.*;
import com.avail.exceptions.AmbiguousNameException;
import com.avail.exceptions.AvailErrorCode;
import com.avail.interpreter.AvailLoader;
import com.avail.interpreter.Interpreter;
import com.avail.interpreter.Primitive;

import static com.avail.descriptor.TypeDescriptor.Types.ATOM;
import static com.avail.interpreter.Primitive.Flag.*;
import java.util.List;

/**
 * <strong>Primitive:</strong> Lookup or create a new {@linkplain AtomDescriptor
 * atom} with the given name.
 *
 * <p>If this method is executed outside the scope of compiling or loading, a
 * new atom will always be created.</p>
 */
public final class P_CreateAtom extends Primitive
{
	/**
	 * The sole instance of this primitive class.  Accessed through reflection.
	 */
	public static final Primitive instance =
		new P_CreateAtom().init(
			1, CanInline);

	@Override
	public Result attempt (
		final List<AvailObject> args,
		final Interpreter interpreter,
		final boolean skipReturnCheck)
	{
		assert args.size() == 1;
		final AvailObject name = args.get(0);
		final AvailLoader loader = interpreter.availLoaderOrNull();
		final A_Atom atom;
		if (loader == null)
		{
			atom = AtomDescriptor.create(name, NilDescriptor.nil());
		}
		else
		{
			try
			{
				atom = loader.lookupName(name);
			}
			catch (final AmbiguousNameException e)
			{
				return interpreter.primitiveFailure(
					AvailErrorCode.E_AMBIGUOUS_NAME);
			}
		}
		return interpreter.primitiveSuccess(atom);
	}

	@Override
	protected A_Type privateBlockTypeRestriction ()
	{
		return FunctionTypeDescriptor.functionType(
			TupleDescriptor.tuple(
				TupleTypeDescriptor.stringType()),
			ATOM.o());
	}

	@Override
	protected A_Type privateFailureVariableType ()
	{
		return AbstractEnumerationTypeDescriptor.enumerationWith(
			SetDescriptor.set(
				AvailErrorCode.E_AMBIGUOUS_NAME));
	}
}
