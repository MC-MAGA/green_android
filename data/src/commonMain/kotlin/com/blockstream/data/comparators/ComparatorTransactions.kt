package com.blockstream.data.comparators

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Transaction

object ComparatorTransactions: Comparator<Transaction> {
    override fun compare(a: Transaction, b: Transaction): Int {
        return when {
            a.blockHeight == 0L && b.blockHeight > 0L -> -1
            b.blockHeight == 0L && a.blockHeight > 0L -> 1
            a.blockHeight == b.blockHeight && a.createdAtTs == b.createdAtTs -> { // if we send to the same account, display first the outgoing tx
                if (a.isIn && b.isOut) {
                    -1
                } else if (b.isIn && a.isOut) {
                    1
                } else {
                    b.createdAtTs.compareTo(a.createdAtTs)
                }
            }

            else -> b.createdAtTs.compareTo(a.createdAtTs)
        }
    }
}