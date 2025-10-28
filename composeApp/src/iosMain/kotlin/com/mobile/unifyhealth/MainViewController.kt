package com.mobile.unifyhealth

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App(HealthService()) }