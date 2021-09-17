/*
 * AvailRoot.kt
 * Copyright © 1993-2021, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of the copyright holder nor the names of the contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
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

package org.availlang.plugin

import org.gradle.api.Project
import java.net.URI

/**
 * `AvailRoot` represents an Avail source root.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @property name
 *   The name of the root.
 * @property uri
 *   The String [URI] location of the root.
 * @property action
 *   A lambda that accepts this [AvailRoot] and is executed after
 *   [AvailExtension.initExtension] is run.
 *
 * @constructor
 * Construct an [AvailRoot].
 *
 * @param name
 *   The name of the root.
 * @param uri
 *   The String [URI] location of the root.
 * @param action
 *   A lambda that accepts this [AvailRoot] and is executed after
 *   [AvailExtension.initExtension] is run.
 */
open class AvailRoot constructor(
	val name: String,
	val uri: String,
	var action: (AvailRoot) -> Unit = {}
): Comparable<AvailRoot>
{
	/** The VM Options, `-DavailRoot`, root string. */
	val rootString: String by lazy { "$name=$uri" }

	override fun compareTo(other: AvailRoot): Int =
		name.compareTo(other.name)

	override fun toString(): String = rootString

	override fun equals(other: Any?): Boolean =
		when
		{
			this === other -> true
			other !is AvailRoot -> false
			name != other.name -> false
			uri != other.uri -> false
			else -> true
		}

	override fun hashCode(): Int
	{
		var result = name.hashCode()
		result = 31 * result + uri.hashCode()
		return result
	}
}

/**
 * `CreateAvailRoot` is an [AvailRoot] that is intended to be created
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @constructor
 * Construct an [AvailRoot].
 *
 * @param name
 *   The name of the root.
 * @param uri
 *   The String [URI] location of the root.
 *
 * @param action
 *   A lambda that accepts this [AvailRoot] and is executed after
 *   [AvailExtension.initExtension] is run.
 */
class CreateAvailRoot constructor(
	name: String,
	uri: String,
	action: (AvailRoot) -> Unit = {}
): AvailRoot(name, uri, action)
{
	/**
	 * Add an [AvailModule] with the given name to the top level of this
	 * [CreateAvailRoot].
	 *
	 * @param name
	 *   The name of the [AvailModule] to create and add.
	 * @param extension
	 *   The Module's file extension. Defaults to `"avail"`.
	 *   Do not prefix with ".".
	 * @param initializer
	 *   A lambda that accepts the created `AvailModule` and enables the user
	 *   to configure it.
	 */
	@Suppress("unused")
	fun module (
		name: String,
		extension: String = "avail",
		initializer: (AvailModule) -> Unit)
	{
		val mod = AvailModule(name, extension)
		initializer(mod)
		modules.add(mod)
	}

	/**
	 * Add an [AvailModulePackage] with the given name to the top level of this
	 * [CreateAvailRoot].
	 *
	 * @param name
	 *   The name of the [AvailModule] to create and add.
	 * @param extension
	 *   The Module's file extension. Defaults to `"avail"`.
	 *   Do not prefix with ".".
	 * @param initializer
	 *   A lambda that accepts the created `AvailModule` and enables the user
	 *   to configure it.
	 */
	@Suppress("unused")
	fun modulePackage (
		name: String,
		extension: String = "avail",
		initializer: (AvailModulePackage) -> Unit)
	{
		val mod = AvailModulePackage(name, extension)
		initializer(mod)
		modulePackages.add(mod)
	}

	/**
	 * The set of [AvailModule]s to add to the top level of this [AvailRoot].
	 */
	private val modules =
		mutableSetOf<AvailModule>()

	/**
	 * The set of [AvailModulePackage]s to add to the top level of this
	 * [AvailRoot].
	 */
	private val modulePackages =
		mutableSetOf<AvailModulePackage>()

	/**
	 * Create the [modules] and [modulePackages] in [roots directory][uri].
	 *
	 * @param project
	 *   The host [Project] running the Avail Plugin.
	 */
	internal fun create (project: Project, extension: AvailExtension)
	{
		modulePackages.forEach {
			if (it.moduleHeaderCommentBody.isEmpty()
				&& extension.moduleHeaderCommentBody.isNotEmpty())
			{
				it.moduleHeaderCommentBody = extension.moduleHeaderCommentBody
			}
			it.create(project, uri)
		}
		modules.forEach {
			it.create(project, uri)
		}
	}
}

