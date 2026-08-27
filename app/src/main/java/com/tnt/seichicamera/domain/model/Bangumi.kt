package com.tnt.seichicamera.domain.model

data class Bangumi(
    val id: Int,
    val title: String,
    val coverUrl: String,
    val region: String?,
    val zoom: Float?
)
