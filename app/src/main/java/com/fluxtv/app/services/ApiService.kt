package com.fluxtv.app.services
import com.fluxtv.app.models.AppVersion
import com.fluxtv.app.models.Channel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {
    private const val BASE = "http://149.104.92.205:25461"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .apply {
            try {
                val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
                sslContext.init(null, null, null)
                sslSocketFactory(
                    com.fluxtv.app.utils.Tls12SocketFactory(sslContext.socketFactory),
                    javax.net.ssl.X509TrustManager::class.java.let {
                        val tmf = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm())
                        tmf.init(null as java.security.KeyStore?)
                        tmf.trustManagers.filterIsInstance<javax.net.ssl.X509TrustManager>().first()
                    }
                )
            } catch (_: Exception) {}
        }.build()
    var token = ""
    var subEnd = ""
    var appContext: android.content.Context? = null

    private const val CACHE_NAME = "flux_cache"

    private fun saveCache(key: String, json: String) {
        appContext?.getSharedPreferences(CACHE_NAME, android.content.Context.MODE_PRIVATE)
            ?.edit()?.putString(key, json)?.apply()
    }

    private fun getCache(key: String): String? =
        appContext?.getSharedPreferences(CACHE_NAME, android.content.Context.MODE_PRIVATE)
            ?.getString(key, null)

    // Devuelve la lista cacheada de canales inmediatamente (puede estar vacia si no hay cache)
    fun getCachedChannels(): List<Channel> {
        val cached = getCache("channels") ?: return emptyList()
        return parseChannels(cached)
    }

    private fun parseChannels(jsonStr: String): List<Channel> {
        val json = JSONObject(jsonStr)
        val arr = json.optJSONArray("channels") ?: return emptyList()
        return (0 until arr.length()).map {
            val ch = arr.getJSONObject(it)
            Channel(ch.optString("_id"), ch.optString("name"), ch.optString("category"),
                ch.optString("logo"), ch.optString("stream_url"), ch.optInt("number", 999),
                drmKeys = ch.optString("drm_keys"),
                drmType = ch.optString("drm_type"),
                drmLicense = ch.optString("drm_license"))
        }
    }

    var loginError = ""
    fun login(email: String, password: String): String? {
        val bodyJson = JSONObject().put("email", email).put("password", password).toString()
        val body = bodyJson.toRequestBody("application/json".toMediaType())
        val res = client.newCall(Request.Builder().url("$BASE/auth/login").post(body).build()).execute()
        val json = JSONObject(res.body?.string() ?: return null)
        loginError = json.optString("error", "")
        subEnd = json.optJSONObject("user")?.optString("subscription_end") ?: ""
        return json.optString("token").takeIf { it.isNotEmpty() }
    }

    fun getChannels(): List<Channel> = try {
        val res = client.newCall(Request.Builder().url("$BASE/channels?limit=5000")
            .header("Authorization", "Bearer $token").build()).execute()
        val bodyStr = res.body?.string() ?: return getCachedChannels()
        saveCache("channels", bodyStr)
        parseChannels(bodyStr)
    } catch (_: Exception) { getCachedChannels() }

    fun getFavorites(): List<Channel> = try {
        val res = client.newCall(Request.Builder().url("$BASE/favorites")
            .header("Authorization", "Bearer $token").build()).execute()
        val json = JSONObject(res.body?.string() ?: return emptyList())
        val arr = json.optJSONArray("channels") ?: return emptyList()
        return (0 until arr.length()).map {
            val ch = arr.getJSONObject(it)
            Channel(ch.optString("_id"), ch.optString("name"), ch.optString("category"),
                ch.optString("logo"), ch.optString("stream_url"), ch.optInt("number", 999),
                drmKeys = ch.optString("drm_keys"),
                drmType = ch.optString("drm_type"),
                drmLicense = ch.optString("drm_license"))
        }
    }

    fun addFavorite(channelId: String) {
        val body = "{}".toRequestBody("application/json".toMediaType())
        client.newCall(Request.Builder().url("$BASE/favorites/$channelId")
            .header("Authorization", "Bearer $token").post(body).build()).execute()
    }

    fun removeFavorite(channelId: String) {
        client.newCall(Request.Builder().url("$BASE/favorites/$channelId")
            .header("Authorization", "Bearer $token").delete().build()).execute()
    }

    fun getMovies(featured: Boolean = false): List<com.fluxtv.app.models.Movie> = try {
        val url = if (featured) "$BASE/movies?featured=true" else "$BASE/movies?limit=200"
        val res = client.newCall(Request.Builder().url(url)
            .header("Authorization", "Bearer $token").build()).execute()
        val json = org.json.JSONObject(res.body?.string() ?: return emptyList())
        val arr = json.optJSONArray("movies") ?: return emptyList()
        return (0 until arr.length()).map {
            val m = arr.getJSONObject(it)
            com.fluxtv.app.models.Movie(m.optString("_id"), m.optString("title"), m.optString("category"),
                m.optString("posterUrl"), m.optString("backdropUrl"), m.optString("stream_url"),
                m.optString("description"), m.optString("rating"), m.optString("year"),
                m.optBoolean("featured"))
        }
    }

    fun getSeries(featured: Boolean = false): List<com.fluxtv.app.models.Serie> = try {
        val url = if (featured) "$BASE/series?featured=true" else "$BASE/series"
        val res = client.newCall(Request.Builder().url(url)
            .header("Authorization", "Bearer $token").build()).execute()
        val json = org.json.JSONObject(res.body?.string() ?: return emptyList())
        val arr = json.optJSONArray("series") ?: return emptyList()
        return (0 until arr.length()).map {
            val s = arr.getJSONObject(it)
            val epArr = s.optJSONArray("episodes")
            val episodes = if (epArr != null) (0 until epArr.length()).map { j ->
                val e = epArr.getJSONObject(j)
                com.fluxtv.app.models.Episode(e.optString("title"), e.optInt("season"), e.optInt("episode"), e.optString("streamUrl"))
            } else emptyList()
            com.fluxtv.app.models.Serie(s.optString("_id"), s.optString("title"), s.optString("category"),
                s.optString("posterUrl"), s.optString("stream_url").ifEmpty { s.optString("streamUrl") }, s.optString("description"),
                s.optString("rating"), s.optString("year"), s.optBoolean("featured"), episodes)
        }
    }

    fun selectProfile(profileId: Int, deviceId: String): org.json.JSONObject {
        val body = org.json.JSONObject()
        body.put("profileId", profileId)
        body.put("deviceId", deviceId)
        val req = okhttp3.Request.Builder()
            .url("$BASE/profile/select")
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val res = client.newCall(req).execute()
        return org.json.JSONObject(res.body?.string() ?: "{}")
    }

    fun setParentalPin(pin: String): org.json.JSONObject {
        val body = org.json.JSONObject()
        body.put("pin", pin)
        val req = okhttp3.Request.Builder()
            .url("$BASE/parental/pin")
            .header("Authorization", "Bearer $token")
            .put(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val res = client.newCall(req).execute()
        return org.json.JSONObject(res.body?.string() ?: "{}")
    }

    fun verifyParentalPin(pin: String): org.json.JSONObject {
        val body = org.json.JSONObject()
        body.put("pin", pin)
        val req = okhttp3.Request.Builder()
            .url("$BASE/parental/verify")
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val res = client.newCall(req).execute()
        return org.json.JSONObject(res.body?.string() ?: "{}")
    }

    fun getProfile(): org.json.JSONObject {
        val req = okhttp3.Request.Builder()
            .url("$BASE/profile")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        val res = client.newCall(req).execute()
        return org.json.JSONObject(res.body?.string() ?: "{}")
    }

    fun getVersion(): AppVersion? = try {
        val res = client.newCall(Request.Builder().url("$BASE/fluxtv/version").build()).execute()
        val json = JSONObject(res.body?.string() ?: return null)
        AppVersion(json.optString("version","1.0.0"), json.optString("apkUrl"),
            json.optBoolean("forceUpdate"), json.optString("changelog"))
    } catch (_: Exception) { null }
}
