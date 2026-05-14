package com.blockstream.domain.account

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasHistoryUseCaseTest {

    @Test
    fun positiveResultIsCached_computeRunsOnce() = runTest {
        val useCase = HasHistoryUseCase()
        var computeCount = 0

        val first = useCase.cachedHasHistory(key = "wallet:network:0") {
            computeCount++
            true
        }
        val second = useCase.cachedHasHistory(key = "wallet:network:0") {
            computeCount++
            true
        }

        assertTrue(first)
        assertTrue(second)
        assertEquals(1, computeCount)
    }

    @Test
    fun negativeResultIsNotCached_computeRunsEachTime() = runTest {
        val useCase = HasHistoryUseCase()
        var computeCount = 0

        val first = useCase.cachedHasHistory(key = "wallet:network:0") {
            computeCount++
            false
        }
        val second = useCase.cachedHasHistory(key = "wallet:network:0") {
            computeCount++
            false
        }

        assertFalse(first)
        assertFalse(second)
        assertEquals(2, computeCount)
    }

    @Test
    fun differentKeysAreCachedIndependently() = runTest {
        val useCase = HasHistoryUseCase()
        var computeCount = 0

        useCase.cachedHasHistory(key = "wallet:network:0") {
            computeCount++
            true
        }
        useCase.cachedHasHistory(key = "wallet:network:1") {
            computeCount++
            true
        }

        assertEquals(2, computeCount)
    }
}