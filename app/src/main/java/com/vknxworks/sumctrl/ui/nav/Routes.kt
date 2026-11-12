package com.vknxworks.sumctrl.ui.nav

import kotlinx.serialization.Serializable

// Tabs
@Serializable data object CalcRoute
@Serializable data object ConvertRoute
@Serializable data object ToolsRoute

// Opened from the profile picture: the You page, and Settings from there
@Serializable data object YouRoute
@Serializable data object SettingsRoute

// Work tools
@Serializable data class ToolRoute(val tool: String)
