package com.example.warehouse.data.local

import com.example.warehouse.model.BeadProfileV2
import com.example.warehouse.model.MuntinProfileV2
import com.example.warehouse.model.SashProfileV2

object V2ProfileCatalog {

    val sashProfiles = listOf(
        SashProfileV2("SASH-82", 82, 70, 45.0),
        SashProfileV2("SASH-70", 70, 65, 45.0),
        SashProfileV2("RENO-FRAME", 70, 50, 45.0)
    )

    val beadProfiles = listOf(
        BeadProfileV2("BEAD-20", 20, 20, 45.0),
        BeadProfileV2("BEAD-14", 14, 14, 45.0),
        BeadProfileV2("BEAD-SQUARE", 18, 18, 90.0)
    )

    val muntinProfiles = listOf(
        MuntinProfileV2("MUNTIN-26", 26, 12, 0.0),
        MuntinProfileV2("MUNTIN-45", 45, 14, 0.0),
        MuntinProfileV2("MUNTIN-18", 18, 10, 0.0)
    )

    fun getSash(code: String) = sashProfiles.find { it.profileNo == code } ?: sashProfiles.first()
    fun getBead(code: String) = beadProfiles.find { it.profileNo == code } ?: beadProfiles.first()
    fun getMuntin(code: String) = muntinProfiles.find { it.profileNo == code } ?: muntinProfiles.first()
}
