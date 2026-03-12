package com.lexa.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class CourseDto(
    val id: Int,
    val title: String,
    val description: String,
    val topic: String
)