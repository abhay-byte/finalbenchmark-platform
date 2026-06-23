package com.zenithblue.fb2Pro.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenithblue.fb2Pro.utils.OsInfo
import com.zenithblue.fb2Pro.utils.OsUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.content.Context

class OsViewModel : ViewModel() {
    
    private val _osInfo = MutableStateFlow<OsInfo?>(null)
    val osInfo: StateFlow<OsInfo?> = _osInfo
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    fun loadOsInfo(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val info = OsUtils.getOsInfo(context)
                _osInfo.value = info
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}