package com.example.data.weather

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class HourlyForecastPoint(
    val timeLabel: String,
    val tempC: Double,
    val rainProbability: Int,
    val weatherCode: Int
)

data class DailyForecastPoint(
    val dateLabel: String,
    val weatherCode: Int,
    val condition: String,
    val maxTempC: Double,
    val minTempC: Double,
    val rainProbabilityMax: Int,
    val uvIndexMax: Double,
    val sunrise: String,
    val sunset: String
)

data class WeatherSnapshot(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val weatherCode: Int,
    val conditionLabel: String,
    val windSpeedKmh: Double,
    val windDirectionLabel: String,
    val isDay: Boolean,
    val uvIndexToday: Double,
    val rainProbabilityToday: Int,
    val sunriseToday: String,
    val sunsetToday: String,
    val weatherAlert: String?,
    val hourly: List<HourlyForecastPoint>,
    val daily: List<DailyForecastPoint>,
    val lastUpdatedMillis: Long
)

data class CitySearchResult(
    val name: String,
    val country: String,
    val admin1: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String
)

class WeatherRepository {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val weatherApi: OpenMeteoWeatherApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoWeatherApi::class.java)

    private val geocodingApi: OpenMeteoGeocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenMeteoGeocodingApi::class.java)

    suspend fun searchCities(query: String): Result<List<CitySearchResult>> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return Result.success(emptyList())
        return try {
            val response = geocodingApi.searchCities(trimmed)
            val list = response.results.orEmpty().map {
                CitySearchResult(
                    name = it.name,
                    country = it.country,
                    admin1 = it.admin1.orEmpty(),
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timezone = it.timezone ?: "UTC"
                )
            }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchWeather(cityName: String, latitude: Double, longitude: Double): Result<WeatherSnapshot> {
        return try {
            val response = weatherApi.getForecast(latitude = latitude, longitude = longitude)
            val current = response.current ?: return Result.failure(IllegalStateException("Weather payload missing current conditions"))
            val hourlyDto = response.hourly
            val dailyDto = response.daily

            val hourlyList = mutableListOf<HourlyForecastPoint>()
            if (hourlyDto != null) {
                val count = minOf(24, hourlyDto.time.size, hourlyDto.temperature2m.size)
                for (i in 0 until count) {
                    val rawTime = hourlyDto.time[i]
                    val hourLabel = rawTime.substringAfter("T", rawTime).take(5)
                    hourlyList.add(
                        HourlyForecastPoint(
                            timeLabel = hourLabel,
                            tempC = hourlyDto.temperature2m[i],
                            rainProbability = hourlyDto.precipitationProbability.getOrElse(i) { 0 },
                            weatherCode = hourlyDto.weatherCode.getOrElse(i) { 0 }
                        )
                    )
                }
            }

            val dailyList = mutableListOf<DailyForecastPoint>()
            if (dailyDto != null) {
                val count = minOf(7, dailyDto.time.size, dailyDto.temperature2mMax.size, dailyDto.temperature2mMin.size)
                for (i in 0 until count) {
                    val code = dailyDto.weatherCode.getOrElse(i) { 0 }
                    dailyList.add(
                        DailyForecastPoint(
                            dateLabel = dailyDto.time[i],
                            weatherCode = code,
                            condition = describeWeatherCode(code),
                            maxTempC = dailyDto.temperature2mMax[i],
                            minTempC = dailyDto.temperature2mMin[i],
                            rainProbabilityMax = dailyDto.precipitationProbabilityMax.getOrElse(i) { 0 },
                            uvIndexMax = dailyDto.uvIndexMax.getOrElse(i) { 0.0 },
                            sunrise = dailyDto.sunrise.getOrElse(i) { "--:--" }.substringAfter("T"),
                            sunset = dailyDto.sunset.getOrElse(i) { "--:--" }.substringAfter("T")
                        )
                    )
                }
            }

            val todayDaily = dailyList.firstOrNull()
            val alert = buildWeatherAlert(
                weatherCode = current.weatherCode,
                uvIndex = todayDaily?.uvIndexMax ?: 0.0,
                windSpeed = current.windSpeed10m,
                rainProb = todayDaily?.rainProbabilityMax ?: 0
            )

            Result.success(
                WeatherSnapshot(
                    cityName = cityName,
                    latitude = response.latitude,
                    longitude = response.longitude,
                    timezone = response.timezone,
                    temperatureC = current.temperature2m,
                    feelsLikeC = current.apparentTemperature,
                    humidityPercent = current.relativeHumidity2m,
                    weatherCode = current.weatherCode,
                    conditionLabel = describeWeatherCode(current.weatherCode),
                    windSpeedKmh = current.windSpeed10m,
                    windDirectionLabel = degreesToCompass(current.windDirection10m),
                    isDay = current.isDay == 1,
                    uvIndexToday = todayDaily?.uvIndexMax ?: 0.0,
                    rainProbabilityToday = todayDaily?.rainProbabilityMax ?: 0,
                    sunriseToday = todayDaily?.sunrise ?: "--:--",
                    sunsetToday = todayDaily?.sunset ?: "--:--",
                    weatherAlert = alert,
                    hourly = hourlyList,
                    daily = dailyList,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildWeatherAlert(
        weatherCode: Int,
        uvIndex: Double,
        windSpeed: Double,
        rainProb: Int
    ): String? = when {
        weatherCode in 95..99 -> "Thunderstorm Advisory: Severe convective activity reported in current forecast."
        weatherCode in listOf(65, 75, 82, 86) -> "Heavy Precipitation Notice: Intense rain or snowfall expected."
        windSpeed >= 45.0 -> "High Wind Advisory: Gusts reaching ${windSpeed.toInt()} km/h."
        uvIndex >= 8.0 -> "Very High UV Index (${uvIndex.toInt()}): Sun protection recommended."
        rainProb >= 80 -> "High Rain Probability ($rainProb%): Carry rain protection."
        else -> null
    }

    companion object {
        fun describeWeatherCode(code: Int): String = when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Severe Storm"
            else -> "Variable Conditions"
        }

        fun degreesToCompass(degrees: Int): String {
            val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = (((degrees % 360) + 360) % 360 + 22) / 45 % 8
            return dirs[index]
        }
    }
}
