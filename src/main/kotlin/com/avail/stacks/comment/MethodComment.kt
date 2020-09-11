/*
 * MethodCommentImplementation.kt
 * Copyright © 1993-2020, The Avail Foundation, LLC.
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

package com.avail.stacks.comment

import com.avail.descriptor.tuples.A_String
import com.avail.descriptor.tuples.StringDescriptor.Companion.stringFrom
import com.avail.stacks.CommentGroup
import com.avail.stacks.LinkingFileMap
import com.avail.stacks.StacksDescription
import com.avail.stacks.StacksErrorLog
import com.avail.stacks.comment.signature.MethodCommentSignature
import com.avail.stacks.module.StacksImportModule
import com.avail.stacks.tags.StacksAliasTag
import com.avail.stacks.tags.StacksAuthorTag
import com.avail.stacks.tags.StacksCategoryTag
import com.avail.stacks.tags.StacksParameterTag
import com.avail.stacks.tags.StacksRaisesTag
import com.avail.stacks.tags.StacksReturnTag
import com.avail.stacks.tags.StacksSeeTag
import com.avail.utility.json.JSONWriter

/**
 * A comment that describes a particular method implementation
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @property parameters
 *   The list of [parameters][StacksParameterTag] of the method implementation.
 * @property returnsContent
 *   The [&quot;@returns&quot;][StacksReturnTag] content
 * @property exceptions
 *   A [MutableList] of any [exceptions][StacksRaisesTag] the method throws.
 *
 * @constructor
 * Construct a new [MethodComment].
 *
 * @param signature
 *   The [signature][MethodCommentSignature] of the class/method the comment
 *   describes.
 * @param commentStartLine
 *   The start line in the module the comment being parsed appears.
 * @param author
 *   The [author][StacksAuthorTag] of the implementation.
 * @param sees
 *   A list of any [&quot;@sees&quot;][StacksSeeTag] references.
 * @param description
 *   The overall description of the implementation
 * @param categories
 *   The categories the implementation appears in
 * @param aliases
 *   The aliases the implementation is known by
 * @param parameters
 *   The list of [parameters][StacksParameterTag] of the method implementation.
 * @param returnsContent
 *   The [&quot;@returns&quot;][StacksReturnTag] content
 * @param exceptions
 *   A [MutableList] of any [exceptions][StacksRaisesTag] the method throws.
 * @param sticky
 *   Whether or not the method should be documented regardless of visibility
 */
class MethodComment constructor(
		signature: MethodCommentSignature,
		commentStartLine: Int,
		author: MutableList<StacksAuthorTag>,
		sees: MutableList<StacksSeeTag>,
		description: StacksDescription,
		categories: MutableList<StacksCategoryTag>,
		aliases: MutableList<StacksAliasTag>,
		internal val parameters: MutableList<StacksParameterTag>,
		val returnsContent: StacksReturnTag,
		internal val exceptions: MutableList<StacksRaisesTag>,
		sticky: Boolean)
	: AvailComment(
		signature,
		commentStartLine,
		author,
		sees,
		description,
		categories,
		aliases,
		sticky)
{

	/**
	 * The hash id for this implementation
	 */
	private val hashID: Int

	init
	{
		this.hashID =
			stringFrom(StringBuilder().apply {
				for (param in signature.orderedInputTypes)
				{
					append(param)
				}
			}.toString()).hash()
	}

	override fun addToImplementationGroup(
		commentGroup: CommentGroup)
	{
		commentGroup.addMethod(this)
	}

	override fun addImplementationToImportModule(
		name: A_String, importModule: StacksImportModule)
	{
		importModule.addMethodImplementation(name, this)
	}

	override fun toJSON(
		linkingFileMap: LinkingFileMap,
		nameOfGroup: String,
		errorLog: StacksErrorLog,
		jsonWriter: JSONWriter)
	{
		signature.toJSON(nameOfGroup, isSticky, jsonWriter)

		if (categories.isNotEmpty())
		{
			categories[0].toJSON(
				linkingFileMap,
				hashID, errorLog, 1, jsonWriter)
		}
		else
		{
			jsonWriter.write("categories")
			jsonWriter.startArray()
			jsonWriter.endArray()
		}

		if (aliases.isNotEmpty())
		{
			aliases[0].toJSON(
				linkingFileMap,
				hashID, errorLog, 1, jsonWriter)
		}
		else
		{
			jsonWriter.write("aliases")
			jsonWriter.startArray()
			jsonWriter.endArray()
		}

		jsonWriter.write("sees")
		jsonWriter.startArray()
		for (see in sees)
		{
			jsonWriter.write(
				see.thingToSee().toJSON(
					linkingFileMap, hashID,
					errorLog, jsonWriter))
		}
		jsonWriter.endArray()

		jsonWriter.write("description")
		description.toJSON(linkingFileMap, hashID, errorLog, jsonWriter)

		//The ordered position of the parameter in the method signature.
		jsonWriter.write("parameters")
		jsonWriter.startArray()
		var position = 1
		for (paramTag in parameters)
		{
			paramTag.toJSON(
				linkingFileMap, hashID, errorLog, position++,
				jsonWriter)
		}
		jsonWriter.endArray()

		returnsContent.toJSON(linkingFileMap, hashID, errorLog, 1, jsonWriter)

		jsonWriter.write("raises")
		jsonWriter.startArray()
		for (exception in exceptions)
		{
			exception.toJSON(linkingFileMap, hashID, errorLog, 1, jsonWriter)
		}
		jsonWriter.endArray()
	}

	override fun toString(): String = signature.toString()
}
