package org.insmont.authenticator.core.model

data class UserPreferences(
    val themeConfig: ThemeConfig,
    val dynamicColorEnabled: Boolean,
    val hapticFeedbackEnabled: Boolean,
    val appLockEnabled: Boolean
)