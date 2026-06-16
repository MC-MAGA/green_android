package com.blockstream.green.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.blockstream.data.extensions.logException
import com.blockstream.data.fcm.FcmCommon
import com.blockstream.green.managers.NotificationManagerAndroid
import com.blockstream.utils.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LightningWork(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), KoinComponent {

    private val firebase: FcmCommon by inject()
    private val notificationManager: NotificationManagerAndroid by inject()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = notificationManager.createLightningForegroundServiceNotification(context)
        return ForegroundInfo(id.hashCode(), notification)
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.Default + logException()) {
            val walletId = inputData.getString(WALLET_ID)

            if (!walletId.isNullOrBlank()) {
                // Signer background-wake is disabled until Greenlight auto-delivers the payment
                // notification hook (LSP-driven node wake). Until then an idle node never receives the
                // payment, so the woken signer has nothing to co-sign. The signer path is verified
                // working. Re-enable the call below once Greenlight supports the notification hook.
                // firebase.doLightningBackgroundWork(walletId)
                Result.success()
            } else {
                logger.d { "Failed to doWork, no walletId" }
                Result.failure()
            }
        }
    }

    companion object : Loggable() {
        private val TAG = LightningWork::class.java.simpleName

        private const val WALLET_ID = "WALLET_ID"

        fun create(walletId: String, context: Context) {

            val work = OneTimeWorkRequestBuilder<LightningWork>().addTag(TAG).setInputData(
                workDataOf(
                    WALLET_ID to walletId
                )
            ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName = "$TAG-$walletId", existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE, request = work
            )
        }
    }
}
