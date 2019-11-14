/*
 * Disconnect.kt
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

package com.avail.server.io

import com.avail.server.messages.Message

/**
 * A `DisconnectOrigin` is an enum that specifies whether it was the client or
 * the server to disconnect the [AvailServerChannel].
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
enum class DisconnectOrigin
{
	/** The client disconnected the connection */
	CLIENT_ORIGIN,

	/** The server disconnected the connection. */
	SERVER_ORIGIN
}

/**
 * The `DisconnectReason` is an interface that defines the behavior for
 * providing a reason for a disconnected [AvailServerChannel].
 *
 * All negative [DisconnectReason.code]s are reserved for system use. Nonzero
 * `codes` are reserved for application specific implementations.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
interface DisconnectReason
{
	/**
	 * The [DisconnectReason] that describes why the channel was closed.
	 */
	val origin: DisconnectOrigin

	/**
	 * A code that represents the [DisconnectReason] in a compact form.
	 *
	 * All negative [DisconnectReason.code]s are reserved for system use.
	 * Nonzero `codes` are reserved for application specific implementations.
	 */
	val code: Int
}

/**
 * The `UnspecifiedDisconnectReason` is the [DisconnectReason] provided for a
 * close when no explicit `DisconnectReason` was supplied.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
object UnspecifiedDisconnectReason: DisconnectReason
{
	override val origin get () = DisconnectOrigin.SERVER_ORIGIN
	override val code get () = -1
}

/**
 * A `ClientDisconnect` is a [DisconnectReason] that specifies that the
 * disconnect originated from the client.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
object ClientDisconnect: DisconnectReason
{
	override val origin get () = DisconnectOrigin.CLIENT_ORIGIN
	override val code get () = -2
}

/**
 * A `HeartbeatFailureDisconnect` is a [DisconnectReason] that specifies that
 * the disconnect originated from the server due to failure to receive any
 * heartbeat responses from the client.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
object HeartbeatFailureDisconnect: DisconnectReason
{
	override val origin get () = DisconnectOrigin.SERVER_ORIGIN
	override val code get () = -3
}

/**
 * A `ServerMessageDisconnect` is a [DisconnectReason] that specifies that the
 * disconnect originated from the server due to a [Message.closeAfterSending]
 * being `true`.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
object ServerMessageDisconnect: DisconnectReason
{
	override val origin get () = DisconnectOrigin.SERVER_ORIGIN
	override val code get () = -4
}