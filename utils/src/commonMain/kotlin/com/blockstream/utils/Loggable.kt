package com.blockstream.utils

import co.touchlab.kermit.Logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

abstract class Loggable(tag: String? = null, bucket: LogBucket? = LogBucket.App) : KoinComponent {

    val logger: Logger by inject(qualifier = bucket?.let { named(COMBINED_LOG_QUALIFIER) }) {
        val resolvedTag = tag ?: this::class.qualifiedName?.removeSuffix(".Companion")?.splitToSequence('.')?.lastOrNull() ?: "Loggable"
        if (bucket != null) parametersOf(resolvedTag, bucket) else parametersOf(resolvedTag)
    }

    companion object {
        const val FILE_LOG_QUALIFIER = "file_log"
        const val COMBINED_LOG_QUALIFIER = "combined_log"
    }
}
