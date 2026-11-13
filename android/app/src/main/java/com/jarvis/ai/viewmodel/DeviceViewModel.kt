package com.jarvis.ai.viewmodel

import androidx.lifecycle.ViewModel
import com.jarvis.ai.device.DeviceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(private val controller: DeviceController) : ViewModel() {
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    fun openApp(pkg: String) { _status.value = controller.openApp(pkg).message }
    fun openSettings(page: String) { _status.value = controller.openSettings(page).message }
    fun setAlarm(h: Int, m: Int) { _status.value = controller.setAlarm(h,m).message }
    fun toggleFlash(on: Boolean) { _status.value = controller.toggleFlashlight(on).message }
    fun batteryInfo(): String = controller.getBatteryInfo()
    fun deviceInfo(): String = controller.getDeviceInfo()
    fun installedApps() = controller.getInstalledApps()
    fun clear() { _status.value = null }
}
