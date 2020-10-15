/*
 * Repositories.kt
 * Copyright © 1993-2020, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
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

package com.avail.persistence.cache

import com.avail.builder.ModuleRoot
import java.io.File

/**
 * {@code Repositories} manages all the system repositories.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
object Repositories
{
	/**
	 * The default directory location where [Repository] repo files will be
	 * stored.
	 */
	private const val defaultDirectoryLocation: String = "repositories/"

	/**
	 * The file extension for a repo.
	 */
	private const val repoExtension = "repo"

	/**
	 * The [Repository]s directory.
	 */
	lateinit var directory: File
		private set

	/**
	 * Set the [directory].
	 *
	 * @param file
	 *   The directory to set.
	 */
	fun setDirectoryLocation (file: File)
	{
		require(file.isDirectory) {
			"Repositories directory, $file, is not a directory!"
		}
		directory = file
	}

	/**
	 * The map from [Repository.rootName] to the corresponding [Repository].
	 */
	private val repositories = mutableMapOf<String, Repository>()

	operator fun get(name: String): Repository? = repositories[name]

	/**
	 * Add a [Repository] for the given [ModuleRoot].
	 *
	 * @param root
	 *   The [ModuleRoot] to add a repo for.
	 */
	fun addRepo (root: ModuleRoot)
	{
		repositories[root.name] =
			Repository(
				root.name,
				File("${directory.absolutePath}/${root.name}.${repoExtension}"))
	}

	/**
	 * Delete the [Repository] for the given [Repository.rootName].
	 *
	 * @param rootName
	 *   The name of the repo that should be removed.
	 */
	fun deleteRepo (rootName: String)
	{
		repositories.remove(rootName)
	}

	/**
	 * [Clear][Repository.clear] all [Repository]s in [Repositories].
	 */
	fun clearAllRepositories ()
	{
		repositories.values.forEach { it.clear() }
	}

	/**
	 * [Clear][Repository.clear] the [Repository] for the given
	 * [Repository.rootName].
	 *
	 * @param rootName
	 *   The name of the repo that should be cleared.
	 */
	fun clearRepoFor (rootName: String)
	{
		repositories[rootName]?.clear()
	}

	/**
	 * [Close][Repository.close] each [Repository] in [Repositories].
	 */
	fun closeAllRepos ()
	{
		repositories.values.forEach { it.close() }
	}

	/**
	 * [Close][Repository.close] each [Repository] in [Repositories], then
	 * completely remove them all.
	 */
	fun closeAndRemoveAllRepos ()
	{
		repositories.values.forEach { it.close() }
		repositories.clear()
	}
}