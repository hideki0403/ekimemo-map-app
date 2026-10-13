package dev.yukineko.ekimemo_map.notification

import androidx.annotation.RawRes
import dev.yukineko.ekimemo_map.R

enum class NotificationSound(@param:RawRes val resId: Int) {
    PB_1(R.raw.notification_pb_1),
    PB_2(R.raw.notification_pb_2),
    PB_3(R.raw.notification_pb_3),
    PS_1(R.raw.notification_ps_1),
    PS_2(R.raw.notification_ps_2),
    PS_3(R.raw.notification_ps_3),
    PS_4(R.raw.notification_ps_4),
    SL_1(R.raw.notification_sl_1),
    SL_2(R.raw.notification_sl_2),
    SL_3(R.raw.notification_sl_3),
    SL_4(R.raw.notification_sl_4),
}
