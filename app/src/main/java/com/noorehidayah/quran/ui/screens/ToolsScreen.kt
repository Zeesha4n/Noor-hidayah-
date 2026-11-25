package com.noorehidayah.quran.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorehidayah.quran.data.model.AsrMethod
import com.noorehidayah.quran.data.model.UserPreferences
import com.noorehidayah.quran.ui.i18n.Strings
import com.noorehidayah.quran.utils.HijriCalendarHelper
import com.noorehidayah.quran.utils.PrayerTimeCalculator
import com.noorehidayah.quran.utils.QiblaCalculator
import kotlin.math.abs

@Composable
fun ToolsScreen(
    prefs: UserPreferences,
    strings: Strings,
    onIncrementTasbih: () -> Unit,
    onResetTasbih: () -> Unit,
    onToggleTasbihSound: (Boolean) -> Unit,
    onSelectTasbihPreset: (String, Int) -> Unit,
    onSelectCity: (Int) -> Unit,
    onSelectAsrMethod: (AsrMethod) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(strings.prayerTimes, strings.qibla, strings.tasbih, "Hijri Calendar")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1) }
                )
            }
        }

        when (selectedTab) {
            0 -> PrayerTimesPanel(prefs, onSelectCity, onSelectAsrMethod)
            1 -> QiblaCompassPanel(prefs)
            2 -> TasbihPanel(prefs, strings, onIncrementTasbih, onResetTasbih, onToggleTasbihSound, onSelectTasbihPreset)
            3 -> HijriCalendarPanel(prefs)
        }
    }
}

