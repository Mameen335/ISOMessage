package com.mameen.isomessage.di

import com.mameen.isomessage.BuildConfig
import com.mameen.isomessage.data.remote.PaymentApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing networking dependencies (OkHttp + Retrofit).
 *
 * OkHttp is the HTTP client — handles connection pooling, timeouts, interceptors.
 * Retrofit is the REST client layer — maps Kotlin interfaces to HTTP calls.
 *
 * Timeout configuration:
 * - Connect timeout: how long to wait for the TCP connection to establish
 * - Read timeout: how long to wait for the server to send data
 * - Write timeout: how long to wait to send the request body
 *
 * In payment systems, timeouts are critical:
 * - Too short = false declines (terminal gives up while host is still processing)
 * - Too long = poor user experience, terminal appears frozen
 * - Industry standard: 30 seconds total for online authorisation
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * OkHttp logging interceptor — logs request/response bodies in debug builds.
     * NEVER enable response body logging in production — it logs card data!
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.ENABLE_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    /**
     * OkHttp client with payment-appropriate timeout settings.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)   // Wait up to 10s for TCP connection
        .readTimeout(30, TimeUnit.SECONDS)       // Wait up to 30s for host response
        .writeTimeout(10, TimeUnit.SECONDS)      // Send request within 10s
        .retryOnConnectionFailure(true)          // Retry on network glitches
        .addInterceptor(loggingInterceptor)
        // Add a custom interceptor that adds ISO8583 metadata headers
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Terminal-ID", "TERM0001")
                .addHeader("X-App-Version", BuildConfig.APP_VERSION)
                .addHeader("User-Agent", "ISO8583-POS-Simulator/1.0 Android")
                .build()
            chain.proceed(request)
        }
        .build()

    /**
     * Retrofit instance configured with Gson for JSON serialisation.
     *
     * Base URL points to Mockoon running on:
     * - 10.0.2.2:5000 = Android emulator's host machine localhost
     * - Change in BuildConfig / Settings screen for real device use
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun providePaymentApiService(retrofit: Retrofit): PaymentApiService =
        retrofit.create(PaymentApiService::class.java)
}
