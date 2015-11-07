/**
 * AvailCompilerBipartiteRendezvous.java
 * Copyright © 1993-2015, The Avail Foundation, LLC.
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

import java.util.*;
import com.avail.compiler.AvailCompiler.Con;
import com.avail.compiler.AvailCompiler.ParserState;
import com.avail.descriptor.*;

/**
 * An {@code AvailCompilerBipartiteRendezvous} comes at parsing from both sides
 * to maximize the freedom of implementation of the parser.  It uses dynamic
 * programming to avoid parsing the same subexpression multiple times.  When a
 * new continuation needs to run against all possible subexpressions, it looks
 * up the current parser state in a map to get the bipartite rendezvous.  That
 * contains the list of subexpressions that have been parsed so far.  They are
 * all run against the new continuation.  The continuation is then added to the
 * bipartite rendezvous's list of actions.  When a new complete subexpression is
 * found it is run against all waiting actions and added to the list of
 * subexpressions.
 *
 * <p>
 * These two cases ensure each continuation runs with each subexpression –
 * without requiring any particular order of execution of the continuations.
 * That allows us to reorder the continuations arbitrarily, including forcing
 * them to run basically in lock-step with the lexical scanner, avoiding
 * scanning too far ahead in the cases of dynamic scanning rules and power
 * strings.  It also allows parallel execution of the parser (in theory).
 * </p>
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public class AvailCompilerBipartiteRendezvous
{

	/**
	 * The solutions that have been encountered so far, and will be passed to
	 * new actions when they arrive.
	 */
	final List<AvailCompilerCachedSolution> solutions =
		new ArrayList<>(3);

	/**
	 * The actions that are waiting to run when new solutions arrive.
	 */
	final List<Con<A_Phrase>> actions = new ArrayList<>(3);

	/**
	 * Record a new solution, and also run any waiting actions with it.
	 *
	 * @param endState The parse position after the parseNode.
	 * @param parseNode The parse node that was parsed.
	 * @throws DuplicateSolutionException
	 *         If the parse node and end state are equal to a solution already
	 *         recorded.
	 */
	void addSolution (
		final ParserState endState,
		final A_Phrase parseNode)
	throws DuplicateSolutionException
	{
		final AvailCompilerCachedSolution solution =
			new AvailCompilerCachedSolution(
				endState, parseNode.makeShared());
		if (solutions.contains(solution))
		{
			throw new DuplicateSolutionException();
		}
		solutions.add(solution);
		for (final Con<A_Phrase> action : actions)
		{
			action.value(endState, parseNode);
		}
	}

	/**
	 * Record a new action, and also run any stored solutions through it.
	 *
	 * @param action The new action.
	 */
	void addAction (final Con<A_Phrase> action)
	{
		actions.add(action);
		for (final AvailCompilerCachedSolution solution : solutions)
		{
			action.value(solution.endState(), solution.parseNode());
		}
	}
}