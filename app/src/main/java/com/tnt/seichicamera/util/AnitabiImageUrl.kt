package com.tnt.seichicamera.util

private const val ANITABI_IMAGE_BASE_URL = "https://image.anitabi.cn/"
private val ANITABI_IMAGE_PLAN_REGEX = Regex("([?&])plan=[^&#]*")

fun String.withAnitabiImagePlan(plan: String): String {
    if (!startsWith(ANITABI_IMAGE_BASE_URL)) return this

    return if (ANITABI_IMAGE_PLAN_REGEX.containsMatchIn(this)) {
        replace(ANITABI_IMAGE_PLAN_REGEX) { match ->
            "${match.groupValues[1]}plan=$plan"
        }
    } else {
        val fragmentStart = indexOf('#').takeIf { it >= 0 } ?: length
        val urlWithoutFragment = substring(0, fragmentStart)
        val fragment = substring(fragmentStart)
        val separator = if ('?' in urlWithoutFragment) '&' else '?'
        "$urlWithoutFragment${separator}plan=$plan$fragment"
    }
}
