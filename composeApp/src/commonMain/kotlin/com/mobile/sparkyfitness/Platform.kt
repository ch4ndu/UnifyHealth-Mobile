package com.mobile.sparkyfitness

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform