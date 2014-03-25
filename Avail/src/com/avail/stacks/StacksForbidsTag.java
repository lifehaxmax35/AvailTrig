/**
 * StacksForbidsTag.java
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

package com.avail.stacks;

import java.util.ArrayList;

/**
 * The "@forbids" tag in an Avail Class comment.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
public class StacksForbidsTag extends AbstractStacksTag
{
	/**
	 * The forbids arity index.
	 */
	final private AbstractStacksToken arityIndex;
	/**
	 * The list of the methods for which the method is "forbidden" to used in
	 * conjunction with.
	 */
	final private ArrayList<QuotedStacksToken> forbidMethods;

	/**
	 * Construct a new {@link StacksForbidsTag}.
	 * @param arityIndex
	 *		The forbids arity index.
	 * @param forbidMethods
	 * 		The list of the methods for which the method is "forbidden" to used
	 * 		in conjunction with.
	 */
	public StacksForbidsTag (final AbstractStacksToken arityIndex,
		final ArrayList<QuotedStacksToken> forbidMethods)
	{
		this.arityIndex = arityIndex;
		this.forbidMethods = forbidMethods;
	}

	/**
	 * @return the forbidMethods
	 */
	public ArrayList<QuotedStacksToken> forbidMethods ()
	{
		return forbidMethods;
	}

	/**
	 * @return the arityIndex
	 */
	public AbstractStacksToken arityIndex ()
	{
		return arityIndex;
	}

	/**
	 * Merge two {@linkplain StacksForbidsTag forbids tags} of the same arity
	 * @param tag
	 * 		The {@linkplain StacksForbidsTag} to merge with
	 */
	public void mergeForbidsTag(final StacksForbidsTag tag)
	{
		forbidMethods.addAll(tag.forbidMethods());
	}

	@Override
	public String toHTML ()
	{
		final int rowSize = forbidMethods.size();
		final StringBuilder stringBuilder = new StringBuilder()
			.append("<td class=\"GCode\" rowspan=\"")
			.append(rowSize).append("\">Argument ").append(arityIndex)
			.append("</td>").append( "<td class=\"GCode\">")
			.append(forbidMethods.get(0).lexeme).append("</td></tr>");

		if (rowSize > 1)
		{
			for (int i = 1; i < rowSize; i++)
			{
				stringBuilder.append("<tr> <td class=\"GCode\">")
					.append(forbidMethods.get(i).lexeme)
					.append("</td></tr>");
			}
		}
		return stringBuilder.toString();
	}

}
