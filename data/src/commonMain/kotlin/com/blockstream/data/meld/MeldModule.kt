package com.blockstream.data.meld

import com.blockstream.data.meld.datasource.MeldLocalDataSource
import com.blockstream.data.meld.datasource.MeldRemoteDataSource
import org.koin.dsl.module

val meldModule = module {
    single {
        MeldHttpClient(get())
    }
    single {
        MeldRemoteDataSource(get())
    }
    single {
        MeldLocalDataSource()
    }
    single {
        MeldRepository(get(), get())
    }
}