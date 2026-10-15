package com.dailyvlog.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

@Serializable
data object HomeRoute

@Serializable
data object ProfileRoute

@Serializable
data object CreateGroupRoute

@Serializable
data class JoinGroupRoute(val prefilledCode: String? = null)

@Serializable
data class GroupDetailRoute(val groupId: String)

@Serializable
data class TodayRoute(val groupId: String)

@Serializable
data class RecordVlogRoute(val groupId: String, val maxDurationSeconds: Int = 60)

@Serializable
data class WatchVlogRoute(val groupId: String, val vlogId: String? = null)
