/**
 * compiler/scanner/AvailScannerException.java
 * Copyright (c) 2011, Mark van Gulik.
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

package com.avail.compiler.scanning;

import com.avail.annotations.NotNull;
import com.avail.descriptor.*;

/**
 * An {@code AvailScannerException} is thrown if a problem occurs while
 * an {@link AvailScanner} attempts to convert an Avail source file into a
 * sequence of {@linkplain TokenDescriptor tokens}.
 *
 * @author Mark van Gulik &lt;ghoul137@gmail.com&gt;
 */
public class AvailScannerException
extends RuntimeException
{
	/**
	 * The serial version identifier.
	 */
	private static final long serialVersionUID = 8191896822852052633L;

	/**
	 * The {@linkplain ByteStringDescriptor error message} indicating why the
	 * scanner failed.
	 */
	final String failureString;

	/**
	 * Return the error mesasge {@link String} indicating why the {@link
	 * AvailScanner} failed.
	 *
	 * @return The reason the scanner could not complete its work.
	 */
	public @NotNull String failureString ()
	{
		return failureString;
	}

	/**
	 * The {@link AvailScanner} that failed.
	 */
	final AvailScanner failedScanner;

	/**
	 * Return the file position at which the {@link AvailScanner} failed.
	 *
	 * @return The position in the file at which the scanner failed.
	 */
	public long failurePosition ()
	{
		return failedScanner.position();
	}

	/**
	 * Return the line number at which the {@link AvailScanner} failed.
	 *
	 * @return The line number at which the scanner failed.
	 */
	public long failureLineNumber ()
	{
		return failedScanner.lineNumber;
	}

	/**
	 * Construct a new {@link AvailScannerException}.
	 *
	 * @param failureString
	 *            The error message indicating why the {@link AvailScanner}
	 *            failed.
	 * @param failedScanner
	 *            The AvailScanner that failed, positioned to the failure point.
	 */
	public AvailScannerException (
		final @NotNull String failureString,
		final AvailScanner failedScanner)
	{
		this.failureString = failureString;
		this.failedScanner= failedScanner;
	}
}