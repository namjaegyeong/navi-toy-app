package com.example.navigationtoyproject

import androidx.lifecycle.ViewModel
import com.example.navigationtoyproject.api.di.RetrofitClient.apiService
import com.example.navigationtoyproject.api.model.NetworkResult
import com.example.navigationtoyproject.api.model.ResponseMapVersionDto
import com.example.navigationtoyproject.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val repository: UserPreferencesRepository
) : ViewModel() {

    // Backing property for network result
    private val _mapVersionResult = MutableStateFlow<NetworkResult<ResponseMapVersionDto>?>(null)
    val mapVersionResult: StateFlow<NetworkResult<ResponseMapVersionDto>?> = _mapVersionResult

    val getMapVersionFlow = repository.getMapVersionFlow

    // 맵 버전 정보 API 요청
    fun fetchMapVersion() {
        CoroutineScope(Dispatchers.IO).launch {
            // 로딩 상태 설정
            _mapVersionResult.value = NetworkResult.Loading()

            val result = repository.handleApi {
                apiService.findRecentMapVersion()
            }

            // result 상태 설정
            _mapVersionResult.value = result
        }
    }

    fun updateMapVersion(mapVersion: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.updateMapVersion(mapVersion)
        }
    }
}