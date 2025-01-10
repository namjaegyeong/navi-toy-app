package com.example.navigationtoyproject

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.navigationtoyproject.api.model.MapDataDto
import com.example.navigationtoyproject.api.model.NetworkResult
import com.example.navigationtoyproject.api.model.ResponseMapVersionDto
import com.example.navigationtoyproject.databinding.ActivityMapBinding
import com.example.navigationtoyproject.datastore.DataStoreHelper
import com.example.navigationtoyproject.datastore.MapDataMapper
import com.example.navigationtoyproject.datastore.MapDataStoreManager
import com.example.navigationtoyproject.factory.MapViewModelFactory
import com.example.navigationtoyproject.repository.UserPreferencesRepository
import com.example.navigationtoyproject.service.TerminationService
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.gps.KN_DEFAULT_POS_X
import com.kakaomobility.knsdk.common.gps.KN_DEFAULT_POS_Y
import com.kakaomobility.knsdk.common.gps.WGS84ToKATEC
import com.kakaomobility.knsdk.common.util.DoublePoint
import com.kakaomobility.knsdk.map.knmaprenderer.objects.KNMapCameraUpdate
import com.kakaomobility.knsdk.map.knmapview.KNMapView
import com.kakaomobility.knsdk.map.uicustomsupport.renewal.KNMapMarker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 *  카카오 맵 초기화 후,
 *  DataStore 에서 가져온 커스텀 마커를 ON/OFF 할 수 있는 Activity
 */

class MapActivity : AppCompatActivity() {
    companion object {
        private const val ICON_WIDTH = 45
        private const val ICON_HEIGHT = 120
    }

    private var mBinding: ActivityMapBinding? = null
    private val binding get() = mBinding!!
    private var resizedIcon: Bitmap? = null

    private lateinit var mapViewModel: MapViewModel
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private var markersVisible: Boolean = true

    private var markers: List<MapData> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMapBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 마커 아이콘 크기 조절
        val originalIcon = BitmapFactory.decodeResource(resources, R.drawable.signal_light)
        resizedIcon = resizeBitmap(originalIcon!!, ICON_WIDTH, ICON_HEIGHT)

        init()

