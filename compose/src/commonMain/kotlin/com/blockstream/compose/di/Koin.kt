package com.blockstream.compose.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.chunked
import co.touchlab.kermit.koin.KermitKoinLogger
import co.touchlab.kermit.koin.kermitLoggerModule
import co.touchlab.kermit.platformLogWriter
import com.blockstream.compose.navigation.NavigateToWallet
import com.blockstream.data.btcpricehistory.btcPriceHistoryModule
import com.blockstream.data.config.AppInfo
import com.blockstream.data.data.AppConfig
import com.blockstream.data.di.commonModule
import com.blockstream.data.di.commonModules
import com.blockstream.data.di.platformModule
import com.blockstream.domain.domainModule
import com.blockstream.utils.FileLogWriterRegistry
import com.blockstream.utils.LogBucket
import com.blockstream.utils.Loggable.Companion.COMBINED_LOG_QUALIFIER
import com.blockstream.utils.Loggable.Companion.FILE_LOG_QUALIFIER
import okio.Path.Companion.toPath
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun initKoin(appInfo: AppInfo, appConfig: AppConfig, vararg appModules: Module): KoinApplication {

    val minSeverity = if (appConfig.isDebug) Severity.Debug else Severity.Info

    // Set minSeverity to Global Logger
    Logger.setMinSeverity(minSeverity)
    Logger.setLogWriters(platformLogWriter().chunked())

    val koinApplication = startKoin {
        logger(
            KermitKoinLogger(Logger.withTag("Koin"))
        )

        modules(
            module {
                single {
                    appInfo
                }
                singleOf(::NavigateToWallet)
            },
            kermitLoggerModule(Logger),
            module {
                single {
                    FileLogWriterRegistry(logsDir = "${appConfig.filesDir}/logs".toPath())
                }
                factory<Logger>(named(FILE_LOG_QUALIFIER)) { (tag: String, bucket: LogBucket) ->
                    Logger(
                        config = StaticConfig(
                            minSeverity = minSeverity,
                            logWriterList = listOf(
                                get<FileLogWriterRegistry>().forBucket(bucket),
                            ),
                        ),
                        tag = tag,
                    )
                }
                factory<Logger>(named(COMBINED_LOG_QUALIFIER)) { (tag: String, bucket: LogBucket) ->
                    Logger(
                        config = StaticConfig(
                            minSeverity = minSeverity,
                            logWriterList = listOf(
                                platformLogWriter().chunked(),
                                get<FileLogWriterRegistry>().forBucket(bucket),
                            ),
                        ),
                        tag = tag,
                    )
                }
            },

            )
        modules(*appModules)
        modules(domainModule)
        modules(commonModules(appConfig))
        modules(commonModule)
        modules(platformModule)
        modules(btcPriceHistoryModule)
    }

    val koin = koinApplication.koin

    val logger = koin.get<Logger> { parametersOf(null) }
    val appInfo = koin.get<AppInfo>()
    logger.v { "Green: version: ${appInfo.version}" }

    return koinApplication
}