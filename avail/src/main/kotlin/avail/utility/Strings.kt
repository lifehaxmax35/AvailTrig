/*
 * Strings.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
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
package avail.utility

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.util.Formatter
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * `Strings` provides various string utilities.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
object Strings
{
	/**
	 * Produce an escaped variant of the specified [string][String].
	 *
	 * @param s
	 *   An arbitrary string.
	 * @return
	 *   An escaped string that is suitable for use as a literal in Java source.
	 */
	fun escape(s: String): String = buildString {
		append('"')
		var i = 0
		while (i < s.length)
		{
			val codePoint = s.codePointAt(i)
			when (codePoint)
			{
				'\\'.code -> append("\\\\")
				'"'.code -> append("\\\"")
				'\b'.code -> append("\\b")
				'\n'.code -> append("\\n")
				'\r'.code -> append("\\r")
				'\t'.code -> append("\\t")
				else -> appendCodePoint(codePoint)
			}
			i += Character.charCount(codePoint)
		}
		append('"')
	}

	/**
	 * Add line numbers to the given string.  Start the numbering at the
	 * specified value.
	 *
	 * @param source
	 *   The string to add line numbers to.
	 * @param pattern
	 *   The pattern to use on each line.  The first pattern argument is the
	 *   line number (int), and the second is the String containing the line,
	 *   including a terminal '\n'.
	 * @param startingLineNumber
	 *   What to number the first line as.
	 * @return The string containing line numbers.
	 */
	fun addLineNumbers(
		source: String,
		pattern: String,
		startingLineNumber: Int
	): String {
		Formatter().use { formatter ->
			var line = startingLineNumber
			var position = 0
			while (position < source.length) {
				var nextStart = source.indexOf('\n', position)
				nextStart = if (nextStart == -1) source.length
					else nextStart + 1
				formatter.format(
					pattern, line, source.substring(position, nextStart))
				position = nextStart
				line++
			}
			return formatter.toString()
		}
	}

	/** Strings containing a reasonably small number of tabs. */
	private val tabs = Array(10) {
		i -> buildString {
			repeat(i) { append('\t') }
		}
	}

	/**
	 * Answer a String containing the specified number of tabs.
	 *
	 * @param indent The number of tabs.
	 * @return The string.
	 */
	fun tabs(indent: Int): String {
		if (indent < tabs.size) return tabs[indent]
		val builder = StringBuilder(indent)
		for (i in 1..indent) {
			builder.append('\t')
		}
		return builder.toString()
	}

	/**
	 * Append the specified number of tab ('\t') characters to the receiver, a
	 * [StringBuilder].
	 *
	 * @receiver A [StringBuilder].
	 * @param indent The number of tabs to append.
	 */
	fun StringBuilder.tab(
		indent: Int
	) {
		for (i in 1..indent) {
			append('\t')
		}
	}

	/**
	 * Append a newline ('\n' = U+000A) then the specified number of tab ('\t' =
	 * U+0009) characters to the given [StringBuilder].
	 *
	 * @receiver A [StringBuilder].
	 * @param indent The number of tabs to append after the newline.
	 */
	fun StringBuilder.newlineTab(
		indent: Int
	) : Unit = append('\n').tab(indent)

	/**
	 * Answer a [String] consisting of [count] repetitions of [string],
	 * concatenated.
	 */
	fun repeated (string: String, count: Int): String
	{
		assert(count >= 0)
		return buildString(string.length * count) {
			repeat(count) {
				append(string)
			}
		}
	}

	/** A regex [Pattern] containing just a line break. */
	val lineBreakPattern: Pattern = Pattern.compile("\n", Pattern.LITERAL)

	/**
	 * Increase the indentation by the given non-negative amount.  The first
	 * line of the string (prior to the first line break) is not affected.
	 *
	 * @param originalString
	 * The [String] to adjust.
	 * @param increasedIndentation
	 * The number of additional tabs ( 0) to add after each line break.
	 * @return The newly indented string.
	 */
	fun increaseIndentation(
		originalString: String,
		increasedIndentation: Int
	): String {
		assert(increasedIndentation >= 0)
		return if (increasedIndentation == 0)
		{
			originalString
		}
		else
		{
			lineBreakPattern
				.matcher(originalString)
				.replaceAll(
					Matcher.quoteReplacement("\n" + tabs(increasedIndentation)))
		}
	}

	/**
	 * Answer the stringification of the [stack][StackTraceElement] for the
	 * specified [exception][Throwable].
	 *
	 * @param e A [Throwable].
	 * @return The stringification of the stack trace.
	 */
	fun traceFor(e: Throwable): String {
		return try
		{
			val traceBytes = ByteArrayOutputStream()
			val trace = PrintStream(
				traceBytes, true, StandardCharsets.UTF_8.name())
			e.printStackTrace(trace)
			String(traceBytes.toByteArray(), StandardCharsets.UTF_8)
		}
		catch (x: UnsupportedEncodingException)
		{
			assert(false) { "This never happens!" }
			throw RuntimeException(x)
		}
	}

	/**
	 * With the given StringBuilder, append the prefix, run the body, and append
	 * the suffix.  Attempt to append the suffix even if the body fails.
	 *
	 * @param prefix
	 *   The first string to append.
	 * @param suffix
	 *   The last string to append.
	 * @param body
	 *   The body function that produces the middle part.
	 */
	fun StringBuilder.wrap(
		prefix: String,
		suffix: String,
		body: StringBuilder.()->Unit)
	{
		append(prefix)
		try
		{
			body()
		}
		finally
		{
			append(suffix)
		}
	}

	/**
	 * Output an XML tag with the given tag name and attributes supplied as
	 * [Pair]s.  The values of the attributes will be escaped and quoted.  Then
	 * run the body function and output a matching close tag.  If at exception
	 * is thrown by the body, still attempt to output the close tag.
	 */
	fun StringBuilder.tag(
		tag: String,
		vararg attributes: Pair<String, String>,
		body: StringBuilder.()->Unit)
	{
		append("<")
		append(tag)
		attributes.forEach { (key, value) ->
			append(" $key=${escape(value)}" )
		}
		append(">")
		try
		{
			body()
		}
		finally
		{
			append("</$tag>")
		}
	}

	/**
	 * Find the characters of [abbreviation] within the receiver, in the order
	 * specified by [abbreviation] but not necessarily contiguously.
	 *
	 * @param abbreviation
	 *   The abbreviation.
	 * @param allowPartials
	 *   Whether to allow matches that do not account for every character of the
	 *   abbreviation. Defaults to `false`.
	 * @param ignoreCase
	 *   Whether to ignore case. Defaults to `false`.
	 * @return
	 *   The positions within the receiver where matching characters occurred.
	 *   Empty if the abbreviation does not match completely.
	 */
	fun String.matchesAbbreviation(
		abbreviation: String,
		allowPartials: Boolean = false,
		ignoreCase: Boolean = false
	): List<Pair<Int, Int>>
	{
		if (!allowPartials && abbreviation.length > length)
		{
			// The abbreviation is longer than the qualified name, so even on
			// its face it can't match.
			return listOf()
		}
		// The logic is a bit messy because it has been hand optimized for
		// speed. The algorithm is straightforward though — walk the target
		// string and the search string together, increment the target string on
		// each iteration, and increment the search pointer only when a code
		// point hit occurs.
		val normalize =
			if (ignoreCase) { s: String -> s.lowercase() }
			else { s: String -> s }
		val search = normalize(abbreviation)
		val searchLength = search.length
		val target = normalize(this)
		val targetLength = target.length
		val matches = mutableListOf<Pair<Int, Int>>()
		var start: Int? = null
		var scan = 0
		var next = search.codePointAt(0)
		var i = 0
		var matchLength = 0
		while (true)
		{
			val c = target.codePointAt(i)
			if (next == c)
			{
				if (start === null)
				{
					start = i
				}
				scan++
				if (scan >= searchLength)
				{
					matches.add(start to (i + 1))
					matchLength += i - start + 1
					break
				}
				next = search.codePointAt(scan)
			}
			else if (start !== null)
			{
				matches.add(start to i)
				matchLength += i - start
				start = null
			}
			i += Character.charCount(c)
			if (i >= targetLength)
			{
				break
			}
		}
		return (
			if (allowPartials || matchLength == searchLength) matches
			else listOf())
	}

	/**
	 * If the receiver is more than [limit] characters, truncate it and append
	 * the [ellipsis], ensuring the resulting string does not exceed the limit.
	 * The limit must be at least the length of the ellipsis.
	 */
	fun String.truncateTo(limit: Int, ellipsis: String = "…"): String
	{
		assert(limit >= ellipsis.length)
		return when {
			length <= limit -> this
			else -> substring(0, limit - ellipsis.length) + ellipsis
		}
	}

	/**
	 * A map translating [Char]s to their entity-escaped forms, suitable for use
	 * in HTML text.
	 */
	private val escapesForHTML = mapOf(
		'&' to "&amp;",
		'<' to "&lt;",
		'>' to "&gt;",
		'"' to "&quot;",
		'\'' to "&#x27;",
		'/' to "&#x2F;")

	/**
	 * Convert a string into HTML-escaped form, allowing the original string to
	 * be presented literally in a web browser or Swing component expecting
	 * HTML text.
	 *
	 * @receiver
	 *   The string to escape.
	 * @return
	 *   The HTML-escaped string.
	 */
	fun String.escapedForHTML() = buildString {
		this@escapedForHTML.forEach { char ->
			when (val transformed = escapesForHTML[char])
			{
				null -> append(char)
				else -> append(transformed)
			}
		}
	}

	/**
	 * Build a Unicode box whose contents are populated by the specified
	 * [builder] function. Only top and bottom borders are drawn.
	 *
	 * @param title
	 *   The title of the box, if any. Defaults to `""`, i.e., untitled.
	 * @param borderColumns
	 *   The number of columns for the top and bottom borders.
	 * @param builder
	 *   How to populate the box with text.
	 * @return
	 *   The Unicode box.
	 */
	fun buildUnicodeBox(
		title: String = "",
		borderColumns: Int = 80,
		builder: StringBuilder.()->Unit
	) = buildString {
		if (title.isEmpty())
		{
			append("┏")
			append("━".repeat(borderColumns - 2))
			append("┓\n")
		}
		else
		{
			// The magic constant is an adjustment for padding around the title.
			val totalBorder = borderColumns - title.length - 2
			val prologue = totalBorder / 2
			val epilogue = prologue + totalBorder % 2
			append("┏")
			append("━".repeat(prologue - 1))
			append(" $title ")
			append("━".repeat(epilogue - 1))
			append("┓\n")
		}
		builder()
		if (this[lastIndex] != '\n') append('\n')
		append("┗")
		append("━".repeat(borderColumns - 2))
		append("┛\n")
	}
}
