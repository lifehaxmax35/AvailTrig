/**
 * Attribute.java
 * Copyright © 1993-2013, Mark van Gulik and Todd L Smith.
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

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.avail.interpreter.jvm.ConstantPool.Utf8Entry;

/**
 * {@code Attributes} are an open-ended mechanism used by the Java class file
 * format to specify arbitrary qualities of {@linkplain Class classes},
 * {@linkplain Field fields}, and {@linkplain Method methods}.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 * @see <a
 *     href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7">
 *     Attributes</a>
 */
public abstract class Attribute
{
	/**
	 * Answer the name of the {@linkplain Attribute attribute}.
	 *
	 * @return The name of the attribute.
	 */
	public abstract String name ();

	/**
	 * Answer the size of the {@linkplain Attribute attribute}.
	 *
	 * @return The size of the attribute.
	 */
	protected abstract int size ();

	/**
	 * Write the body of the {@linkplain Attribute attribute} to the specified
	 * {@linkplain DataOutput binary stream}.
	 *
	 * @param out
	 *        A binary output stream.
	 * @throws IOException
	 *         If the operation fails.
	 */
	public abstract void writeBodyTo (DataOutput out) throws IOException;

	/**
	 * Write the {@linkplain Attribute attribute} as an {@code attribute_info}
	 * structure to the specified {@linkplain DataOutput binary stream}.
	 *
	 * @param out
	 *        A binary output stream.
	 * @param constantPool
	 *        The {@linkplain ConstantPool constant pool} to use to encode the
	 *        {@linkplain #name() attribute name} as a {@link Utf8Entry}.
	 * @throws IOException
	 *         If the operation fails.
	 */
	final void writeTo (final DataOutput out, final ConstantPool constantPool)
		throws IOException
	{
		final Utf8Entry attributeNameEntry = constantPool.utf8(name());
		attributeNameEntry.writeIndexTo(out);
		final int size = size();
		out.writeInt(size);
		writeBodyTo(out);
	}
}