/**
 * Represents an Avail module file to be added. Will only be created if it does
 * not exist when the initialization runs.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @property baseName
 *   The name of the module without the file extension.
 *
 * @constructor
 * Construct an [AvailModule].
 *
 * @param baseName
 *   The name of the module without the file extension.
 * @param fileExtension
 *   The file extension to use for the module. This defaults to `avail`.
 *   Do not prefix with ".".
 */
open class AvailModule constructor(
	private val baseName: String,
	fileExtension: String = "avail")
{
	/**
	 * The file name. *e.g. Avail.avail*.
	 */
	val fileName: String = "$baseName.$fileExtension"

	/**
	 * Raw module header comment. This is typically for a copyright. Will be
	 * wrapped in comment along with file name. If comment body is empty
	 * (*default*), will only provide the file name in the header comment.
	 */
	var moduleHeaderCommentBody: String = ""

	/**
	 * The list of module `Versions` to populate the `Versions` section of the
	 * module header.
	 */
	@Suppress("unused")
	var versions: List<String> = listOf()

	/**
	 * The list of Avail Modules this [AvailModule] will `Extend` for Avail
	 * Modules that `Use`/`Extend` this module as well as use in the `Body`
	 */
	@Suppress("unused")
	var extends: List<String> = listOf()

	/**
	 * The list of Avail Modules this [AvailModule] will be able to `Use`
	 * in the `Body` of this module..
	 */
	@Suppress("unused")
	var uses: List<String> = listOf()

	/**
	 * The file contents that will be written to the file upon a call to create.
	 */
	internal val fileContents: String get() =
		buildString {
			// File header comment
			append("/*\n")
			append(" * ")
			append(fileName)
			if (moduleHeaderCommentBody.isNotEmpty())
			{
				moduleHeaderCommentBody.split("\n").forEach {
					append("\n * ")
					append(it)
				}
			}
			append("\n */")

			// Module
			append("\n\nModule \"")
			append(baseName)
			append('"')

			// Versions
			if (versions.isNotEmpty())
			{
				append("\nVersions")
				versions.forEach {
					append("\n\t\"")
					append(it)
					append('"')
				}
			}

			// Uses
			if (uses.isNotEmpty())
			{
				append("\nUses")
				uses.forEach {
					append("\n\t\"")
					append(it)
					append('"')
				}
			}

			// Extends
			if (extends.isNotEmpty())
			{
				append("\nExtends")
				extends.forEach {
					append("\n\t\"")
					append(it)
					append('"')
				}
			}
			append("\nBody\n")
		}

	/**
	 * Create the Avail Module File. This will do nothing if the file exists;
	 * will only be created if it does not exist when the initialization runs.
	 *
	 * @param project
	 *  The host project executing this task.
	 * @param directory
	 *  The location to place the Module.
	 */
	internal open fun create (project: Project, directory: String)
	{
		val module = project.file("$directory/$fileName")
		if (!module.exists())
		{
			project.mkdir(directory)
			module.writeText(fileContents)
		}
	}

	override fun equals(other: Any?): Boolean =
		when
		{
			this === other -> true
			other !is AvailModule -> false
			fileName != other.fileName -> false
			else -> true
		}


	override fun hashCode(): Int = fileName.hashCode()
}

/**
 * `AvailModulePackage` is an Avail package with a module representative.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @constructor
 * Construct an [AvailModulePackage].
 *
 * @param baseName
 *   The name of the module without the file extension.
 * @param fileExtension
 *   The file extension to use for the module. This defaults to `avail`
 */
class AvailModulePackage constructor(
	baseName: String,
	fileExtension: String = "avail"
): AvailModule(baseName, fileExtension)
{
	override fun create (project: Project, directory: String)
	{
		val modulePackage = "$directory/$fileName"
		project.mkdir(modulePackage)
		val module = project.file("$modulePackage/$fileName")
		if (!module.exists())
		{
			module.writeText(fileContents)
		}
	}
}
