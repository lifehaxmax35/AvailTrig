/**
 * AvailCompiler.java
 * Copyright © 1993-2014, The Avail Foundation, LLC.
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

import static com.avail.descriptor.ParseNodeTypeDescriptor.ParseNodeKind.*;
import static com.avail.descriptor.TokenDescriptor.TokenType.*;
import java.util.*;
import com.avail.annotations.Nullable;
import com.avail.builder.ResolvedModuleName;
import com.avail.compiler.problems.ProblemHandler;
import com.avail.compiler.scanning.AvailScannerResult;
import com.avail.descriptor.*;
import com.avail.interpreter.Interpreter;
import com.avail.io.TextInterface;
import com.avail.utility.evaluation.*;

/**
 * I parse a source file to create a {@linkplain ModuleDescriptor module}.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public class AvailCompiler
extends AbstractAvailCompiler
{
	/**
	 * Construct a new {@link AvailCompiler}.
	 *
	 * @param module
	 *        The current {@linkplain ModuleDescriptor module}.
	 * @param scannerResult
	 *        An {@link AvailScannerResult}.
	 * @param textInterface
	 *        The {@linkplain TextInterface text interface} for any {@linkplain
	 *        A_Fiber fibers} started by this compiler.
	 * @param problemHandler
	 *        The {@link ProblemHandler} used for reporting compilation
	 *        problems.
	 */
	public AvailCompiler (
		final A_Module module,
		final AvailScannerResult scannerResult,
		final TextInterface textInterface,
		final ProblemHandler problemHandler)
	{
		super(module, scannerResult, textInterface, problemHandler);
	}

	/**
	 * Construct a new {@link AvailCompiler}.
	 *
	 * @param moduleName
	 *        The {@link ResolvedModuleName} of the module to compile.
	 * @param scannerResult
	 *        An {@link AvailScannerResult}.
	 * @param textInterface
	 *        The {@linkplain TextInterface text interface} for any {@linkplain
	 *        A_Fiber fibers} started by this compiler.
	 * @param problemHandler
	 *        The {@linkplain ProblemHandler problem handler}.
	 */
	public AvailCompiler (
		final ResolvedModuleName moduleName,
		final AvailScannerResult scannerResult,
		final TextInterface textInterface,
		final ProblemHandler problemHandler)
	{
		super(moduleName, scannerResult, textInterface, problemHandler);
	}

	/**
	 * Parse a top-level statement.  This is the <em>only</em> boundary for the
	 * backtracking grammar (it used to be that <em>all</em> statements had to
	 * be unambiguous, even those in blocks).  The passed continuation will be
	 * invoked at most once, and only if the top-level statement had a single
	 * interpretation.
	 */
	@Override
	void parseOutermostStatement (
		final ParserState start,
		final Con<A_Phrase> continuation,
		final Continuation0 afterFail)
	{
		tryIfUnambiguousThen(
			start,
			new Con<Con<A_Phrase>>("Detect ambiguity")
			{
				@Override
				public void valueNotNull (
					final ParserState realStart,
					final Con<A_Phrase> whenFoundStatement)
				{
					parseExpressionThen(
						realStart,
						new Con<A_Phrase>("End of statement")
						{
							@Override
							public void valueNotNull (
								final ParserState afterExpression,
								final A_Phrase expression)
							{
								if (expression.hasSuperCast())
								{
									afterExpression.expected(
										"an outer level statement, "
										+ "not a supercast");
									return;
								}
								if (expression.expressionType().isTop())
								{
									whenFoundStatement.value(
										afterExpression,
										expression);
								}
								else
								{
									afterExpression.expected(
										"outer level statement "
										+ "to have top type");
								}
							}
						});
				}
			},
			continuation,
			afterFail);
	}

	@Override
	void parseExpressionUncachedThen (
		final ParserState start,
		final Con<A_Phrase> continuation)
	{
		final Con<A_Phrase> newContinuation =
			new Con<A_Phrase>("Optional leading argument send")
			{
				@Override
				public void valueNotNull (
					final ParserState afterSubexpression,
					final A_Phrase subexpression)
				{
					parseOptionalLeadingArgumentSendAfterThen(
						start,
						afterSubexpression,
						subexpression,
						continuation);
				}
			};
		parseLeadingKeywordSendThen(start, newContinuation);
		parseSimpleThen(start, newContinuation);
	}

	@Override
	void completedSendNodeForMacro (
		final ParserState stateBeforeCall,
		final ParserState stateAfterCall,
		final A_Phrase argumentsListNode,
		final A_Bundle bundle,
		final Con<A_Phrase> continuation)
	{
		final A_Method method = bundle.bundleMethod();
		final A_Tuple definitions = method.definitionsTuple();
		assert definitions.tupleSize() == 1;
		final A_Definition macroDefinition = definitions.tupleAt(1);
		final A_Function macroBody = macroDefinition.bodyBlock();
		final A_Type macroBodyKind = macroBody.kind();
		final A_Tuple argumentsTuple = argumentsListNode.expressionsTuple();
		final int argCount = argumentsTuple.tupleSize();
		// Strip off macro substitution wrappers from the arguments.  These
		// were preserved only long enough to test grammatical restrictions.
		final List<A_Phrase> argumentsList = new ArrayList<>(argCount);
		for (final A_Phrase argument : argumentsList)
		{
			argumentsList.add(argument.stripMacro());
		}
		if (!macroBodyKind.acceptsListOfArgValues(argumentsList))
		{
			stateAfterCall.expected(new Describer()
			{
				@Override
				public void describeThen (
					final @Nullable Continuation1<String> c)
				{
					assert c != null;
					final List<Integer> disagreements = new ArrayList<>();
					for (int i = 1; i <= macroBody.code().numArgs(); i++)
					{
						final A_Type type =
							macroBodyKind.argsTupleType().typeAtIndex(i);
						final A_Phrase value = argumentsList.get(i - 1);
						if (!value.isInstanceOf(type))
						{
							disagreements.add(i);
						}
					}
					assert disagreements.size() > 0;
					final List<A_BasicObject> values =
						new ArrayList<A_BasicObject>(argumentsList);
					values.add(macroBodyKind);
					Interpreter.stringifyThen(
						runtime,
						textInterface,
						values,
						new Continuation1<List<String>>()
						{
							@Override
							public void value (
								final @Nullable List<String> list)
							{
								assert list != null;
								final int size = list.size();
								c.value(String.format(
									"macro arguments %s to agree with "
									+ "definition:%n"
									+ "\tmacro = %s%n"
									+ "\texpected = %s%n"
									+ "\targuments = %s%n",
									disagreements,
									bundle.message().atomName(),
									list.get(size - 1),
									list.subList(0, size - 1)));
							}
						});
				}
			});
			return;
		}
		// Declarations introduced in the macro should now be moved
		// out of scope.
		// TODO: [MvG] Use a fiber to store the parser state.
		final ParserState reportedStateDuringValidation = new ParserState(
			stateAfterCall.position - 1,
			stateBeforeCall.clientDataMap);
		final ParserState stateAfter = new ParserState(
			stateAfterCall.position,
			stateBeforeCall.clientDataMap);
		// A macro can't have semantic restrictions, so just run it.
		evaluateFunctionThen(
			macroBody,
			argumentsList,
			false,
			new Continuation1<AvailObject>()
			{
				@Override
				public void value (final @Nullable AvailObject replacement)
				{
					assert replacement != null;
					if (replacement.isInstanceOfKind(
						PARSE_NODE.mostGeneralType()))
					{
						final A_Phrase substitution =
							MacroSubstitutionNodeDescriptor.fromNameAndNode(
								bundle.message(),
								replacement);
						attempt(stateAfter, continuation, substitution);
					}
					else
					{
						stateAfterCall.expected(
							"macro body ("
							+ bundle.message().atomName()
							+ ") to produce a parse node");
					}
				}
			},
			new Continuation1<Throwable>()
			{
				@Override
				public void value (final @Nullable Throwable e)
				{
					assert e != null;
					if (e instanceof AvailRejectedParseException)
					{
						final AvailRejectedParseException rej =
							(AvailRejectedParseException) e;
						stateAfterCall.expected(
							rej.rejectionString().asNativeString());
					}
					else
					{
						stateAfterCall.expected(
							"evaluation of macro body not to raise an "
							+ "unhandled exception:\n\t"
							+ e);
					}
				}
			});
	}

	/**
	 * Parse a literal, then invoke the continuation.
	 *
	 * @param start
	 *            Where to start parsing.
	 * @param continuation
	 *            What to do with the simple parse node.
	 */
	private void parseSimpleThen (
		final ParserState start,
		final Con<A_Phrase> continuation)
	{
		// Try a literal.
		if (start.peekToken().tokenType() == LITERAL)
		{
			final A_Phrase literalNode =
				LiteralNodeDescriptor.fromToken(start.peekToken());
			attempt(start.afterToken(), continuation, literalNode);
		}
		else
		{
			start.expected("simple expression");
		}
	}
}
