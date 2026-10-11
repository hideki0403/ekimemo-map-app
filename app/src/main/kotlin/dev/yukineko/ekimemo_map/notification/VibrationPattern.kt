package dev.yukineko.ekimemo_map.notification

enum class VibrationPattern(val timings: LongArray) {
    PATTERN_1(longArrayOf(0, 100, 100, 100, 100, 100, 300, 100, 100, 100, 100, 100)),
    PATTERN_2(longArrayOf(0, 300, 200, 300, 200)),
    PATTERN_3(longArrayOf(0, 500, 500, 500, 500)),
    PATTERN_4(longArrayOf(0, 100, 100, 500, 300, 100, 100, 500)),
    PATTERN_5(longArrayOf(0, 100, 200, 100, 100, 300, 200, 100, 200, 100, 100, 300)),
}
