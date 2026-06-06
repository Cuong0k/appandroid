package com.vpnapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpnapp.App
import com.vpnapp.api.ApiClient
import com.vpnapp.api.model.GuestConfig
import com.vpnapp.data.AppRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String = "") : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val prefs = App.instance.preferencesManager
    private val repo = AppRepository(prefs)

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    private val _guestConfig = MutableLiveData<GuestConfig>()
    val guestConfig: LiveData<GuestConfig> = _guestConfig

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            repo.login(email, password)
                .onSuccess { data ->
                    prefs.saveAuthData(data.auth_data, data.token, email)
                    _state.value = AuthState.Success()
                }
                .onFailure { e ->
                    _state.value = AuthState.Error(parseError(e.message))
                }
        }
    }

    fun register(email: String, password: String, confirmPwd: String, inviteCode: String?) {
        when {
            email.isBlank() || password.isBlank() ->
                _state.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
            password != confirmPwd ->
                _state.value = AuthState.Error("Mật khẩu xác nhận không khớp")
            password.length < 8 ->
                _state.value = AuthState.Error("Mật khẩu phải ít nhất 8 ký tự")
            else -> {
                _state.value = AuthState.Loading
                viewModelScope.launch {
                    repo.register(email, password, inviteCode?.takeIf { it.isNotBlank() })
                        .onSuccess { data ->
                            prefs.saveAuthData(data.auth_data, data.token, email)
                            _state.value = AuthState.Success()
                        }
                        .onFailure { e ->
                            _state.value = AuthState.Error(parseError(e.message))
                        }
                }
            }
        }
    }

    fun loadGuestConfig() {
        viewModelScope.launch {
            repo.getGuestConfig().getOrNull()?.let { _guestConfig.value = it }
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            prefs.saveBaseUrl(url)
            ApiClient.reset()
        }
    }

    private fun parseError(msg: String?): String {
        if (msg.isNullOrEmpty()) return "Lỗi không xác định"
        return try {
            JSONObject(msg).optString("message", msg)
        } catch (_: Exception) { msg }
    }
}
