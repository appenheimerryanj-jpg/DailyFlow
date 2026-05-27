package com.appenheimer.dailyflow.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.appenheimer.dailyflow.R

enum class DailyFlowSound {
    SOFT_TAP,
    TASK_COMPLETE,
    HABIT_COMPLETE,
    ALL_HABITS_COMPLETE,
    STREAK_UP,
    PREMIUM_SUCCESS,
    LIMIT_OR_ERROR,
    GENTLE_FOCUS_START
}

class SoundManager(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loaded = mutableSetOf<Int>()
    private val soundIds = mutableMapOf<DailyFlowSound, Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded += sampleId
        }
        soundIds += DailyFlowSound.SOFT_TAP to soundPool.load(context, R.raw.soft_tap, 1)
        soundIds += DailyFlowSound.TASK_COMPLETE to soundPool.load(context, R.raw.task_complete, 1)
        soundIds += DailyFlowSound.HABIT_COMPLETE to soundPool.load(context, R.raw.habit_complete, 1)
        soundIds += DailyFlowSound.ALL_HABITS_COMPLETE to soundPool.load(context, R.raw.all_habits_complete, 1)
        soundIds += DailyFlowSound.STREAK_UP to soundPool.load(context, R.raw.streak_up, 1)
        soundIds += DailyFlowSound.PREMIUM_SUCCESS to soundPool.load(context, R.raw.premium_success, 1)
        soundIds += DailyFlowSound.LIMIT_OR_ERROR to soundPool.load(context, R.raw.limit_or_error, 1)
        soundIds += DailyFlowSound.GENTLE_FOCUS_START to soundPool.load(context, R.raw.gentle_focus_start, 1)
    }

    fun play(sound: DailyFlowSound, enabled: Boolean, volume: Float = 0.45f) {
        if (!enabled) return
        val id = soundIds[sound] ?: return
        if (id <= 0 || id !in loaded) return
        runCatching {
            soundPool.play(id, volume, volume, 1, 0, 1f)
        }
    }

    fun release() {
        runCatching { soundPool.release() }
        loaded.clear()
    }
}
