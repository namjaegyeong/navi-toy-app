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

class MapActivity : AppCompatActivity() {
    companion object {
        private const val ICON_WIDTH = 45
        private const val ICON_HEIGHT = 120
    }

    private var mBinding: ActivityMapBinding? = null
    private val binding get() = mBinding!!
    private val markerMap = mutableMapOf<Int, KNMapMarker>()
    private var resizedIcon: Bitmap? = null

    private var mapViewModel: MapViewModel? = null
    private var userPreferencesRepository: UserPreferencesRepository? = null

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

        val originalIcon = BitmapFactory.decodeResource(resources, R.drawable.signal_light)
        resizedIcon = resizeBitmap(originalIcon!!, ICON_WIDTH, ICON_HEIGHT)

        init()

        // 첫 번째 collectLatest: NetworkResult 상태 관찰
        lifecycleScope.launch {
            delay(3000L)

            checkAndHandleRequestFlag()

            mapViewModel?.mapDataList?.collectLatest { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is NetworkResult.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val markers = result.data?.mapDataList
                        val mapVersion = DataStoreHelper.getMapVersion(applicationContext).first() ?: "0.0.0"
                        markers?.forEach { marker ->
                            binding.mapView.addCustomMarker(marker)
                        }
                        binding.mapVersionTextView.text = "Map Version: $mapVersion"
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

        // Start the TerminationService
        val serviceIntent = Intent(this, TerminationService::class.java)
        startService(serviceIntent)
    }

    private fun init() {
        // 지도 초기화
        initMapView(binding.mapView)

        userPreferencesRepository = UserPreferencesRepository()

        // ViewModelFactory 생성
        val factory = MapViewModelFactory(userPreferencesRepository!!)

        // ViewModelProvider를 통해 ViewModel 인스턴스 얻기
        mapViewModel = ViewModelProvider(this, factory)[MapViewModel::class.java]
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

    // 지도 SDK에 맞게 마커 추가 로직 구현
    // 커스텀 이미지는 Bitmap으로 변환 후 마커에 설정
    private fun KNMapView.addCustomMarker(dto: MapDataDto) {
        val markerId = dto.customLocationMarkerId
        val xCoordinate = dto.wgs84_x
        val yCoordinate = dto.wgs84_y

        if (markerMap.containsKey(markerId)) return
        markerMap[markerId] = KNMapMarker(WGS84ToKATEC(xCoordinate, yCoordinate).toFloatPoint()).apply {
            icon = resizedIcon
            tag = markerId
            this@addCustomMarker.addMarker(this)
        }
    }

    private fun resizeBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private suspend fun checkAndHandleRequestFlag() {
        val requestFlag = DataStoreHelper.getRequestFlag(applicationContext).first() ?: 0
        if (requestFlag == 1) {
            Log.d("MainActivity", "Request flag is set to 1. Performing API request.")

            // Perform the desired API request
            mapViewModel?.findMapDataList(
                responseMapVersionDto = ResponseMapVersionDto(
                    mapVersion = DataStoreHelper.getMapVersion(applicationContext).first() ?: "0.0.0"
                )
            )

            // Reset the request flag to 0 after handling
            DataStoreHelper.resetRequestFlag(applicationContext)
            Log.d("MainActivity", "Request flag reset to 0.")
        }
    }

    override fun onDestroy() {
        mBinding = null
        super.onDestroy()
        // Bitmap 회수
        if (!resizedIcon!!.isRecycled) {
            resizedIcon!!.recycle()
        }
    }
}