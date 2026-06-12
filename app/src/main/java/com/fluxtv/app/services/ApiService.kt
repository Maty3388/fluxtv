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
        .readTimeout(20, TimeUnit.SECONDS).build()
    var token = ""
    var subEnd = ""

    fun login(email: String, password: String): String? {
        val body = """{"email":"$email","password":"$password"}""".toRequestBody("application/json".toMediaType())
        val res = client.newCall(Request.Builder().url("$BASE/auth/login").post(body).build()).execute()
        val json = JSONObject(res.body?.string() ?: return null)
        subEnd = json.optJSONObject("user")?.optString("subscription_end") ?: ""
        return json.optString("token").takeIf { it.isNotEmpty() }
    }

    fun getChannels(): List<Channel> {
        val res = client.newCall(Request.Builder().url("$BASE/channels?limit=5000")
            .header("Authorization", "Bearer $token").build()).execute()
        val json = JSONObject(res.body?.string() ?: return emptyList())
        val arr = json.optJSONArray("channels") ?: return emptyList()
        return (0 until arr.length()).map {
            val ch = arr.getJSONObject(it)
            Channel(ch.optString("_id"), ch.optString("name"), ch.optString("category"),
                ch.optString("logo"), ch.optString("stream_url"), ch.optInt("number", 999))
        }
    }

    fun getFavorites(): List<Channel> {
        val res = client.newCall(Request.Builder().url("$BASE/favorites")
            .header("Authorization", "Bearer $token").build()).execute()
        val json = JSONObject(res.body?.string() ?: return emptyList())
        val arr = json.optJSONArray("channels") ?: return emptyList()
        return (0 until arr.length()).map {
            val ch = arr.getJSONObject(it)
            Channel(ch.optString("_id"), ch.optString("name"), ch.optString("category"),
                ch.optString("logo"), ch.optString("stream_url"), ch.optInt("number", 999))
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

    fun getVersion(): AppVersion? = try {
        val res = client.newCall(Request.Builder().url("$BASE/fluxtv/version").build()).execute()
        val json = JSONObject(res.body?.string() ?: return null)
        AppVersion(json.optString("version","1.0.0"), json.optString("apkUrl"),
            json.optBoolean("forceUpdate"), json.optString("changelog"))
    } catch (_: Exception) { null }
}
