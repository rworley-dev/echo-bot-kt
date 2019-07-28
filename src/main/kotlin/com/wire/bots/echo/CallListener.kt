package com.wire.bots.echo

import com.wire.blender.BlenderListener
import com.wire.bots.sdk.ClientRepo
import com.wire.bots.sdk.tools.Logger
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor

class CallListener(private val repo: ClientRepo) : BlenderListener {
    private val executor = ScheduledThreadPoolExecutor(1)

    override fun onCallingMessage(id: String, userId: String, clientId: String, peerId: String, peerClientId: String,
                                  content: String, trans: Boolean) {
        Logger.info(
            "id: $id, user: ($userId-$clientId), peer: ($peerId-$peerClientId), content: $content, transient: $trans")
        executor.execute {
            try {
                repo.getClient(UUID.fromString(id)).use { it.call(content) }
            } catch (e: Exception) {
                Logger.error("onCallingMessage: $e")
            }
        }
    }
}
