/**
 * AvailWorkbench.java
 * Copyright © 1993-2017, The Avail Foundation, LLC.
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

package com.avail.environment;

import com.avail.AvailRuntime;
import com.avail.annotations.InnerAccess;
import com.avail.builder.AvailBuilder;
import com.avail.builder.ModuleName;
import com.avail.builder.ModuleNameResolver;
import com.avail.builder.ModuleRoot;
import com.avail.builder.ModuleRoots;
import com.avail.builder.RenamesFileParser;
import com.avail.builder.ResolvedModuleName;
import com.avail.builder.UnresolvedDependencyException;
import com.avail.descriptor.A_Module;
import com.avail.descriptor.ModuleDescriptor;
import com.avail.environment.actions.*;
import com.avail.environment.editor.ModuleEditor;
import com.avail.environment.editor.ReplaceTextTemplate;
import com.avail.environment.nodes.AbstractBuilderFrameTreeNode;
import com.avail.environment.nodes.EntryPointModuleNode;
import com.avail.environment.nodes.EntryPointNode;
import com.avail.environment.nodes.ModuleOrPackageNode;
import com.avail.environment.nodes.ModuleRootNode;
import com.avail.environment.tasks.BuildTask;
import com.avail.io.ConsoleInputChannel;
import com.avail.io.ConsoleOutputChannel;
import com.avail.io.TextInterface;
import com.avail.stacks.StacksGenerator;
import com.avail.utility.Mutable;
import com.avail.utility.Pair;
import com.sun.javafx.application.PlatformImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static com.avail.environment.AvailWorkbench.StreamStyle.*;
import static com.avail.utility.Nulls.stripNull;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * {@code AvailWorkbench} is a simple user interface for the {@linkplain
 * AvailBuilder Avail builder}.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@SuppressWarnings("serial")
public final class AvailWorkbench
extends JFrame
{
	/**
	 * The prefix string for resources related to the workbench.
	 */
	public static final @Nonnull String resourcePrefix = "/resources/workbench/";

	/** Determine at startup whether we're on a Mac. */
	public static final boolean runningOnMac =
		System.getProperty("os.name").toLowerCase().matches("mac os x.*");

	/** Determine at startup whether we should show developer commands. */
	public static final boolean showDeveloperTools =
		"true".equalsIgnoreCase(System.getProperty("availDeveloper"));

	/**
	 * The numeric mask for the modifier key suitable for the current platform.
	 */
	public static final int menuShortcutMask =
		Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	/**
	 * The current working directory of the Avail virtual machine. Because Java
	 * does not permit the current working directory to be changed, it is safe
	 * to cache the answer at class-loading time.
	 */
	public static final @Nonnull File currentWorkingDirectory;

	// Obtain the current working directory. Try to resolve this location to its
	// real path. If resolution fails, then just use the value of the "user.dir"
	// system property.
	static
	{
		final String userDir = System.getProperty("user.dir");
		final FileSystem fileSystem = FileSystems.getDefault();
		final Path path = fileSystem.getPath(userDir);
		String realPathString;
		try
		{
			realPathString = path.toRealPath().toString();
		}
		catch (final IOException|SecurityException e)
		{
			realPathString = userDir;
		}
		currentWorkingDirectory = new File(realPathString);
	}

	/**
	 * {@code AbstractWorkbenchTask} is a foundation for long running {@link
	 * AvailBuilder} operations.
	 */
	public abstract static class AbstractWorkbenchTask
	extends SwingWorker<Void, Void>
	{
		/** The owning {@link AvailWorkbench}. */
		public final AvailWorkbench workbench;

		/**
		 * The resolved name of the target {@linkplain ModuleDescriptor module}.
		 */
		protected final @Nullable ResolvedModuleName targetModuleName;

		/**
		 * Construct a new {@link AbstractWorkbenchTask}.
		 *
		 * @param workbench
		 *        The owning {@link AvailWorkbench}.
		 * @param targetModuleName
		 *        The resolved name of the target {@linkplain ModuleDescriptor
		 *        module}.
		 */
		public AbstractWorkbenchTask (
			final AvailWorkbench workbench,
			final @Nullable ResolvedModuleName targetModuleName)
		{
			this.workbench = workbench;
			this.targetModuleName = targetModuleName;
		}

		/**
		 * Cancel the current {@linkplain AbstractWorkbenchTask task}.
		 */
		public final void cancel ()
		{
			workbench.availBuilder.cancel();
		}

		/** The start time. */
		private long startTimeMillis;

		/** The stop time. */
		private long stopTimeMillis;

		/** The {@linkplain Throwable exception} that terminated the build. */
		protected @Nullable Throwable terminator;

		/**
		 * Ensure the target module name is not null, then answer it.
		 *
		 * @return The non-null target module name.
		 */
		protected final ResolvedModuleName targetModuleName ()
		{
			return stripNull(targetModuleName);
		}

		/**
		 * Report completion (and timing) to the {@linkplain #transcript
		 * transcript}.
		 */
		protected void reportDone ()
		{
			final long durationMillis = stopTimeMillis - startTimeMillis;
			final String status;
			final Throwable t = terminator;
			if (t != null)
			{
				status = "Aborted ("
					+ t.getClass().getSimpleName()
					+ ")";
			}
			else if (workbench.availBuilder.shouldStopBuild())
			{
				status = workbench.availBuilder.stopBuildReason();
			}
			else
			{
				status = "Done";
			}
			workbench.writeText(
				String.format(
					"%s (%d.%03ds).%n",
					status,
					durationMillis / 1000,
					durationMillis % 1000),
				INFO);
		}

		@Override
		protected final @Nullable Void doInBackground () throws Exception
		{
			startTimeMillis = System.currentTimeMillis();
			try
			{
				// Reopen the repositories if necessary.
				for (final ModuleRoot root :
					workbench.resolver.moduleRoots().roots())
				{
					root.repository().reopenIfNecessary();
				}
				executeTask();
				return null;
			}
			finally
			{
				// Close all the repositories.
				for (final ModuleRoot root :
					workbench.resolver.moduleRoots().roots())
				{
					root.repository().close();
				}
				stopTimeMillis = System.currentTimeMillis();
			}
		}

		/**
		 * Execute this {@link AbstractWorkbenchTask}.
		 *
		 * @throws Exception
		 *         If anything goes wrong.
		 */
		protected abstract void executeTask () throws Exception;
	}


	/**
	 * Whether there is a task queued or running that will check again for
	 * new changes before giving up control.
	 */
	@InnerAccess boolean updatingTranscript = false;

	/**
	 * A {@link List} of {@link Pair}s, where the first part of each pair is a
	 * boolean indicating if the entry is for the error stream (true) or the
	 * regular output stream (false), and the second part of the pair is a
	 * {@link StringBuilder} that accumulates new {@link String} data to be
	 * written with the same style.
	 *
	 * Additionally, this queue is the monitor on which to synchronize writing
	 * to either output stream, and to transfer from the queue to the output
	 * transcript (a {@link StyledDocument}).
	 */
	@InnerAccess
	final Deque<Pair<StreamStyle, StringBuilder>> updateQueue =
		new ArrayDeque<>();

	/**
	 * The {@link StyledDocument} into which to write both error and regular
	 * output.  Lazily initialized.
	 */
	private @Nullable StyledDocument document = null;

	/**
	 * Answer the {@link StyledDocument} into which to write error and regular
	 * output.
	 *
	 * @return The document, retrieving it from the {@link #transcript} if
	 *         necessary.
	 */
	@InnerAccess StyledDocument document ()
	{
		StyledDocument d = document;
		if (d == null)
		{
			d = transcript.getStyledDocument();
			document = d;
		}
		return d;
	}

	/** Truncate the start of the document any time it exceeds this. */
	final int maxDocumentSize = 1_000_000;

	/**
	 * Update the {@linkplain #transcript} by appending the (non-empty) queued
	 * text to it.  Only output what was already queued by the time the UI
	 * runnable starts; if additional output is detected afterward, another UI
	 * runnable will be queued to deal with the residue (iteratively).  This
	 * maximizes efficiency while avoiding starvation of the UI process in the
	 * event that a high volume of data is being written.
	 */
	@InnerAccess void updateTranscript ()
	{
		assert Thread.holdsLock(updateQueue);
		assert updatingTranscript;
		assert !updateQueue.isEmpty();
		invokeLater(() ->
		{
			final List<Pair<StreamStyle, StringBuilder>> allPairs;
			synchronized (updateQueue)
			{
				allPairs = new ArrayList<>(updateQueue);
				updateQueue.clear();
			}
			assert !allPairs.isEmpty();
			final StyledDocument doc = document();
			for (final Pair<StreamStyle, StringBuilder> pair : allPairs)
			{
				try
				{
					final int statusSize = perModuleStatusTextSize;
					final int length = doc.getLength();
					final String newString = pair.second().toString();
					final int newStringLength = newString.length();
					final int newLength = length + newStringLength;
					final int amountToRemove = newLength - maxDocumentSize;
					if (amountToRemove > 0)
					{
						// Remove part of the document just after the status
						// area.  It may still be the case that the string being
						// appended is still very large, but we'll just allow
						// it to be big in that case.
						final int endOfRemoval = min(
							statusSize + amountToRemove, length);
						if (endOfRemoval > statusSize)
						{
							doc.remove(statusSize, endOfRemoval - statusSize);
						}
					}
					doc.insertString(
						doc.getLength(),  // The new length
						newString,
						pair.first().styleIn(doc));
				}
				catch (final BadLocationException e)
				{
					// Just ignore the failed write.
				}
			}
			synchronized (updateQueue)
			{
				if (updateQueue.isEmpty())
				{
					updatingTranscript = false;
				}
				else
				{
					// Queue another task to update the transcript, to
					// avoid starving other UI interactions.
					updateTranscript();
				}
			}
		});
	}

	/** An abstraction of the styles of streams used by the workbench. */
	public enum StreamStyle
	{
		/** The stream style used to echo user input. */
		IN_ECHO("input", new Color(32, 144, 32)),

		/** The stream style used to display normal output. */
		OUT("output", Color.BLACK),

		/** The stream style used to display error output. */
		ERR("error", Color.RED),

		/** The stream style used to display informational text. */
		INFO("info", Color.BLUE),

		/** The stream style used to echo commands. */
		COMMAND("command", Color.MAGENTA),

		/** Progress updates produced by a build. */
		BUILD_PROGRESS("build", new Color(128, 96, 0));

		/** The name of this style. */
		final String styleName;

		/** The foreground color for this style. */
		final Color foregroundColor;

		/**
		 * Construct a new {@link StreamStyle}.
		 *
		 * @param styleName The name of this style.
		 * @param foregroundColor The color of foreground text in this style.
		 */
		StreamStyle (final String styleName, final Color foregroundColor)
		{
			this.styleName = styleName;
			this.foregroundColor = foregroundColor;
		}

		/**
		 * Create my corresponding {@link Style} in the {@link StyledDocument}.
		 *
		 * @param doc The document in which to define this style.
		 */
		void defineStyleIn (final StyledDocument doc)
		{
			final Style defaultStyle =
				StyleContext.getDefaultStyleContext().getStyle(
					StyleContext.DEFAULT_STYLE);
			final Style style = doc.addStyle(styleName, defaultStyle);
			StyleConstants.setForeground(style, foregroundColor);
		}

		/**
		 * Extract this style from the given {@link StyledDocument document}.
		 * Look up my {@link #styleName}.
		 *
		 * @param doc The document.
		 * @return The {@link Style}.
		 */
		public Style styleIn (final StyledDocument doc)
		{
			return doc.getStyle(styleName);
		}
	}

	/**
	 * {@linkplain BuildOutputStream} intercepts writes and updates the UI's
	 * {@linkplain #transcript}.
	 */
	private final class BuildOutputStream
	extends ByteArrayOutputStream
	{
		/**
		 * What {@link StreamStyle style} to render this stream as.
		 */
		final StreamStyle streamStyle;

		/**
		 * Transfer any data in my buffer into the updateQueue, starting up a UI
		 * task to transfer them to the document as needed.
		 */
		private void queueForTranscript ()
		{
			assert Thread.holdsLock(this);
			final String text;
			try
			{
				text = toString(StandardCharsets.UTF_8.name());
			}
			catch (final UnsupportedEncodingException e)
			{
				assert false : "Somehow Java doesn't support characters";
				throw new RuntimeException(e);
			}
			if (text.isEmpty())
			{
				// Nothing new to display.
				return;
			}
			reset();
			writeText(text, streamStyle);
		}

		@Override
		public synchronized void write (final int b)
		{
			super.write(b);
			queueForTranscript();
		}

		@Override
		public synchronized void write (final @Nullable byte[] b)
		throws IOException
		{
			assert b != null;
			super.write(b);
			queueForTranscript();
		}

		@Override
		public synchronized void write (
			final @Nullable byte[] b,
			final int off,
			final int len)
		{
			assert b != null;
			super.write(b, off, len);
			queueForTranscript();
		}

		/**
		 * Construct a new {@link BuildOutputStream}.
		 *
		 * @param streamStyle
		 *        What {@link StreamStyle} should this stream render with?
		 */
		public BuildOutputStream (final StreamStyle streamStyle)
		{
			super(1);
			this.streamStyle = streamStyle;
		}
	}

	/**
	 * {@linkplain BuildInputStream} satisfies reads from the UI's {@linkplain
	 * #inputField input field}. It blocks reads unless some data is available.
	 */
	public final class BuildInputStream
	extends ByteArrayInputStream
	{
		/**
		 * The {@linkplain StyledDocument styled document} underlying the
		 * {@linkplain #transcript}.
		 */
		final StyledDocument doc;

		/**
		 * Clear the {@linkplain BuildInputStream input stream}. All pending
		 * data is discarded and the stream position is reset to zero
		 * ({@code 0}).
		 */
		public synchronized void clear ()
		{
			count = 0;
			pos = 0;
		}

		/**
		 * Update the content of the {@linkplain BuildInputStream stream} with
		 * data from the {@linkplain #inputField input field}.
		 */
		public synchronized void update ()
		{
			final String text = inputField.getText() + "\n";
			final byte[] bytes = text.getBytes();
			if (pos + bytes.length >= buf.length)
			{
				final int newSize = max(
					buf.length << 1, bytes.length + buf.length);
				final byte[] newBuf = new byte[newSize];
				arraycopy(buf, 0, newBuf, 0, buf.length);
				buf = newBuf;
			}
			arraycopy(bytes, 0, buf, count, bytes.length);
			count += bytes.length;
			writeText(text, IN_ECHO);
			inputField.setText("");
			notifyAll();
		}

		/**
		 * The specified command string was just entered.  Present it in the
		 * {@link StreamStyle#COMMAND} style.  Force an extra leading new line
		 * to keep the text area from looking stupid.  Also end with a new line.
		 * The passed command should not itself have a new line included.
		 *
		 * @param commandText
		 *        The command that was entered, with no leading or trailing line
		 *        breaks.
		 */
		public synchronized void feedbackForCommand (
			final String commandText)
		{
			final String textToInsert = "\n" + commandText + "\n";
			writeText(textToInsert, COMMAND);
		}

		@Override
		public boolean markSupported ()
		{
			return false;
		}

		@Override
		public void mark (final int readAheadLimit)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized void reset ()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized int read ()
		{
			// Block until data is available.
			try
			{
				while (pos == count)
				{
					wait();
				}
			}
			catch (final InterruptedException e)
			{
				return -1;
			}
			return buf[pos++] & 0xFF;
		}

		@Override
		public synchronized int read (
			final @Nullable byte[] readBuffer,
			final int start,
			final int requestSize)
		{
			assert readBuffer != null;
			if (requestSize <= 0)
			{
				return 0;
			}
			// Block until data is available.
			try
			{
				while (pos == count)
				{
					wait();
				}
			}
			catch (final InterruptedException e)
			{
				return -1;
			}
			final int bytesToTransfer = min(requestSize, count - pos);
			arraycopy(buf, pos, readBuffer, start, bytesToTransfer);
			pos += bytesToTransfer;
			return bytesToTransfer;
		}

		/**
		 * Construct a new {@link BuildInputStream}.
		 */
		public BuildInputStream ()
		{
			super(new byte[1024], 0, 0);
			this.doc = transcript.getStyledDocument();
		}
	}

	/*
	 * Model components.
	 */

	/** The {@linkplain ModuleNameResolver module name resolver}. */
	public final ModuleNameResolver resolver;

	/** The current {@linkplain AbstractWorkbenchTask background task}. */
	public volatile @Nullable AbstractWorkbenchTask backgroundTask = null;

	/**
	 * The documentation {@linkplain Path path} for the {@linkplain
	 * StacksGenerator Stacks generator}.
	 */
	public @Nonnull Path documentationPath =
		StacksGenerator.defaultDocumentationPath;

	/**
	 * A {@link Map} from a module template name to a module template.
	 */
	public @Nonnull ModuleTemplates moduleTemplates;

	/**
	 * The {@link ReplaceTextTemplate} for the {@link ModuleEditor}.
	 */
	public final @Nonnull ReplaceTextTemplate replaceTextTemplate =
		new ReplaceTextTemplate();

	/**
	 * Answer an array of {@link #moduleTemplates} names.
	 *
	 * @return An array of strings.
	 */
	public String[] templateOptions ()
	{
		return moduleTemplates.moduleTemplates.keySet().
			toArray(new String[moduleTemplates.moduleTemplates.size()]);
	}

	/**
	 * Add a template to the {@linkplain #moduleTemplates}.
	 *
	 * @param templateName
	 *        The new template name.
	 * @param file
	 *        The template file.
	 */
	public void addModuleTemplate (
		final @Nonnull String templateName,
		final @Nonnull File file)
	{
		if (!file.exists())
		{
			final String message = file + " not found!";
				JOptionPane.showMessageDialog(this, message);
		}
		else
		{
			try
			{
				final StringBuilder sb = new StringBuilder();
				final List<String> lines =
					Files.lines(file.toPath()).collect(Collectors.toList());
				final int size = lines.size();
				for (int i = 0; i < size - 1; i++)
				{
					sb.append(lines.get(i));
					sb.append('\n');
				}
				sb.append(lines.get(size - 1));
				moduleTemplates.moduleTemplates.put(
					templateName,
					sb.toString());
			}
			catch (final IOException e)
			{
				System.err.println("Failed to read file");
			}
		}
	}

	/** The {@linkplain BuildInputStream standard input stream}. */
	private final @Nullable BuildInputStream inputStream;

	/**
	 * Answer the {@linkplain BuildInputStream standard input stream}.
	 *
	 * @return The input stream.
	 */
	public BuildInputStream inputStream ()
	{
		return stripNull(inputStream);
	}

	/** The {@linkplain PrintStream standard error stream}. */
	private final @Nullable PrintStream errorStream;

	/**
	 * Answer the {@linkplain PrintStream standard error stream}.
	 *
	 * @return The error stream.
	 */
	public PrintStream errorStream ()
	{
		return stripNull(errorStream);
	}

	/** The {@linkplain PrintStream standard output stream}. */
	private final @Nullable PrintStream outputStream;

	/**
	 * Answer the {@linkplain PrintStream standard output stream}.
	 *
	 * @return The output stream.
	 */
	public PrintStream outputStream ()
	{
		return stripNull(outputStream);
	}

	/* UI components. */

	/** The {@linkplain ModuleDescriptor module} {@linkplain JTree tree}. */
	public final JTree moduleTree;

	/**
	 * The {@linkplain JTree tree} of module {@linkplain A_Module#entryPoints()
	 * entry points}.
	 */
	public final JTree entryPointsTree;

	/**
	 * The {@link AvailBuilder} used by this user interface.
	 */
	public final AvailBuilder availBuilder;

	/**
	 * A {@link Map} from a {@link ResolvedModuleName} to the open {@link
	 * JFrame} displaying the corresponding source module.
	 */
	public final Map<ResolvedModuleName, JFrame> openedSourceModules =
		new HashMap<>();

	/**
	 * The {@linkplain JProgressBar progress bar} that displays the overall
	 * build progress.
	 */
	public final JProgressBar buildProgress;

	/**
	 * The {@linkplain JTextPane text area} that displays the {@linkplain
	 * AvailBuilder build} transcript.
	 */
	public final JTextPane transcript;

	/** The {@linkplain JScrollPane scroll bars} for the {@link #transcript}. */
	public final JScrollPane transcriptScrollArea;

	/**
	 * The {@linkplain JLabel label} that describes the current function of the
	 * {@linkplain #inputField input field}.
	 */
	public final JLabel inputLabel;

	/** The {@linkplain JTextField text field} that accepts standard input. */
	public final JTextField inputField;

	/**
	 * Keep track of recent commands in a history buffer.  Each submitted
	 * command is added to the end of the list.  Cursor-up retrieves the most
	 * recent selected line, and subsequent cursors-up retrieve previous lines,
	 * back to the first entry, then an empty command line, then the last entry
	 * again an so on.  An initial cursor-down selects the first entry and goes
	 * from there.
	 */
	public final List<String> commandHistory = new ArrayList<>();

	/**
	 * Which command was most recently retrieved by a cursor key since the last
	 * command was submitted.  -1 indicates no command has been retrieved by a
	 * cursor key, or that the entire list has been cycled an integral number of
	 * times (and the command line was blanked upon reaching -1).
	 */
	public int commandHistoryIndex = -1;

	/** Cycle one step backward in the command history. */
	final RetrievePreviousCommand retrievePreviousAction =
		new RetrievePreviousCommand(this);

	/** Cycle one step forward in the command history. */
	final RetrieveNextCommand retrieveNextAction =
		new RetrieveNextCommand(this);

	/* Actions. */

	/** The {@linkplain RefreshAction refresh action}. */
	@InnerAccess final RefreshAction refreshAction = new RefreshAction(this);

	/** The {@linkplain AboutAction "about Avail" action}. */
	@InnerAccess final AboutAction aboutAction = new AboutAction(this);

	/** The {@linkplain PreferencesAction "Preferences..." action}. */
	@InnerAccess final PreferencesAction preferencesAction =
		new PreferencesAction(this);

	/** The {@linkplain BuildAction build action}. */
	@InnerAccess final BuildAction buildAction = new BuildAction(this, false);

	/** The {@linkplain UnloadAction unload action}. */
	@InnerAccess final UnloadAction unloadAction = new UnloadAction(this);

	/** The {@linkplain UnloadAllAction unload-all action}. */
	@InnerAccess final UnloadAllAction unloadAllAction =
		new UnloadAllAction(this);

	/** The {@linkplain CancelAction cancel action}. */
	@InnerAccess final CancelAction cancelAction = new CancelAction(this);

	/** The {@linkplain CleanAction clean action}. */
	@InnerAccess final CleanAction cleanAction = new CleanAction(this);

	/** The {@linkplain CleanModuleAction clean module action}. */
	@InnerAccess final CleanModuleAction cleanModuleAction =
		new CleanModuleAction(this);

	/** The {@linkplain CreateProgramAction create program action}. */
	@InnerAccess final CreateProgramAction createProgramAction =
		new CreateProgramAction(this);

	/**
	 * The {@linkplain GenerateDocumentationAction generate documentation
	 * action}.
	 */
	@InnerAccess final GenerateDocumentationAction documentAction =
		new GenerateDocumentationAction(this);

	/** The {@linkplain GenerateGraphAction generate graph action}. */
	@InnerAccess final GenerateGraphAction graphAction =
		new GenerateGraphAction(this);

	/**
	 * The {@linkplain SetDocumentationPathAction documentation path dialog
	 * action}.
	 */
	@InnerAccess final SetDocumentationPathAction setDocumentationPathAction =
		new SetDocumentationPathAction(this);

	/**
	 * The {@linkplain NewModuleAction new module path dialog action}.
	 */
	@InnerAccess final NewModuleAction newModuleAction =
		new NewModuleAction(this);

	/**
	 * The {@linkplain NewPackageAction new module path dialog action}.
	 */
	@InnerAccess final NewPackageAction newPackageAction =
		new NewPackageAction(this);

	/**
	 * The {@linkplain AddModuleTemplateAction module template path dialog
	 * action}.
	 */
	@InnerAccess final AddModuleTemplateAction addModuleTemplateAction =
		new AddModuleTemplateAction(this);

	/** The {@linkplain ShowVMReportAction show VM report action}. */
	@InnerAccess final ShowVMReportAction showVMReportAction =
		new ShowVMReportAction(this);

	/** The {@linkplain ResetVMReportDataAction reset VM report data action}. */
	@InnerAccess final ResetVMReportDataAction resetVMReportDataAction =
		new ResetVMReportDataAction(this);

	/** The {@linkplain ShowCCReportAction show CC report action}. */
	@InnerAccess final @Nullable ShowCCReportAction showCCReportAction;

	/** The {@linkplain ResetCCReportDataAction reset CC report data action}. */
	@InnerAccess final @Nullable
		ResetCCReportDataAction resetCCReportDataAction;

	/** The {@linkplain TraceMacrosAction toggle trace macros action}. */
	@InnerAccess final TraceMacrosAction debugMacroExpansionsAction =
		new TraceMacrosAction(this);

	/** The {@linkplain TraceCompilerAction toggle trace compiler action}. */
	@InnerAccess final TraceCompilerAction debugCompilerAction =
		new TraceCompilerAction(this);

	/** The {@linkplain ToggleFastLoaderAction toggle fast-loader action}. */
	@InnerAccess final ToggleFastLoaderAction toggleFastLoaderAction =
		new ToggleFastLoaderAction(this);

	/** The {@linkplain ToggleDebugInterpreterL1 toggle L1 debug action}. */
	@InnerAccess final ToggleDebugInterpreterL1 toggleDebugL1 =
		new ToggleDebugInterpreterL1(this);

	/** The {@linkplain ToggleDebugInterpreterL2 toggle L2 debug action}. */
	@InnerAccess final ToggleDebugInterpreterL2 toggleDebugL2 =
		new ToggleDebugInterpreterL2(this);

	/**
	 * The {@linkplain ToggleDebugInterpreterPrimitives toggle primitive debug
	 * action}.
	 */
	@InnerAccess final ToggleDebugInterpreterPrimitives toggleDebugPrimitives =
		new ToggleDebugInterpreterPrimitives(this);

	/**
	 * The {@linkplain TraceSummarizeStatementsAction toggle fast-loader
	 * summarization action}.
	 */
	@InnerAccess final TraceSummarizeStatementsAction
		traceSummarizeStatementsAction =
			new TraceSummarizeStatementsAction(this);

	/**
	 * The {@linkplain TraceLoadedStatementsAction toggle load-tracing action}.
	 */
	@InnerAccess final TraceLoadedStatementsAction
		traceLoadedStatementsAction =
		new TraceLoadedStatementsAction(this);

	/** The {@linkplain ParserIntegrityCheckAction}. */
	@InnerAccess final ParserIntegrityCheckAction parserIntegrityCheckAction;

	/** The {@linkplain ClearTranscriptAction clear transcript action}. */
	@InnerAccess final ClearTranscriptAction clearTranscriptAction =
		new ClearTranscriptAction(this);

	/** The {@linkplain InsertEntryPointAction insert entry point action}. */
	@InnerAccess final InsertEntryPointAction insertEntryPointAction =
		new InsertEntryPointAction(this);

	/** The {@linkplain BuildAction action to build an entry point module}. */
	@InnerAccess final BuildAction buildEntryPointModuleAction =
		new BuildAction(this, true);

	/** The {@linkplain EditModuleAction action to open a source module}. */
	@InnerAccess final EditModuleAction editModuleAction =
		new EditModuleAction(this);

	/**
	 * The {@linkplain DisplayCodeCoverageReport action to display the current
	 * code coverage session's report data}.
	 */
