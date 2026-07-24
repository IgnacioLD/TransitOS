package com.glossostudio.transitos.core.repository

interface LanguagePreference {
    val current: String
    fun set(language: String)
}
