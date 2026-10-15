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