//	@InnerAccess final DisplayCodeCoverageReport displayCodeCoverageReport =
//		new DisplayCodeCoverageReport(this, true);

	/**
	 * The {@linkplain ResetCodeCoverageDataAction action to reset the code
	 * coverage data and thereby start a new code coverage session}.
	 */
//	@InnerAccess final ResetCodeCoverageDataAction resetCodeCoverageDataAction =
//		new ResetCodeCoverageDataAction(this, true);

	/** Whether an entry point invocation (command line) is executing. */
	public boolean isRunning = false;

	/**
	 * Enable or disable controls and menu items based on the current state.
	 */
	public void setEnablements ()
	{
		final boolean busy = backgroundTask != null || isRunning;
		buildProgress.setEnabled(busy);
		buildProgress.setVisible(backgroundTask instanceof BuildTask);
		inputField.setEnabled(!busy || isRunning);
		retrievePreviousAction.setEnabled(!busy);
		retrieveNextAction.setEnabled(!busy);
		cancelAction.setEnabled(busy);
		buildAction.setEnabled(!busy && selectedModule() != null);
		unloadAction.setEnabled(!busy && selectedModuleIsLoaded());
		unloadAllAction.setEnabled(!busy);
		cleanAction.setEnabled(!busy);
		cleanModuleAction.setEnabled(
			!busy
				&& (selectedModuleRoot() != null || selectedModule() != null));
		refreshAction.setEnabled(!busy);
		setDocumentationPathAction.setEnabled(!busy);
		documentAction.setEnabled(!busy && selectedModule() != null);
		graphAction.setEnabled(!busy && selectedModule() != null);
		insertEntryPointAction.setEnabled(
			!busy && selectedEntryPoint() != null);
		final ResolvedModuleName selectedEntryPointModule =
			selectedEntryPointModule();
		createProgramAction.setEnabled(
			!busy && selectedEntryPoint() != null
				&& selectedEntryPointModule != null
				&& availBuilder.getLoadedModule(selectedEntryPointModule)
					!= null);
		buildEntryPointModuleAction.setEnabled(
			!busy && selectedEntryPointModule() != null);
		editModuleAction.setEnabled(
			!busy && selectedModuleIsLoaded());
		newModuleAction.setEnabled(!busy &&
			(selectedModule() != null || selectedModuleRoot() != null));
		newPackageAction.setEnabled(!busy &&
			(selectedModule() != null || selectedModuleRoot() != null));
		addModuleTemplateAction.setEnabled(!busy);
		inputLabel.setText(isRunning
			? "Console Input:"
			: "Command:");
		inputField.setBackground(isRunning
			? new Color(192, 255, 192)
			: null);
	}

	/**
	 * Clear the {@linkplain #transcript transcript}.
	 */
	public void clearTranscript ()
	{
		invokeLater(() ->
		{
			final StyledDocument doc = document();
			try
			{
				doc.remove(
					perModuleStatusTextSize,
					doc.getLength() - perModuleStatusTextSize);
			}
			catch (final BadLocationException e)
			{
				// Shouldn't happen.
				assert false;
			}
		});
	}

	/**
	 * Re-parse the package structure from scratch.
	 */
	public void refresh ()
	{
		resolver.clearCache();
		final ResolvedModuleName selection = selectedModule();
		final TreeNode modules = newModuleTree();
		moduleTree.setModel(new DefaultTreeModel(modules));
		for (int i = moduleTree.getRowCount() - 1; i >= 0; i--)
		{
			moduleTree.expandRow(i);
		}
		if (selection != null)
		{
			final TreePath path = modulePath(selection.qualifiedName());
			if (path != null)
			{
				moduleTree.setSelectionPath(path);
			}
		}

		final TreeNode entryPoints = newEntryPointsTree();
		entryPointsTree.setModel(new DefaultTreeModel(entryPoints));
		for (int i = entryPointsTree.getRowCount() - 1; i >= 0; i--)
		{
			entryPointsTree.expandRow(i);
		}
	}

	/**
	 * Answer a {@link FileVisitor} suitable for recursively exploring an
	 * Avail root. A new {@code FileVisitor} should be obtained for each Avail
	 * root.
	 *
	 * @param stack
	 *        The stack on which to place Avail roots and packages.
	 * @param moduleRoot
	 *        The {@link ModuleRoot} within which to scan recursively.
	 * @return A {@code FileVisitor}.
	 */
	private FileVisitor<Path> moduleTreeVisitor (
		final Deque<DefaultMutableTreeNode> stack,
		final ModuleRoot moduleRoot)
	{
		final String extension = ModuleNameResolver.availExtension;
		final Mutable<Boolean> isRoot = new Mutable<>(true);
		final FileVisitor<Path> visitor = new FileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory (
				final @Nullable Path dir,
				final @Nullable BasicFileAttributes unused)
			{
				assert dir != null;
				final DefaultMutableTreeNode parentNode = stack.peekFirst();
				if (isRoot.value)
				{
					// Add a ModuleRoot.
					isRoot.value = false;
					assert stack.size() == 1;
					final ModuleRootNode node =
						new ModuleRootNode(availBuilder, moduleRoot);
					parentNode.add(node);
					stack.addFirst(node);
					return FileVisitResult.CONTINUE;
				}
				final String fileName = dir.getFileName().toString();
				if (fileName.endsWith(extension))
				{
					final String localName = fileName.substring(
						0, fileName.length() - extension.length());
					final ModuleName moduleName;
					if (parentNode instanceof ModuleRootNode)
					{
						// Add a top-level package.
						final ModuleRootNode strongParentNode =
							(ModuleRootNode)parentNode;
						final ModuleRoot thisRoot =
							strongParentNode.moduleRoot();
						assert thisRoot == moduleRoot;
						moduleName = new ModuleName(
							"/" + moduleRoot.name() + "/" + localName);
					}
					else
					{
						// Add a non-top-level package.
						assert parentNode instanceof ModuleOrPackageNode;
						final ModuleOrPackageNode strongParentNode =
							(ModuleOrPackageNode)parentNode;
						assert strongParentNode.isPackage();
						final ResolvedModuleName parentModuleName =
							strongParentNode.resolvedModuleName();
						// The (resolved) parent is a package representative
						// module, so use its parent, the package itself.
						moduleName = new ModuleName(
							parentModuleName.packageName(), localName);
					}
					final ResolvedModuleName resolved;
					try
					{
						resolved = resolver.resolve(moduleName, null);
					}
					catch (final UnresolvedDependencyException e)
					{
						// The directory didn't contain the necessary package
						// representative, so simply skip the whole directory.
						return FileVisitResult.SKIP_SUBTREE;
					}
					final ModuleOrPackageNode node = new ModuleOrPackageNode(
						availBuilder, moduleName, resolved, true);
					parentNode.add(node);
					if (resolved.isRename())
					{
						// Don't examine modules inside a package which is the
						// source of a rename.  They wouldn't have resolvable
						// dependencies anyhow.
						return FileVisitResult.SKIP_SUBTREE;
					}
					stack.addFirst(node);
					return FileVisitResult.CONTINUE;
				}
				return FileVisitResult.SKIP_SUBTREE;
			}

			@Override
			public FileVisitResult postVisitDirectory (
				final @Nullable Path dir,
				final @Nullable IOException ex)
			{
				assert dir != null;
				// Pop the node from the stack.
				stack.removeFirst();
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile (
					final @Nullable Path file,
					final @Nullable BasicFileAttributes attrs)
				throws IOException
			{
				assert file != null;
				final DefaultMutableTreeNode parentNode = stack.peekFirst();
				if (isRoot.value)
				{
					throw new IOException("Avail root should be a directory");
				}
				final String fileName = file.getFileName().toString();
				if (fileName.endsWith(extension))
				{
					final String localName = fileName.substring(
						0, fileName.length() - extension.length());
					final ModuleName moduleName;
					if (parentNode instanceof ModuleRootNode)
					{
						// Add a top-level module (directly in a root).
						final ModuleRootNode strongParentNode =
							(ModuleRootNode)parentNode;
						final ModuleRoot thisRoot =
							strongParentNode.moduleRoot();
						assert thisRoot == moduleRoot;
						moduleName = new ModuleName(
							"/" + moduleRoot.name() + "/" + localName);
					}
					else
					{
						// Add a non-top-level module.
						assert parentNode instanceof ModuleOrPackageNode;
						final ModuleOrPackageNode strongParentNode =
							(ModuleOrPackageNode)parentNode;
						assert strongParentNode.isPackage();
						final ResolvedModuleName parentModuleName =
							strongParentNode.resolvedModuleName();
						moduleName = new ModuleName(
							parentModuleName.packageName(), localName);
					}
					try
					{
						final ResolvedModuleName resolved =
							resolver.resolve(moduleName, null);
						final ModuleOrPackageNode node =
							new ModuleOrPackageNode(
								availBuilder, moduleName, resolved, false);
						if (resolved.isRename() || !resolved.isPackage())
						{
							parentNode.add(node);
						}
					}
					catch (final UnresolvedDependencyException e)
					{
						// TODO MvG - Find a better way of reporting broken
						// dependencies. Ignore for now (during directory scan).
						throw new RuntimeException(e);
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed (
				final @Nullable Path file,
				final @Nullable IOException ex)
			{
				return FileVisitResult.CONTINUE;
			}
		};
		return visitor;
	}

	/**
	 * Answer a {@linkplain TreeNode tree node} that represents the (invisible)
	 * root of the Avail module tree.
	 *
	 * @return The (invisible) root of the module tree.
	 */
	public TreeNode newModuleTree ()
	{
		final ModuleRoots roots = resolver.moduleRoots();
		final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(
			"(packages hidden root)");
		// Put the invisible root onto the work stack.
		final Deque<DefaultMutableTreeNode> stack = new ArrayDeque<>();
		stack.add(treeRoot);
		for (final ModuleRoot root : roots.roots())
		{
			// Obtain the path associated with the module root.
			assert root != null;
			root.repository().reopenIfNecessary();
			final File rootDirectory = stripNull(root.sourceDirectory());
			try
			{
				Files.walkFileTree(
					Paths.get(rootDirectory.getAbsolutePath()),
					EnumSet.of(FileVisitOption.FOLLOW_LINKS),
					Integer.MAX_VALUE,
					moduleTreeVisitor(stack, root));
			}
			catch (final IOException e)
			{
				e.printStackTrace();
				stack.clear();
				stack.add(treeRoot);
			}
		}
		return treeRoot;
	}

	/**
	 * Answer a {@linkplain TreeNode tree node} that represents the (invisible)
	 * root of the Avail entry points tree.
	 *
	 * @return The (invisible) root of the entry points tree.
	 */
	public TreeNode newEntryPointsTree ()
	{
		final Object mutex = new Object();
		final Map<String, DefaultMutableTreeNode> moduleNodes = new HashMap<>();
		availBuilder.traceDirectories(
			(resolvedName, moduleVersion) ->
			{
				assert resolvedName != null;
				assert moduleVersion != null;
				final List<String> entryPoints =
					moduleVersion.getEntryPoints();
				if (!entryPoints.isEmpty())
				{
					final EntryPointModuleNode moduleNode =
						new EntryPointModuleNode(
							availBuilder, resolvedName);
					for (final String entryPoint : entryPoints)
					{
						final EntryPointNode entryPointNode =
							new EntryPointNode(
								availBuilder, resolvedName, entryPoint);
						moduleNode.add(entryPointNode);
					}
					synchronized (mutex)
					{
						moduleNodes.put(
							resolvedName.qualifiedName(), moduleNode);
					}
				}
			});
		final String [] mapKeys = moduleNodes.keySet().toArray(
			new String [moduleNodes.size()]);
		Arrays.sort(mapKeys);
		final DefaultMutableTreeNode entryPointsTreeRoot =
			new DefaultMutableTreeNode("(entry points hidden root)");
		for (final String moduleLabel : mapKeys)
		{
			entryPointsTreeRoot.add(moduleNodes.get(moduleLabel));
		}
		return entryPointsTreeRoot;
	}

	/**
	 * Answer the {@linkplain TreePath path} to the specified module name in the
	 * {@linkplain #moduleTree module tree}.
	 *
	 * @param moduleName A module name.
	 * @return A tree path, or {@code null} if the module name is not present in
	 *         the tree.
	 */
	@SuppressWarnings("unchecked")
	public @Nullable TreePath modulePath (final String moduleName)
	{
		final String[] path = moduleName.split("/");
		final TreeModel model = moduleTree.getModel();
		final DefaultMutableTreeNode treeRoot =
			(DefaultMutableTreeNode) model.getRoot();
		Enumeration<DefaultMutableTreeNode> nodes = treeRoot.children();
		int index = 1;
		while (nodes.hasMoreElements())
		{
			final AbstractBuilderFrameTreeNode node =
				(AbstractBuilderFrameTreeNode) nodes.nextElement();
			if (node.isSpecifiedByString(path[index]))
			{
				index++;
				if (index == path.length)
				{
					return new TreePath(node.getPath());
				}
				nodes = node.children();
			}
		}
		return null;
	}

	/**
	 * Answer the currently selected {@linkplain ModuleRootNode module root
	 * node}, or null.
	 *
	 * @return A {@link ModuleRootNode}, or {@code null} if no module root is
	 *         selected.
	 */
	public @Nullable ModuleRootNode selectedModuleRootNode ()
	{
		final TreePath path = moduleTree.getSelectionPath();
		if (path == null)
		{
			return null;
		}
		final DefaultMutableTreeNode selection =
			(DefaultMutableTreeNode) path.getLastPathComponent();
		if (selection instanceof ModuleRootNode)
		{
			return (ModuleRootNode) selection;
		}
		return null;
	}

	/**
	 * Answer the {@linkplain ModuleRoot} that is currently selected, otherwise
	 * {@code null}.
	 *
	 * @return A {@link ModuleRoot}, or {@code null} if no module root is
	 *         selected.
	 */
	public @Nullable ModuleRoot selectedModuleRoot ()
	{
		final ModuleRootNode node = selectedModuleRootNode();
		if (node == null)
		{
			return null;
		}
		return node.moduleRoot();
	}

	/**
	 * Answer the currently selected {@linkplain ModuleOrPackageNode module
	 * node}.
	 *
	 * @return A module node, or {@code null} if no module is selected.
	 */
	@InnerAccess @Nullable ModuleOrPackageNode selectedModuleNode ()
	{
		final TreePath path = moduleTree.getSelectionPath();
		if (path == null)
		{
			return null;
		}
		final DefaultMutableTreeNode selection =
			(DefaultMutableTreeNode) path.getLastPathComponent();
		if (selection instanceof ModuleOrPackageNode)
		{
			return (ModuleOrPackageNode) selection;
		}
		return null;
	}

	/**
	 * Is the selected {@linkplain ModuleDescriptor module} loaded?
	 *
	 * @return {@code true} if the selected module is loaded, {@code false} if
	 *         no module is selected or the selected module is not loaded.
	 */
	@InnerAccess boolean selectedModuleIsLoaded ()
	{
		final ModuleOrPackageNode node = selectedModuleNode();
		if (node == null)
		{
			return false;
		}
		return node.isLoaded();
	}

	/**
	 * Answer the {@linkplain ResolvedModuleName name} of the currently selected
	 * {@linkplain ModuleDescriptor module}.
	 *
	 * @return A fully-qualified module name, or {@code null} if no module is
	 *         selected.
	 */
	public @Nullable ResolvedModuleName selectedModule ()
	{
		final ModuleOrPackageNode node = selectedModuleNode();
		if (node == null)
		{
			return null;
		}
		return node.resolvedModuleName();
	}

	/**
	 * Answer the currently selected entry point, or {@code null} if none.
	 *
	 * @return An entry point name, or {@code null} if no entry point is
	 *         selected.
	 */
	public @Nullable String selectedEntryPoint ()
	{
		final TreePath path = entryPointsTree.getSelectionPath();
		if (path == null)
		{
			return null;
		}
		final DefaultMutableTreeNode selection =
			(DefaultMutableTreeNode) path.getLastPathComponent();
		if (!(selection instanceof EntryPointNode))
		{
			return null;
		}
		final EntryPointNode strongSelection = (EntryPointNode) selection;
		return strongSelection.entryPointString();
	}

	/**
	 * Answer the resolved name of the module selected in the {@link
	 * #entryPointsTree}, or the module defining the entry point that's
	 * selected, or {@code null} if none.
	 *
	 * @return A {@link ResolvedModuleName} or {@code null}.
	 */
	public @Nullable ResolvedModuleName selectedEntryPointModule ()
	{
		final TreePath path = entryPointsTree.getSelectionPath();
		if (path == null)
		{
			return null;
		}
		final DefaultMutableTreeNode selection =
			(DefaultMutableTreeNode) path.getLastPathComponent();
		if (selection instanceof EntryPointNode)
		{
			return ((EntryPointNode)selection).resolvedModuleName();
		}
		if (selection instanceof EntryPointModuleNode)
		{
			return ((EntryPointModuleNode)selection).resolvedModuleName();
		}
		return null;
	}

	/**
	 * A monitor to serialize access to the current build status information.
	 */
	private final Object buildGlobalUpdateMonitor = new Object();

	/**
	 * The position up to which the current build has completed.  Protected by
	 * {@link #buildGlobalUpdateMonitor}.
	 *//**/
	private long latestGlobalBuildPosition = -1;

	/**
	 * The total number of bytes of code to be loaded.  Protected by {@link
	 * #buildGlobalUpdateMonitor}.
	 */
	private long globalBuildLimit = -1;

	/**
	 * Whether a user interface task for updating the visible build progress has
	 * been queued but not yet completed.  Protected by {@link
	 * #buildGlobalUpdateMonitor}.
	 */
	private boolean hasQueuedGlobalBuildUpdate = false;

	/**
	 * Ensure the new build position information will eventually be presented to
	 * the display.
	 *
	 * @param position
	 *        The global parse position, in bytes.
	 * @param globalCodeSize
	 *        The target number of bytes to parse.
	 */
	public void eventuallyUpdateBuildProgress (
		final long position,
		final long globalCodeSize)
	{
		synchronized (buildGlobalUpdateMonitor)
		{
			latestGlobalBuildPosition = position;
			globalBuildLimit = globalCodeSize;
			if (!hasQueuedGlobalBuildUpdate)
			{
				hasQueuedGlobalBuildUpdate = true;
				availBuilder.runtime.timer.schedule(
					new TimerTask()
					{
						@Override
						public void run ()
						{
							invokeLater(() -> updateBuildProgress());
						}
					},
					100);
			}
		}
	}

	/**
	 * Update the {@linkplain #buildProgress build progress bar}.
	 */
	@InnerAccess void updateBuildProgress ()
	{
		final long position;
		final long max;
		synchronized (buildGlobalUpdateMonitor)
		{
			assert hasQueuedGlobalBuildUpdate;
			position = latestGlobalBuildPosition;
			max = globalBuildLimit;
			hasQueuedGlobalBuildUpdate = false;
		}
		final int perThousand = (int) ((position * 1000) / max);
		buildProgress.setValue(perThousand);
		final float percent = perThousand / 10.0f;
		buildProgress.setString(String.format(
			"Build Progress: %,d / %,d bytes (%3.1f%%)",
			position,
			max,
			percent));
	}


	/** A monitor to protect updates to the per module progress. */
	private final Object perModuleProgressMonitor = new Object();

	/**
	 * The progress map per module.  Protected by {@link
	 * #perModuleProgressMonitor}.
	 */
	private final Map<ModuleName, Pair<Long, Long>> perModuleProgress =
		new HashMap<>();

	/**
	 * The number of characters of text at the start of the transcript which is
	 * currently displaying per-module progress information.  Protected by
	 * {@link #perModuleProgressMonitor}
	 */
	private int perModuleStatusTextSize = 0;

	/**
	 * Whether a user interface task for updating the visible per-module
	 * information has been queued but not yet completed.  Protected by {@link
	 * #perModuleProgressMonitor}.
	 */
	private boolean hasQueuedPerModuleBuildUpdate = false;

	/**
	 * Progress has been made at loading a module.
	 */
	public void eventuallyUpdatePerModuleProgress (
		final ModuleName moduleName,
		final long moduleSize,
		final long position)
	{
		synchronized (perModuleProgressMonitor)
		{
			if (position == moduleSize)
			{
				perModuleProgress.remove(moduleName);
			}
			else
			{
				perModuleProgress.put(
					moduleName, new Pair<>(position, moduleSize));
			}
			if (!hasQueuedPerModuleBuildUpdate)
			{
				hasQueuedPerModuleBuildUpdate = true;
				availBuilder.runtime.timer.schedule(
					new TimerTask()
					{
						@Override
						public void run ()
						{
							invokeLater(() -> updatePerModuleProgress());
						}
					},
					100);
			}
		}
	}

	@InnerAccess void updatePerModuleProgress ()
	{
		final List<Entry<ModuleName, Pair<Long, Long>>> progress;
		synchronized (perModuleProgressMonitor)
		{
			assert hasQueuedPerModuleBuildUpdate;
			progress = new ArrayList<>(perModuleProgress.entrySet());
			hasQueuedPerModuleBuildUpdate = false;
		}
		progress.sort(Comparator.comparing(entry -> entry
			.getKey()
			.qualifiedName()));
		final StringBuilder builder = new StringBuilder(100);
		for (final Entry<ModuleName, Pair<Long, Long>> entry : progress)
		{
			final Pair<Long, Long> pair = entry.getValue();
			builder.append(
				String.format(
					"%,6d / %,6d - %s%n",
					pair.first(),
					pair.second(),
					entry.getKey()));
		}
		final String string = builder.toString();
		final StyledDocument doc = transcript.getStyledDocument();
		try
		{
			doc.remove(0, perModuleStatusTextSize);
			doc.insertString(
				0,
				string,
				BUILD_PROGRESS.styleIn(doc));
		}
		catch (final BadLocationException e)
		{
			// Shouldn't happen.
			assert false;
		}
		synchronized (perModuleProgressMonitor)
		{
			perModuleStatusTextSize = string.length();
		}
	}

	/** The user-specific {@link Preferences} for this application to use. */
	private static final Preferences basePreferences =
		Preferences.userNodeForPackage(AvailWorkbench.class);

	/** The key under which to organize all placement information. */
	private static final String placementByMonitorNamesString =
		"placementByMonitorNames";

	/** The leaf key under which to store a single window placement. */
	public static final String placementLeafKeyString = "placement";

	/** The key under which to organize all templates */
	private static final String templatePreferenceString =
		"templates";

	/** The leaf key under which to store the module template string. */
	public static final String moduleLeafKeyString = "module";

	/** The leaf key under which to store the text template string. */
	public static final String textLeafKeyString = "text template";

	/** The key under which to store the {@link ModuleRoots}. */
	public static final String moduleRootsKeyString = "module roots";

	/** The subkey that holds a root's repository name. */
	public static final String moduleRootsRepoSubkeyString = "repository";

	/** The subkey that holds a root's source directory name. */
	public static final String moduleRootsSourceSubkeyString = "source";

	/** The key under which to store the module rename rules. */
	public static final String moduleRenamesKeyString = "module renames";

	/** The subkey that holds a rename rule's source module name. */
	public static final String moduleRenameSourceSubkeyString = "source";

	/** The subkey that holds a rename rule's replacement module name. */
	public static final String moduleRenameTargetSubkeyString = "target";

	/**
	 * Answer a {@link List} of {@link Rectangle}s corresponding with the
	 * physical monitors into which {@link Frame}s may be positioned.
	 *
	 * @return The list of rectangles to which physical screens are mapped.
	 */
	public static List<String> allScreenNames ()
	{
		final GraphicsEnvironment graphicsEnvironment =
			GraphicsEnvironment.getLocalGraphicsEnvironment();
		final GraphicsDevice[] screens = graphicsEnvironment.getScreenDevices();
		final List<String> allScreens = new ArrayList<>();
		for (final GraphicsDevice screen : screens)
		{
			allScreens.add(screen.getIDstring());
		}
		return allScreens;
	}

	/**
	 * Answer the {@link Preferences} node responsible for holding the default
	 * window position and size for the current monitor configuration.
	 *
	 * @param screenNames
	 *        The list of {@link GraphicsDevice#getIDstring() id strings} of all
	 *        physical screens.
	 * @return The {@code Preferences} node in which placement information for
	 *         the current monitor configuration can be stored and retrieved.
	 */
	public static Preferences placementPreferencesNodeForScreenNames (
		final List<String> screenNames)
	{
		final StringBuilder allNamesString = new StringBuilder();
		for (final String name : screenNames)
		{
			allNamesString.append(name);
			allNamesString.append(";");
		}
		return basePreferences.node(
			placementByMonitorNamesString + "/" + allNamesString);
	}

	/**
	 * Parse the {@link ModuleRoots} from the module roots preferences node.
	 *
	 * @return The {@code ModuleRoots} constructed from the preferences node.
	 */
	public static ModuleRoots loadModuleRoots ()
	{
		final ModuleRoots roots = new ModuleRoots("");
		roots.clearRoots();
		final Preferences node = basePreferences.node(moduleRootsKeyString);
		try
		{
			final String[] childNames = node.childrenNames();
			for (final String childName : childNames)
			{
				final Preferences childNode = node.node(childName);
				final String repoName = childNode.get(
					moduleRootsRepoSubkeyString, "");
				final String sourceName = childNode.get(
					moduleRootsSourceSubkeyString, "");
				roots.addRoot(
					new ModuleRoot(
						childName,
						new File(repoName),
						new File(sourceName)));
			}
		}
		catch (final BackingStoreException e)
		{
			System.err.println(
				"Unable to read Avail roots preferences.");
		}
		return roots;
	}


	/**
	 * Parse the {@link ModuleRoots} from the module roots preferences node.
	 *
	 * @param resolver
	 *        The {@link ModuleNameResolver} used for resolving module names.
	 */
	public static void loadRenameRulesInto (final ModuleNameResolver resolver)
	{
		resolver.clearRenameRules();
		final Preferences node = basePreferences.node(moduleRenamesKeyString);
		try
		{
			final String[] childNames = node.childrenNames();
			for (final String childName : childNames)
			{
				final Preferences childNode = node.node(childName);
				final String source = childNode.get(
					moduleRenameSourceSubkeyString, "");
				final String target = childNode.get(
					moduleRenameTargetSubkeyString, "");
				// Ignore empty sources and targets, although they shouldn't
				// occur.
				if (!source.isEmpty() && !target.isEmpty())
				{
					resolver.addRenameRule(source, target);
				}
			}
		}
		catch (final BackingStoreException e)
		{
			System.err.println(
				"Unable to read Avail rename rule preferences.");
		}
	}

	/**
	 * Save the current {@link ModuleRoots} and rename rules to the preferences
	 * storage.
	 */
	public void saveModuleConfiguration ()
	{
		try
		{
			final Preferences rootsNode = basePreferences.node(moduleRootsKeyString);
			final ModuleRoots roots = resolver.moduleRoots();
			for (final String oldChildName : rootsNode.childrenNames())
			{
				if (roots.moduleRootFor(oldChildName) == null)
				{
					rootsNode.node(oldChildName).removeNode();
				}
			}
			for (final ModuleRoot root : roots)
			{
				final Preferences childNode = rootsNode.node(root.name());
				childNode.put(
					moduleRootsRepoSubkeyString,
					root.repository().fileName().getPath());
				childNode.put(
					moduleRootsSourceSubkeyString,
					root.sourceDirectory().getPath());
			}

			final Preferences renamesNode =
				basePreferences.node(moduleRenamesKeyString);
			final Map<String, String> renames = resolver.renameRules();
			for (final String oldChildName : renamesNode.childrenNames())
			{
				int nameInt;
				try
				{
					nameInt = Integer.valueOf(oldChildName);
				}
				catch (final NumberFormatException e)
				{
					nameInt = -1;
				}
				if (!oldChildName.equals(Integer.toString(nameInt))
					|| nameInt < 0
					|| nameInt >= renames.size())
				{
					renamesNode.node(oldChildName).removeNode();
				}
			}
			int rowCounter = 0;
			for (final Entry<String, String> rename : renames.entrySet())
			{
				final Preferences childNode = renamesNode.node(
					Integer.toString(rowCounter));
				childNode.put(
					moduleRenameSourceSubkeyString, rename.getKey());
				childNode.put(
					moduleRenameTargetSubkeyString, rename.getValue());
				rowCounter++;
			}

			basePreferences.flush();
		}
		catch (final BackingStoreException e)
		{
			System.err.println(
				"Unable to write Avail roots/renames preferences.");
		}
	}

	/**
	 * Answer the {@link Preferences} node responsible for holding templates.
	 *
	 * @return The {@code Preferences} node in which template information can be
	 *         stored and retrieved.
	 */
	public static Preferences templatePreferences ()
	{
		return basePreferences.node(templatePreferenceString);
	}

	/**
	 * Information about the window layout.
	 */
	@InnerAccess
	public static class LayoutConfiguration
	{
		/** The preferred location and size of the window, if specified. */
		@Nullable Rectangle placement = null;

		/**
		 * The width of the left region of the builder frame in pixels, if
		 * specified
		 */
		@Nullable Integer leftSectionWidth = null;

		/**
		 * The preferred location and size of the module editor window,
		 * if specified.
		 */
		public @Nullable Rectangle moduleViewerPlacement = null;

		/**
		 * Answer this configuration's recommended width in pixels for the left
		 * region of the window, supplying a suitable default if necessary.
		 *
		 * @return The recommended width of the left part.
		 */
		int leftSectionWidth ()
		{
			final Integer w = leftSectionWidth;
			return w != null ? w : 200;
		}

		/**
		 * The proportion, if specified, as a float between {@code 0.0} and
		 * {@code 1.0} of the height of the top left module region in relative
		 * proportional to the height of the entire builder frame.
		 */
		@Nullable Double moduleVerticalProportion = null;

		/**
		 * Add this configuration's recommended proportion of height of the
		 * modules list versus the entire frame's height, supplying a default
		 * if necessary.  It must be between 0.0 and 1.0 inclusive.
		 *
		 * @return The vertical proportion of the modules area.
		 */
		double moduleVerticalProportion ()
		{
			final Double h = moduleVerticalProportion;
			return h != null ? max(0.0, min(1.0, h)) : 0.5;
		}

		/**
		 * Answer a string representation of this configuration that is suitable
		 * for being stored and restored via the {@link
		 * LayoutConfiguration#LayoutConfiguration(String)} constructor.
		 *
		 * <p>
		 * The layout should be fairly stable to avoid treating older versions
		 * as malformed.  To that end, we use a simple list of strings, adding
		 * entries for new purposes to the end, and never removing or changing
		 * the meaning of existing entries.
		 * </p>
		 *
		 * @return A string.
		 */
		public String stringToStore ()
		{
			final String [] strings = new String [10];
			final Rectangle p = placement;
			if (p != null)
			{
				strings[0] = Integer.toString(p.x);
				strings[1] = Integer.toString(p.y);
				strings[2] = Integer.toString(p.width);
				strings[3] = Integer.toString(p.height);
			}

			final Integer w = leftSectionWidth;
			if (w != null)
			{
				strings[4] = Integer.toString(w);
			}
			final Double h = moduleVerticalProportion;
			if (h != null)
			{
				strings[5] = Double.toString(h);
			}

			final Rectangle mvp = moduleViewerPlacement;
			if (mvp != null)
			{
				strings[6] = Integer.toString(mvp.x);
				strings[7] = Integer.toString(mvp.y);
				strings[8] = Integer.toString(mvp.width);
				strings[9] = Integer.toString(mvp.height);
			}

			final StringBuilder builder = new StringBuilder();
			boolean first = true;
			for (final String string : strings)
			{
				if (!first)
				{
					builder.append(',');
				}
				if (string != null)
				{
					builder.append(string);
				}
				first = false;
			}
			return builder.toString();
		}

		/**
		 * Construct a new {@link LayoutConfiguration} with
		 * no preferences specified.
		 */
		public LayoutConfiguration ()
		{
			// all null
		}

		/**
		 * Construct a new {@link LayoutConfiguration} with
		 * preferences specified by some private encoding in the provided {@link
		 * String}.
		 *
		 * @param input
		 *        A string in some encoding compatible with that produced
		 *        by {@link #stringToStore()}.
		 */
		public LayoutConfiguration (final String input)
		{
			if (!input.isEmpty())
			{
				final String[] substrings = input.split(",");
				try
				{

					final int x = max(0, Integer.parseInt(substrings[0]));
					final int y = max(0, Integer.parseInt(substrings[1]));
					final int w = max(50, Integer.parseInt(substrings[2]));
					final int h = max(50, Integer.parseInt(substrings[3]));
					placement = new Rectangle(x, y, w, h);
				}
				catch (final NumberFormatException e)
				{
					// ignore
				}

				try
				{
					leftSectionWidth = max(0, Integer.parseInt(substrings[4]));
				}
				catch (final NumberFormatException e)
				{
					// ignore
				}
				try
				{
					moduleVerticalProportion = Double.parseDouble(substrings[5]);
				}
				catch (final NumberFormatException e)
				{
					// ignore
				}

				try
				{
					if (substrings.length >= 9)
					{
						final int x = max(0, Integer.parseInt(substrings[6]));
						final int y = max(0, Integer.parseInt(substrings[7]));
						final int w = max(50, Integer.parseInt(substrings[8]));
						final int h = max(50, Integer.parseInt(substrings[9]));
						moduleViewerPlacement = new Rectangle(x, y, w, h);
					}
				}
				catch (final NumberFormatException e)
				{
					// ignore
				}
			}
		}
	}

	/**
	 * Figure out how to initially lay out the frame, based on previously saved
	 * preference information.
	 *
	 * @return The initial {@link LayoutConfiguration}.
	 */
	@InnerAccess static LayoutConfiguration getInitialConfiguration ()
	{
		final Preferences preferences =
			placementPreferencesNodeForScreenNames(allScreenNames());
		final String configurationString = preferences.get(
			placementLeafKeyString,
			null);
		if (configurationString == null)
		{
			return new LayoutConfiguration();
		}
		return new LayoutConfiguration(configurationString);
	}

	/**
	 * The module templates.
	 */
	@InnerAccess
	public static class ModuleTemplates
	{
		/**
		 * A {@link Map} representing module template name - template pairs.
		 */
		public final Map<String, String> moduleTemplates = new HashMap<>();

		/**
		 * Answer the requested template or an empty string if none found.
		 *
		 * @param templateName
		 *        The name of the template.
		 * @return The requested template or an empty string.
		 */
		public String get (final @Nonnull String templateName)
		{
			return moduleTemplates.getOrDefault(templateName, "");
		}

		/**
		 * Answer the result of a filled out template.
		 *
		 * @param templateName
		 *        The template to fill out.
		 * @param moduleName
		 *        The name of the module being created.
		 * @return A string containing filled out template.
		 */
		public String createNewModuleFromTemplate (
			final @Nonnull String templateName,
			final @Nonnull String moduleName)
		{
			final String year = Integer.toString(LocalDateTime.ofInstant(
				Instant.now(), ZoneOffset.UTC).getYear());

			return Replaceable.replace(get(templateName), moduleName, year);
		}

		/**
		 * An enum of the patterns that are replaceable in a {@link
		 * ModuleTemplates}.
		 */
		enum Replaceable
		{
			MODULE ("${MODULE}"),
			YEAR ("${YEAR}");

			/**
			 * The pattern that can be replaced in the template.
			 */
			private final @Nonnull String pattern;

			/**
			 * Answer the result of a filled out template.
			 *
			 * @param template
			 *        The template to fill out.
			 * @param replacements
			 *        The replacement values for the template.
			 * @return A filled out template.
			 */
			static String replace (
				final @Nonnull String template,
				final @Nonnull String... replacements)
			{
				final Replaceable[] replaceables = Replaceable.values();

				assert replaceables.length == replacements.length;

				String result = template;
				for (int i = 0; i < replacements.length; i++)
				{
					result = result.replace(
						replaceables[i].pattern, replacements[i]);
				}
				return result;
			}

			/**
			 * Construct a {@link Replaceable}.
			 *
			 * @param pattern
			 *        The replacement pattern.
			 */
			Replaceable (final @Nonnull String pattern)
			{
				this.pattern = pattern;
			}
		}

		/**
		 * Answer a string representation of this template store that is
		 * suitable for being stored and restored via the {@link
		 * ModuleTemplates#ModuleTemplates(String)} constructor.
		 *
		 * @return A string.
		 */
		public String stringToStore ()
		{
			final String [] strings = new String [moduleTemplates.size()];
			final int index = 0;
			for (final Entry<String, String> entry : moduleTemplates.entrySet())
			{
				strings[index] = entry.getKey() + '\0' +
					entry.getValue();
			}

			final StringBuilder builder = new StringBuilder();
			boolean first = true;
			for (final String string : strings)
			{
				if (!first)
				{
					builder.append(0x1e);
				}
				if (string != null)
				{
					builder.append(string);
				}
				first = false;
			}
			return builder.toString();
		}

		/**
		 * Construct a new {@link ModuleTemplates} with no templates specified.
		 */
		public ModuleTemplates ()
		{
			// no templates
		}

		/**
		 * Construct a new {@link ModuleTemplates} with preferences specified by
		 * some private encoding in the provided {@link String}.
		 *
		 * @param input
		 *        A string in some encoding compatible with that produced
		 *        by {@link #stringToStore()}.
		 */
		public ModuleTemplates (final @Nonnull String input)
		{
			if (!input.isEmpty())
			{
				final String pairSpliter = "" + '\0';
				final String recordSpliter  = "" + 0x1e;
				final String[] substrings = input.split(recordSpliter);
				for (final String template : substrings)
				{
					final String[] pair = template.split(pairSpliter);
					assert  pair.length == 2;
					moduleTemplates.put(pair[0], pair[1]);
				}
			}
		}
	}

	/**
	 * Figure out how to initially lay out the frame, based on previously saved
	 * preference information.
	 *
	 * @return The initial {@link LayoutConfiguration}.
	 */
	@InnerAccess static ModuleTemplates getInitialModuleTemplate ()
	{
		final Preferences preferences = templatePreferences();
		final String templateString = preferences.get(
			moduleLeafKeyString,
			null);
		if (templateString == null)
		{
			return new ModuleTemplates();
		}
		return new ModuleTemplates(templateString);
	}


	/**
	 * Write text to the transcript with the given {@link StreamStyle}.
	 *
	 * @param text The text to write.
	 * @param streamStyle The style to write it in.
	 */
	public void writeText (
		final String text,
		final StreamStyle streamStyle)
	{
		synchronized (updateQueue)
		{
			int consumed = text.length();
			final Iterator<Pair<StreamStyle, StringBuilder>> iterator =
				updateQueue.iterator();
			if (iterator.hasNext())
			{
				iterator.next();  // Skip the first entry, the oldest one.
				while (iterator.hasNext())
				{
					consumed += iterator.next().second().length();
				}
				// Consumed is now how many chars would be queued if the first
				// queued entry were to be removed and the new text added.
				if (consumed > maxDocumentSize)
				{
					// Remove the oldest entry, since the subsequent entries
					// plus the new text are guaranteed to roll it off.
					updateQueue.removeFirst();
				}
			}
			final Pair<StreamStyle, StringBuilder> entry;
			if (!updateQueue.isEmpty()
				&& updateQueue.peekLast().first() == streamStyle
				&& updateQueue.peekLast().second().length() < maxDocumentSize)
			{
				entry = updateQueue.peekLast();
			}
			else
			{
				entry = new Pair<>(streamStyle, new StringBuilder());
				updateQueue.addLast(entry);
			}
			entry.second().append(text);
			if (!updatingTranscript)
			{
				// The updating Runnable will deal with it afterward.
				updatingTranscript = true;
				updateTranscript();
			}
		}
	}

	/**
	 * The {@link DefaultTreeCellRenderer} that knows how to render tree nodes
	 * for my {@link #moduleTree} and my {@link #entryPointsTree}.
	 */
	private static final DefaultTreeCellRenderer treeRenderer =
		new DefaultTreeCellRenderer()
		{
			@Override
			public Component getTreeCellRendererComponent(
				final @Nullable JTree tree,
				final @Nullable Object value,
				final boolean selected1,
				final boolean expanded,
				final boolean leaf,
				final int row,
				final boolean hasFocus1)
			{
				assert tree != null;
				assert value != null;
				if (value instanceof AbstractBuilderFrameTreeNode)
				{
					final AbstractBuilderFrameTreeNode node =
						(AbstractBuilderFrameTreeNode) value;
					final Icon icon = node.icon(tree.getRowHeight());
					setLeafIcon(icon);
					setOpenIcon(icon);
					setClosedIcon(icon);
					String html = node.htmlText(selected1);
					html = "<html>" + html + "</html>";
					return super.getTreeCellRendererComponent(
						tree, html, selected1, expanded, leaf, row, hasFocus1);
				}
				return super.getTreeCellRendererComponent(
					tree, value, selected1, expanded, leaf, row, hasFocus1);
			}
		};

	/**
	 * Construct a new {@link AvailWorkbench}.
	 *
	 * @param resolver
	 *        The {@linkplain ModuleNameResolver module name resolver}.
	 */
	@InnerAccess AvailWorkbench (
		final ModuleNameResolver resolver)
	{
		//This must be called to circumvent a bug that won't be fixed for swing
		//and JavaFx interaction
		//See https://bugs.openjdk.java.net/browse/JDK-8090517
		PlatformImpl.startup(() -> {});

		// Set module components.
		this.resolver = resolver;
		final AvailRuntime runtime = new AvailRuntime(resolver);
		availBuilder = new AvailBuilder(runtime);

		// Get the existing preferences early for plugging in at the right
		// times during construction.
		final LayoutConfiguration configuration = getInitialConfiguration();
		this.moduleTemplates = getInitialModuleTemplate();
		replaceTextTemplate.initializeTemplatesFromPropertiesFile();

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// Set *just* the window title...
		setTitle("Avail Workbench");
		setResizable(true);

		// Create the menu bar and its menus.
		final JMenuBar menuBar = new JMenuBar();
		final JMenu buildMenu = menu("Build");
		if (!runningOnMac)
		{
			augment(buildMenu, aboutAction, null);
		}
		augment(
			buildMenu,
			buildAction, cancelAction, null,
			unloadAction, unloadAllAction, cleanAction,
//	    		cleanModuleAction,  //TODO MvG Fix implementation and enable.
				null,
			refreshAction);
		menuBar.add(buildMenu);
		final JMenu moduleMenu = menu(
			"Module",
			newPackageAction, newModuleAction, editModuleAction, null,
			addModuleTemplateAction);
		menuBar.add(moduleMenu);
		if (!runningOnMac)
		{
			augment(buildMenu, null, preferencesAction);
		}
		menuBar.add(
			menu(
				"Document",
				documentAction, null,
				setDocumentationPathAction));
		menuBar.add(
			menu(
				"Run",
				insertEntryPointAction, null,
				clearTranscriptAction));
		if (showDeveloperTools)
		{
			showCCReportAction = new ShowCCReportAction(this, runtime);
			resetCCReportDataAction =
				new ResetCCReportDataAction(this, runtime);
			parserIntegrityCheckAction =
				new ParserIntegrityCheckAction(this, runtime);
			menuBar.add(
				menu(
					"Developer",
					showVMReportAction, resetVMReportDataAction, null,
					showCCReportAction, resetCCReportDataAction, null,
						new JCheckBoxMenuItem(debugMacroExpansionsAction),
						new JCheckBoxMenuItem(debugCompilerAction),
						new JCheckBoxMenuItem(traceSummarizeStatementsAction),
						new JCheckBoxMenuItem(traceLoadedStatementsAction),
						new JCheckBoxMenuItem(toggleFastLoaderAction),
						null,
						new JCheckBoxMenuItem(toggleDebugL1),
						new JCheckBoxMenuItem(toggleDebugL2),
						new JCheckBoxMenuItem(toggleDebugPrimitives),
						null,
					parserIntegrityCheckAction, null,
					graphAction));
		}
		else
		{
			showCCReportAction = null;
			resetCCReportDataAction = null;
			parserIntegrityCheckAction = null;
		}
		setJMenuBar(menuBar);

		final JMenu buildPopup = menu(
			"Modules",
			buildAction,
			editModuleAction,
			documentAction,
			null,
			unloadAction,
//			cleanModuleAction,  //TODO MvG Fix implementation and enable.
			null,
			refreshAction);
		// The refresh item needs a little help ...
		InputMap inputMap = getRootPane().getInputMap(
			JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = getRootPane().getActionMap();
		inputMap.put(KeyStroke.getKeyStroke("F5"), "refresh");
		actionMap.put("refresh", refreshAction);

		final JMenu entryPointsPopup = menu(
			"Entry points",
			buildEntryPointModuleAction, null,
			editModuleAction, null,
			insertEntryPointAction, null,
			createProgramAction, null,
			refreshAction);

		final JMenu transcriptPopup = menu("Transcript", clearTranscriptAction);

		// Collect the modules and entry points.
		final TreeNode modules = new DefaultMutableTreeNode(
			"(packages hidden root)");
		final TreeNode entryPoints =
			new DefaultMutableTreeNode("(entry points hidden root)");

		// Create the module tree.
		moduleTree = new JTree(modules);
		moduleTree.setToolTipText("All modules, organized by module root.");
		moduleTree.setComponentPopupMenu(buildPopup.getPopupMenu());
		moduleTree.setEditable(false);
		moduleTree.setEnabled(true);
		moduleTree.setFocusable(true);
		moduleTree.getSelectionModel().setSelectionMode(
			TreeSelectionModel.SINGLE_TREE_SELECTION);
		moduleTree.setToggleClickCount(0);
		moduleTree.setShowsRootHandles(true);
		moduleTree.setRootVisible(false);
		moduleTree.addTreeSelectionListener(event -> setEnablements());
		moduleTree.setCellRenderer(treeRenderer);
		moduleTree.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked (final @Nullable MouseEvent e)
			{
				assert e != null;
				if (buildAction.isEnabled()
					&& e.getClickCount() == 2
					&& e.getButton() == MouseEvent.BUTTON1)
				{
					e.consume();
					buildAction.actionPerformed(
						new ActionEvent(moduleTree, -1, "Build"));
				}
			}
		});
		inputMap = moduleTree.getInputMap(
			JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		actionMap = moduleTree.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke("ENTER"), "build");
		actionMap.put("build", buildAction);
		actionMap.put("view", editModuleAction);
		// Expand rows bottom-to-top to expand only the root nodes.
		for (int i = moduleTree.getRowCount() - 1; i >= 0; i--)
		{
			moduleTree.expandRow(i);
		}

		// Create the entry points tree.
		entryPointsTree = new JTree(entryPoints);
		entryPointsTree.setToolTipText(
			"All entry points, organized by defining module.");
		entryPointsTree.setComponentPopupMenu(entryPointsPopup.getPopupMenu());
		entryPointsTree.setEditable(false);
		entryPointsTree.setEnabled(true);
		entryPointsTree.setFocusable(true);
		entryPointsTree.getSelectionModel().setSelectionMode(
			TreeSelectionModel.SINGLE_TREE_SELECTION);
		entryPointsTree.setToggleClickCount(0);
		entryPointsTree.setShowsRootHandles(true);
		entryPointsTree.setRootVisible(false);
		entryPointsTree.addTreeSelectionListener(event -> setEnablements());
		entryPointsTree.setCellRenderer(treeRenderer);
		entryPointsTree.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked (final @Nullable MouseEvent e)
			{
				assert e != null;
				if (selectedEntryPoint() != null)
				{
					if (insertEntryPointAction.isEnabled()
						&& e.getClickCount() == 2
						&& e.getButton() == MouseEvent.BUTTON1)
					{
						e.consume();
						final ActionEvent actionEvent = new ActionEvent(
							entryPointsTree, -1, "Insert entry point");
						insertEntryPointAction.actionPerformed(actionEvent);
					}
				}
				else if (selectedEntryPointModule() != null)
				{
					if (buildEntryPointModuleAction.isEnabled()
						&& e.getClickCount() == 2
						&& e.getButton() == MouseEvent.BUTTON1)
					{
						e.consume();
						final ActionEvent actionEvent = new ActionEvent(
							entryPointsTree, -1, "Build entry point module");
						buildEntryPointModuleAction.actionPerformed(
							actionEvent);
					}
				}
			}
		});
		inputMap = entryPointsTree.getInputMap(
			JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		actionMap = entryPointsTree.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke("ENTER"), "build");
		actionMap.put("build", buildAction);
		for (int i = 0; i < entryPointsTree.getRowCount(); i++)
		{
			entryPointsTree.expandRow(i);
		}

		// Create the build progress bar.
		buildProgress = new JProgressBar(0, 1000);
		buildProgress.setToolTipText("Progress indicator for the build.");
		buildProgress.setDoubleBuffered(true);
		buildProgress.setEnabled(false);
		buildProgress.setFocusable(false);
		buildProgress.setIndeterminate(false);
		buildProgress.setStringPainted(true);
		buildProgress.setString("Build Progress:");
		buildProgress.setValue(0);

		// Create the transcript.
		final JLabel outputLabel = new JLabel("Transcript:");

		// Make this row and column be where the excess space goes.
		// And reset the weights...
		transcript = new JTextPane();
		transcript.setBorder(BorderFactory.createEtchedBorder());
		transcript.setComponentPopupMenu(transcriptPopup.getPopupMenu());
		transcript.setEditable(false);
		transcript.setEnabled(true);
		transcript.setFocusable(true);
		transcript.setPreferredSize(new Dimension(0, 500));
		transcriptScrollArea = createScrollPane(transcript);

		// Create the input area.
		inputLabel = new JLabel("Command:");
		inputField = new JTextField();
		inputField.setToolTipText(
			"Enter commands and interact with Avail programs.  Press "
				+ "ENTER to submit.");
		inputField.setAction(new SubmitInputAction(this));
		inputMap = inputField.getInputMap(JComponent.WHEN_FOCUSED);
		actionMap = inputField.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke("UP"), "up");
		actionMap.put("up", retrievePreviousAction);
		inputMap.put(KeyStroke.getKeyStroke("DOWN"), "down");
		actionMap.put("down", retrieveNextAction);
		inputField.setColumns(60);
		inputField.setEditable(true);
		inputField.setEnabled(true);
		inputField.setFocusable(true);

		// Subscribe to module loading events.
		availBuilder.subscribeToModuleLoading(
			(loadedModule, loaded) ->
			{
				assert loadedModule != null;
				moduleTree.repaint();
				if (loadedModule.entryPoints().size() > 0)
				{
					entryPointsTree.repaint();
				}
			});

		// Set up styles for the transcript.
		final StyledDocument doc = transcript.getStyledDocument();
		final SimpleAttributeSet attributes = new SimpleAttributeSet();
		final TabStop[] tabStops = new TabStop[500];
		for (int i = 0; i < tabStops.length; i++)
		{
			tabStops[i] = new TabStop(
				32.0f * (i + 1),
				TabStop.ALIGN_LEFT,
				TabStop.LEAD_NONE);
		}
		final TabSet tabSet = new TabSet(tabStops);
		StyleConstants.setTabSet(attributes, tabSet);
		doc.setParagraphAttributes(0, doc.getLength(), attributes, false);
		final Style defaultStyle =
			StyleContext.getDefaultStyleContext().getStyle(
				StyleContext.DEFAULT_STYLE);
		defaultStyle.addAttributes(attributes);
		StyleConstants.setFontFamily(defaultStyle, "Monospaced");
		for (final StreamStyle style : values())
		{
			style.defineStyleIn(doc);
		}

		// Redirect the standard streams.
		try
		{
			outputStream = new PrintStream(
				new BuildOutputStream(OUT),
				false,
				StandardCharsets.UTF_8.name());
			errorStream = new PrintStream(
				new BuildOutputStream(ERR),
				false,
				StandardCharsets.UTF_8.name());
		}
		catch (final UnsupportedEncodingException e)
		{
			// Java must support UTF_8.
			throw new RuntimeException(e);
		}
		inputStream = new BuildInputStream();
		System.setOut(outputStream);
		System.setErr(errorStream);
		System.setIn(inputStream);
		final TextInterface textInterface = new TextInterface(
			new ConsoleInputChannel(inputStream),
			new ConsoleOutputChannel(outputStream),
			new ConsoleOutputChannel(errorStream));
		runtime.setTextInterface(textInterface);
		availBuilder.setTextInterface(textInterface);

		final JSplitPane leftPane = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT,
			true,
			createScrollPane(moduleTree),
			createScrollPane(entryPointsTree));
		leftPane.setDividerLocation(configuration.moduleVerticalProportion());
		leftPane.setResizeWeight(configuration.moduleVerticalProportion());
		final JPanel rightPane = new JPanel();
		final GroupLayout rightPaneLayout = new GroupLayout(rightPane);
		rightPane.setLayout(rightPaneLayout);
		rightPaneLayout.setAutoCreateGaps(true);
		rightPaneLayout.setHorizontalGroup(
			rightPaneLayout.createParallelGroup()
				.addComponent(buildProgress)
				.addComponent(outputLabel)
				.addComponent(transcriptScrollArea)
				.addComponent(inputLabel)
				.addComponent(inputField));
		rightPaneLayout.setVerticalGroup(
			rightPaneLayout.createSequentialGroup()
				.addGroup(rightPaneLayout.createSequentialGroup()
					.addComponent(
						buildProgress,
						GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addGroup(
					rightPaneLayout.createSequentialGroup()
						.addComponent(outputLabel)
						.addComponent(
							transcriptScrollArea,
							0,
							300,
							Short.MAX_VALUE))
				.addGroup(rightPaneLayout.createSequentialGroup()
					.addComponent(inputLabel)
					.addComponent(
						inputField,
						GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE)));

		final JSplitPane mainSplit = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT, true, leftPane, rightPane);
		mainSplit.setDividerLocation(configuration.leftSectionWidth());
		getContentPane().add(mainSplit);
		pack();
		if (configuration.placement != null)
		{
			setBounds(configuration.placement);
		}

		// Save placement when closing.
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing (final @Nullable WindowEvent e)
			{
				final Preferences preferences =
					placementPreferencesNodeForScreenNames(
						allScreenNames());
				final LayoutConfiguration prevConfiguration =
					new LayoutConfiguration(
						preferences.get(placementLeafKeyString, ""));
				final LayoutConfiguration saveConfiguration =
					new LayoutConfiguration();
				if (prevConfiguration.moduleViewerPlacement != null)
				{
					saveConfiguration.moduleViewerPlacement =
						prevConfiguration.moduleViewerPlacement;
				}

				saveConfiguration.placement = getBounds();
				saveConfiguration.leftSectionWidth =
					mainSplit.getDividerLocation();
				saveConfiguration.moduleVerticalProportion =
					leftPane.getDividerLocation()
						/ max(leftPane.getHeight(), 1.0);
				preferences.put(
					placementLeafKeyString,
					saveConfiguration.stringToStore());
				templatePreferences().put(
					moduleLeafKeyString,
					moduleTemplates.stringToStore());
//				templatePreferences().put(
//					textLeafKeyString,
//					replaceTextTemplate.stringToStore());
				super.windowClosing(e);
			}
		});
		if (runningOnMac)
		{
			OSXUtility.setQuitHandler(
				event ->
				{
					// Quit was pressed.  Close the workbench, which should
					// save window position state then exit.
					// Apple's apple.eawt.quitStrategy has never worked, to
					// the best of my knowledge.  It's a trick.  We must
					// close the workbench window explicitly to give it a
					// chance to save.
					final WindowEvent closeEvent =
						new WindowEvent(
							AvailWorkbench.this,
							WindowEvent.WINDOW_CLOSING);
					dispatchEvent(closeEvent);
					return true;
				});
			OSXUtility.setAboutHandler(
				event ->
				{
					aboutAction.showDialog();
					return true;
				});
		}

		// Select an initial module if specified.
		validate();
		setEnablements();
	}

	/**
	 * Create a menu with the given name and entries, which can be null to
	 * indicate a separator, a JMenuItem, or an Action to wrap in a JMenuItem.
	 */
	private static @Nonnull JMenu menu (
		final String name,
		final Object... actionsAndSubmenus)
	{
		final JMenu menu = new JMenu(name);
		augment(menu, actionsAndSubmenus);
		return menu;
	}

	/**
	 * Augment the given menu with the array of entries, which can be null to
	 * indicate a separator, a JMenuItem, or an Action to wrap in a JMenuItem.
	 */
	private static void augment (final JMenu menu, final Object... actionsAndSubmenus)
	{
		for (final Object item : actionsAndSubmenus)
		{
			if (item == null)
			{
				menu.addSeparator();
			}
			else if (item instanceof Action)
			{
				menu.add((Action) item);
			}
			else if (item instanceof JMenuItem)
			{
				menu.add((JMenuItem) item);
			}
			else
			{
				assert false : "Bad argument while building menu";
			}
		}
	}

	/** Answer the pane wrapped in a JScrollPane. */
	private static @Nonnull JScrollPane createScrollPane (
		final Component innerComponent)
	{
		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setMinimumSize(new Dimension(100, 0));
		scrollPane.setViewportView(innerComponent);
		return scrollPane;
	}

	/**
	 * Make the workbench behave more like a Mac application.
	 */
	private static void setUpForMac ()
	{
		assert runningOnMac;
		try
		{
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty(
				"com.apple.mrj.application.apple.menu.about.name",
				"Avail Workbench");
			System.setProperty(
				"com.apple.awt.graphics.UseQuartz",
				"true");

			final Class<?> appClass = OSXUtility.applicationClass;
			final Object application = OSXUtility.macOSXApplication;
//			final Image image =
//				new ImageIcon(resourcePrefix + "AvailHammer.png").getImage();
//			appClass.getMethod("setDockIconImage", Image.class).invoke(
//				application,
//				image);
			OSXUtility.setDockIconBadgeMethod.invoke(application, "DEV");
//			application.setDockIconBadge("DEV");
		}
		catch (final Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Make the workbench instance behave more like a Mac application.
	 */
	private void setUpInstanceForMac ()
	{
		assert runningOnMac;
		try
		{
			// Set up Mac-specific preferences menu handler...
			OSXUtility.setPreferencesHandler(
				event ->
				{
					preferencesAction.actionPerformed(null);
					return true;
				});
		}
		catch (final Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Launch the {@linkplain AvailBuilder Avail builder} {@linkplain
	 * AvailWorkbench UI}.
	 *
	 * @param args
	 *        The command line arguments.
	 * @throws Exception
	 *         If something goes wrong.
	 */
	public static void main (final String[] args) throws Exception
	{
		if (runningOnMac)
		{
			setUpForMac();
		}

		final String rootsString = System.getProperty("availRoots", "");
		final ModuleRoots roots;
		if (rootsString.isEmpty())
		{
			// Read the persistent preferences file...
			roots = loadModuleRoots();
		}
		else
		{
			// Providing availRoots on the command line overrides preferences...
			roots = new ModuleRoots(rootsString);
		}

		final String renames = System.getProperty("availRenames", null);
		final Reader reader;
		if (renames == null)
		{
			// Load the renames from preferences further down.
			reader = new StringReader("");
		}
		else
		{
			// Load the renames from the file specified on the command line...
			final File renamesFile = new File(renames);
			reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(renamesFile), StandardCharsets.UTF_8));
		}
		final RenamesFileParser renameParser = new RenamesFileParser(
			reader, roots);
		final ModuleNameResolver resolver = renameParser.parse();
		if (renames == null)
		{
			// Now load the rename rules from preferences.
			loadRenameRulesInto(resolver);
		}

		// The first application argument, if any, says which module to select.
		final String initial = args.length > 0 ? args[0] : "";

		// Display the UI.
		invokeLater(() ->
		{
			final AvailWorkbench frame = new AvailWorkbench(resolver);
			if (runningOnMac)
			{
				frame.setUpInstanceForMac();
			}
			frame.setVisible(true);
			frame.refresh();

			// Select an initial module if specified.
			if (!initial.isEmpty())
			{
				final TreePath path = frame.modulePath(initial);
				if (path != null)
				{
					frame.moduleTree.setSelectionPath(path);
					frame.moduleTree.scrollRowToVisible(
						frame.moduleTree.getRowForPath(path));
					frame.setEnablements();
				}
			}

		});
	}
}
