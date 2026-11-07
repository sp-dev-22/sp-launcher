package com.example.ui

/** How the hidden-apps screen was opened. */
enum class HiddenAppsMode {
    /** `vault <pin>`: hide, unhide and change the PIN. */
    MANAGE,

    /** `apps <pin>`: launch a hidden app without unhiding it first. */
    LAUNCH
}
