/**
 * com.avail.test/RenamesFileParserTest.java
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

package com.avail.test;

import static org.junit.Assert.*;
import java.io.File;
import java.io.StringReader;
import org.junit.Test;
import com.avail.annotations.NotNull;
import com.avail.compiler.ModuleNameResolver;
import com.avail.compiler.ModuleRoots;
import com.avail.compiler.RenamesFileParser;
import com.avail.compiler.RenamesFileParserException;

/**
 * Unit tests for {@link ModuleNameResolver}.
 *
 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
 */
public final class ModuleNameResolverTest
{
	/** The root of the library path. */
	private static final @NotNull String libraryPath =
		new File("avail").getAbsolutePath();

	/** The root of the experimental path. */
	private static final @NotNull String experimentalPath =
		new File("experimental").getAbsolutePath();

	/** The Avail module path. */
	private static final @NotNull String modulePath =
		"avail=" + libraryPath + ";experimental=" + experimentalPath;

	/** The Avail module path roots. */
	private static final @NotNull ModuleRoots roots =
		new ModuleRoots(modulePath);

	/**
	 * Test: Test basic functionality of {@link RenamesFileParser}.
	 * 
	 * @throws RenamesFileParserException
	 *         If {@link RenamesFileParser#parse()} fails for any reason.
	 */
	@Test
	public void testParse () throws RenamesFileParserException
	{
		final String[][] rules =
		{
			{ "/avail/Happy-happy", "/experimental/Joy-Joy" },
			{ "/avail/Input-Output", "/avail/IO" },
			{ "/avail/Kernel/Tier-4/Tier-3/Effort", "/avail/Process/Process" }
		};

		final String[][] cases =
		{
			{
				"/Happy-happy",
				"Joy-Joy.avail",
				experimentalPath
			},
			{
				"/Input-Output",
				"IO.avail/Main.avail",
				libraryPath
			},
			{
				"/Kernel/Tier-4/Tier-3/Effort",
				"Process.avail/Process.avail",
				libraryPath
			},
			{
				"/Kernel/Tier-4/Collection",
				"Kernel.avail/Tier-4.avail/Collection.avail",
				libraryPath				
			},
			{
				"/Kernel/Tier-4/Tier-3",
				"Kernel.avail/Tier-4.avail/Tier-3.avail/Main.avail",
				libraryPath
			},
			{
				"/Kernel/Tier-4/Tier-3/Factory",
				"Kernel.avail/Tier-4.avail/Factory.avail",
				libraryPath
			},
			{
				"/Kernel/Tier-4/Tier-3/Tier-2/Tier-1/Tier-0/Joy-Joy",
				"Joy-Joy.avail",
				experimentalPath
			},
			{
				"/Kernel/Tier-4/Tier-3/Tier-2/Tier-1/Tier-0/IO",
				"IO.avail/Main.avail",
				libraryPath
			}
		};

		final RenamesFileParser parser =
			new RenamesFileParser(new StringReader(
				RenamesFileParser.renamesFileFromRules(rules)),
				roots);
		final ModuleNameResolver renames = parser.parse();

		for (final String[] aCase : cases)
		{
			final int index = aCase[0].lastIndexOf('/');
			final String moduleGroup = aCase[0].substring(0, index);
			final String localName = aCase[0].substring(index + 1);

			final File expected = new File(aCase[2], aCase[1]);
			final File actual   = renames.resolve(
				"/avail" + moduleGroup, localName);
			assertEquals(expected, actual);
		}
	}
}