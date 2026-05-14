package com.blockstream.data.comparators

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.gdk.GdkSession

class ComparatorEnrichedAssets(private val session: GdkSession): Comparator<EnrichedAsset> {
    override fun compare(a: EnrichedAsset, b: EnrichedAsset): Int {
        val w1 = a.sortWeight(session)
        val w2 = b.sortWeight(session)

        if (w1 != w2) {
            return w2.compareTo(w1)
        }

        return a.name(session).compareTo(b.name(session))
    }
}