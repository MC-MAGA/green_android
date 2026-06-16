package com.blockstream.gms.services

import com.blockstream.data.config.AppInfo
import com.blockstream.data.extensions.tryCatchNull
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.data.notifications.models.BoltzNotificationSimple
import com.blockstream.data.notifications.models.MeldNotificationData
import com.blockstream.data.notifications.models.MeldNotificationType
import com.blockstream.utils.Loggable
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    val fcm: FcmCommon by inject()
    val appInfo: AppInfo by inject()
    val json: Json by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        logger.d { "Received message: ${remoteMessage.data}" }
        val data = remoteMessage.data

        if (data.isEmpty()) return


        when (data["type"]) {
            "BOLTZ_EVENT" -> {
                tryCatchNull {
                    val boltzNotificationData = BoltzNotificationSimple.create(json, data)
                    fcm.handleBoltzPushNotification(boltzNotificationData)
                }
            }

            "GREENLIGHT_EVENT" -> {
                val xpubHashId = data["wallet_hashed_id"]

                if (!xpubHashId.isNullOrBlank()) {
                    fcm.handleGreenlightPushNotification(xpubHashId = xpubHashId)
                } else {
                    logger.d { "Greenlight push missing wallet_hashed_id $data" }
                }
            }

            else -> {
                val notificationType = data["notification_type"]

                // Lightning Notification
                if (notificationType == "payment_received" || notificationType == "tx_confirmed" || notificationType == "address_txs_confirmed") {
                    val xpubHashId = data["app_data"]

                    if (appInfo.isDevelopmentOrDebug) {
                        fcm.showDebugNotification(
                            title = "Notification Received", message = data.toString()
                        )
                    }

                    if (!xpubHashId.isNullOrBlank()) {
                        fcm.handleLightningPushNotification(xpubHashId)
                    } else {
                        logger.d { "No app_data $data" }
                    }
                } else {
                    //we don't have any other notification types
                    //so for now lets just show it as-is
                    //add notification types for other types
                    val notification = MeldNotificationData.create(remoteMessage.data)

                    if (notification.type == MeldNotificationType.MELD_TRANSACTION) {
                        fcm.showBuyTransactionNotification(notification)
                    }
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        logger.d { "Refreshed token: $token" }
        fcm.setToken(token)
    }

    companion object : Loggable()
}

