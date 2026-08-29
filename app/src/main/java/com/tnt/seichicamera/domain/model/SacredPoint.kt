package com.tnt.seichicamera.domain.model

data class SacredPoint(
    val id: String,
    val bangumiId: Int,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val imageUrls: List<String>,
    val originUrl: String?,
    val ep: String?
)
