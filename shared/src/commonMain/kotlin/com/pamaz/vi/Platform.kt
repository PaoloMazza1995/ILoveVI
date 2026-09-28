package com.pamaz.vi

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform