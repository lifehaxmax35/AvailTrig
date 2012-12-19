/**
 * ForwardDefinitionDescriptor.java
 * Copyright © 1993-2012, Mark van Gulik and Todd L Smith.
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

package com.avail.descriptor;

import static com.avail.descriptor.ForwardDefinitionDescriptor.ObjectSlots.*;
import java.util.List;
import com.avail.annotations.*;
import com.avail.descriptor.TypeDescriptor.Types;
import com.avail.serialization.SerializerOperation;

/**
 * This is a forward declaration of a method.  An actual method must be defined
 * with the same signature before the end of the current module.
 *
 * <p>While a call with this method signature can be compiled after the forward
 * declaration, an attempt to actually call the method will result in an error
 * indicating this problem.</p>
 *
 * <p>Because of the nature of forward declarations, it is meaningless to
 * forward declare a macro, so this facility is not provided.  It's
 * meaningless because a "call-site" for a macro causes the body to execute
 * immediately.</p>
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public class ForwardDefinitionDescriptor
extends DefinitionDescriptor
{
	/**
	 * The layout of object slots for my instances.
	 */
	public enum ObjectSlots
	implements ObjectSlotsEnum
	{
		/**
		 * Duplicated from parent.  The method in which this definition occurs.
		 */
		DEFINITION_METHOD,

		/**
		 * The {@linkplain FunctionTypeDescriptor function type} for which this
		 * signature is being specified.
		 */
		BODY_SIGNATURE;

		static
		{
			assert DefinitionDescriptor.ObjectSlots.DEFINITION_METHOD.ordinal()
				== DEFINITION_METHOD.ordinal();
		}
	}

	@Override @AvailMethod
	AvailObject o_BodySignature (final AvailObject object)
	{
		return object.slot(BODY_SIGNATURE);
	}

	@Override @AvailMethod
	int o_Hash (final AvailObject object)
	{
		return object.slot(BODY_SIGNATURE).hash() * 19
			^ object.slot(DEFINITION_METHOD).hash() * 757;
	}

	@Override @AvailMethod
	AvailObject o_Kind (
		final AvailObject object)
	{
		return Types.FORWARD_DEFINITION.o();
	}

	@Override @AvailMethod
	boolean o_IsForwardDefinition (final AvailObject object)
	{
		return true;
	}

	@Override
	SerializerOperation o_SerializerOperation (final AvailObject object)
	{
		return SerializerOperation.FORWARD_DEFINITION;
	}

	@Override
	void printObjectOnAvoidingIndent (
		final AvailObject object,
		final StringBuilder builder,
		final List<AvailObject> recursionList,
		final int indent)
	{
		object.slot(DEFINITION_METHOD).name().printOnAvoidingIndent(
			builder, recursionList, indent);
		builder.append(' ');
		object.slot(BODY_SIGNATURE).printOnAvoidingIndent(
			builder, recursionList, indent + 1);
	}

	/**
	 * Create a forward declaration signature for the given {@linkplain
	 * MethodDescriptor method} and {@linkplain FunctionTypeDescriptor function
	 * type}.
	 *
	 * @param definitionMethod
	 *        The method for which to declare a forward definition.
	 * @param bodySignature
	 *        The function type at which this forward definition should occur.
	 * @return The new forward declaration signature.
	 */
	public static AvailObject create (
		final AvailObject definitionMethod,
		final AvailObject bodySignature)
	{
		final AvailObject instance = mutable().create();
		instance.setSlot(ObjectSlots.DEFINITION_METHOD, definitionMethod);
		instance.setSlot(ObjectSlots.BODY_SIGNATURE, bodySignature);
		instance.makeImmutable();
		return instance;
	}

	/**
	 * Construct a new {@link ForwardDefinitionDescriptor}.
	 *
	 * @param isMutable
	 *        Does the {@linkplain Descriptor descriptor} represent a mutable
	 *        object?
	 */
	protected ForwardDefinitionDescriptor (final boolean isMutable)
	{
		super(isMutable);
	}

	/**
	 * The mutable {@link ForwardDefinitionDescriptor}.
	 */
	private static final ForwardDefinitionDescriptor mutable =
		new ForwardDefinitionDescriptor(true);

	/**
	 * Answer the mutable {@link ForwardDefinitionDescriptor}.
	 *
	 * @return The mutable {@link ForwardDefinitionDescriptor}.
	 */
	public static ForwardDefinitionDescriptor mutable ()
	{
		return mutable;
	}

	/**
	 * The immutable {@link ForwardDefinitionDescriptor}.
	 */
	private static final ForwardDefinitionDescriptor immutable =
		new ForwardDefinitionDescriptor(false);

	/**
	 * Answer the immutable {@link ForwardDefinitionDescriptor}.
	 *
	 * @return The immutable {@link ForwardDefinitionDescriptor}.
	 */
	public static ForwardDefinitionDescriptor immutable ()
	{
		return immutable;
	}
}