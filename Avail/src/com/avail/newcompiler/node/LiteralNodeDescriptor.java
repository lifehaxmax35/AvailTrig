/**
 * com.avail.newcompiler.node/LiteralNodeDescriptor.java
 * Copyright (c) 2010, Mark van Gulik.
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

package com.avail.newcompiler.node;

import com.avail.descriptor.AvailObject;

/**
 * My instances are occurrences of literals parsed from Avail source code.  At
 * the moment only strings and non-negative numbers are supported.
 *
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 */
public class LiteralNodeDescriptor extends ParseNodeDescriptor
{

	/**
	 * My slots of type {@link AvailObject}.
	 *
	 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
	 */
	public enum ObjectSlots
	{
		/**
		 * The token that was transformed into this literal.
		 */
		TOKEN,
	}

	/**
	 * Setter for field token.
	 */
	@Override
	public void o_Token (
		final AvailObject object,
		final AvailObject token)
	{
		object.objectSlotPut(ObjectSlots.TOKEN, token);
	}

	/**
	 * Getter for field token.
	 */
	@Override
	public AvailObject o_Token (
		final AvailObject object)
	{
		return object.objectSlot(ObjectSlots.TOKEN);
	}



	/**
	 * Construct a new {@link LiteralNodeDescriptor}.
	 *
	 * @param isMutable Whether my {@linkplain AvailObject instances} can
	 *                  change.
	 */
	public LiteralNodeDescriptor (final boolean isMutable)
	{
		super(isMutable);
	}

	/**
	 * The mutable {@link LiteralNodeDescriptor}.
	 */
	private final static LiteralNodeDescriptor mutable =
		new LiteralNodeDescriptor(true);

	/**
	 * Answer the mutable {@link LiteralNodeDescriptor}.
	 *
	 * @return The mutable {@link LiteralNodeDescriptor}.
	 */
	public static LiteralNodeDescriptor mutable ()
	{
		return mutable;
	}

	/**
	 * The immutable {@link LiteralNodeDescriptor}.
	 */
	private final static LiteralNodeDescriptor immutable =
		new LiteralNodeDescriptor(false);

	/**
	 * Answer the immutable {@link LiteralNodeDescriptor}.
	 *
	 * @return The immutable {@link LiteralNodeDescriptor}.
	 */
	public static LiteralNodeDescriptor immutable ()
	{
		return immutable;
	}

}