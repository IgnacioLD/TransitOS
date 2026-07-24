package com.glossostudio.transitos.core.repository

import kotlinx.coroutines.flow.Flow

interface TransferBufferPreference {
    val flow: Flow<Int>
    fun current(): Int
    suspend fun set(minutes: Int)
}
