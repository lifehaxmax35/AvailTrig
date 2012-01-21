/**
 * com.avail.tools.bootstrap/Resources.java
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

package com.avail.tools.bootstrap;

import java.util.ResourceBundle;
import java.util.regex.Matcher;
import com.avail.annotations.NotNull;
import com.avail.interpreter.Primitive;

/**
 * {@code Resources} centralizes {@linkplain ResourceBundle resource bundle}
 * paths and keys.
 *
 * @author Todd L Smith &lt;anarakul@gmail.com&gt;
 */
final class Resources
{
	/**
	 * The base name of the {@linkplain ResourceBundle resource bundle} that
	 * contains the preamble.
	 */
	public static final @NotNull String preambleBaseName =
		Resources.class.getPackage().getName() + ".Preamble";

	/**
	 * The name of the package that should contain the generated output.
	 */
	public static final @NotNull String generatedPackageName =
		Resources.class.getPackage().getName() + ".generated";

	/**
	 * The base name of the {@linkplain ResourceBundle resource bundle} that
	 * contains the Avail names of the special objects.
	 */
	public static final @NotNull String specialObjectsBaseName =
		generatedPackageName + ".SpecialObjectNames";

	/**
	 * The base name of the target {@linkplain ResourceBundle resource bundle}
	 * that contains the Avail names of the primitives.
	 */
	public static final @NotNull String primitivesBaseName =
		generatedPackageName + ".PrimitiveNames";

	/**
	 * Answer the local name of the specified {@linkplain ResourceBundle
	 * resource bundle} base name.
	 *
	 * @param bundleName A resource bundle base name.
	 * @return The local name, e.g. the name following the last period (.).
	 */
	public static @NotNull String localName (final @NotNull String bundleName)
	{
		return bundleName.substring(bundleName.lastIndexOf('.') + 1);
	}

	/**
	 * Answer the argument, but embedded in double quotes (").
	 *
	 * @param string
	 *        A {@linkplain String string}.
	 * @return The argument embedded in double quotes (").
	 */
	public static @NotNull String stringify (final @NotNull String string)
	{
		return "\"" + string + "\"";
	}

	/**
	 * Answer the key for the special object name given by {@code index}.
	 *
	 * @param index
	 *        The special object index.
	 * @return A key that may be used to access the Avail name of the special
	 *         object in the appropriate {@linkplain ResourceBundle resource
	 *         bundle}.
	 */
	public static @NotNull String specialObjectKey (final int index)
	{
		return "specialObject" + index;
	}

	/**
	 * Answer the key for the purely alphabetic name of the special object given
	 * by {@code index}.
	 *
	 * @param index
	 *        The special object index.
	 * @return A key that may be used to access a purely alphabetic Avail name
	 *         of the special object in the appropriate {@linkplain
	 *         ResourceBundle resource bundle}. This name should be suitable for
	 *         use as a variable name in a system module.
	 */
	public static @NotNull String specialObjectAlphabeticKey (final int index)
	{
		return specialObjectKey(index) + "_alphabetic";
	}

	/**
	 * Answer the key for the specified special object's comment.
	 *
	 * @param index
	 *        The special object index.
	 * @return A key that may be used to access the special object's comment in
	 *         the appropriate {@linkplain ResourceBundle resource bundle}.
	 */
	public static @NotNull String specialObjectCommentKey (final int index)
	{
		return specialObjectKey(index) + "_comment";
	}

	/**
	 * Answer the key for the {@code index}-th parameter name of the specified
	 * {@linkplain Primitive primitive}.
	 *
	 * @param primitive
	 *        A primitive.
	 * @param index
	 *        The parameter ordinal.
	 * @return A key that may be used to access the name of the primitive's
	 *         {@code index}-th parameter in the appropriate {@linkplain
	 *         ResourceBundle resource bundle}.
	 */
	public static @NotNull String primitiveParameterNameKey (
		final @NotNull Primitive primitive,
		final int index)
	{
		return primitive.name() + "_" + (index - 1);
	}

	/**
	 * Answer the key for the specified {@linkplain Primitive primitive}'s
	 * comment.
	 *
	 * @param primitive
	 *        A primitive.
	 * @return A key that may be used to access the primitive method's comment
	 *         in the appropriate {@linkplain ResourceBundle resource bundle}.
	 */
	public static @NotNull String primitiveCommentKey (
		final @NotNull Primitive primitive)
	{
		return primitive.name() + "_comment";
	}

	/**
	 * Escape line feed characters in the argument.
	 *
	 * @param propertyValue
	 *        A property value that will be written to a properties file.
	 * @return An appropriately escaped property value.
	 */
	public static @NotNull String escape (final @NotNull String propertyValue)
	{
		String newValue = propertyValue.replace("\n", "\\n\\\n");
		if (newValue.indexOf('\n') != newValue.lastIndexOf('\n'))
		{
			newValue = "\\\n" + newValue;
		}
		if (newValue.endsWith("\\n\\\n"))
		{
			newValue = newValue.substring(0, newValue.length() - 2);
		}
		newValue = newValue.replace("\n ", "\n\\ ");
		newValue = newValue.replaceAll(
			"\\\\(?!n|\\n| )", Matcher.quoteReplacement("\\\\"));
		return newValue;
	}

	@SuppressWarnings("all")
	public static enum Key
	{
		propertiesCopyright,
		generatedPropertiesNotice,
		availCopyright,
		methodCommentTemplate,
		methodCommentParametersTemplate,
		methodCommentParameterTemplate,
		methodCommentReturnsTemplate,
		generatedModuleNotice,
		originModuleName,
		originModuleHeader,
		specialObjectsModuleName,
		primitivesModuleName,
		infalliblePrimitivesModuleName,
		falliblePrimitivesModuleName,
		primitivesModuleHeader,
		bootstrapDefiningMethod,
		bootstrapSpecialObject,
		definingMethodUse,
		specialObjectUse,
		parameterPrefix,
		primitiveKeyword,
		primitiveFailureMethod,
		primitiveFailureMethodUse,
		primitiveFailureVariableName,
		primitiveFailureFunctionName,
		primitiveFailureFunctionSetterMethod,
		invokePrimitiveFailureFunctionMethod,
		invokePrimitiveFailureFunctionMethodUse
	}
}