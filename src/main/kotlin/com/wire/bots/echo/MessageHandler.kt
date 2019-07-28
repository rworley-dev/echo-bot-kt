package com.wire.bots.echo

import com.wire.blender.Blender
import com.wire.bots.echo.Service.Config
import com.wire.bots.sdk.MessageHandlerBase
import com.wire.bots.sdk.Server
import com.wire.bots.sdk.WireClient
import com.wire.bots.sdk.assets.FileAsset
import com.wire.bots.sdk.assets.FileAssetPreview
import com.wire.bots.sdk.models.*
import com.wire.bots.sdk.server.model.NewBot
import com.wire.bots.sdk.server.model.SystemMessage
import com.wire.bots.sdk.tools.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MessageHandler(private val server: Server<Config>) : MessageHandlerBase() {
    override fun onNewBot(newBot: NewBot): Boolean {
        Logger.info("onNewBot: bot: ${newBot.id}, username: ${newBot.origin.handle}")
        return newBot.conversation.members.all {
            it.service?.run {
                Logger.warning("Rejecting NewBot. Provider: $providerId, service: $id")
                return@all false // we don't want to be in a conv if other bots are there
            }
            true
        }
    }

    override fun onNewConversation(client: WireClient, message: SystemMessage) {
        Logger.info("onNewConversation: bot: ${client.id}, conv: ${client.conversationId}")
        try {
            client.sendText("Hello! I am Echo-KT. I echo everything you write")
        } catch (e: Exception) {
            Logger.error("onNewConversation: $e")
        }
    }

    override fun onText(client: WireClient, msg: TextMessage) {
        Logger.info("Received Text. bot: ${client.id}, from: ${msg.userId}, messageId: ${msg.messageId}")
        try {
            val mId = client.sendText("You wrote: ${msg.text}") // send echo back to user
            Logger.info("Text sent back in conversation: ${client.conversationId}, messageId: $mId, bot: ${client.id}")
        } catch (e: Exception) {
            Logger.error("onText: $e")
        }
    }

    override fun onImage(client: WireClient, msg: ImageMessage) {
        Logger.info("Received Image: type: ${msg.mimeType}, size: ${"%,d".format(msg.size / 1024)} KB, "
                + "h: ${msg.height}, w: ${msg.width}, tag: ${msg.tag}")
        try { // download this image from Wire server and echo it back to user
            client.sendPicture(client.downloadAsset(msg.assetKey, msg.assetToken, msg.sha256, msg.otrKey), msg.mimeType)
        } catch (e: Exception) {
            Logger.error("onImage: $e")
        }
    }

    override fun onAudio(client: WireClient, msg: AudioMessage) {
        Logger.info("Received Audio: name: ${msg.name}, type: ${msg.mimeType}, "
                + "size: ${"%,d".format(msg.size / 1024)} KB, duration: ${"%,d".format(msg.duration / 1000)} sec")
        try { // download this audio from Wire Server and echo it back to user
            client.sendAudio(client.downloadAsset(msg.assetKey, msg.assetToken, msg.sha256, msg.otrKey),
                msg.name, msg.mimeType, msg.duration)
        } catch (e: Exception) {
            Logger.error("onAudio: $e")
        }
    }

    override fun onVideo(client: WireClient, msg: VideoMessage) {
        Logger.info("Received Video: name: ${msg.name}, type: ${msg.mimeType}, "
                + "size: ${"%,d".format(msg.size / 1024)} KB, duration: ${"%,d".format(msg.duration / 1000)} sec")
        try { // download this video from Wire Server and echo it back to user
            client.sendVideo(client.downloadAsset(msg.assetKey, msg.assetToken, msg.sha256, msg.otrKey),
                msg.name, msg.mimeType, msg.duration, msg.height, msg.width)
        } catch (e: Exception) {
            Logger.error("onVideo: $e")
        }
    }

    override fun onAttachment(client: WireClient, msg: AttachmentMessage) {
        Logger.info("Received Attachment: name: ${msg.name}, type: ${msg.mimeType}, "
                + "size: ${"%,d".format(msg.size / 1024)} KB")
        try { // echo this file back to user
            val messageId = UUID.randomUUID()
            client.sendDirectFile(FileAssetPreview(msg.name, msg.mimeType, msg.size, messageId),
                FileAsset(msg.assetKey, msg.assetToken, msg.sha256, messageId), msg.userId.toString())
        } catch (e: Exception) {
            Logger.error("onAttachment: $e")
        }
    }

    override fun onMemberJoin(client: WireClient, message: SystemMessage) {
        try {
            client.getUsers(message.users.map { it.toString() }).forEach {
                Logger.info("onMemberJoin: bot: ${client.id}, user: ${it.id}/${it.name} @${it.handle}")
                client.sendText("Hi there ${it.name}") // say Hi to new participant
            }
        } catch (e: Exception) {
            Logger.error("onMemberJoin: $e")
        }
    }

    override fun onMemberLeave(client: WireClient, message: SystemMessage) {
        Logger.info("onMemberLeave: users: ${message.users}, bot: ${client.id}")
    }

    override fun onBotRemoved(botId: UUID, msg: SystemMessage) {
        Logger.info("Bot: $botId got removed from the conversation :(")
    }

    override fun onCalling(client: WireClient, msg: CallingMessage) {
        blender(client.id)?.recvMessage(client.id, msg.userId.toString(), msg.clientId, msg.content)
    }

    private val blenders = ConcurrentHashMap<String, Blender?>()

    private fun blender(botId: String) =
        blenders.computeIfAbsent(botId) {
            try {
                val clientId = server.storageFactory.create(botId).state.client
                Blender().apply {
                    with(server.config) { init(module, botId, clientId, ingress, portMin, portMax) }
                    registerListener(CallListener(server.repo))
                }
            } catch (e: Exception) {
                Logger.error(e.toString())
                null
            }
        }
}
