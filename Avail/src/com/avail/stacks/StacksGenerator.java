/**
 * StacksGenerator.java
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

package com.avail.stacks;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import com.avail.AvailRuntime;
import com.avail.builder.ModuleName;
import com.avail.builder.ModuleNameResolver;
import com.avail.compiler.AbstractAvailCompiler.ModuleHeader;
import com.avail.compiler.AbstractAvailCompiler.ModuleImport;
import com.avail.descriptor.A_Tuple;
import com.avail.descriptor.CommentTokenDescriptor;
import com.avail.descriptor.ModuleDescriptor;
import com.avail.descriptor.TupleDescriptor;
import com.avail.utility.IO;

/**
 * An Avail documentation generator.  It takes tokenized method/class comments
 * in .avail files and creates navigable documentation from them.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
public class StacksGenerator
{
	/**
	 * The default path to the output directory for module-oriented
	 * documentation and data files.
	 */
	public static final Path defaultDocumentationPath = Paths.get("stacks");

	/**
	 * The path for documentation storage as provided by the user.
	 */
	Path providedDocumentPath;

	/**
	 * The original incoming base path.
	 */
	final Path outputPath;

	/**
	 *  The location useds for storing any log files such as error-logs.
	 */
	public final Path logPath;

	/**
	 * A {@linkplain ModuleNameResolver} to resolve {@linkplain
	 * 			ModuleImport}
	 */
	final ModuleNameResolver resolver;

	/**
	 * The error log file for the malformed comments.
	 */
	StacksErrorLog errorLog;

	/**
	 * The {@linkplain HTMLFileMap} is a map for all html files in
	 * stacks
	 */
	private final HTMLFileMap htmlFileMap;

	/**
	 * A map of {@linkplain ModuleName module names} to a list of all the method
	 * names exported from said module
	 */
	HashMap<String,StacksCommentsModule> moduleToComments;

	/**
	 * Construct a new {@link StacksGenerator}.
	 *
	 * @param outputPath
	 *        The {@linkplain Path path} to the output {@linkplain
	 *        BasicFileAttributes#isDirectory() directory} for documentation and
	 *        data files.
	 * @param resolver
	 * 			A {@linkplain ModuleNameResolver} to resolve {@linkplain
	 * 			ModuleImport}
	 * @throws IllegalArgumentException
	 *         If the output path exists but does not specify a directory.
	 */
	public StacksGenerator(final Path outputPath,
		final ModuleNameResolver resolver)
		throws IllegalArgumentException
	{
		if (Files.exists(outputPath) && !Files.isDirectory(outputPath))
		{
			throw new IllegalArgumentException(
				outputPath + " exists and is not a directory");
		}
		this.outputPath = outputPath;
		this.htmlFileMap = new HTMLFileMap();
		this.resolver = resolver;

		this.logPath = outputPath
			.resolve("logs");
		this.errorLog = new StacksErrorLog(logPath);

		this.providedDocumentPath = outputPath
			.resolve("library-documentation");


		this.moduleToComments =
			new HashMap<String,StacksCommentsModule>(50);
	}

	/**
	 * Inform the {@linkplain StacksGenerator generator} about the documentation
	 * and linkage of a {@linkplain ModuleDescriptor module}.
	 *
	 * @param header
	 *        The {@linkplain ModuleHeader header} of the module.
	 * @param commentTokens
	 *        The complete {@linkplain TupleDescriptor collection} of
	 *        {@linkplain CommentTokenDescriptor comments} produced for the
	 *        given module.
	 */
	public synchronized void add (
		final ModuleHeader header,
		final A_Tuple commentTokens)
	{
		System.out.println("Starting scanning of comments in "
			+ header.moduleName.qualifiedName());

		StacksCommentsModule commentsModule = null;

		commentsModule = new StacksCommentsModule(
			header,commentTokens,errorLog, resolver,
			moduleToComments,htmlFileMap);
		updateModuleToComments(commentsModule);
	}

	/**
	 * Update moduleToComments with a new {@linkplain StacksCommentsModule}.
	 * @param commentModule
	 * 		A new {@linkplain StacksCommentsModule} to add to moduleToComments;
	 */
	private void updateModuleToComments (
		final StacksCommentsModule commentModule)
	{

		moduleToComments.put(commentModule.moduleName(), commentModule);
	}

	/**
	 * Generate complete Stacks documentation.
	 *
	 * @param runtime
	 *        An {@linkplain AvailRuntime runtime}.
	 * @param outermostModule
	 *        The outermost {@linkplain ModuleDescriptor module} for the
	 *        generation request.
	 */
	public synchronized void generate (
		final AvailRuntime runtime,
		final ModuleName outermostModule)
	{
		System.out.println("Generating Documentation…");

		final ByteBuffer closeHTML = ByteBuffer.wrap(String.format(
			"</ol>\n<h4>Error Count: %d</h4>\n</body>\n</html>"
				,errorLog.errorCount())
			.getBytes(StandardCharsets.UTF_8));

		errorLog.addLogEntry(closeHTML,0);

		final StacksCommentsModule outerMost = moduleToComments
			.get(outermostModule.qualifiedName());

		IO.close(errorLog.file());

		final int fileToOutPutCount =
			outerMost.calculateFinalImplementationGroupsMap(htmlFileMap);

		createJSFiles();

		//Create the main HTML landing page
		createIndexHTML();

		if (fileToOutPutCount > 0)
		{
			final StacksSynchronizer synchronizer =
				new StacksSynchronizer(fileToOutPutCount);

			moduleToComments
				.get(outermostModule.qualifiedName())
					.writeMethodsToHTMLFiles(providedDocumentPath,synchronizer,
						runtime,htmlFileMap);

			synchronizer.waitForWorkUnitsToComplete();
		}

		clear();

	}

	/**
	 * Clear all internal data structures and reinitialize the {@linkplain
	 * StacksGenerator generator} for subsequent usage.
	 */
	public synchronized void clear ()
	{
		moduleToComments.clear();
		htmlFileMap.clear();
	}

	/**
	 * Create all necessary JS files.
	 */
	private void createJSFiles()
	{
		final Path categoriesFilePath =
			outputPath.resolve("stacksApp.js");

		try
		{
			Files.createDirectories(outputPath);
		}
		catch (final IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try
		{
			final FileChannel categoriesJson =
				FileChannel.open(categoriesFilePath,
				EnumSet.of(StandardOpenOption.CREATE,
					StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING));
			final ByteBuffer buffer = ByteBuffer.wrap(
				(htmlFileMap.toStacksAppJS().getBytes(StandardCharsets.UTF_8)));
			categoriesJson.write(buffer);
			IO.close(categoriesJson);
		}
		catch (final IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Create the main HTML landing page for Stacks.
	 */
	private void createIndexHTML()
	{
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder
			.append("<!doctype html>\n")
		.append("<!--[if lt IE 7]> <html class=\"ie6 oldie\"> <![endif]-->\n")
		.append("<!--[if IE 7]>    <html class=\"ie7 oldie\"> <![endif]-->\n")
		.append("<!--[if IE 8]>    <html class=\"ie8 oldie\"> <![endif]-->\n")
		.append("<!--[if gt IE 8]><!-->\n")
		.append("<html>\n")
		.append("<!--<![endif]-->\n")
		.append("\t<head>\n")
		.append("\t\t<!--#include virtual=\"/_include/head.ssi\" -->\n")
		.append("\t\t<meta charset=\"utf-8\">\n")
		.append("\t\t<title>Library Documentation</title>\n")
		.append("\t\t<link href=\"/_css/stacks.css\" "
		 	+ "rel=\"stylesheet\" type=\"text/css\">\n")
		.append("\t\t<script type=\"text/javascript\" "
			+ "src=\"/_javascript/angular.min.js\"></script>\n")
		.append("\t\t<script type=\"text/javascript\" "
			+ "src=\"stacksApp.js\"></script>\n")
		.append("\t</head>\n")
		.append("\t<body class=\"gradient-logo\">\n")
		.append("\t\t<!--#include virtual=\"/_include/body-top.ssi\" -->\n")
		.append("\t\t<h2 class=\"content-title\">"
			+"The Avail Standard Library</h2>\n")
		.append("\t\t<div ng-app=\"stacksApp\">\n")
		.append("\t\t\t<div ng-controller=\"CategoriesCntrl\">\n")
		.append("\t\t\t\t<input type=\"text\" ng-model=\"search.category\">\n")
		.append("\t\t\t\t<ol>\n")
		.append("\t\t\t\t\t<li ng-repeat=\"stacksCategory in categories.content"
			+ " | orderBy: 'category' | filter : search \">\n")
		.append("\t\t\t\t\t\t<a ng-click=\"collapsed=!collapsed\">"
			+ "{{stacksCategory.category}}</a>\n")
		.append("\t\t\t\t\t\t<div ng-show=\"collapsed\">\n")
		.append("\t\t\t\t\t\t\t<ul>\n")
		.append("\t\t\t\t\t\t\t\t<li ng-repeat=\"method in "
			+ "stacksCategory.methods | orderBy: 'methodName' \">\n")
		.append("\t\t\t\t\t\t\t\t\t<a href=\"{{'library-documentation' "
			+ "+ method.link}}\">{{method.methodName}}</a>\n")
		.append("\t\t\t\t\t\t\t\t</li>\n")
		.append("\t\t\t\t\t\t\t</ul>\n")
		.append("\t\t\t\t\t\t</div>\n")
		.append("\t\t\t\t\t</li>\n")
		.append("\t\t\t\t</ol>\n")
		.append("\t\t\t</div>\n")
		.append("\t\t</div>\n")
		.append("\t\t<!--#include virtual=\"/_include/body-bottom.ssi\" -->\n")
		.append("\t</body>\n")
		.append("</html>");

		final Path indexFilePath =
			outputPath.resolve("index.html");

		try
		{
			final FileChannel indexHTML =
				FileChannel.open(indexFilePath,
				EnumSet.of(StandardOpenOption.CREATE,
					StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING));
			final ByteBuffer buffer = ByteBuffer.wrap(
				(stringBuilder.toString().getBytes(StandardCharsets.UTF_8)));
			indexHTML.write(buffer);
			IO.close(indexHTML);
		}
		catch (final IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
