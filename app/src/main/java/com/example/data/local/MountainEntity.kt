package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mountains")
data class MountainEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val persianName: String,
    val province: String,
    val persianProvince: String,
    val range: String = "",
    val latitude: Double,
    val longitude: Double,
    val altitude: Int,
    val isPinned: Boolean = false,
    val isCustom: Boolean = false,
    val type: String = "iran_peak",
    val slopeAngle: Double = 30.0,
    val aspect: String = "N"
)
