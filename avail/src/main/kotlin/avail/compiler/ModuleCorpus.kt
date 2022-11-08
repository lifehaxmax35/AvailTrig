/*
 * ModuleCorpus.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
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

package avail.compiler

import avail.compiler.ModuleImport.Companion.fromSerializedTuple
import avail.descriptor.maps.MapDescriptor
import avail.descriptor.methods.MethodDescriptor.SpecialMethodAtom
import avail.descriptor.sets.SetDescriptor
import avail.descriptor.tuples.A_String
import avail.descriptor.tuples.A_Tuple
import avail.descriptor.tuples.A_Tuple.Companion.tupleAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tupleFromArray
import avail.descriptor.tuples.StringDescriptor
import avail.descriptor.tuples.TupleDescriptor
import org.availlang.persistence.MalformedSerialStreamException

/**
 * Information that a [ModuleHeader] uses to keep track of a "Corpus"
 * declaration inside a package representative.
 *
 * A [ModuleCorpus] entry comprises a locally resolvable module name and a file
 * name pattern.  The idea is that when a package representative has a corpus
 * entry, any files within that package which match the file name pattern will
 * be treated as modules. Such modules *do not have a module header*, and
 * instead have an effective header which simply imports (for "Extends") the
 * module name that was mentioned in the corpus entry.
 *
 * @property isExtension
 *   Whether this [ModuleCorpus] is due to an Extends clause rather than a
 *   `Uses` clause, as indicated by
 *   [module&#32;header][SpecialMethodAtom.MODULE_HEADER].
 * @property wildcard
 *   Whether to include all names exported by the predecessor module that are
 *   not otherwise excluded by this import.
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 *
 * @constructor
 *
 * Construct a new [ModuleCorpus].
 *
 * @param moduleName
 *   The non-resolved [name][StringDescriptor] of the module to import.
 * @param acceptableVersions
 *   The [set][SetDescriptor] of version [strings][String] from which to look
 *   for a match in the actual imported module's list of compatible versions.
 * @param isExtension
 *   `true` if these imported declarations are supposed to be re-exported from
 *   the current module.
 * @param names
 *   The [set][SetDescriptor] of names ([strings][A_String]) imported from the
 *   module.  They will be cause atoms to be looked up within the predecessor
 *   module, and will be re-exported verbatim if `isExtension` is `true`.
 * @param renames
 *   The [map][MapDescriptor] from new names to old names (both
 *   [strings][StringDescriptor]) that are imported from the module.  The new
 *   names will become new atoms in the importing module, and exported if
 *   `isExtension` is `true`.
 * @param excludes
 *   The [set][SetDescriptor] of names ([strings][String]) to exclude from being
 *   imported.
 * @param wildcard
 *   Whether to import any published names not explicitly excluded.
 * @throws ImportValidationException
 *   If the specification is invalid.
 */
class ModuleCorpus constructor(
	val moduleName: A_String,
	val filePattern: A_String)
{
	init
	{
		moduleName.makeShared()
		filePattern.makeShared()
	}

	/**
	 * Answer a tuple suitable for serializing this import information.
	 *
	 * This currently consists of exactly 2 elements:
	 *
	 *  1. The module name that defines the DSL.
	 *  2. The file pattern used to identify headerless modules in this
	 *     directory.
	 *
	 * @see [fromSerializedTuple]
	 * @return
	 *   The tuple to serialize.
	 */
	internal val tupleForSerialization
		get() = tupleFromArray(
			moduleName,
			filePattern)

	companion object
	{
		/**
		 * Convert the provided [tuple][TupleDescriptor] into a `ModuleImport`.
		 * This is the reverse of the transformation provided by
		 * [tupleForSerialization].
		 *
		 * @param serializedTuple
		 *   The tuple from which to build a `ModuleImport`.
		 * @return
		 *   The `ModuleImport`.
		 * @throws MalformedSerialStreamException
		 *   If the module import specification is invalid.
		 */
		@Throws(MalformedSerialStreamException::class)
		fun fromSerializedTuple(serializedTuple: A_Tuple): ModuleCorpus
		{
			assert(serializedTuple.tupleSize == 2)
			return ModuleCorpus(
				moduleName = serializedTuple.tupleAt(1),
				filePattern = serializedTuple.tupleAt(2))
		}
	}
}
