/**
 * CommandLineConfigurator.java
 * Copyright © 1993-2016, The Avail Foundation, LLC.
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

package com.avail.server.configuration;

import static com.avail.server.configuration.CommandLineConfigurator.OptionKey.*;
import static java.util.Arrays.asList;
import java.io.File;
import com.avail.annotations.InnerAccess;
import com.avail.annotations.Nullable;
import com.avail.builder.ModuleRoots;
import com.avail.builder.RenamesFileParser;
import com.avail.server.AvailServer;
import com.avail.tools.options.GenericHelpOption;
import com.avail.tools.options.GenericOption;
import com.avail.tools.options.OptionProcessingException;
import com.avail.tools.options.OptionProcessor;
import com.avail.tools.options.OptionProcessorFactory;
import com.avail.utility.MutableOrNull;
import com.avail.utility.configuration.ConfigurationException;
import com.avail.utility.configuration.Configurator;
import com.avail.utility.evaluation.Continuation2;

/**
 * Provides the {@linkplain AvailServerConfiguration configuration} for the
 * {@linkplain AvailServer Avail server}. Specifies the options that are
 * available as arguments to the server.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public final class CommandLineConfigurator
implements Configurator<AvailServerConfiguration>
{
	/**
	 * {@code OptionKey} enumerates the valid configuration options.
	 */
	static enum OptionKey
	{
		/**
		 * Specification of the {@linkplain File path} to the {@linkplain
		 * RenamesFileParser renames file}.
		 */
		AVAIL_RENAMES,

		/**
		 * Specification of the {@linkplain ModuleRoots Avail roots}.
		 */
		AVAIL_ROOTS,

		/**
		 * Specification of the server authority.
		 */
		SERVER_AUTHORITY,

		/**
		 * Specification of the server port.
		 */
		SERVER_PORT,

		/**
		 * Specification of the web document root.
		 */
		DOCUMENT_ROOT,

		/**
		 * Request display of help text.
		 */
		HELP
	}

	/**
	 * Create an {@linkplain OptionProcessor option processor} suitable for
	 * {@linkplain #updateConfiguration() updating} a {@linkplain
	 * AvailServerConfiguration server configuration}.
	 *
	 * @return An option processor.
	 */
	private OptionProcessor<OptionKey> createOptionProcessor ()
	{
		final MutableOrNull<OptionProcessor<OptionKey>> processor =
			new MutableOrNull<>();
		final OptionProcessorFactory<OptionKey> factory =
			new OptionProcessorFactory<>(OptionKey.class);
		factory.addOption(new GenericOption<OptionKey>(
			AVAIL_RENAMES,
			asList("availRenames"),
			"The path to the renames file. This option overrides environment "
			+ "variables.",
			new Continuation2<String, String>()
			{
				@Override
				public void value (
					final @Nullable String keyword,
					final @Nullable String renamesString)
				{
					assert renamesString != null;
					processor.value().checkEncountered(AVAIL_RENAMES, 0);
					configuration.setRenamesFilePath(renamesString);
				}
			}));
		factory.addOption(new GenericOption<OptionKey>(
			AVAIL_ROOTS,
			asList("availRoots"),
			"The Avail roots, as a semicolon (;) separated list of module root "
			+ "specifications. Each module root specification comprises a "
			+ "logical root name, then an equals (=), then a module root "
			+ "location. A module root location comprises the absolute path to "
			+ "a binary module repository, then optionally a comma (,) and the "
			+ "absolute path to a source package. This option overrides " +
			"environment variables.",
			new Continuation2<String, String>()
			{
				@Override
				public void value (
					final @Nullable String keyword,
					final @Nullable String rootsString)
				{
					assert rootsString != null;
					processor.value().checkEncountered(AVAIL_ROOTS, 0);
					configuration.setAvailRootsPath(rootsString);
				}
			}));
		factory.addOption(new GenericOption<OptionKey>(
			SERVER_AUTHORITY,
			asList("serverAuthority"),
			"The server authority, i.e., the name of the Avail server. If not "
			+ "specified, then the server authority defaults to \"localhost\".",
			new Continuation2<String, String>()
			{
				@Override
				public void value (
					final @Nullable String keyword,
					final @Nullable String nameString)
				{
					assert nameString != null;
					processor.value().checkEncountered(SERVER_AUTHORITY, 0);
					configuration.setServerAuthority(nameString);
				}
			}));
		factory.addOption(new GenericOption<OptionKey>(
			SERVER_PORT,
			asList("serverPort"),
			"The server port. If not specified, then the server port defaults "
			+ "to 40000.",
			new Continuation2<String, String>()
			{
				@Override
				public void value (
					final @Nullable String keyword,
					final @Nullable String portString)
				{
					assert portString != null;
					processor.value().checkEncountered(SERVER_PORT, 0);
					final int port;
					try
					{
						port = Integer.parseInt(portString);
					}
					catch (final NumberFormatException e)
					{
						throw new OptionProcessingException(
							"expected an integer \"p\" where 0 ≤ p < 65535",
							e);
					}
					configuration.setServerPort(port);
				}
			}));
		factory.addOption(new GenericOption<OptionKey>(
			DOCUMENT_ROOT,
			asList("documentRoot"),
			"The document root, as a path to a directory. The document root "
			+ "contains static files that should be served by the Avail "
			+ "server. These files are available through GET requests under "
			+ "the URI /doc. If not specified, then the Avail server will "
			+ "reject all such requests.",
			new Continuation2<String, String>()
			{
				@Override
				public void value (
					final @Nullable String keyword,
					final @Nullable String pathString)
				{
					assert pathString != null;
					processor.value().checkEncountered(DOCUMENT_ROOT, 0);
					configuration.setDocumentPath(pathString);
				}
			}));
		factory.addOption(new GenericHelpOption<OptionKey>(
			HELP,
			processor,
			"The Avail server understands the following options: ",
			helpStream));
		processor.value = factory.createOptionProcessor();
		return processor.value();
	}

	/** The {@linkplain AvailServerConfiguration configuration}. */
	@InnerAccess final AvailServerConfiguration configuration;

	/** The command line arguments. */
	private final String[] commandLineArguments;

	/**
	 * The {@linkplain Appendable appendable} to which help text should be
	 * written.
	 */
	private final Appendable helpStream;

	@Override
	public AvailServerConfiguration configuration ()
	{
		return configuration;
	}

	/**
	 * Has the {@linkplain CommandLineConfigurator configurator} been run yet?
	 */
	private boolean isConfigured;

	@Override
	public synchronized void updateConfiguration ()
		throws ConfigurationException
	{
		if (!isConfigured)
		{
			final OptionProcessor<OptionKey> optionProcessor;
			try
			{
				optionProcessor = createOptionProcessor();
				optionProcessor.processOptions(commandLineArguments);
				isConfigured = true;
			}
			catch (final Exception e)
			{
				throw new ConfigurationException(
					"unexpected configuration error", e);
			}
		}
	}

	/**
	 * Construct a new {@link CommandLineConfigurator}.
	 *
	 * @param configuration
	 *        The base {@linkplain AvailServerConfiguration server
	 *        configuration}.
	 * @param commandLineArguments
	 *        The command-line arguments.
	 * @param helpStream
	 *        The {@link Appendable} to which help text should be written.
	 */
	public CommandLineConfigurator (
		final AvailServerConfiguration configuration,
		final String[] commandLineArguments,
		final Appendable helpStream)
	{
		this.configuration = configuration;
		this.commandLineArguments = commandLineArguments;
		this.helpStream = helpStream;
	}
}