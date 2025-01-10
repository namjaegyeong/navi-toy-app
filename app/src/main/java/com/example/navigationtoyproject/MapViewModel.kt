package com.example.navigationtoyproject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.navigationtoyproject.api.di.RetrofitClient.apiService
import com.example.navigationtoyproject.api.model.MapDataListDto
import com.example.navigationtoyproject.api.model.NetworkResult
import com.example.navigationtoyproject.api.model.ResponseMapVersionDto
import com.example.navigationtoyproject.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val repository: UserPreferencesRepository
) : ViewModel() {

    // Backing property for network result
    private val _mapVersionResult = MutableStateFlow<NetworkResult<ResponseMapVersionDto>?>(null)
    val mapVersionResult: StateFlow<NetworkResult<ResponseMapVersionDto>?> = _mapVersionResult

    // Backing property for map data list
    private val _mapDataList = MutableStateFlow<NetworkResult<MapDataListDto>?>(null)
    val mapDataList: StateFlow<NetworkResult<MapDataListDto>?> = _mapDataList

    // 맵 버전 정보 API 요청
    fun fetchMapVersion() {
        viewModelScope.launch {
            // 로딩 상태 설정
            _mapVersionResult.value = NetworkResult.Loading()

            val result = repository.handleApi {
                apiService.findRecentMapVersion()
            }

            // result 상태 설정
            _mapVersionResult.value = result
        }
    }

    // 맵 데이터 리스트 API 요청
    fun findMapDataList(responseMapVersionDto: ResponseMapVersionDto) {
        viewModelScope.launch {
            _mapDataList.value = NetworkResult.Loading()

            val result = repository.handleApi {
                apiService.findMapDataList(responseMapVersionDto)
            }

            _mapDataList.value = result
        }
    }
}