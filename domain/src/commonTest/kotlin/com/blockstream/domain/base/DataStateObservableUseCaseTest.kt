package com.blockstream.domain.base

import com.blockstream.data.data.DataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataStateObservableUseCaseTest {

    private class TestUseCase(private val canExecute: () -> Boolean) :
        DataStateObservableUseCase<Unit, Int>() {

        var workCount = 0
            private set

        override fun shouldExecute(params: Unit): Boolean = canExecute()

        override suspend fun doAsyncWork(params: Unit) {
            workCount++
            set(workCount)
        }

        override fun createObservable(params: Unit): Flow<DataState<Int>> = get()
    }

    @Test
    fun doAsyncWorkIsSkippedWhenShouldExecuteIsFalse() = runTest {
        val useCase = TestUseCase(canExecute = { false })

        useCase.invoke(Unit)

        assertEquals(0, useCase.workCount)
        assertTrue(useCase.getCurrent().isLoading())
    }

    @Test
    fun doAsyncWorkRunsWhenShouldExecuteIsTrue() = runTest {
        val useCase = TestUseCase(canExecute = { true })

        useCase.invoke(Unit)

        assertEquals(1, useCase.workCount)
        assertEquals(DataState.Success(1), useCase.getCurrent())
    }
}