        // 코루틴으로 마커 데이터 처리
        lifecycleScope.launch {
            // 테스트용 목데이터 생성 (필요시 주석 해제)
            // makeMockData()

            // 카카오 Map 초기화 완료 대기
            delay(3000)

            // 맵 버전을 체크하는 서비스 종료 이후, 만약 맵 버전이 최신화되지 않았다면(requestFlag == 1), 최신화된 주제도를 요청
            // 맵 버전이 최신화됐다면(requestFlag == 0), 저장된 주제도를 표출
            checkAndHandleRequestFlag()

            // 현재 저장된 맵 버전 수집
            val mapVersion = DataStoreHelper.getMapVersion(applicationContext).first() ?: "0.0.0"
            binding.mapVersionTextView.text = "Map Version: $mapVersion"

            // ViewModel 에서 커스텀 마커 데이터 수집
            mapViewModel.mapDataList.collectLatest { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is NetworkResult.Success -> {
                        binding.progressBar.visibility = View.GONE

                        val mapDataList = result.data?.mapDataList!!
                        MapDataStoreManager.setMapDataList(this@MapActivity, MapDataMapper.dtoListToProtoList(mapDataList))
                        markers = MapDataStoreManager.getMapDataList(this@MapActivity).first()?.mapDataListList ?: listOf()

                        markers.forEach { marker ->
                            binding.mapView.addCustomMarker(marker)
                        }
                    }
                    is NetworkResult.Error -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        // TerminationService 시작
        val serviceIntent = Intent(this, TerminationService::class.java)
        startService(serviceIntent)
    }

    private fun init() {
        // 지도 초기화
        initMapView(binding.mapView)

        userPreferencesRepository = UserPreferencesRepository()

        // ViewModelFactory 생성
        val factory = MapViewModelFactory(userPreferencesRepository)

        // ViewModelProvider를 통해 ViewModel 인스턴스 생성
        mapViewModel = ViewModelProvider(this, factory)[MapViewModel::class.java]

        // 마커 표시 토글 버튼 설정
        binding.overlayButton.setOnClickListener {
            if (markersVisible) {
                removeMarkers()
                binding.overlayButton.text = "Show Markers"
            } else {
                addMarkers()
                binding.overlayButton.text = "Hide Markers"
            }
            markersVisible = !markersVisible
        }
    }

    private fun initMapView(mapView: KNMapView) {
        KNSDK.bindingMapView(mapView, mapView.mapTheme) { error ->
            if (error != null) {
                Toast.makeText(this, "맵 초기화 작업이 실패하였습니다. \n[${error.code}] : ${error.msg}",Toast.LENGTH_LONG).show()
                return@bindingMapView
            }

            val lastPos = KNSDK.sharedGpsManager()?.lastValidGpsData?.pos ?: DoublePoint(KN_DEFAULT_POS_X.toDouble(), KN_DEFAULT_POS_Y.toDouble())
            val center = WGS84ToKATEC(127.4534669,36.6298231)

            mapView.moveCamera(KNMapCameraUpdate.targetTo(center.toFloatPoint()).zoomTo(2.5f).tiltTo(0f), withUserLocation = false, useNorthHeadingMode = false)
            mapView.userLocation?.apply {
                isVisible = true
                isVisibleGuideLine = true
                coordinate = center.toFloatPoint()
            }
        }
    }

    // 지도에 커스텀 마커 추가 함수
    // 커스텀 이미지는 Bitmap으로 변환 후 커스텀 마커 설정
    private fun KNMapView.addCustomMarker(data: MapData) {
        val markerId = data.customLocationMarkerId
        val xCoordinate = data.wgs84X
        val yCoordinate = data.wgs84Y

        KNMapMarker(WGS84ToKATEC(xCoordinate, yCoordinate).toFloatPoint()).apply {
            icon = resizedIcon
            tag = markerId
            this@addCustomMarker.addMarker(this)
        }
    }

    // Bitmap 크기 조절 함수
    private fun resizeBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    // 요청 플래그 확인 및 처리 함수
    private suspend fun checkAndHandleRequestFlag() {
        val requestFlag = DataStoreHelper.getRequestFlag(applicationContext).first() ?: 0
        if (requestFlag == 1) {
            Log.d("MainActivity", "Request flag is set to 1. Performing API request.")

            mapViewModel.findMapDataList(
                responseMapVersionDto = ResponseMapVersionDto(
                    mapVersion = DataStoreHelper.getMapVersion(applicationContext).first() ?: "0.0.0"
                )
            )

            // 처리 후 요청 플래그를 0으로 리셋
            DataStoreHelper.resetRequestFlag(applicationContext)
            Log.d("MainActivity", "Request flag reset to 0.")
        } else {
            markers =  MapDataStoreManager.getMapDataList(this@MapActivity).first()?.mapDataListList ?: listOf()

            markers.forEach { marker ->
                binding.mapView.addCustomMarker(marker)
            }
        }
    }

    // 테스트용 목데이터 생성 및 DataStore에 저장 함수
    private suspend fun makeMockData() {
        DataStoreHelper.setMapVersion(applicationContext, "1.0.2")

        val mockList = listOf(
            MapDataDto(1, 127.451386, 36.631166, "1.0.2"),
            MapDataDto(2, 127.452995, 36.632604, "1.0.2"),
            MapDataDto(3, 127.454529, 36.630899, "1.0.2"),
            MapDataDto(4, 127.456138, 36.629987, "1.0.2"),
            MapDataDto(5, 127.455806, 36.628351, "1.0.2"),
            MapDataDto(6, 127.457149, 36.625925, "1.0.2"),
            MapDataDto(7, 127.452365, 36.628352, "1.0.2"),
            MapDataDto(8, 127.452166, 36.626559, "1.0.2"),
            MapDataDto(9, 127.454579, 36.627479, "1.0.2"),
            MapDataDto(10, 127.455423, 36.626040, "1.0.2")
        )

        MapDataStoreManager.setMapDataList(this@MapActivity, MapDataMapper.dtoListToProtoList(mockList))
    }

    // 마커 추가 함수
    private fun addMarkers() {
        markers.forEach { marker ->
            binding.mapView.addCustomMarker(marker)
        }
    }

    // 모든 마커 제거 함수
    private fun removeMarkers() {
        binding.mapView.removeMarkersAll()
    }

    override fun onDestroy() {
        mBinding = null
        super.onDestroy()
        // Bitmap 자원 해제
        resizedIcon?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }
}