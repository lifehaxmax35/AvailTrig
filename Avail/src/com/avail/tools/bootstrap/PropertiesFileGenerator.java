/**
 * PropertiesFileGenerator.java
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

package com.avail.tools.bootstrap;

import static com.avail.tools.bootstrap.Resources.*;
import static com.avail.tools.bootstrap.Resources.Key.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import com.avail.annotations.NotNull;

/**
 * {@code PropertiesFileGenerator} defines state and operations common to the
 * Avail properties file generators.
 *
 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
 */
abstract class PropertiesFileGenerator
{
	/** The base name of the {@linkplain ResourceBundle resource bundle}. */
	protected final @NotNull String baseName;

	/** The target {@linkplain Locale locale}. */
	protected final @NotNull Locale locale;

	/**
	 * The {@linkplain ResourceBundle resource bundle} that contains file
	 * preamble information.
	 */
	protected final @NotNull ResourceBundle preambleBundle;

	/**
	 * Generate the preamble for the properties file. This includes the
	 * copyright and machine generation warnings.
	 *
	 * @param writer
	 *        The {@linkplain PrintWriter output stream}.
	 */
	protected void generatePreamble (final @NotNull PrintWriter writer)
	{
		writer.println(MessageFormat.format(
			preambleBundle.getString(propertiesCopyright.name()),
			localName(baseName) + "_" + locale.getLanguage(),
			new Date()));
		writer.println(MessageFormat.format(
			preambleBundle.getString(generatedPropertiesNotice.name()),
			getClass().getName(),
			new Date()));
	}

	/**
	 * Write the names of the properties.
	 *
	 * @param properties
	 *        The existing {@linkplain Properties properties}. These should be
	 *        copied into the resultant {@linkplain ResourceBundle properties
	 *        resource bundle}.
	 * @param writer
	 *        The {@linkplain PrintWriter output stream}.
	 */
	protected abstract void generateProperties (
		final @NotNull Properties properties,
		final @NotNull PrintWriter writer);

	/**
	 * Generate the target {@linkplain Properties properties} file.
	 *
	 * @throws IOException
	 *         If an exceptional situation arises while reading properties.
	 */
	public void generate () throws IOException
	{
		final File fileName = new File(String.format(
			"src/%s_%s.properties",
			baseName.replace('.', '/'),
			locale.getLanguage()));
		assert fileName.getPath().endsWith(".properties");
		final Properties properties = new Properties();
		try
		{
			properties.load(new InputStreamReader(
				new FileInputStream(fileName), "UTF-8"));
		}
		catch (final FileNotFoundException e)
		{
			// Ignore. It's okay if the file doesn't already exist.
		}
		final PrintWriter writer = new PrintWriter(fileName, "UTF-8");
		generatePreamble(writer);
		generateProperties(properties, writer);
		writer.close();
	}

	/**
	 * Construct a new {@link PropertiesFileGenerator}.
	 *
	 * @param baseName
	 *        The base name of the {@linkplain ResourceBundle resource bundle}.
	 * @param locale
	 *        The target {@linkplain Locale locale}.
	 */
	protected PropertiesFileGenerator (
		final @NotNull String baseName,
		final @NotNull Locale locale)
	{
		this.baseName = baseName;
		this.locale = locale;
		this.preambleBundle = ResourceBundle.getBundle(
			preambleBaseName,
			locale,
			Resources.class.getClassLoader(),
			new UTF8ResourceBundleControl());
	}
}