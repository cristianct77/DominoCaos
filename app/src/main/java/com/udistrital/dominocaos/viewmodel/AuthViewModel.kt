package com.udistrital.dominocaos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udistrital.dominocaos.data.repository.AuthRepository
import com.udistrital.dominocaos.logic.AuthError
import com.udistrital.dominocaos.logic.AuthValidator
import com.udistrital.dominocaos.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de las pantallas de login y registro. */
data class AuthUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: AuthError? = null,
    val firebaseMessage: String? = null,
    val user: UserProfile? = null
)

/** ViewModel de autenticación (MVVM): la vista solo observa [uiState]. */
class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(user = repository.currentUser()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, error = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onConfirmChange(value: String) = _uiState.update { it.copy(confirmPassword = value, error = null) }

    fun login() {
        val s = _uiState.value
        AuthValidator.validateLogin(s.email, s.password)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }
        launchAuth { repository.login(s.email, s.password) }
    }

    fun register() {
        val s = _uiState.value
        AuthValidator.validateRegister(s.username, s.email, s.password, s.confirmPassword)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return
        }
        launchAuth { repository.register(s.username, s.email, s.password) }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState()
    }

    fun clearError() = _uiState.update { it.copy(error = null, firebaseMessage = null) }

    private fun launchAuth(call: suspend () -> Result<UserProfile>) {
        _uiState.update { it.copy(loading = true, error = null, firebaseMessage = null) }
        viewModelScope.launch {
            call()
                .onSuccess { user ->
                    _uiState.update { it.copy(loading = false, user = user, password = "", confirmPassword = "") }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, error = AuthError.FIREBASE, firebaseMessage = e.localizedMessage)
                    }
                }
        }
    }
}
