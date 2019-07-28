package com.wire.bots.echo

import com.wire.bots.sdk.server.model.*
import com.wire.bots.sdk.server.model.Service
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageHandlerTest {
    private val handler = MessageHandler(mockk())

    @Test
    fun `onNewBot, no other bots in conversation, returns true`() {
        assertTrue { handler.onNewBot(newBot()) }
    }

    @Test
    fun `onNewBot, other bots in conversation, returns false`() {
        assertFalse { handler.onNewBot(newBot(Service())) }
    }

    private fun newBot(other: Service? = null) = NewBot().apply {
        origin = User()
        conversation = Conversation().apply { members = listOf(Member().apply { service = other }) }
    }
}
