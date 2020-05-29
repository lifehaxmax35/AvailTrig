/*
 * AvailTask.java
 * Copyright © 1993-2019, The Avail Foundation, LLC.
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

package com.avail;

import com.avail.descriptor.fiber.A_Fiber;
import com.avail.descriptor.fiber.FiberDescriptor;
import com.avail.descriptor.fiber.FiberDescriptor.ExecutionState;
import com.avail.descriptor.representation.AvailObject;
import com.avail.exceptions.PrimitiveThrownException;
import com.avail.interpreter.execution.Interpreter;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import javax.annotation.Nullable;

import static com.avail.descriptor.fiber.FiberDescriptor.ExecutionState.ABORTED;
import static com.avail.descriptor.fiber.FiberDescriptor.ExecutionState.RETIRED;
import static com.avail.descriptor.fiber.FiberDescriptor.ExecutionState.RUNNING;
import static com.avail.descriptor.fiber.FiberDescriptor.ExecutionState.SUSPENDED;
import static com.avail.descriptor.fiber.FiberDescriptor.ExecutionState.TERMINATED;
import static com.avail.descriptor.fiber.FiberDescriptor.SynchronizationFlag.BOUND;
import static com.avail.descriptor.fiber.FiberDescriptor.SynchronizationFlag.SCHEDULED;

/**
 * An {@code AvailTask} extends {@link Runnable} with a priority. Instances are
 * intended to be executed only by {@linkplain AvailThread Avail threads}.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
public final class AvailTask
implements Comparable<AvailTask>, Runnable
{
	/** The action to perform for this task. */
	private final Function0<Unit> body;

	/**
	 * The priority of the {@linkplain AvailTask task}.  It must be a value in
	 * the range 0..255.
	 *
	 * @see #quasiDeadline
	 */
	public final int priority;

	/**
	 * The quasi-deadline of the task.  This is the moment that the task can no
	 * longer be preceded by a new task.  Given a priority in the range 0..255,
	 * the quasi-deadline is System.currentTimeMillis() plus a time delta.  The
	 * delta is 1000ms * (1 - (priority / 256)).  That places it one second in
	 * the future for priority = 0, and places it essentially at the current
	 * moment for priority = 255.
	 */
	private final long quasiDeadline;

	/**
	 * Construct a new {@code AvailTask}.
	 *
	 * @param priority
	 *        The desired priority, a long tied to milliseconds since the
	 *        current epoch.
	 * @param body
	 *        The action to execute for this task.
	 */
	public AvailTask (
		final int priority,
		final Function0<Unit> body)
	{
		assert priority >= 0 && priority <= 255;
		this.priority = priority;
		final long deltaNanos = (1_000_000_000L * (255 - priority)) >> 8;
		this.quasiDeadline = System.nanoTime() + deltaNanos;
		this.body = body;
	}

	/**
	 * Answer an {@code AvailTask} suitable for resuming the specified
	 * {@linkplain FiberDescriptor fiber} using the specified action. If the
	 * continuation fails for any reason, then it {@linkplain
	 * Interpreter#abortFiber() aborts} the fiber and invokes the {@linkplain
	 * AvailObject#failureContinuation() failure continuation} with the terminal
	 * {@linkplain Throwable throwable}.
	 *
	 * @param fiber
	 *        A fiber.
	 * @param body
	 *        What to do to resume execution of the fiber.
	 * @return An action that sets the execution state of the fiber to
	 *         {@linkplain ExecutionState#RUNNING running}, binds it to the
	 *         running {@linkplain AvailThread thread}'s {@linkplain Interpreter
	 *         interpreter}, and then runs the specified continuation.
	 */
	public static Function0<Unit> forFiberResumption (
		final A_Fiber fiber,
		final Function0<Unit> body)
	{
		assert fiber.executionState().indicatesSuspension();
		final boolean scheduled =
			fiber.getAndSetSynchronizationFlag(SCHEDULED, true);
		assert !scheduled;
		return () ->
		{
			final Interpreter interpreter = Interpreter.current();
			assert interpreter.fiberOrNull() == null;
			fiber.lock(() ->
			{
				assert fiber.executionState().indicatesSuspension();
				final boolean bound =
					fiber.getAndSetSynchronizationFlag(BOUND, true);
				assert !bound;
				final boolean wasScheduled =
					fiber.getAndSetSynchronizationFlag(SCHEDULED, false);
				assert wasScheduled;
				fiber.executionState(RUNNING);
				interpreter.fiber(fiber, "forFiberResumption");
				return Unit.INSTANCE;
			});
			try
			{
				body.invoke();
			}
			catch (final PrimitiveThrownException e)
			{
				// If execution failed, terminate the fiber and invoke its
				// failure continuation with the throwable.
				interpreter.adjustUnreifiedCallDepthBy(
					-interpreter.unreifiedCallDepth());
				if (!fiber.executionState().indicatesTermination())
				{
					assert interpreter.fiberOrNull() == fiber;
					interpreter.abortFiber();
				}
				else
				{
					fiber.executionState(ABORTED);
				}
				fiber.failureContinuation().invoke(e);
				fiber.executionState(RETIRED);
				interpreter.runtime().unregisterFiber(fiber);
			}
			catch (final Throwable e)
			{
				System.err.println("An unrecoverable VM error has occurred.");
				throw e;
			}
			finally
			{
				// This is the first point at which *some other* Thread may have
				// had a chance to resume the fiber and update its state.
				final @Nullable Function0<Unit> postExit =
					interpreter.getPostExitContinuation();
				if (postExit != null)
				{
					interpreter.postExitContinuation(null);
					postExit.invoke();
				}
			}
			// If the fiber has terminated, then report its result via its
			// result continuation.
			fiber.lock(() ->
			{
				if (fiber.executionState() == TERMINATED)
				{
					fiber.resultContinuation().invoke(fiber.fiberResult());
					fiber.executionState(RETIRED);
					interpreter.runtime().unregisterFiber(fiber);
				}
				return Unit.INSTANCE;
			});
			assert interpreter.fiberOrNull() == null;
			return Unit.INSTANCE;
		};
	}

	/**
	 * Answer an {@code AvailTask} suitable for performing activities on behalf
	 * of an unbound {@linkplain ExecutionState#SUSPENDED} {@linkplain
	 * FiberDescriptor fiber}. If the continuation fails for any reason, then it
	 * transitions the fiber to the {@linkplain ExecutionState#ABORTED aborted}
	 * state and invokes the fiber's {@linkplain
	 * AvailObject#failureContinuation() failure continuation} with the terminal
	 * {@linkplain Throwable throwable}.
	 *
	 * @param fiber
	 *        A fiber.
	 * @param continuation
	 *        What to do to on behalf of the unbound fiber.
	 * @return An action that runs the provided continuation and handles any
	 *         errors appropriately.
	 */
	public static Function0<Unit> forUnboundFiber (
		final A_Fiber fiber,
		final Function0<Unit> continuation)
	{
		assert fiber.executionState() == SUSPENDED;
		final boolean scheduled =
			fiber.getAndSetSynchronizationFlag(SCHEDULED, true);
		assert !scheduled;
		return () ->
		{
			final boolean wasScheduled =
				fiber.getAndSetSynchronizationFlag(SCHEDULED, false);
			assert wasScheduled;
			try
			{
				continuation.invoke();
			}
			catch (final Throwable e)
			{
				// If execution failed for any reason, then terminate the
				// fiber and invoke its failure continuation with the
				// throwable.
				fiber.executionState(ABORTED);
				fiber.failureContinuation().invoke(e);
			}
			assert Interpreter.current().fiberOrNull() == null;
			return Unit.INSTANCE;
		};
	}

	@Override
	public int compareTo (final @Nullable AvailTask other)
	{
		assert other != null;
		// Note that using Long.compare(long) would deal with counter overflow
		// in a way we don't want.  Instead, assume a task doesn't sit in the
		// queue for a century or two(!), and use a technically non-transitive
		// comparison operation, checking if the difference is negative.
		// noinspection SubtractionInCompareTo
		return Long.compare(quasiDeadline - other.quasiDeadline, 0);
	}

	@Override
	public void run ()
	{
		try
		{
			body.invoke();
		}
		catch (final Throwable e)
		{
			// Report the exception immediately, then suppress the error.  If we
			// allowed the error to propagate, it would cause an Interpreter to
			// be silently lost, and the next creation could overflow the
			// maximum number of Interpreters.
			System.err.println("Unexpected internal failure in AvailTask:\n");
			e.printStackTrace();
		}
	}
}
