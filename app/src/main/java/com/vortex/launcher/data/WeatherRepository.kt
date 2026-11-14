package com.vortex.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

enum class WeatherKind { SUN, CLOUD, RAIN, SNOW, FOG }
data class Weather(val temp: Int, val text: String, val kind: WeatherKind)

@Singleton
class WeatherRepository @Inject constructor() {
    suspend fun load(city: String): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val q = URLEncoder.encode(city, "UTF-8")
            val geo = JSONObject(URL("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=ru").readText())
                .getJSONArray("results").getJSONObject(0)
            val lat = geo.getDouble("latitude"); val lon = geo.getDouble("longitude")
            val cur = JSONObject(URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code").readText())
                .getJSONObject("current")
            val (text, kind) = when (cur.getInt("weather_code")) {
                0 -> "Ясно" to WeatherKind.SUN
                1, 2, 3 -> "Облачно" to WeatherKind.CLOUD
                45, 48 -> "Туман" to WeatherKind.FOG
                in 51..67, in 80..82 -> "Дождь" to WeatherKind.RAIN
                in 71..77, 85, 86 -> "Снег" to WeatherKind.SNOW
                else -> "Гроза" to WeatherKind.RAIN
            }
            Weather(Math.round(cur.getDouble("temperature_2m")).toInt(), text, kind)
        }.getOrNull()
    }
}
