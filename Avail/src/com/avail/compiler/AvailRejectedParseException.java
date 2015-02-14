/**
 * AvailRejectedParseException.java
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

package com.avail.compiler;

import com.avail.descriptor.*;
import com.avail.exceptions.PrimitiveThrownException;

/**
 * An {@code AvailCompilerException} is thrown by the {@linkplain
 * AbstractAvailCompiler Avail compiler} when compilation fails for any reason.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public class AvailRejectedParseException
extends PrimitiveThrownException
{
	/**
	 * The serial version identifier.
	 */
	private static final long serialVersionUID = -5638050952579212324L;

	/**
	 * The {@linkplain StringDescriptor error message} indicating why a
	 * particular parse was rejected.
	 */
	final A_String rejectionString;

	/**
	 * Return the {@linkplain StringDescriptor error message} indicating why
	 * a particular parse was rejected.
	 *
	 * @return The reason the parse was rejected.
	 */
	public A_String rejectionString ()
	{
		return rejectionString;
	}

	/**
	 * Construct a new {@link AvailRejectedParseException}.  If this diagnostic
	 * is deemed relevant, the string will be presented after the word
	 * "Expected...".
	 *
	 * @param rejectionString
	 *        The {@linkplain StringDescriptor error message} indicating why
	 *        a particular parse was rejected.
	 */
	public AvailRejectedParseException (
		final A_String rejectionString)
	{
		this.rejectionString = rejectionString;
	}

	/**
	 * Construct a new {@link AvailRejectedParseException} with a Java {@link
	 * String} as the explanation.  If this diagnostic is deemed relevant, the
	 * string will be presented after the word "Expected...".
	 *
	 * @param rejectionJavaString
	 *        The Java {@link String} indicating why a particular parse was
	 *        rejected.
	 */
	public AvailRejectedParseException (
		final String rejectionJavaString)
	{
		this.rejectionString = StringDescriptor.from(rejectionJavaString);
	}
}
