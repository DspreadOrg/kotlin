package com.dspread.demoui.activities

//import Decoder.BASE64Encoder
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.view.ViewGroup.MarginLayoutParams
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.dspread.demoui.R
import com.dspread.demoui.USBClass
import com.dspread.demoui.utils.*
import com.dspread.demoui.utils.FileUtils
import com.dspread.demoui.widget.InnerListview
import com.dspread.xpos.CQPOSService
import com.dspread.xpos.QPOSService
import com.dspread.xpos.QPOSService.*
import com.dspread.xpos.QPOSService.Display
import com.dspread.xpos.utils.BASE64Encoder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_mifare.*
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*


/**
 *Time:2020/5/19
 *Author:Qianmeng Chen
 *Description:
 */
class MainActivity : BaseActivity(), ShowGuideView.onGuideViewListener {
    private var pos: QPOSService? = null
    private var updateThread: UpdateThread? = null
    private var usbDevice: UsbDevice? = null
    private var cmdSp: Spinner? = null
    private var m_ListView: InnerListview? = null
    private var blockAdd: EditText? = null

    //    private EditText statusEditText, blockAdd, status,status11
    private var m_Adapter: MyListViewAdapter? = null
    private lateinit var imvAnimScan: ImageView
    private lateinit var animScan: AnimationDrawable
    private lateinit var appListView: ListView
    private lateinit var lstDevScanned: List<BluetoothDevice>

    //private List<TagApp> appList
    private lateinit var mafireLi: LinearLayout
    private lateinit var mafireUL: LinearLayout
    private var dialog: Dialog? = null
    private lateinit var mafireSpinner: Spinner
    private lateinit var doTradeButton: Button
    private lateinit var operateCardBtn: Button
    private lateinit var pollBtn: Button
    private lateinit var pollULbtn: Button
    private lateinit var veriftBtn: Button
    private lateinit var veriftULBtn: Button
    private lateinit var readBtn: Button
    private lateinit var writeBtn: Button
    private lateinit var finishBtn: Button
    private lateinit var finishULBtn: Button
    private lateinit var getULBtn: Button
    private lateinit var readULBtn: Button
    private lateinit var fastReadUL: Button
    private lateinit var writeULBtn: Button
    private lateinit var transferBtn: Button
    private lateinit var btnQuickEMV: Button
    private lateinit var btnDisconnect: Button
    private lateinit var updateFwBtn: Button
    private var btnBT: Button? = null
    private lateinit var mKeyIndex: EditText
    private var btnUSB: Button? = null

    private var nfcLog: String? = ""
    private var pubModel = ""
    private var amount = ""
    private var cashbackAmount = ""
    private var blueTootchAddress = ""
    private var blueTitle = ""
    private var mTitle = ""
    private var isPinCanceled = false
    private var isNormalBlu = false//to judge if is normal bluetooth
    private var type = 0
    private lateinit var showGuideView: ShowGuideView

    private var posType = POS_TYPE.BLUETOOTH
    private var selectBTFlag = false
    private var start_time = 0L

    private companion object {
        const val FILENAME = "dsp_axdd"
        const val REQUEST_WRITE_EXTERNAL_STORAGE = 1001
        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        initView()
        initIntent()
        initListener()
    }

    override fun onToolbarLinstener() {
        finish()
    }

    override val layoutId: Int
        get() {
            return R.layout.activity_main
        }

    override fun onGuideListener(btn: Button?) {
        when (btn?.id) {
            R.id.doTradeButton -> showGuideView.show(
                btnDisconnect,
                this@MainActivity,
                getString(R.string.msg_disconnect)
            )

            R.id.disconnect -> showGuideView.show(
                btnUSB,
                this@MainActivity,
                getString(R.string.msg_conn_usb)
            )

            R.id.btnBT -> showGuideView.show(
                doTradeButton,
                this@MainActivity,
                getString(R.string.msg_do_trade)
            )
        }
    }

    private enum class POS_TYPE {
        BLUETOOTH, AUDIO, UART, USB, OTG, BLUETOOTH_BLE
    }

    private fun onBTPosSelected(activity: Activity, itemView: View, index: Int) {
        if (isNormalBlu) {
            pos!!.stopScanQPos2Mode()
        } else {
            pos!!.stopScanQposBLE()
        }
        start_time = Date().time
        val dev = m_Adapter!!.getItem(index) as Map<String, Any>
        blueTootchAddress = dev["ADDRESS"] as String
        blueTitle = dev.get("TITLE") as String
        blueTitle = blueTitle.split("\\(")[0]
        sendMsg(1001)
    }

    @SuppressLint("MissingPermission")
    private fun generateAdapterData(): List<Map<String, Any>> {
        if (isNormalBlu) {
            lstDevScanned = pos!!.getDeviceList()
            TRACE.i("=====" + lstDevScanned.size)
        } else {
            lstDevScanned = pos!!.getBLElist()
        }
        TRACE.d("lstDevScanned----$lstDevScanned")
        var data = ArrayList<Map<String, Any>>()
        for (dev: BluetoothDevice in lstDevScanned) {
            TRACE.i("++++++++++")
            var itm = HashMap<String, Any>()
            itm.put(
                "ICON",
                if (dev.bondState == BluetoothDevice.BOND_BONDED) Integer.valueOf(R.drawable.bluetooth_blue) else Integer.valueOf(
                    R.drawable.bluetooth_blue_unbond
                )
            )
            itm.put("TITLE", dev.name + "(" + dev.address + ")")
            itm.put("ADDRESS", dev.address)
            data.add(itm)
        }
        return data
    }

    private fun refreshAdapter() {
        if (m_Adapter != null) {
            m_Adapter!!.clearData()
            m_Adapter = null
        }
        var data = generateAdapterData()
        val mLayoutInflater = LayoutInflater.from(this@MainActivity)
        m_Adapter = MyListViewAdapter(
            this@MainActivity,
            data as MutableList<Map<String, Any>>,
            mLayoutInflater
        )
        m_ListView!!.adapter = (m_Adapter)
        TRACE.w("m_Adapter$m_Adapter")
        setListViewHeightBasedOnChildren(m_ListView!!)
    }

