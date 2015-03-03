/**
 * MacroCommentImplementation.java
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

package com.avail.stacks;

import java.util.ArrayList;
import com.avail.descriptor.A_String;
import com.avail.descriptor.StringDescriptor;

/**
 * A comment that describes a particular macro implementation
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
public class MacroCommentImplementation extends AbstractCommentImplementation
{

	/**
	 * The list of {@link StacksParameterTag parameters} of the macro
	 * implementation.
	 */
	final ArrayList<StacksParameterTag> parameters;

	/**
	 * The {@link StacksReturnTag "@returns"} content
	 */
	final StacksReturnTag returnsContent;

	/**
	 *
	 */
	final ArrayList<StacksRaisesTag> exceptions;

	/**
	 * The hash id for this implementation
	 */
	final private int hashID;

	/**
	 * Construct a new {@link MacroCommentImplementation}.
	 *
	 * @param signature
	 * 		The {@link MethodCommentSignature signature} of the macro the
	 * 		comment describes.
	 * @param commentStartLine
	 * 		The start line in the module the comment being parsed appears.
	 * @param author
	 * 		The {@link StacksAuthorTag author} of the implementation.
	 * @param sees
	 * 		A {@link ArrayList} of any {@link StacksSeeTag "@sees"} references.
	 * @param description
	 * 		The overall description of the implementation
	 * @param categories
	 * 		The categories the implementation appears in
	 * @param aliases
	 * 		The aliases the implementation is known by
	 * @param parameters
	 * 		The list of {@link StacksParameterTag parameters} of the method
	 * 		implementation.
	 * @param returnsContent
	 * 		The {@link StacksReturnTag "@returns"} content
	 * @param exceptions
	 * 		A {@link ArrayList} of any {@link StacksRaisesTag exceptions} the method
	 * 		throws.
	 */
	public MacroCommentImplementation (
		final MethodCommentSignature signature,
		final int commentStartLine,
		final ArrayList<StacksAuthorTag> author,
		final ArrayList<StacksSeeTag> sees,
		final StacksDescription description,
		final ArrayList<StacksCategoryTag> categories,
		final ArrayList<StacksAliasTag> aliases,
		final ArrayList<StacksParameterTag> parameters,
		final StacksReturnTag returnsContent,
		final ArrayList<StacksRaisesTag> exceptions)
	{
		super(signature, commentStartLine, author, sees, description,
			categories,aliases);
		this.parameters = parameters;
		this.returnsContent = returnsContent;
		this.exceptions = exceptions;

		final StringBuilder concatenatedInputParams = new StringBuilder();

		for (final String param : signature.orderedInputTypes)
		{
			concatenatedInputParams.append(param);
		}

		this.hashID = StringDescriptor.from(
			concatenatedInputParams.toString()).hash();
	}

	@Override
	public void addToImplementationGroup (
		final ImplementationGroup implementationGroup)
	{
		implementationGroup.addMacro(this);

	}

	@Override
	public void addImplementationToImportModule (
		final A_String name,
		final StacksImportModule importModule)
	{
		importModule.addMacroImplementation(name, this);

	}

	@Override
	public String toHTML (
		final HTMLFileMap htmlFileMap,
		final String nameOfGroup,
		final StacksErrorLog errorLog)
	{

		final int paramCount = parameters.size();
		final int exceptionCount = exceptions.size();
		int colSpan = 1;
		final StringBuilder stringBuilder = new StringBuilder()
			.append(signature().toHTML(nameOfGroup));

		if (categories.size() > 0)
		{
			stringBuilder.append(categories.get(0).toHTML(htmlFileMap,
				hashID, errorLog, 1));
		}

		if (aliases.size() > 0)
		{
			stringBuilder.append(aliases.get(0).toHTML(htmlFileMap,
				hashID, errorLog, 1));
		}

		stringBuilder.append(tabs(2) + "<div "
				+ HTMLBuilder.tagClass(HTMLClass.classSignatureDescription)
				+ ">\n")
			.append(tabs(3) + description.toHTML(htmlFileMap, hashID, errorLog))
			.append("\n" + tabs(2) + "</div>\n")
			.append(tabs(2) + "<table "
            	+ HTMLBuilder.tagClass(HTMLClass.classStacks)
            	+ ">\n")
			.append(tabs(3) + "<thead>\n")
			.append(tabs(4) + "<tr>\n")
			.append(tabs(5) + "<th "
				+ HTMLBuilder.tagClass(HTMLClass.classTransparent)
				+ " scope=\"col\"></th>\n");
		if (paramCount > 0)
		{
			stringBuilder.append(tabs(5) + "<th "
				+ HTMLBuilder.tagClass(
					HTMLClass.classStacks, HTMLClass.classIColLabelNarrow)
				+ " scope=\"col\">Position</th>\n");
			stringBuilder.append(tabs(5) + "<th "
				+ HTMLBuilder.tagClass(
					HTMLClass.classStacks, HTMLClass.classIColLabelNarrow)
				+ " scope=\"col\">Name</th>\n");
			colSpan = 3;
		}

		stringBuilder
			.append(tabs(5) + "<th "
				+ HTMLBuilder.tagClass(
					HTMLClass.classStacks, HTMLClass.classIColLabelNarrow)
				+ " scope=\"col\">Type</th>\n")
			.append(tabs(5) + "<th "
				+ HTMLBuilder.tagClass(
					HTMLClass.classStacks, HTMLClass.classIColLabelWide)
				+ " scope=\"col\">Description</th>\n")
			.append(tabs(4) + "</tr>\n")
			.append(tabs(3) + "</thead>\n")
			.append(tabs(3) + "<tbody>\n");

		if (paramCount > 0)
		{
			stringBuilder.append(tabs(4) + "<tr>\n")
				.append(tabs(5) + "<th "
				+ HTMLBuilder.tagClass(
					HTMLClass.classStacks, HTMLClass.classIRowLabel)
				+ " rowspan=\"")
			.append(paramCount + 1).append("\">Parameters</th>\n")
			.append(tabs(4) + "</tr>\n");
		}

		//The ordered position of the parameter in the method signature.
		int position = 1;
		for (final StacksParameterTag paramTag : parameters)
		{
			stringBuilder.append(paramTag.toHTML(htmlFileMap,
				hashID, errorLog, position++));
		}

		stringBuilder.append(tabs(4) + "<tr>\n")
			.append(tabs(5) + "<th "
				+ HTMLBuilder.tagClass(
					HTMLClass.classStacks, HTMLClass.classIRowLabel)
				+ " colspan=\"")
			.append(colSpan).append("\">Returns</th>\n")
			.append(returnsContent.toHTML(htmlFileMap,
				hashID, errorLog, 1));

		if (exceptionCount > 0)
		{
			stringBuilder.append(tabs(5) + "<th "
					+ HTMLBuilder.tagClass(
						HTMLClass.classStacks, HTMLClass.classIRowLabel)
					+ " colspan=\"")
				.append(colSpan).append("\" rowspan=\"")
				.append(exceptionCount+1).append("\">Raises</th>\n");

			for (final StacksRaisesTag exception : exceptions)
			{
				stringBuilder.append(exception.toHTML(htmlFileMap,
					hashID, errorLog, 1));
			}
		}

		return stringBuilder.append(tabs(3) + "</tbody>\n")
			.append(tabs(2) + "</table>\n").toString();
	}
}
