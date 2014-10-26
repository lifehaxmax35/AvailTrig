/**
 * JVMCodeGenerationException.java
 * Copyright © 1993-2014, The Avail Foundation, LLC.
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

package com.avail.interpreter.jvm;

/**
 * An exception that occurs in the process of generating JVM class files.
 *
 * @author Rich Arriaga &lt;rich@availlang.org&gt;
 */
public class JVMCodeGenerationException extends Exception
{
	/** The serial version identifier. */
	private static final long serialVersionUID = -2285521985624140958L;

	/**
	 * Construct a new {@link JVMCodeGenerationException}.
	 *
	 */
	public JVMCodeGenerationException ()
	{

	}

	/**
	 * Construct a new {@link JVMCodeGenerationException}.
	 *
	 * @param message
	 */
	public JVMCodeGenerationException (final String message)
	{
		super(message);
	}

	/**
	 * Construct a new {@link JVMCodeGenerationException}.
	 *
	 * @param cause
	 */
	public JVMCodeGenerationException (final Throwable cause)
	{
		super(cause);
	}

	/**
	 * Construct a new {@link JVMCodeGenerationException}.
	 *
	 * @param message
	 * @param cause
	 */
	public JVMCodeGenerationException (final String message,
		final Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Construct a new {@link JVMCodeGenerationException}.
	 *
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public JVMCodeGenerationException (
		final String message,
		final Throwable cause,
		final boolean enableSuppression,
		final boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
