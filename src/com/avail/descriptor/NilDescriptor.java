/**
 * NilDescriptor.java
 * Copyright © 1993-2017, The Avail Foundation, LLC.
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

import com.avail.annotations.AvailMethod;
import com.avail.annotations.ThreadSafe;
import com.avail.descriptor.MapDescriptor.Entry;
import com.avail.descriptor.MapDescriptor.MapIterable;
import com.avail.serialization.SerializerOperation;

import java.util.IdentityHashMap;
import java.util.NoSuchElementException;

import static com.avail.descriptor.BottomTypeDescriptor.bottom;
import static com.avail.descriptor.TypeDescriptor.Types.TOP;

/**
 * {@code NilDescriptor} implements the Avail {@linkplain #nil() null
 * object}, the sole instance of the invisible and uninstantiable root type, ⊤
 * (pronounced top).
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
public final class NilDescriptor
extends Descriptor
{
	@Override
	@AvailMethod @ThreadSafe
	boolean o_Equals (final AvailObject object, final A_BasicObject another)
	{
		return another.equalsNil();
	}

	@Override
	@AvailMethod @ThreadSafe
	int o_Hash (final AvailObject object)
	{
		// Nil should hash to zero, because the only place it can appear in a
		// data structure is as a filler object. This currently (as of July
		// 1998) applies to sets, maps, variables, and continuations.
		return 0;
	}

	@Override
	@AvailMethod @ThreadSafe
	A_Type o_Kind (final AvailObject object)
	{
		return TOP.o();
	}

	@Override
	@AvailMethod @ThreadSafe
	int o_BinSize (final AvailObject object)
	{
		// Nil acts as an empty bin.
		return 0;
	}

	@Override @AvailMethod @ThreadSafe
	SerializerOperation o_SerializerOperation (final AvailObject object)
	{
		return SerializerOperation.NIL;
	}

	@Override
	A_BasicObject o_MapBinAtHashPutLevelCanDestroy (
		final AvailObject object,
		final A_BasicObject key,
		final int keyHash,
		final A_BasicObject value,
		final byte myLevel,
		final boolean canDestroy)
	{
		return LinearMapBinDescriptor.createSingle(
			key,
			keyHash,
			value,
			myLevel);
	}

	@Override
	A_Type o_MapBinKeyUnionKind (final AvailObject object)
	{
		return bottom();
	}

	@Override
	A_Type o_MapBinValueUnionKind (final AvailObject object)
	{
		return bottom();
	}

	@Override
	AvailObject o_MapBinAtHash (
		final AvailObject object,
		final A_BasicObject key,
		final int keyHash)
	{
		return nil();
	}

	@Override
	int o_MapBinKeysHash (final AvailObject object)
	{
		return 0;
	}

	@Override
	int o_MapBinValuesHash (final AvailObject object)
	{
		return 0;
	}

	@Override
	MapIterable o_MapBinIterable (final AvailObject object)
	{
		return new MapIterable()
		{
			@Override
			public Entry next ()
			{
				throw new NoSuchElementException();
			}

			@Override
			public boolean hasNext ()
			{
				return false;
			}
		};
	}

	@Override
	@ThreadSafe
	public void printObjectOnAvoidingIndent (
		final AvailObject object,
		final StringBuilder builder,
		final IdentityHashMap<A_BasicObject, Void> recursionMap,
		final int indent)
	{
		builder.append("nil");
	}

	/**
	 * Construct a new {@link NilDescriptor}.
	 *
	 * @param mutability
	 *        The {@linkplain Mutability mutability} of the new descriptor.
	 */
	private NilDescriptor (final Mutability mutability)
	{
		super(mutability, TypeTag.NIL_TAG, null, null);
	}

	/** The mutable {@link NilDescriptor}. */
	private static final NilDescriptor mutable =
		new NilDescriptor(Mutability.MUTABLE);

	@Override
	NilDescriptor mutable ()
	{
		return mutable;
	}

	/** The immutable {@link NilDescriptor}. */
	private static final NilDescriptor immutable =
		new NilDescriptor(Mutability.IMMUTABLE);

	@Override
	NilDescriptor immutable ()
	{
		return immutable;
	}

	/** The shared {@link NilDescriptor}. */
	private static final NilDescriptor shared =
		new NilDescriptor(Mutability.SHARED);

	@Override
	NilDescriptor shared ()
	{
		return shared;
	}

	/** The sole instance of {@linkplain #nil() nil}. */
	private static final AvailObject soleInstance = shared.create();

	/**
	 * Answer the sole instance of nil.
	 *
	 * @return The sole instance of the nil.
	 */
	public static AvailObject nil ()
	{
		return soleInstance;
	}
}