    private class MyListViewAdapter(
        private val context: Context,
        private val m_DataMap: MutableList<Map<String, Any>>,
        private val m_Inflater: LayoutInflater
    ) : BaseAdapter() {
        fun clearData() {
            m_DataMap.clear()
        }

        fun addData(map: Map<String, Any>) {
            m_DataMap.add(map)
        }

        override fun getCount(): Int {
            TRACE.w("getCount " + m_DataMap!!.size)
            return m_DataMap!!.size
        }

        override fun getItem(position: Int): Any {
            return m_DataMap!![position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View?
            if (convertView == null) {
                view = m_Inflater!!.inflate(R.layout.bt_qpos_item, null)
            } else {
                view = convertView
            }
            TRACE.w("view+ $view")
            var m_Icon = view!!.findViewById<ImageView>(R.id.item_iv_icon)
            var m_TitleName = view!!.findViewById<TextView>(R.id.item_tv_lable)
            var itemdata = m_DataMap!!.get(position)
            var idIcon = itemdata.get("ICON") as Int
            var sTitleName = itemdata.get("TITLE") as String
            m_Icon.setBackgroundResource(idIcon)
            m_TitleName.text = sTitleName
            return view
        }

    }

    //set listview's height
    private fun setListViewHeightBasedOnChildren(listView: ListView) {
        var listAdapter: ListAdapter? = listView.adapter ?: return
        var totalHeight = 0
        TRACE.w("listAdapter" + listAdapter!!.count)
        if (listAdapter!!.count == 0) {
            return
        }
        for (i in 0 until listAdapter!!.count) {
            TRACE.w("count$i")
            var listItem = listAdapter.getView(i, null, listView)
            listItem.measure(0, 0)
            totalHeight += listItem.measuredHeight
        }
        var params = listView.layoutParams
        params.height = totalHeight
        +(listView.dividerHeight * (listAdapter.count - 1))
        (params as (MarginLayoutParams)).setMargins(10, 10, 10, 10)
        listView.layoutParams = params
    }

    private fun initIntent() {
        var intent = intent
        type = intent.getIntExtra("connect_type", 0)
        when (type) {
            3 -> {//normal bluetooth
                btnBT!!.visibility = View.VISIBLE
                this.isNormalBlu = true
                mTitle = getString(R.string.title_blu)
            }

            4 -> {//Ble
                btnBT!!.visibility = View.VISIBLE
                isNormalBlu = false
                mTitle = getString(R.string.title_ble)
            }
        }
        setTitle(mTitle)
    }

    private fun initView() {
        showGuideView = ShowGuideView()
        imvAnimScan = findViewById(R.id.img_anim_scanbt)
        animScan = getResources().getDrawable(R.drawable.progressanmi) as AnimationDrawable
        imvAnimScan.setBackgroundDrawable(animScan)
        mafireLi = findViewById(R.id.mifareid)
        mafireUL = findViewById(R.id.ul_ll)
//        status = (EditText) findViewById(R.id.status)
//        status11 = (EditText) findViewById(R.id.status11)
        operateCardBtn = findViewById(R.id.operate_card)
        updateFwBtn = findViewById(R.id.updateFW)
        cmdSp = findViewById(R.id.cmd_spinner)
        var cmdList = arrayOf("add", "reduce", "restore")
        var cmdAdapter =
            ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_item, cmdList)
        cmdSp!!.adapter = (cmdAdapter)
        mafireSpinner = findViewById(R.id.verift_spinner)
        blockAdd = findViewById(R.id.block_address)
        var keyClass = arrayOf("Key A", "Key B")
        var spinneradapter =
            ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_item, keyClass)
        mafireSpinner.setAdapter(spinneradapter)
        doTradeButton = findViewById(R.id.doTradeButton)//start to do trade
        btnUSB = findViewById(R.id.btnUSB)
        btnBT = findViewById(R.id.btnBT)
//        statusEditText = (EditText) findViewById(R.id.statusEditText)
        btnDisconnect = findViewById(R.id.disconnect)//disconnect
        btnQuickEMV = findViewById(R.id.btnQuickEMV)
        pollBtn = findViewById(R.id.search_card)
        pollULbtn = findViewById(R.id.poll_ulcard)
        veriftBtn = findViewById(R.id.verify_card)
        veriftULBtn = findViewById(R.id.verify_ulcard)
        readBtn = findViewById(R.id.read_card)
        writeBtn = findViewById(R.id.write_card)
        finishBtn = findViewById(R.id.finish_card)
        finishULBtn = findViewById(R.id.finish_ulcard)
        getULBtn = findViewById(R.id.get_ul)
        readULBtn = findViewById(R.id.read_ulcard)
        fastReadUL = findViewById(R.id.fast_read_ul)
        writeULBtn = findViewById(R.id.write_ul)
        transferBtn = findViewById(R.id.transfer_card)
        var parentScrollView = findViewById<ScrollView>(R.id.parentScrollview)
        parentScrollView.smoothScrollTo(0, 0)
        m_ListView = findViewById<InnerListview>(R.id.lv_indicator_BTPOS)
        mKeyIndex = findViewById(R.id.keyindex)
        btnBT!!.post(Runnable {
            showGuideView.show(btnBT, this@MainActivity, getString(R.string.msg_select_device))
        })
        showGuideView.setListener(this)
    }

    private fun initListener() {
        m_ListView!!.onItemClickListener = OnItemClickListener { _, view, position, _ ->
            if (view != null) {
                onBTPosSelected(this@MainActivity, view, position)
            }
            m_ListView!!.setVisibility(View.GONE)
            animScan.stop()
            imvAnimScan.setVisibility(View.GONE)
        }
        var myOnClickListener = MyOnClickListener()
        //btn click
        doTradeButton.setOnClickListener(myOnClickListener)
        btnBT!!.setOnClickListener(myOnClickListener)
        btnDisconnect.setOnClickListener(myOnClickListener)
        btnUSB!!.setOnClickListener(myOnClickListener)
        updateFwBtn.setOnClickListener(myOnClickListener)
        btnQuickEMV.setOnClickListener(myOnClickListener)
        pollBtn.setOnClickListener(myOnClickListener)
        pollULbtn.setOnClickListener(myOnClickListener)
        finishBtn.setOnClickListener(myOnClickListener)
        finishULBtn.setOnClickListener(myOnClickListener)
        readBtn.setOnClickListener(myOnClickListener)
        writeBtn.setOnClickListener(myOnClickListener)
        veriftBtn.setOnClickListener(myOnClickListener)
        veriftULBtn.setOnClickListener(myOnClickListener)
        operateCardBtn.setOnClickListener(myOnClickListener)
        getULBtn.setOnClickListener(myOnClickListener)
        readULBtn.setOnClickListener(myOnClickListener)
        fastReadUL.setOnClickListener(myOnClickListener)
        writeULBtn.setOnClickListener(myOnClickListener)
        transferBtn.setOnClickListener(myOnClickListener)
    }

    /**
     * open and init bluetooth
     *
     * @param mode
     */
    private fun open(mode: QPOSService.CommunicationMode) {
        TRACE.d("open")
        //pos=null
//        MyPosListener listener = new MyPosListener()
        var listener = MyQposClass()
        pos = QPOSService.getInstance(this, mode)
        if (pos == null) {
            statusEditText.setText("CommunicationMode unknow")
            return
        }
        if (mode == CommunicationMode.USB_OTG_CDC_ACM) {
            pos!!.setUsbSerialDriver(QPOSService.UsbOTGDriver.CDCACM)
        }
//        pos!!.setConext(this@MainActivity)
        //init handler
//        var handler = Handler(Looper.myLooper())
        pos!!.initListener(listener)

        var sdkVersion = pos!!.getSdkVersion()
        Toast.makeText(this@MainActivity, "sdkVersion--" + sdkVersion, Toast.LENGTH_SHORT).show()
    }

    /**
     * close device
     */
    private fun close() {
        TRACE.d("close")
        if (pos == null) {
            return
        } else if (posType == POS_TYPE.AUDIO) {
//            pos!!.closeAudio()
        } else if (posType == POS_TYPE.BLUETOOTH) {
            pos!!.disconnectBT()
//			pos!!.disConnectBtPos()
        } else if (posType == POS_TYPE.BLUETOOTH_BLE) {
            pos!!.disconnectBLE()
        } else if (posType == POS_TYPE.UART) {
            pos!!.closeUart()
        } else if (posType == POS_TYPE.USB) {
            pos!!.closeUsb()
        } else if (posType == POS_TYPE.OTG) {
            pos!!.closeUsb()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (pos != null) {
//            if (pos!!.getAudioControl()) {
            audioitem!!.setTitle(R.string.audio_open)
//            } else {
//                audioitem!!.setTitle(R.string.audio_close)
//            }
        } else {
            audioitem!!.setTitle(R.string.audio_unknow)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    var audioitem: MenuItem? = null
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        var inflater = getMenuInflater()
        inflater.inflate(R.menu.activity_main, menu)
        audioitem = menu!!.findItem(R.id.audio_test)
        if (pos != null) {
//            if (pos!!.getAudioControl()) {
            audioitem!!.setTitle("Audio Control : Open")
//            } else {
//                audioitem!!.setTitle("Audio Control : Close")
//            }
        } else {
            audioitem!!.setTitle("Audio Control : Check")
        }
        return true
    }

    inner class UpdateThread : Thread() {
        override fun run() {
            super.run()
            while (!concelFlag) {
                var i = 0
                while (!concelFlag && i < 100) {
                    try {
                        Thread.sleep(1)
                    } catch (e: InterruptedException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                    i++
                }
                if (concelFlag) {
                    break
                }
                if (pos == null) {
                    return
                }
                var progress = pos!!.getUpdateProgress()
                if (progress < 100) {

                    this@MainActivity.runOnUiThread(Runnable {
                        statusEditText.setText(progress.toString() + "%")
                    })
                    continue
                }
                this@MainActivity.runOnUiThread(Runnable() {
                    statusEditText.setText("Update Finished 100%")
                })
                break
            }
        }

        private var concelFlag = false
        public fun concelSelf() {
            concelFlag = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return super.onOptionsItemSelected(item)
        if (pos == null) {
            Toast.makeText(getApplicationContext(), "Device Disconnect", Toast.LENGTH_LONG).show()
            return true
        } else if (item!!.getItemId() == R.id.reset_qpos) {
            var a = pos!!.resetPosStatus()
            if (a) {
                statusEditText.setText("pos reset")
            }
        } else if (item.getItemId() == R.id.doTradeLogOperation) {
            pos!!.doTradeLogOperation(DoTransactionType.GetAll, 0)
        } else if (item.getItemId() == R.id.get_update_key) {//get the key value
            pos!!.getUpdateCheckValue()
        } else if (item.getItemId() == R.id.get_device_public_key) {//get the key value

            pos!!.getDevicePublicKey(5)
        } else if (item.getItemId() == R.id.set_sleepmode_time) {//set pos sleep mode time
//            0~Integer.MAX_VALUE
            pos!!.setSleepModeTime(20)//the time is in 10s and 10000s
        } else if (item.getItemId() == R.id.set_shutdowm_time) {
            pos!!.setShutDownTime(15 * 60)
        }
        //update ipek
        else if (item.getItemId() == R.id.updateIPEK) {
            var keyIndex = getKeyIndex()
            var ipekGrop = "0" + keyIndex
            pos!!.doUpdateIPEKOperation(
                ipekGrop,
                "09118012400705E00000",
                "C22766F7379DD38AA5E1DA8C6AFA75AC",
                "B2DE27F60A443944",
                "09118012400705E00000",
                "C22766F7379DD38AA5E1DA8C6AFA75AC",
                "B2DE27F60A443944",
                "09118012400705E00000",
                "C22766F7379DD38AA5E1DA8C6AFA75AC",
                "B2DE27F60A443944"
            )
        } else if (item.getItemId() == R.id.getSleepTime) {
            pos!!.getShutDownTime()
        } else if (item.getItemId() == R.id.updateEMVAPP) {
            statusEditText.setText("updating emvapp...")
            var appTLV =
                "9F0610000000000000000000000000000000005F2A0201565F3601009F01060000000000009F090200969F150200009F161e3030303030303030303030303030303030303030303030303030303030309F1A0201569F1B04000000009F1C0800000000000000009F1E0800000000000000009F33032009C89F3501229F3901009F3C0201569F3D01009F4005F000F0A0019F4E0f0000000000000000000000000000009F6604300040809F7B06000000001388DF010101DF11050000000000DF12050000000000DF13050000000000DF1400DF150400000000DF160100DF170100DF1906000000001388DF2006999999999999DF2106000000020000DF7208F4F0F0FAAFFE8000DF730101DF74010FDF750101DF7600DF7806000000020000DF7903E0F8C8DF7A05F000F0A001DF7B00"
            pos!!.updateEmvAPPByTlv(EMVDataOperation.Add, appTLV)

        } else if (item.getItemId() == R.id.updateEMVCAPK) {
            statusEditText.setText("updating emvcapk...")
            var capkTLV =
                "9F0605A0000003339F22010BDF0281f8CF9FDF46B356378E9AF311B0F981B21A1F22F250FB11F55C958709E3C7241918293483289EAE688A094C02C344E2999F315A72841F489E24B1BA0056CFAB3B479D0E826452375DCDBB67E97EC2AA66F4601D774FEAEF775ACCC621BFEB65FB0053FC5F392AA5E1D4C41A4DE9FFDFDF1327C4BB874F1F63A599EE3902FE95E729FD78D4234DC7E6CF1ABABAA3F6DB29B7F05D1D901D2E76A606A8CBFFFFECBD918FA2D278BDB43B0434F5D45134BE1C2781D157D501FF43E5F1C470967CD57CE53B64D82974C8275937C5D8502A1252A8A5D6088A259B694F98648D9AF2CB0EFD9D943C69F896D49FA39702162ACB5AF29B90BADE005BC157DF0314BD331F9996A490B33C13441066A09AD3FEB5F66CDF0403000003DF050420311222DF060101DF070101"
            pos!!.updateEmvCAPKByTlv(EMVDataOperation.Add, capkTLV)

        } else if (item.getItemId() == R.id.updateEMVByXml) {
            statusEditText.setText("updating emv config, please wait...")
            updateEmvConfigByXml()
        } else if (item.getItemId() == R.id.setBuzzer) {
            pos!!.doSetBuzzerOperation(3)//set buzzer
        } else if (item.getItemId() == R.id.menu_get_deivce_info) {
            statusEditText.setText(R.string.getting_info)
            pos!!.getQposInfo()

        } else if (item.getItemId() == R.id.menu_get_deivce_key_checkvalue) {
            statusEditText.setText("get_deivce_key_checkvalue..............")
            var keyIdex = getKeyIndex()
            pos!!.getKeyCheckValue(keyIdex, QPOSService.CHECKVALUE_KEYTYPE.DUKPT_MKSK_ALLTYPE)
        } else if (item.getItemId() == R.id.menu_get_pos_id) {
            pos!!.getQposId()
            statusEditText.setText(R.string.getting_pos_id)
        } else if (item.getItemId() == R.id.setMasterkey) {
            //key:0123456789ABCDEFFEDCBA9876543210
            //result；0123456789ABCDEFFEDCBA9876543210
            var keyIndex = getKeyIndex()
            pos!!.setMasterKey("1A4D672DCA6CB3351FD1B02B237AF9AE", "08D7B4FB629D0885", keyIndex)
        } else if (item.getItemId() == R.id.menu_get_pin) {
            statusEditText.setText(R.string.input_pin)
            pos!!.getPin(1, 0, 6, "please input pin", "622262XXXXXXXXX4406", "", 20)
        } else if (item.getItemId() == R.id.isCardExist) {
            pos!!.isCardExist(30)
        } else if (item.getItemId() == R.id.menu_operate_mafire) {
            showSingleChoiceDialog()
        } else if (item.getItemId() == R.id.menu_operate_update) {
            if (updateFwBtn.getVisibility() == View.VISIBLE || btnQuickEMV.getVisibility() == View.VISIBLE) {
                updateFwBtn.setVisibility(View.GONE)
                btnQuickEMV.setVisibility(View.GONE)
            } else {
                updateFwBtn.setVisibility(View.VISIBLE)
                btnQuickEMV.setVisibility(View.VISIBLE)
            }
        } else if (item.getItemId() == R.id.resetSessionKey) {
            //key：0123456789ABCDEFFEDCBA9876543210
            //result：0123456789ABCDEFFEDCBA9876543210
            var keyIndex = getKeyIndex()
//            pos!!.udpateWorkKey(
//                    "1A4D672DCA6CB3351FD1B02B237AF9AE", "08D7B4FB629D0885",//PIN KEY
//                    "1A4D672DCA6CB3351FD1B02B237AF9AE", "08D7B4FB629D0885",  //TRACK KEY
//                    "1A4D672DCA6CB3351FD1B02B237AF9AE", "08D7B4FB629D0885", //MAC KEY
//                    keyIndex, 5)
        } else if (item.getItemId() == R.id.cusDisplay) {
            deviceShowDisplay("test info")
        } else if (item.getItemId() == R.id.closeDisplay) {

            pos!!.lcdShowCloseDisplay()

        }
        return true
    }

    override fun onPause() {
        super.onPause()
        TRACE.d("onPause")
        if (type == 3 || type == 4) {
            if (pos != null) {
                if (isNormalBlu) {
                    //stop to scan bluetooth
                    pos!!.stopScanQPos2Mode()
                } else {
                    //stop to scan ble
                    pos!!.stopScanQposBLE()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TRACE.d("onDestroy")
        if (updateThread != null) {
            updateThread!!.concelSelf()
        }
        if (pos != null) {
            close()
//            pos = null
        }
    }

    private var yourChoice = 0
    private fun showSingleChoiceDialog() {
        var items: Array<String> = arrayOf("Mifare classic 1", "Mifare UL")
//	    yourChoice = -1
        var singleChoiceDialog = AlertDialog.Builder(this@MainActivity)
        singleChoiceDialog.setTitle("please select one")
        singleChoiceDialog.setSingleChoiceItems(items, 0, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                yourChoice = which
            }
        })
        /* The second parameter is default */
        singleChoiceDialog.setPositiveButton("OK", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                if (yourChoice == 0) {
                    mafireLi.setVisibility(View.VISIBLE)//display m1 mafire card
                    mafireUL.setVisibility(View.GONE)//display ul mafire card
                } else if (yourChoice == 1) {
                    mafireLi.setVisibility(View.GONE)
                    mafireUL.setVisibility(View.VISIBLE)
                }
            }
        })
        singleChoiceDialog.show()
    }

    public fun dismissDialog() {
        if (dialog != null) {
            dialog!!.dismiss()
            dialog = null
        }
    }

    /**
     * @author qianmengChen
     * @ClassName: MyPosListener
     * @date: 2016-11-10 下午6:35:06
     */
    inner class MyQposClass : CQPOSService() {
        override fun onDoTradeResult(
            result: DoTradeResult?,
            decodeData: Hashtable<String, String>?
        ) {
            super.onDoTradeResult(result, decodeData)
            TRACE.d("(DoTradeResult result, Hashtable<String, String> decodeData) " + result.toString() + TRACE.NEW_LINE + "decodeData:" + decodeData)
            runOnUiThread {
                dismissDialog()
                var cardNo = ""
                if (result == DoTradeResult.NONE) {
                    statusEditText.setText(getString(R.string.no_card_detected))
                } else if (result == DoTradeResult.ICC) {
                    statusEditText.setText(getString(R.string.icc_card_inserted))
                    TRACE.d("EMV ICC Start")
                    pos!!.doEmvApp(EmvOption.START)
                } else if (result == DoTradeResult.NOT_ICC) {
                    statusEditText.setText(getString(R.string.card_inserted))
                } else if (result == DoTradeResult.BAD_SWIPE) {
                    statusEditText.setText(getString(R.string.bad_swipe))
                } else if (result == DoTradeResult.MCR) {//磁条卡
                    var content = getString(R.string.card_swiped)
                    val formatID = decodeData!!.get("formatID")
                    if (formatID.equals("31") || formatID.equals("40") || formatID.equals("37") || formatID.equals(
                            "17"
                        ) || formatID.equals("11") || formatID.equals("10")
                    ) {
                        val maskedPAN = decodeData!!.get("maskedPAN")
                        val expiryDate = decodeData!!.get("expiryDate")
                        val cardHolderName = decodeData!!.get("cardholderName")
                        val serviceCode = decodeData!!.get("serviceCode")
                        val trackblock = decodeData!!.get("trackblock")
                        val psamId = decodeData!!.get("psamId")
                        val posId = decodeData!!.get("posId")
                        val pinblock = decodeData!!.get("pinblock")
                        val macblock = decodeData!!.get("macblock")
                        val activateCode = decodeData!!.get("activateCode")
                        val trackRandomNumber = decodeData!!.get("trackRandomNumber")
                        content += getString(R.string.format_id) + " " + formatID + "\n"
                        content += getString(R.string.masked_pan) + " " + maskedPAN + "\n"
                        content += getString(R.string.expiry_date) + " " + expiryDate + "\n"
                        content += getString(R.string.cardholder_name) + " " + cardHolderName + "\n"
                        content += getString(R.string.service_code) + " " + serviceCode + "\n"
                        content += "trackblock: $trackblock\n"
                        content += "psamId: $psamId\n"
                        content += "posId: $posId\n"
                        content += getString(R.string.pinBlock) + " " + pinblock + "\n"
                        content += "macblock: $macblock\n"
                        content += "activateCode: $activateCode\n"
                        content += "trackRandomNumber: $trackRandomNumber\n"
                        cardNo = maskedPAN!!
                    } else if (formatID.equals("FF")) {
                        val type = decodeData!!.get("type")
                        val encTrack1 = decodeData!!.get("encTrack1")
                        val encTrack2 = decodeData!!.get("encTrack2")
                        val encTrack3 = decodeData!!.get("encTrack3")
                        content += "cardType: $type\n"
                        content += "track_1: $encTrack1\n"
                        content += "track_2: $encTrack2\n"
                        content += "track_3: $encTrack3\n"
                    } else {
                        val orderID = decodeData!!.get("orderId")
                        val maskedPAN = decodeData!!.get("maskedPAN")
                        val expiryDate = decodeData!!.get("expiryDate")
                        val cardHolderName = decodeData!!.get("cardholderName")
//					val ksn = decodeData!!.get("ksn")
                        val serviceCode = decodeData!!.get("serviceCode")
                        val track1Length = decodeData!!.get("track1Length")
                        val track2Length = decodeData!!.get("track2Length")
                        val track3Length = decodeData!!.get("track3Length")
                        val encTracks = decodeData!!.get("encTracks")
                        val encTrack1 = decodeData!!.get("encTrack1")
                        val encTrack2 = decodeData!!.get("encTrack2")
                        val encTrack3 = decodeData!!.get("encTrack3")
                        val partialTrack = decodeData!!.get("partialTrack")
                        // TODO
                        val pinKsn = decodeData!!.get("pinKsn")
                        val trackksn = decodeData!!.get("trackksn")
                        val pinBlock = decodeData!!.get("pinBlock")
                        val encPAN = decodeData!!.get("encPAN")
                        val trackRandomNumber = decodeData!!.get("trackRandomNumber")
                        val pinRandomNumber = decodeData!!.get("pinRandomNumber")
                        if (orderID != null && !"".equals(orderID)) {
                            content += "orderID:$orderID"
                        }
                        content += getString(R.string.format_id) + " " + formatID + "\n"
                        content += getString(R.string.masked_pan) + " " + maskedPAN + "\n"
                        content += getString(R.string.expiry_date) + " " + expiryDate + "\n"
                        content += getString(R.string.cardholder_name) + " " + cardHolderName + "\n"
//					content += getString(R.string.ksn) + " " + ksn + "\n"
                        content += getString(R.string.pinKsn) + " " + pinKsn + "\n"
                        content += getString(R.string.trackksn) + " " + trackksn + "\n"
                        content += getString(R.string.service_code) + " " + serviceCode + "\n"
                        content += getString(R.string.track_1_length) + " " + track1Length + "\n"
                        content += getString(R.string.track_2_length) + " " + track2Length + "\n"
                        content += getString(R.string.track_3_length) + " " + track3Length + "\n"
                        content += getString(R.string.encrypted_tracks) + " " + encTracks + "\n"
                        content += getString(R.string.encrypted_track_1) + " " + encTrack1 + "\n"
                        content += getString(R.string.encrypted_track_2) + " " + encTrack2 + "\n"
                        content += getString(R.string.encrypted_track_3) + " " + encTrack3 + "\n"
                        content += getString(R.string.partial_track) + " " + partialTrack + "\n"
                        content += getString(R.string.pinBlock) + " " + pinBlock + "\n"
                        content += "encPAN: $encPAN\n"
                        content += "trackRandomNumber: $trackRandomNumber\n"
                        content += "pinRandomNumber: $pinRandomNumber\n"
                        cardNo = maskedPAN!!
                        var realPan = ""
                        if (!TextUtils.isEmpty(trackksn) && !TextUtils.isEmpty(encTrack2)) {
                            val clearPan = DUKPK2009_CBC.getDate(
                                trackksn,
                                encTrack2,
                                DUKPK2009_CBC.Enum_key.DATA,
                                DUKPK2009_CBC.Enum_mode.CBC
                            )
                            content += "encTrack2:" + " " + clearPan + "\n"
                            realPan = clearPan.substring(0, maskedPAN.length)
                            content += """realPan: $realPan"""
                        }
                        if (!TextUtils.isEmpty(pinKsn) && !TextUtils.isEmpty(pinBlock) && !TextUtils.isEmpty(
                                realPan
                            )
                        ) {
                            val date = DUKPK2009_CBC.getDate(
                                pinKsn,
                                pinBlock,
                                DUKPK2009_CBC.Enum_key.PIN,
                                DUKPK2009_CBC.Enum_mode.CBC
                            )
                            val parsCarN = "0000" + realPan!!.substring(
                                realPan!!.length - 13,
                                realPan.length - 1
                            )
                            val s = DUKPK2009_CBC.xor(parsCarN, date)
                            content += "PIN: $s\n"
                        }
                    }
                    statusEditText.setText(content)
                } else if ((result == DoTradeResult.NFC_ONLINE) || (result == DoTradeResult.NFC_OFFLINE)) {
                    nfcLog = decodeData!!["nfcLog"]
                    var content = getString(R.string.tap_card)
                    val formatID = decodeData!!.get("formatID")
                    if (formatID.equals("31") || formatID.equals("40")
                        || formatID.equals("37") || formatID.equals("17")
                        || formatID.equals("11") || formatID.equals("10")
                    ) {
                        val maskedPAN = decodeData!!.get("maskedPAN")
                        val expiryDate = decodeData!!.get("expiryDate")
                        val cardHolderName = decodeData!!.get("cardholderName")
                        val serviceCode = decodeData!!.get("serviceCode")
                        val trackblock = decodeData!!.get("trackblock")
                        val psamId = decodeData!!.get("psamId")
                        val posId = decodeData!!.get("posId")
                        val pinblock = decodeData!!.get("pinblock")
                        val macblock = decodeData!!.get("macblock")
                        val activateCode = decodeData!!.get("activateCode")
                        val trackRandomNumber = decodeData
                            .get("trackRandomNumber")

                        content += getString(R.string.format_id) + " " + formatID + "\n"
                        content += getString(R.string.masked_pan) + " " + maskedPAN + "\n"
                        content += getString(R.string.expiry_date) + " " + expiryDate + "\n"
                        content += getString(R.string.cardholder_name) + " " + cardHolderName + "\n"

                        content += getString(R.string.service_code) + " " + serviceCode + "\n"
                        content += "trackblock: $trackblock\n"
                        content += "psamId: $psamId\n"
                        content += "posId: $posId\n"
                        content += getString(R.string.pinBlock) + " " + pinblock + "\n"
                        content += "macblock: $macblock\n"
                        content += "activateCode: $activateCode\n"
                        content += "trackRandomNumber: $trackRandomNumber\n"
                        cardNo = maskedPAN!!
                    } else {
                        val maskedPAN = decodeData!!.get("maskedPAN")
                        val expiryDate = decodeData!!.get("expiryDate")
                        val cardHolderName = decodeData!!.get("cardholderName")
//					val ksn = decodeData!!.get("ksn")
                        val serviceCode = decodeData!!.get("serviceCode")
                        val track1Length = decodeData!!.get("track1Length")
                        val track2Length = decodeData!!.get("track2Length")
                        val track3Length = decodeData!!.get("track3Length")
                        val encTracks = decodeData!!.get("encTracks")
                        val encTrack1 = decodeData!!.get("encTrack1")
                        val encTrack2 = decodeData!!.get("encTrack2")
                        val encTrack3 = decodeData!!.get("encTrack3")
                        val partialTrack = decodeData!!.get("partialTrack")
                        val pinKsn = decodeData!!.get("pinKsn")
                        val trackksn = decodeData!!.get("trackksn")
                        val pinBlock = decodeData!!.get("pinBlock")
                        val encPAN = decodeData!!.get("encPAN")
                        val trackRandomNumber = decodeData
                            .get("trackRandomNumber")
                        val pinRandomNumber = decodeData!!.get("pinRandomNumber")

                        content += getString(R.string.format_id) + " " + formatID + "\n"
                        content += getString(R.string.masked_pan) + " " + maskedPAN + "\n"
                        content += getString(R.string.expiry_date) + " " + expiryDate + "\n"
                        content += getString(R.string.cardholder_name) + " " + cardHolderName + "\n"
//					content += getString(R.string.ksn) + " " + ksn + "\n"
                        content += getString(R.string.pinKsn) + " " + pinKsn + "\n"
                        content += getString(R.string.trackksn) + " " + trackksn + "\n"
                        content += getString(R.string.service_code) + " " + serviceCode + "\n"
                        content += getString(R.string.track_1_length) + " " + track1Length + "\n"
                        content += getString(R.string.track_2_length) + " " + track2Length + "\n"
                        content += getString(R.string.track_3_length) + " " + track3Length + "\n"
                        content += getString(R.string.encrypted_tracks) + " " + encTracks + "\n"
                        content += getString(R.string.encrypted_track_1) + " " + encTrack1 + "\n"
                        content += getString(R.string.encrypted_track_2) + " " + encTrack2 + "\n"
                        content += getString(R.string.encrypted_track_3) + " " + encTrack3 + "\n"
                        content += getString(R.string.partial_track) + " " + partialTrack + "\n"
                        content += getString(R.string.pinBlock) + " " + pinBlock + "\n"
                        content += "encPAN: $encPAN\n"
                        content += "trackRandomNumber: $trackRandomNumber\n"
                        content += "pinRandomNumber: $pinRandomNumber\n"
                        cardNo = maskedPAN!!
                    }
//				TRACE.w("swipe card:" + content)
                    statusEditText.setText(content)
                    sendMsg(8003)
                } else if ((result == DoTradeResult.NFC_DECLINED)) {
                    statusEditText.setText(getString(R.string.transaction_declined))
                } else if (result == DoTradeResult.NO_RESPONSE) {
                    statusEditText.setText(getString(R.string.card_no_response))
                }
            }
        }

        override fun onRequestWaitingUser() {//wait user to insert/swipe/tap card
            TRACE.d("onRequestWaitingUser()")
            runOnUiThread {
                dismissDialog()
                statusEditText.setText(getString(R.string.waiting_for_card))
            }
        }

        override fun onQposInfoResult(posInfoData: Hashtable<String, String>) {
            TRACE.d("onQposInfoResult$posInfoData")
            runOnUiThread {
                val isSupportedTrack1 =
                    if (posInfoData.get("isSupportedTrack1") == null) "" else posInfoData.get("isSupportedTrack1")
                val isSupportedTrack2 =
                    if (posInfoData.get("isSupportedTrack2") == null) "" else posInfoData.get("isSupportedTrack2")
                val isSupportedTrack3 =
                    if (posInfoData.get("isSupportedTrack3") == null) "" else posInfoData.get("isSupportedTrack3")
                val bootloaderVersion =
                    if (posInfoData.get("bootloaderVersion") == null) "" else posInfoData.get("bootloaderVersion")
                val firmwareVersion =
                    if (posInfoData.get("firmwareVersion") == null) "" else posInfoData.get("firmwareVersion")
                val isUsbConnected =
                    if (posInfoData.get("isUsbConnected") == null) "" else posInfoData.get("isUsbConnected")
                val isCharging =
                    if (posInfoData.get("isCharging") == null) "" else posInfoData.get("isCharging")
                val batteryLevel =
                    if (posInfoData.get("batteryLevel") == null) "" else posInfoData.get("batteryLevel")
                val batteryPercentage =
                    if (posInfoData.get("batteryPercentage") == null) "" else posInfoData.get("batteryPercentage")
                val hardwareVersion =
                    if (posInfoData.get("hardwareVersion") == null) "" else posInfoData.get("hardwareVersion")
                val SUB = if (posInfoData.get("SUB") == null) "" else posInfoData.get("SUB")
                val pciFirmwareVersion =
                    if (posInfoData.get("PCI_firmwareVersion") == null) "" else posInfoData.get("PCI_firmwareVersion")
                val pciHardwareVersion =
                    if (posInfoData.get("PCI_hardwareVersion") == null) "" else posInfoData.get("PCI_hardwareVersion")
                var content = ""
                content += getString(R.string.bootloader_version) + bootloaderVersion + "\n"
                content += getString(R.string.firmware_version) + firmwareVersion + "\n"
                content += getString(R.string.usb) + isUsbConnected + "\n"
                content += getString(R.string.charge) + isCharging + "\n"
//			if (batteryPercentage==null || "".equals(batteryPercentage)) {
                content += getString(R.string.battery_level) + batteryLevel + "\n"
//			}else {
                content += getString(R.string.battery_percentage) + batteryPercentage + "\n"
//			}
                content += getString(R.string.hardware_version) + hardwareVersion + "\n"
                content += "SUB : $SUB\n"
                content += getString(R.string.track_1_supported) + isSupportedTrack1 + "\n"
                content += getString(R.string.track_2_supported) + isSupportedTrack2 + "\n"
                content += getString(R.string.track_3_supported) + isSupportedTrack3 + "\n"
                content += "PCI FirmwareVresion:$pciFirmwareVersion\n"
                content += "PCI HardwareVersion:$pciHardwareVersion\n"
                statusEditText.setText(content)
            }
        }

        /**
         * @see com.dspread.xpos!!.QPOSService.QPOSServiceListener#onRequestTransactionResult(com.dspread.xpos!!.QPOSService.TransactionResult)
         */
        override fun onRequestTransactionResult(transactionResult: TransactionResult) {
            runOnUiThread {
                TRACE.d("onRequestTransactionResult()$transactionResult")
                if (transactionResult == TransactionResult.CARD_REMOVED) {
                    clearDisplay()
                }
                dismissDialog()
                dialog = Dialog(this@MainActivity)
                dialog!!.setContentView(R.layout.alert_dialog)
                dialog!!.setTitle(R.string.transaction_result)
                val messageTextView = dialog!!.findViewById(R.id.messageTextView) as TextView
                if (transactionResult == TransactionResult.APPROVED) {
                    TRACE.d("TransactionResult.APPROVED")
                    var message =
                        getString(R.string.transaction_approved) + "\n" + getString(R.string.amount) + ": $" + amount + "\n"
                    if (cashbackAmount != "") {
                        message += getString(R.string.cashback_amount) + ": INR" + cashbackAmount
                    }
                    messageTextView.text = message
//                    deviceShowDisplay("APPROVED")
                } else if (transactionResult == TransactionResult.TERMINATED) {
                    clearDisplay()
                    messageTextView.text = getString(R.string.transaction_terminated)
                } else if (transactionResult == TransactionResult.DECLINED) {
                    messageTextView.text = getString(R.string.transaction_declined)
//                    deviceShowDisplay("DECLINED")
                } else if (transactionResult == TransactionResult.CANCEL) {
                    clearDisplay()
                    messageTextView.text = getString(R.string.transaction_cancel)
                } else if (transactionResult == TransactionResult.CAPK_FAIL) {
                    messageTextView.text = getString(R.string.transaction_capk_fail)
                } else if (transactionResult == TransactionResult.NOT_ICC) {
                    messageTextView.text = getString(R.string.transaction_not_icc)
                } else if (transactionResult == TransactionResult.SELECT_APP_FAIL) {
                    messageTextView.text = getString(R.string.transaction_app_fail)
                } else if (transactionResult == TransactionResult.DEVICE_ERROR) {
                    messageTextView.text = getString(R.string.transaction_device_error)
                } else if (transactionResult == TransactionResult.TRADE_LOG_FULL) {
                    statusEditText.setText("pls clear the trace log and then to begin do trade")
                    messageTextView.text = "the trade log has fulled!pls clear the trade log!"
                } else if (transactionResult == TransactionResult.CARD_NOT_SUPPORTED) {
                    messageTextView.text = getString(R.string.card_not_supported)
                } else if (transactionResult == TransactionResult.MISSING_MANDATORY_DATA) {
                    messageTextView.text = getString(R.string.missing_mandatory_data)
                } else if (transactionResult == TransactionResult.CARD_BLOCKED_OR_NO_EMV_APPS) {
                    messageTextView.text = getString(R.string.card_blocked_or_no_evm_apps)
                } else if (transactionResult == TransactionResult.INVALID_ICC_DATA) {
                    messageTextView.text = getString(R.string.invalid_icc_data)
                } else if (transactionResult == TransactionResult.FALLBACK) {
                    messageTextView.text = "trans fallback"
                } else if (transactionResult == TransactionResult.NFC_TERMINATED) {
                    clearDisplay()
                    messageTextView.text = "NFC Terminated"
                } else if (transactionResult == TransactionResult.CARD_REMOVED) {
                    clearDisplay()
                    messageTextView.text = "CARD REMOVED"
                }
                dialog!!.findViewById<Button>(R.id.confirmButton)
                    .setOnClickListener { dismissDialog() }
                dialog!!.show()
                amount = ""
                cashbackAmount = ""
            }
        }

        override fun onRequestBatchData(tlv: String) {
            runOnUiThread {
                TRACE.d("ICC trade finished")
                var content = getString(R.string.batch_data)
                TRACE.d("onRequestBatchData(String tlv):" + tlv)
                content += tlv
                statusEditText.setText(content)
            }
        }

        override fun onRequestTransactionLog(tlv: String) {
            runOnUiThread {
                TRACE.d("onRequestTransactionLog(String tlv):$tlv")
                dismissDialog()
                var content = getString(R.string.transaction_log)
                content += tlv
                statusEditText.setText(content)
            }
        }

        override fun onQposIdResult(posIdTable: Hashtable<String, String>) {
            runOnUiThread {
                TRACE.w("onQposIdResult():$posIdTable")
                var posId = if (posIdTable.get("posId") == null) "" else posIdTable.get("posId")
                var csn = if (posIdTable.get("csn") == null) "" else posIdTable.get("csn")
                var psamId = if (posIdTable.get("psamId") == null) "" else posIdTable.get("psamId")
                var NFCId = if (posIdTable.get("nfcID") == null) "" else posIdTable.get("nfcID")
                var content = ""
                content += getString(R.string.posId) + posId + "\n"
                content += "csn: $csn\n"
                content += "conn: " + pos!!.bluetoothState + "\n"
                content += "psamId: $psamId\n"
                content += "NFCId: $NFCId\n"
                statusEditText.setText(content)
            }
        }

        override fun onRequestSelectEmvApp(appList: ArrayList<String>) {
            runOnUiThread {
                TRACE.d("onRequestSelectEmvApp():$appList")
                TRACE.d("Please select App -- S，emv card config")
                dismissDialog()
                dialog = Dialog(this@MainActivity)
                dialog!!.setContentView(R.layout.emv_app_dialog)
                dialog!!.setTitle(R.string.please_select_app)
                var appNameList = arrayOfNulls<String>(appList.size)
                for (i in appNameList.indices) {
                    appNameList[i] = appList.get(i)
                }
                appListView = dialog!!.findViewById(R.id.appList)
                appListView.setAdapter(
                    ArrayAdapter<String>(
                        this@MainActivity,
                        android.R.layout.simple_list_item_1,
                        appNameList
                    )
                )
                appListView.setOnItemClickListener(object : OnItemClickListener {
                    override fun onItemClick(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        pos!!.selectEmvApp(position)
                        TRACE.d("select emv app position = $position")
                        dismissDialog()
                    }
                })
                dialog!!.findViewById<Button>(R.id.cancelButton).setOnClickListener() {
                    pos!!.cancelSelectEmvApp()
                    dismissDialog()
                    dialog!!.show()
                }
            }
        }

        override fun onRequestSetAmount() {
            runOnUiThread {
                TRACE.d("input amount -- S")
                TRACE.d("onRequestSetAmount()")
                dismissDialog()
                dialog = Dialog(this@MainActivity)
                dialog!!.setContentView(R.layout.amount_dialog)
                dialog!!.setTitle(getString(R.string.set_amount))
                var transactionTypes = arrayOf(
                    "GOODS", "SERVICES", "CASH", "CASHBACK", "INQUIRY",
                    "TRANSFER", "ADMIN", "CASHDEPOSIT",
                    "PAYMENT", "PBOCLOG||ECQ_INQUIRE_LOG", "SALE",
                    "PREAUTH", "ECQ_DESIGNATED_LOAD", "ECQ_UNDESIGNATED_LOAD",
                    "ECQ_CASH_LOAD", "ECQ_CASH_LOAD_VOID", "CHANGE_PIN", "REFOUND", "SALES_NEW"
                )
                (dialog!!.findViewById(R.id.transactionTypeSpinner) as (Spinner)).setAdapter(
                    ArrayAdapter<String>(
                        this@MainActivity, android.R.layout.simple_spinner_item,
                        transactionTypes
                    )
                )

                dialog!!.findViewById<Button>(R.id.setButton)
                    .setOnClickListener(object : View.OnClickListener {
                        override fun onClick(v: View?) {

                            var amount =
                                ((dialog!!.findViewById<EditText>(R.id.amountEditText))).getText()
                                    .toString()
                            var cashbackAmount =
                                ((dialog!!.findViewById<EditText>(R.id.cashbackAmountEditText))).getText()
                                    .toString()
                            var transactionTypeString =
                                (dialog!!.findViewById(R.id.transactionTypeSpinner) as (Spinner)).getSelectedItem()

                            if (transactionTypeString == "GOODS") {
                                transactionType = TransactionType.GOODS
                            } else if (transactionTypeString == "SERVICES") {
                                transactionType = TransactionType.SERVICES
                            } else if (transactionTypeString == "CASH") {
                                transactionType = TransactionType.CASH
                            } else if (transactionTypeString == "CASHBACK") {
                                transactionType = TransactionType.CASHBACK
                            } else if (transactionTypeString == "INQUIRY") {
                                transactionType = TransactionType.INQUIRY
                            } else if (transactionTypeString == "TRANSFER") {
                                transactionType = TransactionType.TRANSFER
                            } else if (transactionTypeString == "ADMIN") {
                                transactionType = TransactionType.ADMIN
                            } else if (transactionTypeString == "CASHDEPOSIT") {
                                transactionType = TransactionType.CASHDEPOSIT
                            } else if (transactionTypeString == "PAYMENT") {
                                transactionType = TransactionType.PAYMENT
                            } else if (transactionTypeString == "PBOCLOG||ECQ_INQUIRE_LOG") {
                                transactionType = TransactionType.PBOCLOG
                            } else if (transactionTypeString == "SALE") {
                                transactionType = TransactionType.SALE
                            } else if (transactionTypeString == "PREAUTH") {
                                transactionType = TransactionType.PREAUTH
                            } else if (transactionTypeString == "ECQ_DESIGNATED_LOAD") {
                                transactionType = TransactionType.ECQ_DESIGNATED_LOAD
                            } else if (transactionTypeString == "ECQ_UNDESIGNATED_LOAD") {
                                transactionType = TransactionType.ECQ_UNDESIGNATED_LOAD
                            } else if (transactionTypeString == "ECQ_CASH_LOAD") {
                                transactionType = TransactionType.ECQ_CASH_LOAD
                            } else if (transactionTypeString == "ECQ_CASH_LOAD_VOID") {
                                transactionType = TransactionType.ECQ_CASH_LOAD_VOID
                            } else if (transactionTypeString == "CHANGE_PIN") {
                                transactionType = TransactionType.UPDATE_PIN
                            } else if (transactionTypeString == "REFOUND") {
                                transactionType = TransactionType.REFUND
                            } else if (transactionTypeString == "SALES_NEW") {
                                transactionType = TransactionType.SALES_NEW
                            }
                            this@MainActivity.amount = amount
                            this@MainActivity.cashbackAmount = cashbackAmount
                            pos!!.setAmount(amount, cashbackAmount, "156", transactionType)
                            dismissDialog()
                        }

                    })
                dialog!!.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                    pos!!.cancelSetAmount()
                    dialog!!.dismiss()
                }
                dialog!!.setCanceledOnTouchOutside(false)
                dialog!!.show()
            }
        }

        /**
         * @see com.dspread.xpos!!.QPOSService.QPOSServiceListener#onRequestIsServerConnected()
         */
        override fun onRequestIsServerConnected() {
            TRACE.d("onRequestIsServerConnected()")
            pos!!.isServerConnected(true)
        }

        override fun onRequestOnlineProcess(tlv: String) {
            runOnUiThread {
                TRACE.d("onRequestOnlineProcess$tlv")
                dismissDialog()
                dialog = Dialog(this@MainActivity)
                dialog!!.setContentView(R.layout.alert_dialog)
                dialog!!.setTitle(R.string.request_data_to_server)
                var decodeData = pos!!.anlysEmvIccData(tlv) as Hashtable<String, String>
                TRACE.d("anlysEmvIccData(tlv):$decodeData")

                if (isPinCanceled) {
                    (dialog!!.findViewById(R.id.messageTextView) as TextView)
                        .setText(R.string.replied_failed)
                } else {
                    (dialog!!.findViewById(R.id.messageTextView) as (TextView))
                        .setText(R.string.replied_success)
                }
                try {
//                    analyData(tlv)// analy tlv ,get the tag you need
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                dialog!!.findViewById<Button>(R.id.confirmButton).setOnClickListener {
                    if (isPinCanceled) {
                        pos!!.sendOnlineProcessResult(null)
                    } else {
                        //									String str = "5A0A6214672500000000056F5F24032307315F25031307085F2A0201565F34010182027C008407A00000033301018E0C000000000000000002031F009505088004E0009A031406179C01009F02060000000000019F03060000000000009F0702AB009F080200209F0902008C9F0D05D86004A8009F0E0500109800009F0F05D86804F8009F101307010103A02000010A010000000000CE0BCE899F1A0201569F1E0838333230314943439F21031826509F2608881E2E4151E527899F2701809F3303E0F8C89F34030203009F3501229F3602008E9F37042120A7189F4104000000015A0A6214672500000000056F5F24032307315F25031307085F2A0201565F34010182027C008407A00000033301018E0C000000000000000002031F00"
                        //									str = "9F26088930C9018CAEBCD69F2701809F101307010103A02802010A0100000000007EF350299F370415B4E5829F360202179505000004E0009A031504169C01009F02060000000010005F2A02015682027C009F1A0201569F03060000000000009F330360D8C89F34030203009F3501229F1E0838333230314943438408A0000003330101019F090200209F410400000001"
                        var str = "8A023030"//Currently the default value,
                        // should be assigned to the server to return data,
                        // the data format is TLV
                        pos!!.sendOnlineProcessResult(str)//脚本通知/55域/ICCDATA

                    }
                    dismissDialog()
                }
                dialog!!.show()
            }
        }

        override fun onRequestTime() {
            TRACE.d("onRequestTime")
            runOnUiThread {
                dismissDialog()
                var terminalTime =
                    SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime())
                pos!!.sendTime(terminalTime)
                statusEditText.setText(getString(R.string.request_terminal_time) + " " + terminalTime)
            }
        }

        override fun onRequestDisplay(displayMsg: Display) {
            TRACE.d("onRequestDisplay(Display displayMsg):$displayMsg")
            runOnUiThread {
                dismissDialog()
                var msg = ""
                if (displayMsg == Display.CLEAR_DISPLAY_MSG) {
                    msg = ""
                } else if (displayMsg == Display.MSR_DATA_READY) {
                    var builder = AlertDialog.Builder(this@MainActivity)
                    builder.setTitle("Audio")
                    builder.setMessage("Success,Contine ready")
                    builder.setPositiveButton("Confirm", null)
                    builder.show()
                } else if (displayMsg == Display.PLEASE_WAIT) {
                    msg = getString(R.string.wait)
                } else if (displayMsg == Display.REMOVE_CARD) {
                    msg = getString(R.string.remove_card)
                } else if (displayMsg == Display.TRY_ANOTHER_INTERFACE) {
                    msg = getString(R.string.try_another_interface)
                } else if (displayMsg == Display.PROCESSING) {
                    msg = getString(R.string.processing)

                } else if (displayMsg == Display.INPUT_PIN_ING) {
                    msg = "please input pin on pos"

                } else if (displayMsg == Display.INPUT_OFFLINE_PIN_ONLY || displayMsg == Display.INPUT_LAST_OFFLINE_PIN) {
                    msg = "please input offline pin on pos"

                } else if (displayMsg == Display.MAG_TO_ICC_TRADE) {
                    msg = "please insert chip card on pos"
                } else if (displayMsg == Display.CARD_REMOVED) {
                    msg = "card removed"
                }
                statusEditText.setText(msg)
            }
        }

        override fun onRequestFinalConfirm() {
            TRACE.d("onRequestFinalConfirm() ")
            TRACE.d("onRequestFinalConfirm - S")
            runOnUiThread {
                dismissDialog()
                if (!isPinCanceled) {
                    dialog = Dialog(this@MainActivity)
                    dialog!!.setContentView(R.layout.confirm_dialog)
                    dialog!!.setTitle(getString(R.string.confirm_amount))

                    var message = getString(R.string.amount) + ": $" + amount
                    if (cashbackAmount != "") {
                        message += "\n" + getString(R.string.cashback_amount) + ": $" + cashbackAmount
                    }
                    (dialog!!.findViewById<TextView>(R.id.messageTextView)).text = (message)
                    dialog!!.findViewById<Button>(R.id.confirmButton).setOnClickListener {
                        pos!!.finalConfirm(true)
                        dialog!!.dismiss()
                    }
                    dialog!!.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                        pos!!.finalConfirm(false)
                        dialog!!.dismiss()
                    }
                    dialog!!.show()
                } else {
                    pos!!.finalConfirm(false)
                }
            }
        }

        override fun onRequestNoQposDetected() {
            TRACE.d("onRequestNoQposDetected()")
            runOnUiThread {
                dismissDialog()
                statusEditText.setText(getString(R.string.no_device_detected))
            }
        }

        override fun onRequestQposConnected() {
            TRACE.d("onRequestQposConnected()")
            runOnUiThread {
                Toast.makeText(this@MainActivity, "onRequestQposConnected", Toast.LENGTH_LONG)
                    .show()
                dismissDialog()
                var use_time = Date().time - start_time
                // statusEditText.setText(getString(R.string.device_plugged))
                statusEditText.setText(
                    getString(R.string.device_plugged) + "--" + getResources().getString(
                        R.string.used
                    ) + QPOSUtil.formatLongToTimeStr(use_time, this@MainActivity)
                )
                doTradeButton.isEnabled = true
                btnDisconnect.isEnabled = true
                btnQuickEMV.isEnabled = true
                setTitle(
                    mTitle + "(" + blueTitle.substring(0, 6) + "..." + blueTitle.substring(
                        blueTitle.length - 3,
                        blueTitle.length
                    ) + ")"
                )
            }
        }

        override fun onRequestQposDisconnected() {
            runOnUiThread {
                dismissDialog()
                setTitle(mTitle)
                TRACE.d("onRequestQposDisconnected()")
                statusEditText.setText(getString(R.string.device_unplugged))
                btnDisconnect.isEnabled = false
                doTradeButton.isEnabled = false
            }
        }

        override fun onError(errorState: Error) {
            runOnUiThread {
                if (updateThread != null) {
                    updateThread!!.concelSelf()
                }
                TRACE.d("onError$errorState")
                dismissDialog()
                when (errorState) {
                    Error.CMD_NOT_AVAILABLE -> {
                        statusEditText.setText(getString(R.string.command_not_available))
                    }

                    Error.TIMEOUT -> {
                        statusEditText.setText(getString(R.string.device_no_response))
                    }

                    Error.DEVICE_RESET -> {
                        statusEditText.setText(getString(R.string.device_reset))
                    }

                    Error.UNKNOWN -> {
                        statusEditText.setText(getString(R.string.unknown_error))
                    }

                    Error.DEVICE_BUSY -> {
                        statusEditText.setText(getString(R.string.device_busy))
                    }

                    Error.INPUT_OUT_OF_RANGE -> {
                        statusEditText.setText(getString(R.string.out_of_range))
                    }

                    Error.INPUT_INVALID_FORMAT -> {
                        statusEditText.setText(getString(R.string.invalid_format))
                    }

                    Error.INPUT_ZERO_VALUES -> {
                        statusEditText.setText(getString(R.string.zero_values))
                    }

                    Error.INPUT_INVALID -> {
                        statusEditText.setText(getString(R.string.input_invalid))
                    }

                    Error.CASHBACK_NOT_SUPPORTED -> {
                        statusEditText.setText(getString(R.string.cashback_not_supported))
                    }

                    Error.CRC_ERROR -> {
                        statusEditText.setText(getString(R.string.crc_error))
                    }

                    Error.COMM_ERROR -> {
                        statusEditText.setText(getString(R.string.comm_error))
                    }

                    Error.MAC_ERROR -> {
                        statusEditText.setText(getString(R.string.mac_error))
                    }

                    Error.APP_SELECT_TIMEOUT -> {
                        statusEditText.setText(getString(R.string.app_select_timeout_error))
                    }

                    Error.CMD_TIMEOUT -> {
                        statusEditText.setText(getString(R.string.cmd_timeout))
                    }

                    Error.ICC_ONLINE_TIMEOUT -> {
                        if (pos == null) {
                            return@runOnUiThread
                        }
                        pos!!.resetPosStatus()
                        statusEditText.setText(getString(R.string.device_reset))
                    }
                }
            }
        }

        override fun onReturnReversalData(tlv: String) {
            runOnUiThread {
                var content = getString(R.string.reversal_data)
                content += tlv
                TRACE.d("onReturnReversalData(): $tlv")
                statusEditText.setText(content)
            }
        }

        override fun onReturnGetPinResult(result: Hashtable<String, String>) {
            TRACE.d("onReturnGetPinResult(Hashtable<String, String> result):$result")
            runOnUiThread {
                var pinBlock = result["pinBlock"]
                var pinKsn = result["pinKsn"]
                var content = "get pin result\n"
                content += getString(R.string.pinKsn) + " " + pinKsn + "\n"
                content += getString(R.string.pinBlock) + " " + pinBlock + "\n"
                statusEditText.setText(content)
                TRACE.i(content)
            }
        }

        override fun onReturnApduResult(arg0: Boolean, arg1: String, arg2: Int) {
            TRACE.d("onReturnApduResult(boolean arg0, String arg1, int arg2):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2)
        }

        override fun onReturnPowerOnIccResult(
            arg0: Boolean,
            arg1: String,
            arg2: String,
            arg3: Int
        ) {
            TRACE.d("onReturnPowerOnIccResult(boolean arg0, String arg1, String arg2, int arg3) :" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2 + TRACE.NEW_LINE + arg3)
            if (arg0) {
                pos!!.sendApdu("123456")
            }
        }

        override fun onReturnSetSleepTimeResult(isSuccess: Boolean) {
            TRACE.d("onReturnSetSleepTimeResult(boolean isSuccess):$isSuccess")
            runOnUiThread {
                var content = ""
                content = if (isSuccess) {
                    "set the sleep time success."
                } else {
                    "set the sleep time failed."
                }
                statusEditText.setText(content)
            }
        }

        override fun onGetCardNoResult(cardNo: String) {
            TRACE.d("onGetCardNoResult(String cardNo):$cardNo")
            runOnUiThread {
                statusEditText.setText("cardNo: $cardNo")
            }
        }

        override fun onRequestCalculateMac(calMac: String) {
            TRACE.d("onRequestCalculateMac(String calMac):$calMac")
            runOnUiThread {
                if (calMac != null && "" != calMac) {
                    var calMac2 = QPOSUtil.byteArray2Hex(calMac.toByteArray())
                    statusEditText.setText("calMac: $calMac2")
                    TRACE.d("calMac_result: calMac=> e: $calMac")
                }
            }
        }

        override fun onRequestSignatureResult(arg0: ByteArray) {
            TRACE.d("onRequestSignatureResult(byte[] arg0):$arg0")
        }

        override fun onRequestUpdateWorkKeyResult(result: UpdateInformationResult) {
            TRACE.d("onRequestUpdateWorkKeyResult(UpdateInformationResult result):$result")
            runOnUiThread {
                when (result) {
                    UpdateInformationResult.UPDATE_SUCCESS -> {
                        statusEditText.setText("update work key success")
                    }

                    UpdateInformationResult.UPDATE_FAIL -> {
                        statusEditText.setText("update work key fail")
                    }

                    UpdateInformationResult.UPDATE_PACKET_VEFIRY_ERROR -> {
                        statusEditText.setText("update work key packet vefiry error")
                    }

                    UpdateInformationResult.UPDATE_PACKET_LEN_ERROR -> {
                        statusEditText.setText("update work key packet len error")
                    }
                }
            }
        }

        override fun onReturnCustomConfigResult(isSuccess: Boolean, result: String) {
            TRACE.d("onReturnCustomConfigResult(boolean isSuccess, String result):" + isSuccess + TRACE.NEW_LINE + result)
            runOnUiThread {
                statusEditText.setText("result: $isSuccess\ndata: $result")
            }
        }

        override fun onRequestSetPin() {
            TRACE.i("onRequestSetPin()")
            runOnUiThread {
                dismissDialog()
                dialog = Dialog(this@MainActivity)
                dialog!!.setContentView(R.layout.pin_dialog)
                dialog!!.setTitle(getString(R.string.enter_pin))
                dialog!!.findViewById<Button>(R.id.confirmButton).setOnClickListener {
                    var pin = (dialog!!.findViewById<EditText>(R.id.pinEditText)).text.toString()
                    if (pin.length in 4..12) {
                        if (pin == "000000") {
                            pos!!.sendEncryptPin("5516422217375116")

                        } else {
                            pos!!.sendPin(pin.toByteArray())
                        }
                        dismissDialog()
                    }
                }
                dialog!!.findViewById<Button>(R.id.bypassButton).setOnClickListener {
//                pos!!.bypassPin()
                    pos!!.sendPin(byteArrayOf())
                    dismissDialog()
                }

                dialog!!.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                    isPinCanceled = true
                    pos!!.cancelPin()
                    dismissDialog()
                }
                dialog!!.show()
            }
        }

        override fun onReturnSetMasterKeyResult(isSuccess: Boolean) {
            TRACE.d("onReturnSetMasterKeyResult(boolean isSuccess) : $isSuccess")
            runOnUiThread {
                statusEditText.setText("result: $isSuccess")
            }
        }

        override fun onReturnBatchSendAPDUResult(batchAPDUResult: LinkedHashMap<Int, String>?) {
            super.onReturnBatchSendAPDUResult(batchAPDUResult)
            TRACE.d("onReturnBatchSendAPDUResult(LinkedHashMap<Integer, String> batchAPDUResult):$batchAPDUResult")
            runOnUiThread {
            var sb = StringBuilder()
            sb.append("APDU Responses: \n")
            for (entry in batchAPDUResult!!.entries) {
                sb.append("[" + entry.key + "]: " + entry.value + "\n")
            }
            statusEditText.setText("\n" + sb.toString())
                }
        }

        override fun onBluetoothBondFailed() {
            TRACE.d("onBluetoothBondFailed()")
            runOnUiThread {
                statusEditText.setText("bond failed")
            }
        }

        override fun onBluetoothBondTimeout() {
            TRACE.d("onBluetoothBondTimeout()")
            runOnUiThread {
                statusEditText.setText("bond timeout")
            }
        }

        override fun onBluetoothBonded() {
            TRACE.d("onBluetoothBonded()")
            runOnUiThread {
                statusEditText.setText("bond success")
            }
        }

        override fun onBluetoothBonding() {
            TRACE.d("onBluetoothBonding()")
            runOnUiThread {
                statusEditText.setText("bonding .....")
            }
        }

        override fun onReturniccCashBack(result: Hashtable<String, String>) {
            TRACE.d("onReturniccCashBack(Hashtable<String, String> result):$result")
            runOnUiThread {
                var s = "serviceCode: " + result.get("serviceCode")
                s += "\n"
                s += "trackblock: " + result.get("trackblock")
                statusEditText.setText(s)
            }
        }

        override fun onLcdShowCustomDisplay(arg0: Boolean) {
            TRACE.d("onLcdShowCustomDisplay(boolean arg0):$arg0")
        }

        override fun onUpdatePosFirmwareResult(arg0: UpdateInformationResult) {
            TRACE.d("onUpdatePosFirmwareResult(UpdateInformationResult arg0):$arg0")
            runOnUiThread {
                if (arg0 != UpdateInformationResult.UPDATE_SUCCESS) {
                    updateThread!!.concelSelf()
                }
                statusEditText.setText("onUpdatePosFirmwareResult$arg0")
            }
        }

        override fun onReturnDownloadRsaPublicKey(map: HashMap<String, String>) {
            TRACE.d("onReturnDownloadRsaPublicKey(HashMap<String, String> map):$map")
            runOnUiThread {
                if (map == null) {
                    TRACE.d("MainActivity++++++++++++++map == null")
                    return@runOnUiThread
                }
                var randomKeyLen = map.get("randomKeyLen")
                var randomKey = map.get("randomKey")
                var randomKeyCheckValueLen = map.get("randomKeyCheckValueLen")
                var randomKeyCheckValue = map.get("randomKeyCheckValue")
                TRACE.d("randomKey$randomKey    \n    randomKeyCheckValue$randomKeyCheckValue")
                statusEditText.setText(
                    "randomKeyLen:" + randomKeyLen + "\nrandomKey:" + randomKey + "\nrandomKeyCheckValueLen:" + randomKeyCheckValueLen + "\nrandomKeyCheckValue:"
                            + randomKeyCheckValue
                )
            }
        }

        override fun onGetPosComm(mod: Int, amount: String, posid: String) {
            TRACE.d("onGetPosComm(int mod, String amount, String posid):" + mod + TRACE.NEW_LINE + amount + TRACE.NEW_LINE + posid)
        }

        override fun onPinKey_TDES_Result(arg0: String) {
            TRACE.d("onPinKey_TDES_Result(String arg0):$arg0")
            runOnUiThread {
                statusEditText.setText("result:$arg0")
            }
        }

        override fun onUpdateMasterKeyResult(arg0: Boolean, arg1: Hashtable<String, String>) {
            TRACE.d("onUpdateMasterKeyResult(boolean arg0, Hashtable<String, String> arg1):" + arg0 + TRACE.NEW_LINE + arg1.toString())
        }

        override fun onEmvICCExceptionData(arg0: String) {
            TRACE.d("onEmvICCExceptionData(String arg0):$arg0")
        }

        override fun onSetParamsResult(arg0: Boolean, arg1: Hashtable<String, Any>) {
            TRACE.d("onSetParamsResult(boolean arg0, Hashtable<String, Object> arg1):" + arg0 + TRACE.NEW_LINE + arg1.toString())
        }

        override fun onGetInputAmountResult(arg0: Boolean, arg1: String) {
            TRACE.d("onGetInputAmountResult(boolean arg0, String arg1):" + arg0 + TRACE.NEW_LINE + arg1.toString())
        }

        override fun onReturnNFCApduResult(arg0: Boolean, arg1: String, arg2: Int) {
            TRACE.d("onReturnNFCApduResult(boolean arg0, String arg1, int arg2):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2)
            runOnUiThread {
                statusEditText.setText("onReturnNFCApduResult(boolean arg0, String arg1, int arg2):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2)
            }
        }

        override fun onReturnPowerOffNFCResult(arg0: Boolean) {
            TRACE.d(" onReturnPowerOffNFCResult(boolean arg0) :$arg0")
            runOnUiThread {
                statusEditText.setText(" onReturnPowerOffNFCResult(boolean arg0) :$arg0")
            }
        }

        override fun onReturnPowerOnNFCResult(
            arg0: Boolean,
            arg1: String,
            arg2: String,
            arg3: Int
        ) {
            runOnUiThread {
                TRACE.d("onReturnPowerOnNFCResult(boolean arg0, String arg1, String arg2, int arg3):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2 + TRACE.NEW_LINE + arg3)
                statusEditText.setText("onReturnPowerOnNFCResult(boolean arg0, String arg1, String arg2, int arg3):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2 + TRACE.NEW_LINE + arg3)
            }
        }

        override fun onCbcMacResult(result: String) {
            TRACE.d("onCbcMacResult(String result):" + result)
            runOnUiThread {
                if (result == null || "".equals(result)) {
                    statusEditText.setText("cbc_mac:false")
                } else {
                    statusEditText.setText("cbc_mac: " + result)
                }
            }
        }

        override fun onReadBusinessCardResult(arg0: Boolean, arg1: String) {
            TRACE.d(" onReadBusinessCardResult(boolean arg0, String arg1):" + arg0 + TRACE.NEW_LINE + arg1)
        }

        override fun onWriteBusinessCardResult(arg0: Boolean) {
            TRACE.d(" onWriteBusinessCardResult(boolean arg0):$arg0")
        }

        override fun onConfirmAmountResult(arg0: Boolean) {
            TRACE.d("onConfirmAmountResult(boolean arg0):$arg0")
        }

        override fun onQposIsCardExist(cardIsExist: Boolean) {
            TRACE.d("onQposIsCardExist(boolean cardIsExist):$cardIsExist")
            runOnUiThread {
                if (cardIsExist) {
                    statusEditText.setText("cardIsExist:" + cardIsExist)
                } else {
                    statusEditText.setText("cardIsExist:" + cardIsExist)
                }
            }
        }

        override fun onSearchMifareCardResult(arg0: Hashtable<String, String>) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onSearchMifareCardResult(Hashtable<String, String> arg0):$arg0")
                    val statuString = arg0["status"]
                    val cardTypeString = arg0["cardType"]
                    val cardUidLen = arg0["cardUidLen"]
                    val cardUid = arg0["cardUid"]
                    val cardAtsLen = arg0["cardAtsLen"]
                    val cardAts = arg0["cardAts"]
                    val ATQA = arg0["ATQA"]
                    val SAK = arg0["SAK"]
                    statusEditText.setText(
                        "statuString:" + statuString + "\n" + "cardTypeString:" + cardTypeString + "\ncardUidLen:" + cardUidLen
                                + "\ncardUid:" + cardUid + "\ncardAtsLen:" + cardAtsLen + "\ncardAts:" + cardAts
                                + "\nATQA:" + ATQA + "\nSAK:" + SAK
                    )
                } else {
                    statusEditText.setText("poll card failed")
                }
            }
        }

        override fun onBatchReadMifareCardResult(
            msg: String,
            cardData: Hashtable<String, List<String>>
        ) {
            if (cardData != null) {
                TRACE.d("onBatchReadMifareCardResult(boolean arg0):$msg$cardData")
            }
        }

        override fun onBatchWriteMifareCardResult(
            msg: String,
            cardData: Hashtable<String, List<String>>
        ) {
            if (cardData != null) {
                TRACE.d("onBatchWriteMifareCardResult(boolean arg0):$msg$cardData")
            }
        }

        override fun onSetBuzzerResult(arg0: Boolean) {
            TRACE.d("onSetBuzzerResult(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText.setText("Set buzzer success")
                } else {
                    statusEditText.setText("Set buzzer failed")
                }
            }
        }

        override fun onSetBuzzerTimeResult(b: Boolean) {
            TRACE.d("onSetBuzzerTimeResult(boolean b):$b")
        }

        override fun onSetBuzzerStatusResult(b: Boolean) {
            TRACE.d("onSetBuzzerStatusResult(boolean b):$b")
        }

        override fun onGetBuzzerStatusResult(s: String) {
            TRACE.d("onGetBuzzerStatusResult(String s):$s")
        }

        override fun onSetManagementKey(arg0: Boolean) {
            TRACE.d("onSetManagementKey(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText.setText("Set master key success")
                } else {
                    statusEditText.setText("Set master key failed")
                }
            }
        }

        override fun onReturnUpdateIPEKResult(arg0: Boolean) {
            TRACE.d("onReturnUpdateIPEKResult(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText.setText("update IPEK success")
                } else {
                    statusEditText.setText("update IPEK fail")
                }
            }
        }

        override fun onReturnUpdateEMVRIDResult(arg0: Boolean) {
            TRACE.d("onReturnUpdateEMVRIDResult(boolean arg0):$arg0")
        }

        override fun onReturnUpdateEMVResult(arg0: Boolean) {
            TRACE.d("onReturnUpdateEMVResult(boolean arg0):$arg0")
        }

        override fun onBluetoothBoardStateResult(arg0: Boolean) {
            TRACE.d("onBluetoothBoardStateResult(boolean arg0):$arg0")
        }

        @SuppressLint("MissingPermission")
        override fun onDeviceFound(arg0: BluetoothDevice) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onDeviceFound(BluetoothDevice arg0):" + arg0.getName() + ":" + arg0.toString())
                    m_ListView!!.visibility = View.VISIBLE
                    animScan.start()
                    imvAnimScan.visibility = View.VISIBLE
                    if (m_Adapter != null) {
                        var itm = hashMapOf<String, Any>()
                        itm["ICON"] =
                            if (arg0.bondState == BluetoothDevice.BOND_BONDED) Integer.valueOf(R.drawable.bluetooth_blue) else Integer.valueOf(
                                R.drawable.bluetooth_blue_unbond
                            )
                        itm["TITLE"] = arg0.name + "(" + arg0.address + ")"
                        itm["ADDRESS"] = arg0.address
                        m_Adapter!!.addData(itm)
                        m_Adapter!!.notifyDataSetChanged()
                    } else {
                        refreshAdapter()
                    }
                    var address = arg0.address
                    var name = arg0.name
                    name += address + "\n"
                    statusEditText.setText(name)
                    TRACE.d("found new device$name")
                } else {
                    statusEditText.setText("Don't found new device")
                    TRACE.d("Don't found new device")
                }
            }
        }

        override fun onSetSleepModeTime(arg0: Boolean) {
            TRACE.d("onSetSleepModeTime(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText.setText("set the Sleep timee Success")
                } else {
                    statusEditText.setText("set the Sleep timee unSuccess")
                }
            }
        }

        override fun onReturnGetEMVListResult(arg0: String) {
            TRACE.d("onReturnGetEMVListResult(String arg0):$arg0")
            runOnUiThread {
                if (arg0 != null && arg0.isNotEmpty()) {
                    statusEditText.setText("The emv list is : $arg0")
                }
            }
        }

        override fun onWaitingforData(arg0: String) {
            TRACE.d("onWaitingforData(String arg0):$arg0")
        }

        override fun onRequestDeviceScanFinished() {
            TRACE.d("onRequestDeviceScanFinished()")
            runOnUiThread {
                Toast.makeText(this@MainActivity, R.string.scan_over, Toast.LENGTH_LONG).show()
            }
        }

        override fun onRequestUpdateKey(arg0: String) {
            TRACE.d("onRequestUpdateKey(String arg0):$arg0")
            runOnUiThread {
                statusEditText.setText("update checkvalue : $arg0")
            }
        }

        override fun onReturnGetQuickEmvResult(arg0: Boolean) {
            TRACE.d("onReturnGetQuickEmvResult(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText.setText("emv configed")
                    pos!!.setQuickEmv(true)
                } else {
                    statusEditText.setText("emv don't configed")
                }
            }
        }

        override fun onQposDoGetTradeLogNum(arg0: String) {
            TRACE.d("onQposDoGetTradeLogNum(String arg0):$arg0")
            runOnUiThread {
                val a = Integer.parseInt(arg0, 16)
                if (a >= 188) {
                    statusEditText.setText("the trade num has become max value!!")
                    return@runOnUiThread
                }
                statusEditText.setText("get log num:$a")
            }
        }

        override fun onQposDoTradeLog(arg0: Boolean) {
            TRACE.d("onQposDoTradeLog(boolean arg0) :$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText.setText("clear log success!")
                } else {
                    statusEditText.setText("clear log fail!")
                }
            }
        }

        override fun onAddKey(arg0: Boolean) {
            TRACE.d("onAddKey(boolean arg0) :$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText.setText("ksn add 1 success")
                } else {
                    statusEditText.setText("ksn add 1 failed")
                }
            }
        }

