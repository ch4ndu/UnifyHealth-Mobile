package com.mobile.sparkyfitness

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App(HealthService()) }