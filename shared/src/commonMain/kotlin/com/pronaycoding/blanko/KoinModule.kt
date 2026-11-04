package com.pronaycoding.blanko

import com.pronaycoding.blanko.core.di.databaseModule
import com.pronaycoding.blanko.core.di.repositoryModule
import com.pronaycoding.blanko.core.di.serviceModule
import com.pronaycoding.blanko.core.di.useCaseModule
import com.pronaycoding.blanko.core.di.viewModelModule
import org.koin.dsl.module

val appModule = module {
    single { com.pronaycoding.blanko.core.common.util.SnackbarController() }
    includes(
        databaseModule,
        repositoryModule,
        useCaseModule,
        viewModelModule,
        serviceModule,
    )
}