//        override fun onEncryptData(arg0:String) {
//            if (arg0 != null) {
//                TRACE.d("onEncryptData(String arg0) :$arg0")
//                statusEditText.setText("get the encrypted result is :$arg0")
//                TRACE.d("get the encrypted result is :$arg0")
//            }
//        }

        override fun onQposKsnResult(arg0: Hashtable<String, String>) {
            TRACE.d("onQposKsnResult(H ashtable<String, String> arg0):$arg0")
            var pinKsn = arg0.get("pinKsn")
            var trackKsn = arg0.get("trackKsn")
            var emvKsn = arg0.get("emvKsn")
            TRACE.d("get the ksn result is :pinKsn$pinKsn\ntrackKsn$trackKsn\nemvKsn$emvKsn")
        }

        override fun onQposDoGetTradeLog(arg0: String, arg1: String) {
            TRACE.d("onQposDoGetTradeLog(String arg0, String arg1):" + arg0 + TRACE.NEW_LINE + arg1)
            runOnUiThread {
                var arg2 = QPOSUtil.convertHexToString(arg1)
                statusEditText.setText("orderId:$arg2\ntrade log:$arg0")
            }
        }

        override fun onRequestDevice() {
            var deviceList = getPermissionDeviceList()
            var mManager = this@MainActivity.getSystemService(Context.USB_SERVICE) as (UsbManager)
            for (i in deviceList.indices) {
                var usbDevice = deviceList.get(i)
                if (usbDevice!!.vendorId == 2965 || usbDevice!!.vendorId == 0x03EB) {
                    if (mManager.hasPermission(usbDevice)) {
                        pos!!.setPermissionDevice(usbDevice)
                    } else {
                        devicePermissionRequest(mManager, usbDevice)
                    }
                }
            }
        }

        //        override fun onGetKeyCheckValue(checkValue:List<String>) {
//            if (checkValue != null) {
//                var buffer =  StringBuffer()
//                buffer.append("{")
//                for (i in checkValue.indices) {
//                    buffer.append(checkValue.get(i)).append(",")
//                }
//                buffer.append("}")
//                statusEditText.setText(buffer.toString())
//            }
//        }
//
        override fun onGetKeyCheckValue(checkValue: Hashtable<String, String>?) {
            super.onGetKeyCheckValue(checkValue)
            runOnUiThread {
                if (checkValue != null) {

                    statusEditText.setText(checkValue.toString())
                }
            }
        }


        override fun onGetDevicePubKey(clearKeys: Hashtable<String, String>?) {
            super.onGetDevicePubKey(clearKeys)
            pubModel = clearKeys!!.get("modulus").toString();
//            String clearKey = clearKeys.get("modulus");
//            statusEditText.setText(clearKey);
//            pubModel = clearKey;
        }


//        override fun onGetDevicePubKey(clearKeys:String) {
//            TRACE.d("onGetDevicePubKey(clearKeys):$clearKeys")
//            statusEditText.setText(clearKeys)
//            var lenStr = clearKeys.substring(0, 4)
//            var sum = 0
//            for (i in 0..3) {
//                var bit = Integer.parseInt(lenStr.substring(i, i + 1))
//                sum += bit.times(Math.pow(16.0, (3 - i)as Double)) as Int
//            }
//            pubModel = clearKeys.substring(4, 4 + sum * 2)
//        }
//
//        override fun onSetPosBlePinCode(b:Boolean) {
//            TRACE.d("onSetPosBlePinCode(b):$b")
//            if (b) {
//                statusEditText.setText("onSetPosBlePinCode success")
//            } else {
//                statusEditText.setText("onSetPosBlePinCode fail")
//            }
//        }

        override fun onTradeCancelled() {
            TRACE.d("onTradeCancelled")
            runOnUiThread {
                dismissDialog()
            }
        }

        override fun onReturnSignature(b: Boolean, signaturedData: String) {
            runOnUiThread {
                if (b) {
                    var base64Encoder = BASE64Encoder()
                    var encode = base64Encoder.encode(signaturedData.toByteArray())
                    statusEditText.setText("signature data (Base64 encoding):$encode")
                }
            }
        }

        override fun onReturnConverEncryptedBlockFormat(result: String) {
            runOnUiThread {
                statusEditText.setText(result)
            }
        }

        override fun onFinishMifareCardResult(arg0: Boolean) {
            TRACE.d("onFinishMifareCardResult(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText.setText("finish success")
                } else {
                    statusEditText.setText("finish fail")
                }
            }
        }

        override fun onVerifyMifareCardResult(arg0: Boolean) {
            TRACE.d("onVerifyMifareCardResult(boolean arg0):$arg0")
            runOnUiThread {
            if (arg0) {
                statusEditText.setText(" onVerifyMifareCardResult success")
            } else {
                statusEditText.setText("onVerifyMifareCardResult fail")
            }
            }
        }

        override fun onReadMifareCardResult(arg0: Hashtable<String, String>) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onReadMifareCardResult(Hashtable<String, String> arg0):$arg0")
                    var addr = arg0["addr"]
                    var cardDataLen = arg0["cardDataLen"]
                    var cardData = arg0["cardData"]
                    statusEditText.setText("addr:$addr\ncardDataLen:$cardDataLen\ncardData:$cardData")
                } else {
//				statusEditText.setText("onReadWriteMifareCardResult fail"+msg)
                }
            }
        }

        override fun onWriteMifareCardResult(arg0: Boolean) {
            TRACE.d("onWriteMifareCardResult(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText.setText("write data success!")
                } else {
                    statusEditText.setText("write data fail!")
                }
            }
        }

        override fun onOperateMifareCardResult(arg0: Hashtable<String, String>) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onOperateMifareCardResult(Hashtable<String, String> arg0):$arg0")
                    var cmd = arg0.get("Cmd")
                    var blockAddr = arg0.get("blockAddr")
                    statusEditText.setText("Cmd:$cmd\nBlock Addr:$blockAddr")
                } else {
                    statusEditText.setText("operate failed")
                }
            }
        }

        override fun getMifareCardVersion(arg0: Hashtable<String, String>) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("getMifareCardVersion(Hashtable<String, String> arg0):$arg0")

                    var verLen = arg0.get("versionLen")
                    var ver = arg0.get("cardVersion")
                    statusEditText.setText("versionLen:$verLen\nverison:$ver")
                } else {
                    statusEditText.setText("get mafire UL version failed")
                }
            }
        }

        override fun getMifareFastReadData(arg0: Hashtable<String, String>) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("getMifareFastReadData(Hashtable<String, String> arg0):$arg0")
                    var startAddr = arg0.get("startAddr")
                    var endAddr = arg0.get("endAddr")
                    var dataLen = arg0.get("dataLen")
                    var cardData = arg0.get("cardData")
                    statusEditText.setText(
                        "startAddr:" + startAddr + "\nendAddr:" + endAddr + "\ndataLen:" + dataLen
                                + "\ncardData:" + cardData
                    )
                } else {
                    statusEditText.setText("read fast UL failed")
                }
            }
        }

        override fun getMifareReadData(arg0: Hashtable<String, String>) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("getMifareReadData(Hashtable<String, String> arg0):$arg0")
                    var blockAddr = arg0.get("blockAddr")
                    var dataLen = arg0.get("dataLen")
                    var cardData = arg0.get("cardData")
                    statusEditText.setText("blockAddr:$blockAddr\ndataLen:$dataLen\ncardData:$cardData")
                } else {
                    statusEditText.setText("read mafire UL failed")
                }
            }
        }

        override fun writeMifareULData(arg0: String) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("writeMifareULData(String arg0):$arg0")
                    statusEditText.setText("addr:" + arg0)
                } else {
                    statusEditText.setText("write UL failed")
                }
            }
        }

        override fun verifyMifareULData(arg0: Hashtable<String, String>) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("verifyMifareULData(Hashtable<String, String> arg0):$arg0")
                    var dataLen = arg0.get("dataLen")
                    var pack = arg0.get("pack")
                    statusEditText.setText("dataLen:$dataLen\npack:$pack")
                } else {
                    statusEditText.setText("verify UL failed")
                }
            }
        }

        override fun onGetSleepModeTime(arg0: String) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onGetSleepModeTime(String arg0):$arg0")

                    var time = Integer.parseInt(arg0, 16)
                    statusEditText.setText("time is ： $time seconds")
                } else {
                    statusEditText.setText("get the time is failed")
                }
            }
        }

        override fun onGetShutDownTime(arg0: String) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onGetShutDownTime(String arg0):$arg0")
                    statusEditText.setText(
                        "shut down time is : " + Integer.parseInt(
                            arg0,
                            16
                        ) + "s"
                    )
                } else {
                    statusEditText.setText("get the shut down time is fail!")
                }
            }
        }

        override fun onQposDoSetRsaPublicKey(arg0: Boolean) {
            runOnUiThread {
                TRACE.d("onQposDoSetRsaPublicKey(boolean arg0):$arg0")
                if (arg0) {
                    statusEditText.setText("set rsa is successed!")
                } else {
                    statusEditText.setText("set rsa is failed!")
                }
            }
        }

        override fun onQposGenerateSessionKeysResult(arg0: Hashtable<String, String>) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onQposGenerateSessionKeysResult(Hashtable<String, String> arg0):$arg0")
                    val rsaFileName = arg0["rsaReginString"]
                    val enPinKeyData = arg0["enPinKey"]
                    val enKcvPinKeyData = arg0["enPinKcvKey"]
                    val enCardKeyData = arg0["enDataCardKey"]
                    val enKcvCardKeyData = arg0["enKcvDataCardKey"]
                    statusEditText.setText(
                        "rsaFileName:" + rsaFileName + "\nenPinKeyData:" + enPinKeyData + "\nenKcvPinKeyData:" +
                                enKcvPinKeyData + "\nenCardKeyData:" + enCardKeyData + "\nenKcvCardKeyData:" + enKcvCardKeyData
                    )
                } else {
                    statusEditText.setText("get key failed,pls try again!")
                }
            }
        }

        override fun transferMifareData(arg0: String) {
            TRACE.d("transferMifareData(String arg0):$arg0")
            runOnUiThread {
                if (arg0 != null) {
                    statusEditText.setText("response data:$arg0")
                } else {
                    statusEditText.setText("transfer data failed!")
                }
            }
        }

        override fun onReturnRSAResult(arg0: String) {
            TRACE.d("onReturnRSAResult(String arg0):$arg0")
            runOnUiThread {
                if (arg0 != null) {
                    statusEditText.setText("rsa data:\n$arg0")
                } else {
                    statusEditText.setText("get the rsa failed")
                }
            }
        }

        override fun onRequestNoQposDetectedUnbond() {
            TRACE.d("onRequestNoQposDetectedUnbond()")
        }

    }

    private fun deviceShowDisplay(diplay: String) {
        Log.e("execut start:", "deviceShowDisplay")
        var customDisplayString = ""
        try {
            var paras = diplay.toByteArray(Charset.forName("GBK"))
            customDisplayString = QPOSUtil.byteArray2Hex(paras)
            pos!!.lcdShowCustomDisplay(LcdModeAlign.LCD_MODE_ALIGNCENTER, customDisplayString, 60)
        } catch (e: Exception) {
            e.printStackTrace()
            TRACE.d("gbk error")
            Log.e("execut error:", "deviceShowDisplay")
        }
        Log.e("execut end:", "deviceShowDisplay")
    }

    private var mUsbReceiver: BroadcastReceiver = mBroadcastReceiver()
    private fun devicePermissionRequest(mManager: UsbManager, usbDevice: UsbDevice) {
        var mPermissionIntent = PendingIntent.getBroadcast(
            this@MainActivity, 0, Intent(
                "com.android.example.USB_PERMISSION"
            ), PendingIntent.FLAG_IMMUTABLE
        )
        var filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(mUsbReceiver, filter)
        mManager.requestPermission(usbDevice, mPermissionIntent)
    }

    private inner class mBroadcastReceiver : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context?, intent: Intent?) {
            var action = intent!!.action
            if (ACTION_USB_PERMISSION == (action)) {
                synchronized(this@MainActivity) {
//                    var device = intent!!.getParcelableExtra(UsbManager.EXTRA_DEVICE) as (UsbDevice)
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        TODO("VERSION.SDK_INT < TIRAMISU")
                    }
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false
                        )
                    ) {
                        if (device != null) {
                            // call method to set up device communication
                            TRACE.i(
                                "usb" + "permission granted for device "
                                        + device
                            )
                            pos?.setPermissionDevice(device)
                        }
                    } else {
                        TRACE.i("usbpermission denied for device $device")
                    }
                    this@MainActivity.unregisterReceiver(this@mBroadcastReceiver)
                }
            }
        }
    }

    private fun getPermissionDeviceList(): List<UsbDevice> {
        var mManager = this@MainActivity.getSystemService(Context.USB_SERVICE) as UsbManager
        var deviceList = ArrayList<UsbDevice>()
        // check for existing devices
        for (device: UsbDevice in mManager.getDeviceList().values) {
            deviceList.add(device)
        }
        return deviceList
    }

    private fun clearDisplay() {
        statusEditText.setText("")
    }

    private var terminalTime =
        SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime())
    private var transactionType = TransactionType.GOODS

    inner class MyOnClickListener : OnClickListener {
        override fun onClick(v: View?) {
            statusEditText.setText("")
            if (selectBTFlag) {
                statusEditText.setText(R.string.wait)
                return
            } else if (v == doTradeButton) {
                if (pos == null) {
                    statusEditText.setText(R.string.scan_bt_pos_error)
                    return
                }
                isPinCanceled = false
                statusEditText.setText(R.string.starting)
                terminalTime =
                    SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().time)
                if (posType == POS_TYPE.UART) {
                    pos!!.doTrade(terminalTime, 0, 30)
                } else {
                    var keyIdex = getKeyIndex()
                    pos!!.doTrade(keyIdex, 30)//start do trade
                }
            } else if (v!!.id == R.id.btnUSB) {
                var usb = USBClass()
                var deviceList = usb.GetUSBDevices(getBaseContext())
                if (deviceList == null) {
                    Toast.makeText(this@MainActivity, "No Permission", Toast.LENGTH_SHORT).show()
                    return
                }
                deviceList.toArray()
                var items = deviceList.toArray(arrayOfNulls<CharSequence>(deviceList.size))
                var builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Select a Reader")
                builder.setSingleChoiceItems(items, -1) { dialog, which ->
                    var selectedDevice = items[which]
                    dialog!!.dismiss()
                    usbDevice = USBClass.getMdevices().get(selectedDevice)!!
                    open(CommunicationMode.USB_OTG_CDC_ACM)
                    posType = POS_TYPE.OTG
                    pos!!.openUsb(usbDevice)
                }
                var alert = builder.create()
                alert.show()
            } else if (v == btnBT) {
                TRACE.d("type==$type")
//                pos = null//reset the pos
                if (pos == null) {
                    if (type == 3) {
                        open(CommunicationMode.BLUETOOTH)
                        posType = POS_TYPE.BLUETOOTH
                    } else if (type == 4) {
                        open(CommunicationMode.BLUETOOTH_BLE)
                        posType = POS_TYPE.BLUETOOTH_BLE
                    }
                }
                pos!!.clearBluetoothBuffer()
                if (isNormalBlu) {
                    TRACE.d("begin scan====")
                    pos!!.scanQPos2Mode(this@MainActivity, 20)
                } else {
                    pos!!.startScanQposBLE(6)
                }
                animScan.start()
                imvAnimScan.setVisibility(View.VISIBLE)
                refreshAdapter()
                if (m_Adapter != null) {
                    TRACE.d("+++++=$m_Adapter")
                    m_Adapter!!.notifyDataSetChanged()
                }
            } else if (v == btnDisconnect) {
                close()
            } else if (v == btnQuickEMV) {
                statusEditText.setText("updating emv config, please wait...")
                updateEmvConfig()
            } else if (v == pollBtn) {
                pos!!.pollOnMifareCard(20)
//                pos!!.doMifareCard("01", 20)
            } else if (v == pollULbtn) {
                pos!!.pollOnMifareCard(20)
//                pos!!.doMifareCard("01", 20)
            } else if (v == finishBtn) {
                pos!!.finishMifareCard(20)
//                pos!!.doMifareCard("0E", 20)
            } else if (v == finishULBtn) {
                pos!!.finishMifareCard(20)
//                pos!!.doMifareCard("0E", 20)
            } else if (v == veriftBtn) {
                var keyValue = status.text.toString()
                var blockaddr = blockAdd!!.text.toString()
                var keyclass = mafireSpinner.selectedItem as String
                pos!!.setBlockaddr(blockaddr)
                pos!!.setKeyValue(keyValue)
//                pos!!.doMifareCard("02" + keyclass, 20)
                pos!!.authenticateMifareCard(
                    QPOSService.MifareCardType.CLASSIC,
                    keyclass,
                    blockaddr,
                    keyValue,
                    20
                )
            } else if (v == veriftULBtn) {
                var keyValue = status11.text.toString()
                pos!!.setKeyValue(keyValue)
//                pos!!.doMifareCard("0D", 20)
                pos!!.authenticateMifareCard(
                    QPOSService.MifareCardType.UlTRALIGHT,
                    "",
                    "",
                    keyValue,
                    20
                )
            } else if (v == readBtn) {
                var blockaddr = blockAdd!!.text.toString()
                pos!!.setBlockaddr(blockaddr)
//                pos!!.doMifareCard("03", 20)
                pos!!.readMifareCard(QPOSService.MifareCardType.CLASSIC, blockaddr, 20)
            } else if (v == writeBtn) {
                var blockaddr = blockAdd!!.text.toString()
                var cardData = status.text.toString()
//				SpannableString s = new SpannableString("please input card data")
//		        status.setHint(s)
                pos!!.setBlockaddr(blockaddr)
                pos!!.setKeyValue(cardData)
//                pos!!.doMifareCard("04", 20)
                pos!!.writeMifareCard(QPOSService.MifareCardType.CLASSIC, blockaddr, cardData, 20)
            } else if (v == operateCardBtn) {
                var blockaddr = blockAdd!!.text.toString()
                var cardData = status.text.toString()
                var cmd = cmdSp!!.selectedItem as (String)
                pos!!.setBlockaddr(blockaddr)
                pos!!.setKeyValue(cardData)
                if (cmd == "add") {
                    pos!!.operateMifareCardData(
                        QPOSService.MifareCardOperationType.ADD,
                        blockaddr,
                        cardData,
                        20
                    )
                }
            } else if (v == getULBtn) {
//                pos!!.doMifareCard("06", 20)
                pos!!.getMifareCardInfo(20)
            } else if (v == readULBtn) {
                var blockaddr = blockAdd!!.text.toString()
                pos!!.setBlockaddr(blockaddr)
//                pos!!.doMifareCard("07", 20)
                pos!!.readMifareCard(QPOSService.MifareCardType.CLASSIC, blockaddr, 20)
            } else if (v == fastReadUL) {
                var endAddr = blockAdd!!.text.toString()
                var startAddr = status11.text.toString()
                pos!!.setKeyValue(startAddr)
                pos!!.setBlockaddr(endAddr)
//                pos!!.doMifareCard("08", 20)
//                pos!!.faseReadMifareCardData(startAddr,endAddr,20)
            } else if (v == writeULBtn) {
                var addr = blockAdd!!.text.toString()
                var data = status11.text.toString()
                pos!!.setKeyValue(data)
                pos!!.setBlockaddr(addr)
                pos!!.writeMifareCard(QPOSService.MifareCardType.UlTRALIGHT, addr, data, 20)
            } else if (v == transferBtn) {
            } else if (v == updateFwBtn) {//update firmware
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    //request permission
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_WRITE_EXTERNAL_STORAGE
                    )
                } else {
                    var data: ByteArray? = null
                    var allFiles: List<String>? = null
//                    allFiles = FileUtils.getAllFiles(FileUtils.POS_Storage_Dir)
                    if (allFiles != null) {
                        for (fileName: String in allFiles) {
                            if (!TextUtils.isEmpty(fileName)) {
                                if (fileName.toUpperCase().endsWith(".asc".toUpperCase())) {
                                    data = FileUtils.readLine(fileName)
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Upgrade package path:" +
                                                Environment.getExternalStorageDirectory()
                                                    .getAbsolutePath() + File.separator + "dspread" + File.separator + fileName,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    break
                                }
                            }
                        }
                    }
                    if (data == null || data.size == 0) {
                        data = FileUtils.readAssetsLine("upgrader.asc", this@MainActivity)
                    }
                    var a = pos!!.updatePosFirmware(data, blueTootchAddress)
                    if (a == -1) {
                        Toast.makeText(
                            this@MainActivity,
                            "please keep the device charging",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }
                    updateThread = UpdateThread()
                    updateThread!!.start()
                }
            }
        }
    }

    private fun getKeyIndex(): Int {
        var s = mKeyIndex.text.toString()
        if (TextUtils.isEmpty(s)) {
            return 0
        }
        var i = 0
        try {
            i = Integer.parseInt(s)
            if (i > 9 || i < 0) {
                i = 0
            }
        } catch (e: Exception) {
            i = 0
            return i
        }
        return i
    }

    private fun sendMsg(what: Int) {
        var msg = Message()
        msg.what = what
        mHandler.sendMessage(msg)
    }

    private var mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
//            super.handleMessage(msg)
            when (msg?.what) {
                1001 -> {
                    btnBT!!.isEnabled = false
                    btnQuickEMV.isEnabled = false
                    doTradeButton.isEnabled = false
                    selectBTFlag = true
                    statusEditText.setText(R.string.connecting_bt_pos)
                    sendMsg(1002)
                }

                1002 -> {
                    if (isNormalBlu) {
                        pos!!.connectBluetoothDevice(true, 25, blueTootchAddress)
                    } else {
                        pos!!.connectBLE(blueTootchAddress)
                    }
                    btnBT!!.isEnabled = true
                    selectBTFlag = false
                }

                8003 -> {
                    try {
                        Thread.sleep(200)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    var content = ""
                    content = if (nfcLog == null) {
                        var h = pos!!.getNFCBatchData()
                        var tlv = h.get("tlv")
                        TRACE.i("nfc batchdata1: $tlv")
                        statusEditText.text.toString() + "\nNFCbatchData: " + h["tlv"]
                    } else {
                        statusEditText.text.toString() + "\nNFCbatchData: " + nfcLog
                    }
                    statusEditText.setText(content)
                }

            }
        }
    }

    fun updateEmvConfig() {
//      update emv config by bin files
        val emvAppCfg =
            QPOSUtil.byteArray2Hex(FileUtils.readAssetsLine("emv_app.bin", this@MainActivity))
        val emvCapkCfg =
            QPOSUtil.byteArray2Hex(FileUtils.readAssetsLine("emv_capk.bin", this@MainActivity))
        TRACE.d("emvAppCfg: $emvAppCfg")
        TRACE.d("emvCapkCfg: $emvCapkCfg")
        pos?.updateEmvConfig(emvAppCfg, emvCapkCfg)
    }

    fun updateEmvConfigByXml() {
        pos?.updateEMVConfigByXml(
            this@MainActivity.assets.open("MEXICO-QPOS cute,CR100,D20,D30,D60,D50,D70.xml").bufferedReader().use {
                val text = it.readText()
                text
            })
    }

    /**
     * desc:save object
     *
     * @param context
     * @param key
     * @param obj     The object to be saved can only save objects that implement serializable
     */
//    public static void saveObject(Context context, String key, Object obj) {
//        try {
//            // 保存对象
//            SharedPreferences.Editor sharedata = context.getSharedPreferences(FILENAME, 0).edit()
//            //先将序列化结果写到byte缓存中，其实就分配一个内存空间
//            ByteArrayOutputStream bos = new ByteArrayOutputStream()
//            ObjectOutputStream os = new ObjectOutputStream(bos)
//            //将对象序列化写入byte缓存
//            os.writeObject(obj)
//            //将序列化的数据转为16进制保存
//            String bytesToHexString = QPOSUtil.byteArray2Hex(bos.toByteArray())
//            //保存该16进制数组
//            sharedata.putString(key, bytesToHexString)
//            sharedata.apply()
//        } catch (IOException e) {
//            e.printStackTrace()
//            Log.e("", "保存obj失败")
//        }
//    }

    /**
     * desc:Get saved Object
     *
     * @param context
     * @param key
     * @return modified:
     */
//    public Object readObject(Context context, String key) {
//        try {
//            SharedPreferences sharedata = context.getSharedPreferences(FILENAME, 0)
//            if (sharedata.contains(key)) {
//                String string = sharedata.getString(key, "")
//                if (string == null || "".equals(string)) {
//                    return null
//                } else {
//                    //将16进制的数据转为数组，准备反序列化
//                    byte[] stringToBytes = QPOSUtil.HexStringToByteArray(string)
//                    ByteArrayInputStream bis = new ByteArrayInputStream(stringToBytes)
//                    ObjectInputStream is = new ObjectInputStream(bis)
//                    //返回反序列化得到的对象
//                    Object readObject = is.readObject()
//                    return readObject
//                }
//            }
//        } catch (StreamCorruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace()
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace()
//        } catch (ClassNotFoundException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace()
//        }
//        //所有异常返回null
//        return null
//    }

}
