package com.example.navigationtoyproject

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.navigationtoyproject.databinding.ActivityMapBinding
import com.example.navigationtoyproject.dto.MapDataDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.gps.KN_DEFAULT_POS_X
import com.kakaomobility.knsdk.common.gps.KN_DEFAULT_POS_Y
import com.kakaomobility.knsdk.common.gps.WGS84ToKATEC
import com.kakaomobility.knsdk.common.util.DoublePoint
import com.kakaomobility.knsdk.map.knmaprenderer.objects.KNMapCameraUpdate
import com.kakaomobility.knsdk.map.knmapview.KNMapView
import com.kakaomobility.knsdk.map.uicustomsupport.renewal.KNMapMarker
import java.io.IOException


class MapActivity : AppCompatActivity() {
    companion object {
        private const val ICON_WIDTH = 45
        private const val ICON_HEIGHT = 120
    }

    private var mBinding: ActivityMapBinding? = null
    private val binding get() = mBinding!!
    private val markerMap = mutableMapOf<Int, KNMapMarker>()
    private var resizedIcon: Bitmap? = null

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

        // 지도 초기화
        initMapView(binding.mapView)
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

            // JSON 데이터 로드 및 파싱
            val markers = loadMockData()

            // 지도에 마커 추가
            markers.forEach { marker ->
                mapView.addCustomMarker(marker)
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

    private fun loadMockData(): List<MapDataDto> {
        val jsonString: String
        try {
            jsonString = assets.open("mock_data.json")
                .bufferedReader()
                .use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return emptyList()
        }

        val gson = Gson()
        val listMarkerType = object : TypeToken<List<MapDataDto>>() {}.type
        return gson.fromJson(jsonString, listMarkerType)
    }

    private fun resizeBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(source, width, height, true)
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