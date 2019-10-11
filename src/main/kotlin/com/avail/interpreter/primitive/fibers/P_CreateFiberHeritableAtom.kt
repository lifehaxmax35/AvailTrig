/*
 * P_CreateFiberHeritableAtom.java
 * Copyright © 1993-2018, The Avail Foundation, LLC.
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

package com.avail.interpreter.primitive.fibers

import com.avail.descriptor.A_Atom
import com.avail.descriptor.A_Module
import com.avail.descriptor.A_Set
import com.avail.descriptor.A_String
import com.avail.descriptor.A_Type
import com.avail.descriptor.AtomDescriptor
import com.avail.descriptor.AtomDescriptor.SpecialAtom
import com.avail.descriptor.FiberDescriptor
import com.avail.exceptions.AvailErrorCode
import com.avail.interpreter.Interpreter
import com.avail.interpreter.Primitive
import com.avail.optimizer.jvm.ReferencedInGeneratedCode
import com.avail.utility.MutableOrNull

import com.avail.descriptor.AbstractEnumerationTypeDescriptor.enumerationWith
import com.avail.descriptor.AtomDescriptor.SpecialAtom.HERITABLE_KEY
import com.avail.descriptor.AtomDescriptor.createAtom
import com.avail.descriptor.AtomDescriptor.trueObject
import com.avail.descriptor.FunctionTypeDescriptor.functionType
import com.avail.descriptor.ModuleDescriptor.currentModule
import com.avail.descriptor.NilDescriptor.nil
import com.avail.descriptor.ObjectTupleDescriptor.tuple
import com.avail.descriptor.SetDescriptor.set
import com.avail.descriptor.TupleTypeDescriptor.stringType
import com.avail.descriptor.TypeDescriptor.Types.ATOM
import com.avail.exceptions.AvailErrorCode.E_AMBIGUOUS_NAME
import com.avail.exceptions.AvailErrorCode.E_ATOM_ALREADY_EXISTS
import com.avail.interpreter.Primitive.Flag.CanInline

/**
 * **Primitive:** Create a new [atom][AtomDescriptor]
 * with the given name that represents a [ ][SpecialAtom.HERITABLE_KEY] [fiber][FiberDescriptor]
 * variable.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
object P_CreateFiberHeritableAtom : Primitive(1, CanInline)
{

	override fun attempt(
		interpreter: Interpreter): Primitive.Result
	{
		interpreter.checkArgumentCount(1)
		val name = interpreter.argument(0)
		val module = currentModule()
		val trueName = MutableOrNull<A_Atom>()
		val errorCode = MutableOrNull<AvailErrorCode>()
		if (!module.equalsNil())
		{
			module.lock {
				val trueNames = module.trueNamesForStringName(name)
				if (trueNames.setSize() == 0)
				{
					val newName = createAtom(name, module)
					newName.setAtomProperty(
						HERITABLE_KEY.atom, trueObject())
					module.addPrivateName(newName)
					trueName.value = newName
				}
				else if (trueNames.setSize() == 1)
				{
					errorCode.value = E_ATOM_ALREADY_EXISTS
				}
				else
				{
					errorCode.value = E_AMBIGUOUS_NAME
				}
			}
		}
		else
		{
			val newName = createAtom(name, nil)
			newName.setAtomProperty(HERITABLE_KEY.atom, trueObject())
			trueName.value = newName
		}
		return if (errorCode.value != null)
		{
			interpreter.primitiveFailure(errorCode.value!!)
		}
		else interpreter.primitiveSuccess(trueName.value().makeShared())
	}

	override fun privateBlockTypeRestriction(): A_Type
	{
		return functionType(
			tuple(stringType()),
			ATOM.o())
	}

	override fun privateFailureVariableType(): A_Type
	{
		return enumerationWith(set(E_ATOM_ALREADY_EXISTS, E_AMBIGUOUS_NAME))
	}

}