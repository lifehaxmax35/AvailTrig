/**
 * compiler/AvailCompilerCachedSolution.java
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

import com.avail.compiler.AvailCompilerScopeStack;
import com.avail.compiler.AvailParseNode;

public class AvailCompilerCachedSolution
{
	AvailParseNode _parseNode;
	int _endPosition;
	AvailCompilerScopeStack _scopeStack;


	// accessing

	int endPosition ()
	{
		return _endPosition;
	}

	void endPosition (
			final int anInteger)
	{

		_endPosition = anInteger;
	}

	AvailParseNode parseNode ()
	{
		return _parseNode;
	}

	void parseNode (
			final AvailParseNode aParseNode)
	{

		_parseNode = aParseNode;
	}

	AvailCompilerScopeStack scopeStack ()
	{
		return _scopeStack;
	}

	void scopeStack (
			final AvailCompilerScopeStack aScopeStack)
	{

		_scopeStack = aScopeStack;
	}



	// java printing

	public String toString ()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Solution(@");
		builder.append(endPosition());
		if (scopeStack().name() == null)
		{
			builder.append(", no bindings");
		}
		else
		{
			builder.append(", last binding=" + scopeStack().name());
		}
		builder.append(") = ");
		builder.append(parseNode().toString());
		return builder.toString();
	}





	// Constructor

	AvailCompilerCachedSolution (
		AvailParseNode parseNode,
		int endPosition,
		AvailCompilerScopeStack scopeStack)
	{
		_parseNode = parseNode;
		_endPosition = endPosition;
		_scopeStack = scopeStack;
	}

}
