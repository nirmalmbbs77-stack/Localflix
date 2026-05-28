package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class DetailsRoute(val videoId: Long)

@Serializable
data class PlayerRoute(val videoId: Long)

@Serializable
object SearchRoute

@Serializable
object SettingsRoute
