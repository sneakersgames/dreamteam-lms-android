package com.engagecraft.gaming.mygame.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engagecraft.gaming.core.lib.Gaming
import com.engagecraft.gaming.core.lib.model.NotificationChannel
import kotlinx.coroutines.launch

internal class DemoViewModel : ViewModel() {

    var notificationsChecked by mutableStateOf(false)
        private set
    var isNotificationsEnabled by mutableStateOf(false)
        private set
    val notificationChannels = mutableStateListOf<NotificationChannel>()

    fun checkNotifications(gameId: String) {
        viewModelScope.launch {
            val response = Gaming.areNotificationsEnabled(gameId)
            isNotificationsEnabled = response.isEnabled
            notificationChannels.clear()
            notificationChannels.addAll(response.channels)
            notificationsChecked = true
        }
    }

    fun enableNotifications(gameId: String) {
        Gaming.enableNotifications(gameId) { isEnabled ->
            isNotificationsEnabled = isEnabled
        }
    }

    fun setupChannel(gameId: String, channel: NotificationChannel, isEnabled: Boolean) {
        viewModelScope.launch {
            if (Gaming.setNotificationChannel(gameId, channel.id, isEnabled)) {
                notificationChannels[notificationChannels.indexOfFirst { it.id == channel.id }] =
                    channel.copy(isEnabled = isEnabled)
            }
        }
    }

}