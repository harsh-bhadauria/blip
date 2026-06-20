package com.raven.blip.domain.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkinRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private var vibeCache: Map<String, List<String>>? = null

    suspend fun getVibeLines(skinId: String): Map<String, List<String>> = withContext(Dispatchers.IO) {
        if (vibeCache != null && skinId == "default") {
            return@withContext vibeCache!!
        }

        try {
            val jsonString = context.assets.open("skins/$skinId/vibe.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            val result: Map<String, List<String>> = gson.fromJson(jsonString, type)
            if (skinId == "default") {
                vibeCache = result
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }
}
