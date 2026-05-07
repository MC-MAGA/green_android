// Identifies a logical channel for logs. Each bucket maps to its own file via
// FileLogWriterRegistry, so callers can fetch a bucket-scoped Logger from Koin.

package com.blockstream.utils

import co.touchlab.kermit.Severity

enum class LogBucket(val fileName: String, val minSeverity: Severity = Severity.Info) {
    App("app.log"),
    Gdk("gdk.log"),
    Lwk("lwk.log", minSeverity = Severity.Debug),
    Lightning("lightning.log", minSeverity = Severity.Debug),
}
