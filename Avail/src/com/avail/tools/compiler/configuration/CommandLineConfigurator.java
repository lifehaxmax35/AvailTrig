/**
 * CommandLineConfigurator.java
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

package com.avail.tools.compiler.configuration;

import static java.util.Arrays.asList;
import static com.avail.tools.compiler.configuration.CommandLineConfigurator.OptionKey.*;
import java.io.File;
import com.avail.annotations.InnerAccess;
import com.avail.annotations.Nullable;
import com.avail.builder.ModuleName;
import com.avail.builder.ModuleRoots;
import com.avail.builder.RenamesFileParser;
import com.avail.tools.configuration.ConfigurationException;
import com.avail.tools.configuration.Configurator;
import com.avail.tools.options.DefaultOption;
import com.avail.tools.options.GenericHelpOption;
import com.avail.tools.options.GenericOption;
import com.avail.tools.options.OptionProcessingException;
import com.avail.tools.options.OptionProcessor;
import com.avail.tools.options.OptionProcessorFactory;
import com.avail.utility.Continuation2;
import com.avail.utility.MutableOrNull;

/**
 * TODO: [LAS] Document CommandLineConfigurator!
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public class CommandLineConfigurator
implements Configurator<CompilerConfiguration>
{
	/**
	 * {@code OptionKey} enumerates the valid configuration options.
	 */
	static enum OptionKey
	{
		/**
		 * Specification of the {@linkplain ModuleRoots Avail roots}.
		 */
		AVAIL_ROOTS,

		/**
		 * Specification of the {@linkplain File path} to the {@linkplain
		 * RenamesFileParser renames file}.
		 */
		AVAIL_RENAMES,

		/**
		 * Specification of the target {@linkplain ModuleName module name}.
		 */
		TARGET_MODULE_NAME,

		/**
		 * Request display of help text.
		 */
		HELP
	}

	/**
	 * Create an {@linkplain OptionProcessor option processor} suitable for
	 * {@linkplain #updateConfiguration() updating} a {@linkplain
	 * CompilerConfiguration compiler configuration}.
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
			AVAIL_ROOTS,
			asList("availRoots"),
			"The Avail roots, as a semicolon (;) separated list of module root "
			+ "specifications. Each module root specification comprises a "
			+ "logical root name, then an equals (=), then a module root "
			+ "location. A module root location comprises the absolute path to "
			+ "a binary module repository, then optionally a comma (,) and the "
			+ "absolute path to a source package.",
			new Continuation2<String, String>()
			{
				@Override
				public void value (
					final @Nullable String keyword,
					final @Nullable String rootsString)
				{
					assert rootsString != null;
					processor.value().checkEncountered(AVAIL_ROOTS, 0);
					try
					{
						configuration.setAvailRootsPath(rootsString);
					}
					catch (final OptionProcessingException e)
					{
						throw new OptionProcessingException(
							keyword + ": " + e.getMessage(),
							e);
					}
				}
			}));
/////////////////////////////////////////////////////////////////// LS NEW start
		factory.addOption(new GenericOption<OptionKey>(
			AVAIL_RENAMES,
			asList("availRenames"),
			"The absolute path to the renames file.",
			new Continuation2<String, String>()
			{
				@Override
				public void value (
					final @Nullable String keyword,
					final @Nullable String renamesString)
				{
					assert renamesString != null;
					processor.value().checkEncountered(AVAIL_RENAMES, 0);
					try
					{
						configuration.setRenamesFilePath(renamesString);
					}
					catch (final OptionProcessingException e)
					{
						throw new OptionProcessingException(
							keyword + ": " + e.getMessage(),
							e);
					}
				}
			}));
		factory.addOption(new DefaultOption<OptionKey>(
			TARGET_MODULE_NAME,
			"The target module name for compilation.",
			new Continuation2<String, String>()
			{
				@Override
				public void value (
					final @Nullable String keyword,
					final @Nullable String targetModuleString)
				{
					assert targetModuleString != null;
					processor.value().checkEncountered(TARGET_MODULE_NAME, 0);
					try
					{
						configuration.setTargetModuleName(
							new ModuleName(targetModuleString));
					}
					catch (final OptionProcessingException e)
					{
						throw new OptionProcessingException(
							"«default»: " + e.getMessage(),
							e);
					}
				}
			}));
/////////////////////////////////////////////////////////////////// LS NEW end
		factory.addOption(new GenericHelpOption<OptionKey>(
			HELP,
			processor,
			"The Avail compiler understands the following options: ",
			helpStream));
		processor.value = factory.createOptionProcessor();
		return processor.value();
	}

	/** The {@linkplain CompilerConfiguration configuration}. */
	@InnerAccess final CompilerConfiguration configuration;

	/** The command line arguments. */
	private final String[] commandLineArguments;

	/**
	 * The {@linkplain Appendable appendable} to which help text should be
	 * written.
	 */
	private final Appendable helpStream;

	@Override
	public CompilerConfiguration configuration ()
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
	 *        The base {@linkplain CompilerConfiguration compiler
	 *        configuration}.
	 * @param commandLineArguments
	 *        The command-line arguments.
	 * @param helpStream
	 *        The {@link Appendable} to which help text should be written.
	 */
	public CommandLineConfigurator (
		final CompilerConfiguration configuration,
		final String[] commandLineArguments,
		final Appendable helpStream)
	{
		this.configuration = configuration;
		this.commandLineArguments = commandLineArguments;
		this.helpStream = helpStream;
	}
}
