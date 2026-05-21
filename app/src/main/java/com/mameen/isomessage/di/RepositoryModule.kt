package com.mameen.isomessage.di

import com.mameen.isomessage.data.repository.PaymentRepositoryImpl
import com.mameen.isomessage.domain.repository.PaymentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding the repository interface to its implementation.
 *
 * Why a separate module for bindings?
 * @Binds is more efficient than @Provides for simple interface → impl bindings.
 * It tells Hilt: "whenever PaymentRepository is injected, provide PaymentRepositoryImpl".
 *
 * This is the key dependency inversion in Clean Architecture:
 * - UseCases depend on PaymentRepository (interface, in domain layer)
 * - Hilt provides PaymentRepositoryImpl (implementation, in data layer)
 * - Domain layer never imports anything from data layer
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        impl: PaymentRepositoryImpl
    ): PaymentRepository
}
