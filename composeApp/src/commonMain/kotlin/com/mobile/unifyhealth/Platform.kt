package com.mobile.unifyhealth

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform