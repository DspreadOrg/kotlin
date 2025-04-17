package com.dspread.demoui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.dspread.demoui.R
import kotlinx.android.synthetic.main.activity_welcome.audio
import kotlinx.android.synthetic.main.activity_welcome.normal_bluetooth
import kotlinx.android.synthetic.main.activity_welcome.other_bluetooth
import kotlinx.android.synthetic.main.activity_welcome.serial_port

/**
 *Time:2020/5/20
 *Author:Qianmeng Chen
 *Description:
 */
class WelcomeActivity : BaseActivity(), View.OnClickListener {
    private val mIntent: Intent? = null
    private val LOCATION_CODE = 101

    //【位置管理】
    private var lm: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.title_welcome))
        other_bluetooth.isEnabled = false
        audio.isEnabled = false
        audio.setOnClickListener(this)
        serial_port.setOnClickListener(this)
        normal_bluetooth.setOnClickListener(this)
        other_bluetooth.setOnClickListener(this)
        bluetoothRelaPer()
    }

    override fun onToolbarLinstener() {
    }

    override val layoutId: Int
        get() {
            return R.layout.activity_welcome
        }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.audio -> {
                intent = Intent(this, OtherActivity::class.java)
                intent.putExtra("connect_type", 1)
                startActivity(intent)
            }

            R.id.serial_port -> {
                intent = Intent(this, OtherActivity::class.java)
                intent.putExtra("connect_type", 2)
                startActivity(intent)
            }

            R.id.normal_bluetooth -> {
                intent = Intent(this, MainActivity::class.java)
                intent.putExtra("connect_type", 3)
                startActivity(intent)
            }

            R.id.other_bluetooth -> {
                intent = Intent(this, MainActivity::class.java)
                intent.putExtra("connect_type", 4)
                startActivity(intent)
            }
        }
    }

    val BLUETOOTH_CODE: Int = 100

    @SuppressLint("WrongConstant")
    fun bluetoothRelaPer() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null && !adapter.isEnabled) { //表示蓝牙不可用 add one fix
            val enabler = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enabler)
        }
        lm = this@WelcomeActivity.getSystemService("location") as LocationManager
        val ok = lm!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (ok) { //开了定位服务

            if (ContextCompat.checkSelfPermission(
                    this@WelcomeActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                !== PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(
                            this@WelcomeActivity,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) !== PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                            this@WelcomeActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) !== PackageManager.PERMISSION_GRANTED || (ContextCompat.checkSelfPermission(
                            this@WelcomeActivity, Manifest.permission.BLUETOOTH_ADVERTISE
                        ) !== PackageManager.PERMISSION_GRANTED)
                    ) {
                        val list = arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                        )
                        ActivityCompat.requestPermissions(
                            this@WelcomeActivity,
                            list,
                            BLUETOOTH_CODE
                        )
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        this@WelcomeActivity,
                        arrayOf<String>(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ),
                        LOCATION_CODE
                    )
                }

            } else {
                // have permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                    if (ContextCompat.checkSelfPermission(
                            this@WelcomeActivity,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) !== PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                            this@WelcomeActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) !== PackageManager.PERMISSION_GRANTED || (ContextCompat.checkSelfPermission(
                            this@WelcomeActivity, Manifest.permission.BLUETOOTH_ADVERTISE
                        ) !== PackageManager.PERMISSION_GRANTED)
                    ) {

                        val list = arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                        )
                        ActivityCompat.requestPermissions(
                            this@WelcomeActivity,
                            list,
                            BLUETOOTH_CODE
                        )
                    }
                }
            }


        } else {
            Log.e("BRG", "系统检测到未开启GPS定位服务")
            Toast.makeText(this@WelcomeActivity, "系统检测到未开启GPS定位服务", Toast.LENGTH_SHORT)
                .show()
            val intent = Intent()
            intent.action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
            startActivityForResult(intent, 1315)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_CODE -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // 权限被用户同意。
                    Toast.makeText(
                        this@WelcomeActivity,
                        getString(R.string.msg_allowed_location_permission),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // 权限被用户拒绝了。
                    Toast.makeText(
                        this@WelcomeActivity,
                        getString(R.string.msg_not_allowed_loaction_permission),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


}