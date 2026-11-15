package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PaymentSessionRequest(
    val orderId: String,
    val amountBdt: Double,
    val paymentMethod: String,
    val customerPhone: String,
    val customerName: String
)

@JsonClass(generateAdapter = true)
data class PaymentSessionResponse(
    val sessionId: String,
    val gatewayUrl: String,
    val status: String
)

@JsonClass(generateAdapter = true)
data class PaymentVerifyRequest(
    val orderId: String,
    val sessionId: String,
    val paymentMethod: String
)

@JsonClass(generateAdapter = true)
data class PaymentVerifyResponse(
    val verified: Boolean,
    val transactionRef: String,
    val status: String
)

interface EfoodBackendApi {
    @POST("api/payments/initiate")
    suspend fun initiatePayment(
        @Header("Authorization") bearerToken: String,
        @Body request: PaymentSessionRequest
    ): PaymentSessionResponse

    @POST("api/payments/verify")
    suspend fun verifyPayment(
        @Header("Authorization") bearerToken: String,
        @Body request: PaymentVerifyRequest
    ): PaymentVerifyResponse

    companion object {
        fun createOrNull(): EfoodBackendApi? {
            val baseUrl = BuildConfig.EFOOD_API_BASE_URL.trim()
            if (baseUrl.isEmpty()) return null
            val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()
            return Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(EfoodBackendApi::class.java)
        }
    }
}
