/*
 * L2_LOOKUP_BY_VALUES.java
 * Copyright © 1993-2018, The Avail Foundation, LLC.
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
package com.avail.interpreter.levelTwo.operation;

import com.avail.AvailRuntime;
import com.avail.descriptor.*;
import com.avail.exceptions.AvailException;
import com.avail.exceptions.MethodDefinitionException;
import com.avail.interpreter.Interpreter;
import com.avail.interpreter.levelTwo.L2Instruction;
import com.avail.interpreter.levelTwo.L2Operation;
import com.avail.interpreter.levelTwo.operand.L2PcOperand;
import com.avail.interpreter.levelTwo.operand.L2ReadPointerOperand;
import com.avail.interpreter.levelTwo.operand.L2WritePointerOperand;
import com.avail.interpreter.levelTwo.register.L2ObjectRegister;
import com.avail.optimizer.L2Translator;
import com.avail.optimizer.RegisterSet;
import com.avail.optimizer.jvm.JVMTranslator;
import com.avail.optimizer.jvm.ReferencedInGeneratedCode;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static com.avail.descriptor.AbstractEnumerationTypeDescriptor.enumerationWith;
import static com.avail.descriptor.SetDescriptor.set;
import static com.avail.descriptor.SetDescriptor.setFromCollection;
import static com.avail.descriptor.TypeDescriptor.Types.ANY;
import static com.avail.exceptions.AvailErrorCode.*;
import static com.avail.interpreter.levelTwo.L2NamedOperandType.Purpose.FAILURE;
import static com.avail.interpreter.levelTwo.L2NamedOperandType.Purpose.SUCCESS;
import static com.avail.interpreter.levelTwo.L2OperandType.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * Look up the method to invoke. Use the provided vector of arguments to
 * perform a polymorphic lookup. Write the resulting function into the
 * specified destination register. If the lookup fails, then branch to the
 * specified {@linkplain Interpreter#offset(int) offset}.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public class L2_LOOKUP_BY_VALUES
extends L2Operation
{
	/**
	 * Initialize the sole instance.
	 */
	public static final L2Operation instance =
		new L2_LOOKUP_BY_VALUES().init(
			SELECTOR.is("message bundle"),
			READ_VECTOR.is("arguments"),
			WRITE_POINTER.is("looked up function"),
			WRITE_POINTER.is("error code"),
			PC.is("lookup succeeded", SUCCESS),
			PC.is("lookup failed", FAILURE));

	/**
	 * The error codes that can be produced by a failed lookup.
	 */
	public static final A_Type lookupErrorsType = enumerationWith(
		set(
			E_NO_METHOD,
			E_NO_METHOD_DEFINITION,
			E_AMBIGUOUS_METHOD_DEFINITION,
			E_ABSTRACT_METHOD_DEFINITION,
			E_FORWARD_METHOD_DEFINITION));

	@Override
	protected void propagateTypes (
		final L2Instruction instruction,
		final List<RegisterSet> registerSets,
		final L2Translator translator)
	{
		// Find all possible definitions (taking into account the types of the
		// argument registers).  Then build an enumeration type over those
		// functions.
		final A_Bundle bundle = instruction.bundleAt(0);
		final List<L2ReadPointerOperand> argRegs =
			instruction.readVectorRegisterAt(1);
		final L2WritePointerOperand functionReg =
			instruction.writeObjectRegisterAt(2);
		final L2WritePointerOperand errorCodeReg =
			instruction.writeObjectRegisterAt(3);
//		final int lookupSucceeded = instruction.pcOffsetAt(4);
//		final int lookupFailed = instruction.pcOffsetAt(5);

		// If the lookup fails, then only the error code register changes.
		registerSets.get(0).typeAtPut(
			errorCodeReg.register(), lookupErrorsType, instruction);
		// If the lookup succeeds, then the situation is more complex.
		final RegisterSet registerSet = registerSets.get(1);
		final int numArgs = argRegs.size();
		final List<A_Type> argTypeBounds = new ArrayList<>(numArgs);
		for (final L2ReadPointerOperand argRegister : argRegs)
		{
			final A_Type type = registerSet.hasTypeAt(argRegister.register())
				? registerSet.typeAt(argRegister.register())
				: ANY.o();
			argTypeBounds.add(type);
		}
		// Figure out what could be invoked at runtime given these argument
		// type constraints.
		final List<A_Function> possibleFunctions = new ArrayList<>();
		final List<A_Definition> possibleDefinitions =
			bundle.bundleMethod().definitionsAtOrBelow(argTypeBounds);
		for (final A_Definition definition : possibleDefinitions)
		{
			if (definition.isMethodDefinition())
			{
				possibleFunctions.add(definition.bodyBlock());
			}
		}
		if (possibleFunctions.size() == 1)
		{
			// Only one function could be looked up (it's monomorphic for
			// this call site).  Therefore we know strongly what the
			// function is.
			registerSet.constantAtPut(
				functionReg.register(),
				possibleFunctions.get(0),
				instruction);
		}
		else
		{
			final A_Type enumType =
				enumerationWith(setFromCollection(possibleFunctions));
			registerSet.typeAtPut(
				functionReg.register(), enumType, instruction);
		}
	}

	/**
	 * Perform the lookup.
	 *
	 * @param interpreter
	 *        The {@link Interpreter}.
	 * @param bundle
	 *        The {@link A_Bundle}.
	 * @param values
	 *        The {@linkplain AvailObject values} for the lookup.
	 * @return The unique {@linkplain A_Function function}.
	 * @throws MethodDefinitionException
	 *         If the lookup did not resolve to a unique executable function.
	 */
	@ReferencedInGeneratedCode
	public static A_Function lookup (
			final Interpreter interpreter,
			final A_Bundle bundle,
			final AvailObject[] values)
		throws MethodDefinitionException
	{
		if (Interpreter.debugL2)
		{
			Interpreter.log(
				Interpreter.loggerDebugL2,
				Level.FINER,
				"{0}Lookup {1}",
				interpreter.debugModeString,
				bundle.message().atomName());
		}

		final List<AvailObject> valuesList = new ArrayList<>(values.length);
		Collections.addAll(valuesList, values);

		final A_Method method = bundle.bundleMethod();
		final long before = AvailRuntime.captureNanos();
		final A_Definition definitionToCall;
		try
		{
			definitionToCall = method.lookupByValuesFromList(valuesList);
		}
		finally
		{
			final long after = AvailRuntime.captureNanos();
			interpreter.recordDynamicLookup(bundle, after - before);
		}
		if (definitionToCall.isAbstractDefinition())
		{
			throw MethodDefinitionException.abstractMethod();
		}
		if (definitionToCall.isForwardDefinition())
		{
			throw MethodDefinitionException.forwardMethod();
		}
		return definitionToCall.bodyBlock();
	}

	@Override
	public void translateToJVM (
		final JVMTranslator translator,
		final MethodVisitor method,
		final L2Instruction instruction)
	{
		final A_Bundle bundle = instruction.bundleAt(0);
		final List<L2ReadPointerOperand> argTypeRegs =
			instruction.readVectorRegisterAt(1);
		final L2ObjectRegister functionReg =
			instruction.writeObjectRegisterAt(2).register();
		final L2ObjectRegister errorCodeReg =
			instruction.writeObjectRegisterAt(3).register();
		final int lookupSucceeded = instruction.pcOffsetAt(4);
		final L2PcOperand lookupFailed = instruction.pcAt(5);

		// :: try {
		final Label tryStart = new Label();
		final Label catchStart = new Label();
		method.visitTryCatchBlock(
			tryStart,
			catchStart,
			catchStart,
			getInternalName(MethodDefinitionException.class));
		method.visitLabel(tryStart);
		// ::    function = lookup(interpreter, bundle, types);
		translator.loadInterpreter(method);
		translator.literal(method, bundle);
		translator.objectArray(method, argTypeRegs, AvailObject.class);
		method.visitMethodInsn(
			INVOKESTATIC,
			getInternalName(L2_LOOKUP_BY_VALUES.class),
			"lookup",
			getMethodDescriptor(
				getType(A_Function.class),
				getType(Interpreter.class),
				getType(A_Bundle.class),
				getType(AvailObject[].class)),
			false);
		translator.store(method, functionReg);
		// ::    goto lookupSucceeded;
		// Note that we cannot potentially eliminate this branch with a
		// fall through, because the next instruction expects a
		// MethodDefinitionException to be pushed onto the stack. So always do
		// the jump.
		method.visitJumpInsn(GOTO, translator.labelFor(lookupSucceeded));
		// :: } catch (MethodDefinitionException e) {
		method.visitLabel(catchStart);
		// ::    errorCode = e.numericCode();
		method.visitMethodInsn(
			INVOKEVIRTUAL,
			getInternalName(AvailException.class),
			"numericCode",
			getMethodDescriptor(getType(A_Number.class)),
			false);
		translator.store(method, errorCodeReg);
		// ::    goto lookupFailed;
		translator.branch(method, instruction, lookupFailed);
		// :: }
	}
}
