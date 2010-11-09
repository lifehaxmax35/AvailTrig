/**
 * compiler/AvailVariableDeclarationNode.java
 * Copyright (c) 2010, Mark van Gulik.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this *   list of conditions and the following disclaimer.
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

import com.avail.compiler.AvailCodeGenerator;
import com.avail.compiler.scanner.AvailToken;
import com.avail.descriptor.AvailObject;
import com.avail.descriptor.VoidDescriptor;
import static com.avail.descriptor.AvailObject.*;

public class AvailVariableDeclarationNode extends AvailParseNode
{
	AvailToken _name;
	boolean _isArgument;
	AvailObject _declaredType;


	// accessing

	public AvailObject declaredType ()
	{
		return _declaredType;
	}

	public void declaredType (
			final AvailObject aType)
	{
		//  Might be a bit leaky, but it was too eager last time I checked.
		_declaredType = aType.makeImmutable();
	}

	public boolean isArgument ()
	{
		return _isArgument;
	}

	public void isArgument (
			final boolean aBoolean)
	{
		_isArgument = aBoolean;
	}

	public boolean isConstant ()
	{
		return false;
	}

	public boolean isInitializing ()
	{
		return false;
	}

	public AvailToken name ()
	{
		return _name;
	}

	public void name (
			final AvailToken token)
	{

		_name = token;
	}

	public AvailObject type ()
	{
		error("Don't ask for the type of a variable declaration node");
		return VoidDescriptor.voidObject();
	}



	// code generation

	public void emitEffectOn (
			final AvailCodeGenerator codeGenerator)
	{
		//  This is a declaration, so it was handled on a separate pass.  Do nothing.

		return;
	}

	public void emitValueOn (
			final AvailCodeGenerator codeGenerator)
	{
		//  This is a declaration, so it shouldn't produce a value.

		error("Consistency error - declaration can't be last statement of a value-returning block");
		return;
	}

	public void emitVariableAssignmentOn (
			final AvailCodeGenerator codeGenerator)
	{
		//  Emit an assignment to this variable.

		assert (! isConstant());
		assert (! isArgument());
		codeGenerator.emitSetLocalOrOuter(this);
	}

	public void emitVariableReferenceOn (
			final AvailCodeGenerator codeGenerator)
	{
		//  Emit a reference to this variable.

		assert (! isConstant());
		assert (! isArgument());
		codeGenerator.emitPushLocalOrOuter(this);
	}

	public void emitVariableValueOn (
			final AvailCodeGenerator codeGenerator)
	{
		//  Emit the value of this variable (or constant or label or module variable or module constant).

		if (isArgument())
		{
			codeGenerator.emitPushLocalOrOuter(this);
		}
		else
		{
			codeGenerator.emitGetLocalOrOuter(this);
		}
	}



	// java printing

	public void printOnIndent (
			final StringBuilder aStream, 
			final int indent)
	{
		aStream.append(_name.string());
		aStream.append(" : ");
		aStream.append(_declaredType.toString());
	}



	// testing

	public boolean isDeclaration ()
	{
		//  Answer whether the receiver is a local variable declaration.

		return true;
	}

	public boolean isSyntheticVariableDeclaration ()
	{
		return false;
	}





}
