package com.github.ilife798.data.api

import kotlinx.serialization.Serializable

@Serializable
data class SetUseScoreRequest(
    val useScore: Int,
)
