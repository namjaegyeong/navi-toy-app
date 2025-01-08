package com.example.navigationtoyproject

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.navigationtoyproject.databinding.ActivityMainBinding
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError_Code_C103
import com.kakaomobility.knsdk.common.objects.KNError_Code_C302

class MainActivity : AppCompatActivity() {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val NAVI_ROTATION = "app debug"
    }

    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        initializeSDK()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // 앱 초기화
    private fun initializeSDK() {
        KNSDK.initializeWithAppKey(
            getString(R.string.KAKAO_NATIVE_APP_KEY),
            getString(R.string.VERSION_NAME),
            null,
            KNLanguageType.KNLanguageType_KOREAN,
            aCompletion = {
                if (it != null) {
                    when (it.code) {
                        KNError_Code_C103 -> {
                            Log.d(NAVI_ROTATION, "내비 인증 실패: $it")
                            return@initializeWithAppKey
                        }

                        KNError_Code_C302 -> {
                            Log.d(NAVI_ROTATION, "내비 권한 오류 : $it")
                            requestLocationPermission()
                            return@initializeWithAppKey
                        }

                        else -> {
                            Log.d(NAVI_ROTATION, "내비 초기화 실패: $it")
                            return@initializeWithAppKey
                        }
                    }
                } else {
                    Log.d(NAVI_ROTATION, "내비 초기화 성공")

                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            })
    }

    // 런타임 권한 요청
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // 런타임 권한 처리 결과 핸들링
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한 승인 시
                    initializeSDK()
                } else {
                    // 권한 거절 시
                    Toast.makeText(
                        this,
                        "Location permission is required to use this feature.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        mBinding = null
        super.onDestroy()
    }
}