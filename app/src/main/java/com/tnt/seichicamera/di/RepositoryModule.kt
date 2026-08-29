package com.tnt.seichicamera.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repositories use @Inject constructor, so no @Provides needed.
    // This module exists for future bindings (e.g., interface-to-impl).
}