@Composable
private fun HijriCalendarPanel(prefs: UserPreferences) {
    val hijriToday = remember { HijriCalendarHelper.todayHijri() }
    val cities = PrayerTimeCalculator.builtInCities
    val city = cities[prefs.selectedCityIndex.coerceIn(0, cities.lastIndex)]
    val times = remember(city, prefs.prayerMethod, prefs.asrMethod) {
        PrayerTimeCalculator.calculate(city, prefs.prayerMethod, prefs.asrMethod)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${hijriToday.day} ${hijriToday.monthName} ${hijriToday.year} AH",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ramadan / Fast Timings (${city.name}): Sehri Ends ${times.fajr} • Iftar ${times.maghrib}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            Text(
                text = "Important Islamic Dates",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(HijriCalendarHelper.importantEvents) { ev ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = ev.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = ev.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QiblaCompassPanel(prefs: UserPreferences) {
    val context = LocalContext.current
    val cities = PrayerTimeCalculator.builtInCities
    val city = cities[prefs.selectedCityIndex.coerceIn(0, cities.lastIndex)]
    val qiblaBearing = remember(city) { QiblaCalculator.qiblaBearingDegrees(city.latitude, city.longitude) }
    val distanceKm = remember(city) { QiblaCalculator.distanceToMakkahKm(city.latitude, city.longitude) }
    val cardinal = remember(qiblaBearing) { QiblaCalculator.cardinalDirection(qiblaBearing) }

    var azimuthDeg by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationSensor == null && (accelSensor == null || magSensor == null)) {
            sensorAvailable = false
            onDispose { }
        } else {
            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            val rotMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        SensorManager.getOrientation(rotMatrix, orientation)
                        val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                        azimuthDeg = azimuthDeg * 0.8f + deg * 0.2f
                    } else {
                        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                            System.arraycopy(event.values, 0, gravity, 0, 3)
                        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        }
                        if (SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotMatrix, orientation)
                            val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                            azimuthDeg = azimuthDeg * 0.8f + deg * 0.2f
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (rotationSensor != null) {
                sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
            }

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    val diff = abs(((qiblaBearing - azimuthDeg + 540f) % 360f) - 180f)
    val isAligned = sensorAvailable && diff <= 6f
    val primaryColor = MaterialTheme.colorScheme.primary
    val goldColor = MaterialTheme.colorScheme.secondary
    val ringColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${city.name} → Makkah",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Qibla: ${qiblaBearing.toInt()}° $cardinal • $distanceKm km",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 12f

                drawCircle(
                    color = if (isAligned) primaryColor else ringColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = if (isAligned) 8f else 4f)
                )

                rotate(degrees = -azimuthDeg, pivot = center) {
                    drawLine(
                        color = Color.Red,
                        start = Offset(center.x, center.y - radius),
                        end = Offset(center.x, center.y - radius + 22f),
                        strokeWidth = 6f
                    )
                }

                rotate(degrees = qiblaBearing - azimuthDeg, pivot = center) {
                    val arrowPath = Path().apply {
                        moveTo(center.x, center.y - radius + 26f)
                        lineTo(center.x - 18f, center.y + 18f)
                        lineTo(center.x, center.y + 4f)
                        lineTo(center.x + 18f, center.y + 18f)
                        close()
                    }
                    drawPath(path = arrowPath, color = if (isAligned) primaryColor else goldColor)
                }

                drawCircle(color = primaryColor, radius = 10f, center = center)
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAligned) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = when {
                    !sensorAvailable -> "Is device mein compass sensor nahi hai. Phone ko ${qiblaBearing.toInt()}° $cardinal ki taraf rakhein."
                    isAligned -> "Qibla Sahi Disha Mein Hai! (${qiblaBearing.toInt()}° $cardinal)"
                    else -> "Calibration: Phone ko figure-8 mein ghumayein, phir flat rakh kar teer upar seedha karein."
                },
                modifier = Modifier.padding(14.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isAligned) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrayerTimesPanel(
    prefs: UserPreferences,
    onSelectCity: (Int) -> Unit,
    onSelectAsrMethod: (AsrMethod) -> Unit
) {
    val cities = PrayerTimeCalculator.builtInCities
    val city = cities[prefs.selectedCityIndex.coerceIn(0, cities.lastIndex)]
    val times = remember(city, prefs.prayerMethod, prefs.asrMethod) {
        PrayerTimeCalculator.calculate(city, prefs.prayerMethod, prefs.asrMethod)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Next: ${times.nextPrayerName} (${times.nextPrayerTime})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${city.name} • ${prefs.prayerMethod.displayName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = times.nextPrayerCountdown,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AsrMethod.entries.forEach { method ->
                    FilterChip(
                        selected = prefs.asrMethod == method,
                        onClick = { onSelectAsrMethod(method) },
                        label = { Text(method.displayName, fontSize = 12.sp) }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                val rows = listOf(
                    "Fajr (Sehri Ends)" to times.fajr,
                    "Sunrise" to times.sunrise,
                    "Dhuhr" to times.dhuhr,
                    "Asr" to times.asr,
                    "Maghrib (Iftar)" to times.maghrib,
                    "Isha" to times.isha
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    rows.forEachIndexed { idx, (name, time) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                time,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (idx < rows.lastIndex) HorizontalDivider()
                    }
                }
            }
        }

        item {
            Text("Offline City Selector", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                cities.forEachIndexed { idx, c ->
                    FilterChip(
                        selected = idx == prefs.selectedCityIndex,
                        onClick = { onSelectCity(idx) },
                        label = { Text(c.name, fontSize = 12.sp) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasbihPanel(
    prefs: UserPreferences,
    strings: Strings,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onSelectPreset: (String, Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val toneGen = remember {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 40)
        } catch (_: Exception) {
            null
        }
    }
    DisposableEffect(Unit) {
        onDispose { toneGen?.release() }
    }

    val presets = listOf(
        "SubhanAllah" to 33,
        "Alhamdulillah" to 33,
        "Allahu Akbar" to 34,
        "Astaghfirullah" to 100
    )
    val progress = (prefs.tasbihCount.toFloat() / prefs.tasbihTarget.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Dhikr: ${prefs.tasbihDailyTotal}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onToggleSound(!prefs.tasbihSoundEnabled) }) {
                    Icon(
                        imageVector = if (prefs.tasbihSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Sound"
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { (label, target) ->
                    FilterChip(
                        selected = prefs.tasbihLabel == label,
                        onClick = { onSelectPreset(label, target) },
                        label = { Text("$label ($target)", fontSize = 12.sp) }
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = prefs.tasbihLabel,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${prefs.tasbihCount} / ${prefs.tasbihTarget}",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(0.7f).height(8.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (prefs.tasbihSoundEnabled) {
                        toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
                    }
                    onIncrement()
                },
                shape = CircleShape,
                modifier = Modifier.size(160.dp)
            ) {
                Text(strings.tapToCount, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(onClick = onReset) {
            Text(strings.reset)
        }
    }
}
