package com.zenithblue.fb2Pro.utils

import android.content.Context
import android.content.pm.PackageManager

class OnboardingPreferences(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("onboarding_preferences", Context.MODE_PRIVATE)
    
    companion object {
        private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
        private const val ONBOARDING_VERSION_KEY = "onboarding_version"
        private const val CURRENT_ONBOARDING_VERSION = 3 // Version 3 includes theme selection screen
    }
    
    fun isOnboardingCompleted(): Boolean {
        // Check if onboarding version matches current version
        val onboardingVersion = sharedPreferences.getInt(ONBOARDING_VERSION_KEY, 0)
        if (onboardingVersion < CURRENT_ONBOARDING_VERSION) {
            // Reset onboarding for new version
            resetOnboarding()
            return false
        }
        
        return sharedPreferences.getBoolean(ONBOARDING_COMPLETED_KEY, false)
    }
    
    fun setOnboardingCompleted() {
        sharedPreferences.edit()
            .putBoolean(ONBOARDING_COMPLETED_KEY, true)
            .putInt(ONBOARDING_VERSION_KEY, CURRENT_ONBOARDING_VERSION)
            .apply()
    }
    
    fun resetOnboarding() {
        sharedPreferences.edit()
            .putBoolean(ONBOARDING_COMPLETED_KEY, false)
            .putInt(ONBOARDING_VERSION_KEY, CURRENT_ONBOARDING_VERSION)
            .apply()
    }
}