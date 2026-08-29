package com.tnt.seichicamera.domain.model

data class CheckIn(
    val id: Long = 0,
    val pointId: String,
    val photoUri: String,
    val timestamp: Long,
    val comparisonUri: String? = null
)
