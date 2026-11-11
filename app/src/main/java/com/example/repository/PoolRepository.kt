package com.example.repository

import android.content.Context
import com.example.model.PoolCouponDto
import com.example.model.PoolCouponUpload
import com.example.network.PoolAuthCredentials
import com.example.network.PoolAuthResponse
import com.example.network.PoolNetworkModule
import com.example.network.PoolRefreshRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Member-owned public coupons. Passwords are never saved; an Auth session is
 * kept in private app storage. The server's RLS validates ownership on writes.
 */
class PoolRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("toto_pool_session", Context.MODE_PRIVATE)
    private val authMutex = Mutex()
    private val api = PoolNetworkModule.service
    private val key = PoolNetworkModule.PUBLISHABLE_KEY

    val hasSession: Boolean get() = !prefs.getString("refresh_token", null).isNullOrEmpty()

    suspend fun register(email: String, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isValidCredentials(email, password)) {
            return@withContext Result.failure(IllegalArgumentException("Geçerli e-posta ve en az 6 karakterli şifre girin."))
        }
        try {
            val response = api.signUp(key, PoolAuthCredentials(email.trim(), password))
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("Kayıt yapılamadı. E-posta adresini ve şifrenizi kontrol edin."))
            }
            val auth = response.body()
            val signedIn = auth?.accessToken != null && auth.refreshToken != null
            if (signedIn) save(auth!!)
            Result.success(signedIn) // false = email confirmation required.
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Kayıt sırasında bağlantı kurulamadı."))
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isValidCredentials(email, password)) {
            return@withContext Result.failure(IllegalArgumentException("Geçerli e-posta ve şifre girin."))
        }
        try {
            val response = api.signIn(key, body = PoolAuthCredentials(email.trim(), password))
            val auth = response.body()
            if (!response.isSuccessful || auth?.accessToken.isNullOrEmpty() || auth?.refreshToken.isNullOrEmpty()) {
                return@withContext Result.failure(IllegalStateException("Giriş başarısız. E-posta doğrulamasını, şifreyi ve hesabı kontrol edin."))
            }
            save(auth!!)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Giriş sırasında bağlantı kurulamadı."))
        }
    }

    fun signOut() {
        prefs.edit().clear().apply()
    }

    suspend fun getWeek(weekKey: String): Result<List<PoolCouponDto>> = withContext(Dispatchers.IO) {
        try {
            val token = usableToken() ?: return@withContext Result.failure(
                IllegalStateException("Kupon havuzu için üye girişi yapın.")
            )
            val response = api.listShared(key, "Bearer $token", "eq.$weekKey")
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("Havuz verileri alınamadı (HTTP ${response.code()})."))
            }
            Result.success(response.body().orEmpty())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Havuz sunucusuna bağlanılamıyor."))
        }
    }

    suspend fun share(coupon: PoolCouponUpload): Result<Unit> = withContext(Dispatchers.IO) {
        if (coupon.matchLabels.size != 15 || coupon.columns.isEmpty() || coupon.columns.size > 4096 ||
            coupon.columns.any { !it.matches(Regex("[1X2]{15}")) } || !coupon.confirmedByMember) {
            return@withContext Result.failure(IllegalArgumentException("Paylaşılacak kupon 15 maç içermeli ve oynandığı onaylanmalıdır."))
        }
        try {
            val token = usableToken() ?: return@withContext Result.failure(
                IllegalStateException("Paylaşmak için giriş yapın.")
            )
            val response = api.publish(key, "Bearer $token", coupon)
            if (!response.isSuccessful) {
                val message = if (response.code() == 409) "Bu kupon daha önce aynı hafta için paylaşıldı."
                else "Kupon paylaşılamadı (HTTP ${response.code()})."
                return@withContext Result.failure(IllegalStateException(message))
            }
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Kupon paylaşılırken bağlantı kesildi."))
        }
    }

    private suspend fun usableToken(): String? = authMutex.withLock {
        val token = prefs.getString("access_token", null)
        val expiresAt = prefs.getLong("expires_at_ms", 0L)
        if (!token.isNullOrEmpty() && System.currentTimeMillis() + 60_000L < expiresAt) return@withLock token
        val refreshToken = prefs.getString("refresh_token", null) ?: return@withLock null
        try {
            val response = api.refresh(key, body = PoolRefreshRequest(refreshToken))
            val body = response.body()
            if (response.isSuccessful && body?.accessToken != null && body.refreshToken != null) {
                save(body)
                return@withLock body.accessToken
            }
            signOut()
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null // Transient failure should not discard a valid refresh token.
        }
    }

    private fun save(body: PoolAuthResponse) {
        prefs.edit()
            .putString("access_token", body.accessToken)
            .putString("refresh_token", body.refreshToken)
            .putString("user_id", body.user?.id)
            .putLong("expires_at_ms", System.currentTimeMillis() + (body.expiresIn ?: 3600L) * 1000L)
            .apply()
    }

    private fun isValidCredentials(email: String, password: String): Boolean =
        email.trim().contains('@') && password.length >= 6
}
