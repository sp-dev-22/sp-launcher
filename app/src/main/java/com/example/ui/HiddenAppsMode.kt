package com.example.ui

/** Which tab of the hidden-apps screen is showing. */
enum class HiddenAppsMode {
    /** Manage tab: hide, unhide and change the PIN. */
    MANAGE,

    /** Launch tab: launch a hidden app without unhiding it first. */
    LAUNCH
}
