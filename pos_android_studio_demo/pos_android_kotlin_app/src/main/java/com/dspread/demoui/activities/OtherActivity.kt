package com.dspread.demoui.activities

//import Decoder.BASE64Encoder
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.ActivityInfo
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.dspread.demoui.R
import com.dspread.demoui.USBClass
import com.dspread.demoui.keyboard.KeyboardUtil
import com.dspread.demoui.keyboard.MyKeyboardView
import com.dspread.demoui.utils.DUKPK2009_CBC
import com.dspread.demoui.utils.QPOSUtil
import com.dspread.demoui.utils.TRACE
import com.dspread.xpos.CQPOSService
import com.dspread.xpos.QPOSService
import com.dspread.xpos.QPOSService.*
import com.dspread.xpos.utils.BASE64Encoder
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class OtherActivity : BaseActivity() {
    private var doTradeButton: Button? = null
    private var serialBtn: Button? = null
    private var statusEditText: EditText? = null
    private var appListView: ListView? = null
    private var dialog: Dialog? = null
    private var nfcLog: String? = ""
    private var btnUSB: Button? = null
    private var btnDisconnect: Button? = null
    private var mKeyIndex: EditText? = null
    private var mhipStatus: EditText? = null
    private var pos: QPOSService? = null
    private val updateThread: UpdateThread? = null
    private var pubModel: String? = null
    private var amount = ""
    private var cashbackAmount = ""
    private var isPinCanceled = false
    private var blueTootchAddress = ""
    private val isUart = true
    private var lin: LinearLayout? = null
    private var type = 0
    private var usbDevice: UsbDevice? = null
    private lateinit var mContext: Context

    private val autoDoTrade = false
    private var mafireLi: LinearLayout? = null
    private var mafireUL: LinearLayout? = null
    private var operateCardBtn: Button? = null
    private var pollBtn: Button? = null
    private var pollULbtn: Button? = null
    private var veriftBtn: Button? = null
    private var veriftULBtn: Button? = null
    private var readBtn: Button? = null
    private var writeBtn: Button? = null
    private var finishBtn: Button? = null
    private var finishULBtn: Button? = null
    private var getULBtn: Button? = null
    private var readULBtn: Button? = null
    private var fastReadUL: Button? = null
    private var writeULBtn: Button? = null
    private var transferBtn: Button? = null
    private var mafireSpinner: Spinner? = null
    private var blockAdd: EditText? = null
    private var status: EditText? = null
    private var status11: EditText? = null
    private var cmdSp: Spinner? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //When the window is visible to the user, keep the device normally open and keep the brightness unchanged
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (!isUart) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        mContext = this
        initView()
        initIntent()
        initListener()
        open(QPOSService.CommunicationMode.UART)
        posType = POS_TYPE.UART
        //                        blueTootchAddress = "/dev/ttyMT0";//tongfang is s1，tianbo is s3
        blueTootchAddress = "/dev/ttyS1" //tongfang is s1，tianbo is s3，D20 is dev/ttyS1
        pos!!.setDeviceAddress(blueTootchAddress)
        pos!!.openUart()
    }

    override fun onToolbarLinstener() {
        finish()
    }

    override val layoutId: Int
        get() {
            return R.layout.activity_other
        }

    private fun initIntent() {
        val intent = intent
        type = intent.getIntExtra("connect_type", 0)
        when (type) {
            1 -> {
                setTitle(getString(R.string.title_audio))
                open(QPOSService.CommunicationMode.AUDIO)
                posType = POS_TYPE.AUDIO
//                pos!!.openAudio()
            }

            2 -> {
                setTitle(getString(R.string.serial_port))
                serialBtn!!.visibility = View.VISIBLE
                serialBtn!!.setOnClickListener { // TODO Auto-generated method stub
                    open(QPOSService.CommunicationMode.UART)
                    posType = POS_TYPE.UART
                    //                        blueTootchAddress = "/dev/ttyMT0";//tongfang is s1，tianbo is s3
                    blueTootchAddress = "/dev/ttyS1" //tongfang is s1，tianbo is s3
                    //                        blueTootchAddress = "/dev/ttyHSL1";//tongfang is s1，tianbo is s3
                    pos!!.setDeviceAddress(blueTootchAddress)
                    pos!!.openUart()
                }
            }
        }
    }

    private fun initView() {
        doTradeButton = findViewById<View>(R.id.doTradeButton) as Button //start to do trade
        serialBtn = findViewById<View>(R.id.serialPort) as Button
        statusEditText = findViewById<View>(R.id.statusEditText) as EditText
        btnUSB = findViewById<View>(R.id.btnUSB) as Button //Scan  USB device
        btnDisconnect = findViewById<View>(R.id.disconnect) as Button //disconnect
        mKeyIndex = findViewById<View>(R.id.keyindex) as EditText
        mhipStatus = findViewById(R.id.chipStatus)
        lin = findViewById(R.id.lin)
        pollBtn = findViewById<View>(R.id.search_card) as Button
        pollULbtn = findViewById<View>(R.id.poll_ulcard) as Button
        veriftBtn = findViewById<View>(R.id.verify_card) as Button
        veriftULBtn = findViewById<View>(R.id.verify_ulcard) as Button
        readBtn = findViewById<View>(R.id.read_card) as Button
        writeBtn = findViewById<View>(R.id.write_card) as Button
        finishBtn = findViewById<View>(R.id.finish_card) as Button
        finishULBtn = findViewById<View>(R.id.finish_ulcard) as Button
        getULBtn = findViewById<View>(R.id.get_ul) as Button
        readULBtn = findViewById<View>(R.id.read_ulcard) as Button
        fastReadUL = findViewById<View>(R.id.fast_read_ul) as Button
        writeULBtn = findViewById<View>(R.id.write_ul) as Button
        transferBtn = findViewById<View>(R.id.transfer_card) as Button
        mafireSpinner = findViewById<View>(R.id.verift_spinner) as Spinner
        blockAdd = findViewById<View>(R.id.block_address) as EditText
        val keyClass = arrayOf("Key A", "Key B")
        val spinneradapter =
            ArrayAdapter(this@OtherActivity, android.R.layout.simple_spinner_item, keyClass)
        mafireSpinner!!.adapter = spinneradapter
        cmdSp = findViewById<View>(R.id.cmd_spinner) as Spinner
        val cmdList = arrayOf("add", "reduce", "restore")
        val cmdAdapter =
            ArrayAdapter(this@OtherActivity, android.R.layout.simple_spinner_item, cmdList)
        cmdSp!!.adapter = cmdAdapter
        status = findViewById<View>(R.id.status) as EditText
        status11 = findViewById<View>(R.id.status11) as EditText
        operateCardBtn = findViewById<View>(R.id.operate_card) as Button
        mafireLi = findViewById<View>(R.id.mifareid) as LinearLayout
        mafireUL = findViewById<View>(R.id.ul_ll) as LinearLayout
    }

    private fun initListener() {
        val myOnClickListener: MyOnClickListener = MyOnClickListener()
        //The following is the click event of the button
        doTradeButton!!.setOnClickListener(myOnClickListener) //start
        btnDisconnect!!.setOnClickListener(myOnClickListener)
        btnUSB!!.setOnClickListener(myOnClickListener)
        pollBtn!!.setOnClickListener(myOnClickListener)
        pollULbtn!!.setOnClickListener(myOnClickListener)
        finishBtn!!.setOnClickListener(myOnClickListener)
        finishULBtn!!.setOnClickListener(myOnClickListener)
        readBtn!!.setOnClickListener(myOnClickListener)
        writeBtn!!.setOnClickListener(myOnClickListener)
        veriftBtn!!.setOnClickListener(myOnClickListener)
        veriftULBtn!!.setOnClickListener(myOnClickListener)
        operateCardBtn!!.setOnClickListener(myOnClickListener)
        getULBtn!!.setOnClickListener(myOnClickListener)
        readULBtn!!.setOnClickListener(myOnClickListener)
        fastReadUL!!.setOnClickListener(myOnClickListener)
        writeULBtn!!.setOnClickListener(myOnClickListener)
        transferBtn!!.setOnClickListener(myOnClickListener)
    }

    private var posType = POS_TYPE.BLUETOOTH
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private enum class POS_TYPE {
        BLUETOOTH, AUDIO, UART, USB, OTG, BLUETOOTH_BLE
    }

    /**
     * open and get the class object,  start listening
     *
     * @param mode
     */
    private fun open(mode: CommunicationMode) {
        TRACE.d("open")
        //pos=null;
        val listener = MyPosListener()
        //implement singleton mode
        pos = QPOSService.getInstance(this, mode)
        if (pos == null) {
            statusEditText!!.setText("CommunicationMode unknow")
            return
        }
//        val handler = Handler(Looper. myLooper())
        if (mode == CommunicationMode.USB_OTG_CDC_ACM) {
            pos!!.setUsbSerialDriver(QPOSService.UsbOTGDriver.CDCACM)
        }
        pos.run {
            this!!.setD20Trade(true)
//            this.setConext(this@OtherActivity)
            //init handler
            this.initListener(listener)
        }
        val sdkVersion = pos!!.getSdkVersion()
        Toast.makeText(this, "sdkVersion--$sdkVersion", Toast.LENGTH_SHORT).show()
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // MenuItem audioitem = menu.findItem(R.id.audio_test);
        if (pos != null) {
//            if (pos!!.audioControl) {
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
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_main, menu)
        audioitem = menu.findItem(R.id.audio_test)
        if (pos != null) {
//            if (pos!!.audioControl) {
            audioitem!!.setTitle(R.string.audio_open)
//            } else {
//                audioitem!!.setTitle(R.string.audio_close)
//            }
        } else {
            audioitem!!.setTitle(R.string.audio_unknow)
        }
        return true
    }

    internal inner class UpdateThread : Thread() {
        private var concelFlag = false
        override fun run() {
            while (!concelFlag) {
                var i = 0
                while (!concelFlag && i < 100) {
                    try {
                        sleep(1)
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
                val progress = pos!!.updateProgress
                if (progress < 100) {
                    runOnUiThread { statusEditText!!.setText("$progress%") }
                    continue
                }
                runOnUiThread { statusEditText!!.setText("Update Finished 100%") }
                break
            }
        }

        fun concelSelf() {
            concelFlag = true
        }
    }

    /**
     * Click event of the menu bar
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (pos == null) {
            Toast.makeText(
                applicationContext,
                getString(R.string.device_not_connect),
                Toast.LENGTH_LONG
            ).show()
            return true
        } else if (item.itemId == R.id.reset_qpos) {
            val a = pos!!.resetPosStatus()
            if (a) {
                statusEditText!!.setText("pos reset")
            }
        } else if (item.itemId == R.id.get_ksn) {
            pos!!.getKsn()
        } else if (item.itemId == R.id.getEncryptData) {
            //get encrypt data
            pos!!.getEncryptData("70563".toByteArray(), "1", "0", 10)
        } else if (item.itemId == R.id.addKsn) {
            pos!!.addKsn("00")
        } else if (item.itemId == R.id.doTradeLogOperation) {
            pos!!.doTradeLogOperation(QPOSService.DoTransactionType.GetAll, 0)
        } else if (item.itemId == R.id.get_update_key) { //get the key value
            pos!!.updateCheckValue
        } else if (item.itemId == R.id.get_device_public_key) { //get the key value
            pos!!.getDevicePublicKey(5)
        } else if (item.itemId == R.id.set_sleepmode_time) { //set pos sleep mode time
//            0~Integer.MAX_VALUE
            pos!!.setSleepModeTime(20) //the time is in 10s and 10000s
        } else if (item.itemId == R.id.set_shutdowm_time) {
            pos!!.setShutDownTime(15 * 60)
        } else if (item.itemId == R.id.updateIPEK) {
            val keyIndex = keyIndex
            val ipekGrop = "0$keyIndex"
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
        } else if (item.itemId == R.id.getSleepTime) {
            pos!!.getShutDownTime()
        } else if (item.getItemId() == R.id.updateEMVByXml) {
            statusEditText!!.setText("updating emv config, please wait...")
            updateEmvConfigByXml()
        } else if (item.itemId == R.id.getQuickEmvStatus) {
            pos!!.getQuickEMVStatus(
                QPOSService.EMVDataOperation.getEmv,
                "9F061000000000000000000000000000000000"
            )
        } else if (item.itemId == R.id.setQuickEmvStatus) {
//            pos!!.setQuickEmvStatus(true)
        } else if (item.itemId == R.id.audio_test) {
//            if (pos!!.audioControl) {
//                pos!!.audioControl = false
//                item.title = getString(R.string.audio_close)
//            } else {
//                pos!!.audioControl = true
//                item.title = getString(R.string.audio_open)
//            }
        } else if (item.itemId == R.id.about) {
            statusEditText!!.setText("SDK Version：" + pos!!.getSdkVersion())
        } else if (item.itemId == R.id.setBuzzer) {
            pos!!.doSetBuzzerOperation(3) //set buzzer
        } else if (item.itemId == R.id.menu_get_deivce_info) {
            statusEditText!!.setText(R.string.getting_info)
            pos!!.getQposInfo()
        } else if (item.itemId == R.id.menu_get_deivce_key_checkvalue) {
            statusEditText!!.setText("get_deivce_key_checkvalue..............")
            val keyIdex = keyIndex
            pos!!.getKeyCheckValue(keyIdex, QPOSService.CHECKVALUE_KEYTYPE.DUKPT_MKSK_ALLTYPE)
        } else if (item.itemId == R.id.menu_get_pos_id) {
            pos!!.getQposId()
            statusEditText!!.setText(R.string.getting_pos_id)
        } else if (item.itemId == R.id.setMasterkey) {
            //key:0123456789ABCDEFFEDCBA9876543210
            //result；0123456789ABCDEFFEDCBA9876543210
            val keyIndex = keyIndex
            pos!!.setMasterKey("1A4D672DCA6CB3351FD1B02B237AF9AE", "08D7B4FB629D0885", keyIndex)
        } else if (item.itemId == R.id.menu_get_pin) {
            statusEditText!!.setText(R.string.input_pin)
            pos!!.getPin(1, 0, 6, "please input pin", "622262XXXXXXXXX4406", "", 20)
        } else if (item.itemId == R.id.isCardExist) {
            pos!!.isCardExist(30)
        } else if (item.itemId == R.id.resetSessionKey) {
            val keyIndex = keyIndex
            pos!!.updateWorkKey(
                "1A4D672DCA6CB3351FD1B02B237AF9AE", "08D7B4FB629D0885",  //PIN KEY
                "1A4D672DCA6CB3351FD1B02B237AF9AE", "08D7B4FB629D0885",  //TRACK KEY
                "1A4D672DCA6CB3351FD1B02B237AF9AE", "08D7B4FB629D0885",  //MAC KEY
                keyIndex, 5
            )
        } /*else if(item.getItemId() == R.id.updateFirmWare){
            if (ActivityCompat.checkSelfPermission(OtherActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    //request permission
                    ActivityCompat.requestPermissions(OtherActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                    LogFileConfig.getInstance().setWriteFlag(true);
                    byte[] data = null;
                    List<String> allFiles = null;
//                    allFiles = FileUtils.getAllFiles(FileUtils.POS_Storage_Dir);
                    if (allFiles != null) {
                        for (String fileName : allFiles) {
                            if (!TextUtils.isEmpty(fileName)) {
                                if (fileName.toUpperCase().endsWith(".asc".toUpperCase())) {
                                    data = FileUtils.readLine(fileName);
                                    Toast.makeText(OtherActivity.this, "Upgrade package path:" +
                                            Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "dspread" + File.separator + fileName, Toast.LENGTH_SHORT).show();
                                    break;
                                }
                            }
                        }
                    }
                    if (data == null || data.length == 0) {
                        data = FileUtils.readAssetsLine("D20_master.asc", OtherActivity.this);
                    }
                    int a = pos.updatePosFirmware(data, blueTootchAddress);
                    if (a == -1) {
                        Toast.makeText(OtherActivity.this, "please keep the device charging", Toast.LENGTH_LONG).show();
                        return true;
                    }

                    updateThread = new UpdateThread();
                    updateThread.start();

            }

        }*/ else if (item.itemId == R.id.cusDisplay) {
            deviceShowDisplay("test info")
        } else if (item.itemId == R.id.closeDisplay) {
            pos!!.lcdShowCloseDisplay()
        } else if (item.itemId == R.id.menu_operate_mafire) {
            statusEditText!!.setText("operate mafire card")
            showSingleChoiceDialog()
        }
        return true
    }

    private var yourChoice = 0
    private fun showSingleChoiceDialog() {
        val items = arrayOf("Mifare classic 1", "Mifare UL")
        //	    yourChoice = -1;
        val singleChoiceDialog = AlertDialog.Builder(this@OtherActivity)
        singleChoiceDialog.setTitle("please select one")
        // The second parameter is default
        singleChoiceDialog.setSingleChoiceItems(
            items, 0
        ) { dialog, which -> yourChoice = which }
        singleChoiceDialog.setPositiveButton(
            "OK"
        ) { dialog, which ->
            if (yourChoice == 0) {
                mafireLi!!.visibility = View.VISIBLE //display m1 mafire card
                mafireUL!!.visibility = View.GONE //display ul mafire card
            } else if (yourChoice == 1) {
                mafireLi!!.visibility = View.GONE
                mafireUL!!.visibility = View.VISIBLE
            }
        }
        singleChoiceDialog.show()
    }

    public override fun onPause() {
        super.onPause()
        TRACE.d("onPause")
    }

    public override fun onResume() {
        super.onResume()
        TRACE.d("onResume")
    }

    public override fun onDestroy() {
        super.onDestroy()
        TRACE.d("onDestroy")
        updateThread?.concelSelf()
        if (pos != null) {
            close()
            pos = null
        }
    }

    fun dismissDialog() {
        if (dialog != null) {
            dialog!!.dismiss()
            dialog = null
        }
    }

    private var keyboardUtil: KeyboardUtil? = null
    private var keyBoardList: List<String> = ArrayList()

    /**
     * @author qianmengChen
     * @ClassName: MyPosListener
     * @Function: TODO ADD FUNCTION
     * @date: 2016-11-10 下午6:35:06
     */
    internal inner class MyPosListener : CQPOSService() {
        override fun onQposRequestPinResult(dataList: List<String>, offlineTime: Int) {
            runOnUiThread {
                super.onQposRequestPinResult(dataList, offlineTime)
                keyBoardList = dataList
                MyKeyboardView.setKeyBoardListener { value -> //                    statusEditText.setText("Pls click "+dataList.get(0));
                    pos!!.pinMapSync(value, 20)
                }
                keyboardUtil = KeyboardUtil(this@OtherActivity, lin, dataList)
                keyboardUtil!!.initKeyboard(
                    MyKeyboardView.KEYBOARDTYPE_Only_Num_Pwd,
                    statusEditText
                ) //Random keyboard
            }
        }

        override fun onReturnGetKeyBoardInputResult(result: String) {
            runOnUiThread {
                super.onReturnGetKeyBoardInputResult(result)
                mhipStatus!!.setText(result)
            }
        }

        override fun onReturnGetPinInputResult(num: Int) {
            super.onReturnGetPinInputResult(num)
            runOnUiThread {
                var s = ""
                if (num == -1) {
                    if (keyboardUtil != null) {
                        keyboardUtil!!.hide()
                    }
                } else {
                    for (i in 0 until num) {
                        s += "*"
                    }
                    statusEditText!!.setText("result is ：$s")
                }
            }
        }

        override fun onRequestWaitingUser() { //wait for card
            runOnUiThread {
                TRACE.d("onRequestWaitingUser()")
                dismissDialog()
                statusEditText!!.setText(getString(R.string.waiting_for_card))
            }
        }

        /**
         * return the result of the transaction
         */
        override fun onDoTradeResult(
            result: DoTradeResult,
            decodeData: Hashtable<String, String>?
        ) {
            TRACE.d("(DoTradeResult result, Hashtable<String, String> decodeData) " + result.toString() + TRACE.NEW_LINE + "decodeData:" + decodeData)
            runOnUiThread {
                dismissDialog()
                if (result == DoTradeResult.NONE) {
                    statusEditText!!.setText(getString(R.string.no_card_detected))
                } else if (result == DoTradeResult.TRY_ANOTHER_INTERFACE) {
                    statusEditText!!.setText(getString(R.string.try_another_interface))
                } else if (result == DoTradeResult.ICC) {
                    statusEditText!!.setText(getString(R.string.icc_card_inserted))
                    TRACE.d("EMV ICC Start")
                    pos!!.doEmvApp(QPOSService.EmvOption.START)
                } else if (result == DoTradeResult.NOT_ICC) {
                    statusEditText!!.setText(getString(R.string.card_inserted))
                } else if (result == DoTradeResult.BAD_SWIPE) {
                    statusEditText!!.setText(getString(R.string.bad_swipe))
                } else if (result == DoTradeResult.MCR) { //Magnetic card
                    var content = getString(R.string.card_swiped)
                    val formatID = decodeData!!["formatID"]
                    if (formatID == "31" || formatID == "40" || formatID == "37" || formatID == "17" || formatID == "11" || formatID == "10") {
                        val maskedPAN = decodeData["maskedPAN"]
                        val expiryDate = decodeData["expiryDate"]
                        val cardHolderName = decodeData["cardholderName"]
                        val serviceCode = decodeData["serviceCode"]
                        val trackblock = decodeData["trackblock"]
                        val psamId = decodeData["psamId"]
                        val posId = decodeData["posId"]
                        val pinblock = decodeData["pinblock"]
                        val macblock = decodeData["macblock"]
                        val activateCode = decodeData["activateCode"]
                        val trackRandomNumber = decodeData["trackRandomNumber"]
                        content += "${getString(R.string.format_id)} $formatID\n"
                        content += "${getString(R.string.masked_pan)} $maskedPAN\n"
                        content += "${getString(R.string.expiry_date)} $expiryDate\n"
                        content += "${getString(R.string.cardholder_name)} $cardHolderName\n"
                        content += "${getString(R.string.service_code)} $serviceCode\n"
                        content += "trackblock: $trackblock\n"
                        content += "psamId: $psamId\n"
                        content += "posId: $posId\n"
                        content += "${getString(R.string.pinBlock)} $pinblock\n"
                        content += "macblock: $macblock\n"
                        content += "activateCode: $activateCode\n"
                        content += "trackRandomNumber: $trackRandomNumber\n"
                    } else if (formatID == "FF") {
                        val type = decodeData["type"]
                        val encTrack1 = decodeData["encTrack1"]
                        val encTrack2 = decodeData["encTrack2"]
                        val encTrack3 = decodeData["encTrack3"]
                        content += "cardType: $type\n"
                        content += "track_1: $encTrack1\n"
                        content += "track_2: $encTrack2\n"
                        content += "track_3: $encTrack3\n"
                    } else {
                        val orderID = decodeData["orderId"]
                        val maskedPAN = decodeData["maskedPAN"]
                        val expiryDate = decodeData["expiryDate"]
                        val cardHolderName = decodeData["cardholderName"]
                        //					String ksn = decodeData.get("ksn");
                        val serviceCode = decodeData["serviceCode"]
                        val track1Length = decodeData["track1Length"]
                        val track2Length = decodeData["track2Length"]
                        val track3Length = decodeData["track3Length"]
                        val encTracks = decodeData["encTracks"]
                        val encTrack1 = decodeData["encTrack1"]
                        val encTrack2 = decodeData["encTrack2"]
                        val encTrack3 = decodeData["encTrack3"]
                        val partialTrack = decodeData["partialTrack"]
                        val pinKsn = decodeData["pinKsn"]
                        val trackksn = decodeData["trackksn"]
                        val pinBlock = decodeData["pinBlock"]
                        val encPAN = decodeData["encPAN"]
                        val trackRandomNumber = decodeData["trackRandomNumber"]
                        val pinRandomNumber = decodeData["pinRandomNumber"]
                        if (orderID != null && "" != orderID) {
                            content += "orderID:$orderID"
                        }
                        content += "${getString(R.string.format_id)} $formatID\n"
                        content += "${getString(R.string.masked_pan)} $maskedPAN\n"
                        content += "${getString(R.string.expiry_date)} $expiryDate\n"
                        content += "${getString(R.string.cardholder_name)} $cardHolderName\n"
                        //					content += getString(R.string.ksn) + " " + ksn + "\n";
                        content += "${getString(R.string.pinKsn)} $pinKsn\n"
                        content += "${getString(R.string.trackksn)} $trackksn\n"
                        content += "${getString(R.string.service_code)} $serviceCode\n"
                        content += "${getString(R.string.track_1_length)} $track1Length\n"
                        content += "${getString(R.string.track_2_length)} $track2Length\n"
                        content += "${getString(R.string.track_3_length)} $track3Length\n"
                        content += "${getString(R.string.encrypted_tracks)} $encTracks\n"
                        content += "${getString(R.string.encrypted_track_1)} $encTrack1\n"
                        content += "${getString(R.string.encrypted_track_2)} $encTrack2\n"
                        content += "${getString(R.string.encrypted_track_3)} $encTrack3\n"
                        content += "${getString(R.string.partial_track)} $partialTrack\n"
                        content += "${getString(R.string.pinBlock)} $pinBlock\n"
                        content += "encPAN: $encPAN\n"
                        content += "trackRandomNumber: $trackRandomNumber\n"
                        content += "pinRandomNumber: $pinRandomNumber\n"
                        var realPan: String? = null
                        if (!TextUtils.isEmpty(trackksn) && !TextUtils.isEmpty(encTrack2)) {
                            val clearPan = DUKPK2009_CBC.getDate(
                                trackksn,
                                encTrack2,
                                DUKPK2009_CBC.Enum_key.DATA,
                                DUKPK2009_CBC.Enum_mode.CBC
                            )
                            content += "encTrack2: $clearPan\n"
                            realPan = clearPan.substring(0, maskedPAN!!.length)
                            content += "realPan: $realPan\n"
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
                                realPan.length - 13,
                                realPan.length - 1
                            )
                            val s = DUKPK2009_CBC.xor(parsCarN, date)
                            content += "PIN: $s\n"
                        }
                    }
                    statusEditText!!.setText(content)
                    //                autoDoTrade(0);
                } else if (result == DoTradeResult.NFC_ONLINE || result == DoTradeResult.NFC_OFFLINE) {
                    nfcLog = decodeData!!["nfcLog"]
                    var content = getString(R.string.tap_card)
                    val formatID = decodeData["formatID"]
                    if (formatID == "31" || formatID == "40" || formatID == "37" || formatID == "17" || formatID == "11" || formatID == "10") {
                        val maskedPAN = decodeData["maskedPAN"]
                        val expiryDate = decodeData["expiryDate"]
                        val cardHolderName = decodeData["cardholderName"]
                        val serviceCode = decodeData["serviceCode"]
                        val trackblock = decodeData["trackblock"]
                        val psamId = decodeData["psamId"]
                        val posId = decodeData["posId"]
                        val pinblock = decodeData["pinblock"]
                        val macblock = decodeData["macblock"]
                        val activateCode = decodeData["activateCode"]
                        val trackRandomNumber = decodeData["trackRandomNumber"]
                        content += "${getString(R.string.format_id)} $formatID\n"
                        content += "${getString(R.string.masked_pan)} $maskedPAN\n"
                        content += "${getString(R.string.expiry_date)} $expiryDate\n"
                        content += "${getString(R.string.cardholder_name)} $cardHolderName\n"
                        content += "${getString(R.string.service_code)} $serviceCode\n"
                        content += "trackblock: $trackblock\n"
                        content += "psamId: $psamId\n"
                        content += "posId: $posId\n"
                        content += "${getString(R.string.pinBlock)} $pinblock\n"
                        content += "macblock: $macblock\n"
                        content += "activateCode: $activateCode\n"
                        content += "trackRandomNumber: $trackRandomNumber\n"
                    } else {
                        val maskedPAN = decodeData["maskedPAN"]
                        val expiryDate = decodeData["expiryDate"]
                        val cardHolderName = decodeData["cardholderName"]
                        //					String ksn = decodeData.get("ksn");
                        val serviceCode = decodeData["serviceCode"]
                        val track1Length = decodeData["track1Length"]
                        val track2Length = decodeData["track2Length"]
                        val track3Length = decodeData["track3Length"]
                        val encTracks = decodeData["encTracks"]
                        val encTrack1 = decodeData["encTrack1"]
                        val encTrack2 = decodeData["encTrack2"]
                        val encTrack3 = decodeData["encTrack3"]
                        val partialTrack = decodeData["partialTrack"]
                        val pinKsn = decodeData["pinKsn"]
                        val trackksn = decodeData["trackksn"]
                        val pinBlock = decodeData["pinBlock"]
                        val encPAN = decodeData["encPAN"]
                        val trackRandomNumber = decodeData["trackRandomNumber"]
                        val pinRandomNumber = decodeData["pinRandomNumber"]
                        content += "${getString(R.string.format_id)} $formatID\n"
                        content += "${getString(R.string.masked_pan)} $maskedPAN\n"
                        content += "${getString(R.string.expiry_date)} $expiryDate\n"
                        content += "${getString(R.string.cardholder_name)} $cardHolderName\n"
                        //					content += getString(R.string.ksn) + " " + ksn + "\n";
                        content += "${getString(R.string.pinKsn)} $pinKsn\n"
                        content += "${getString(R.string.trackksn)} $trackksn\n"
                        content += "${getString(R.string.service_code)} $serviceCode\n"
                        content += "${getString(R.string.track_1_length)} $track1Length\n"
                        content += "${getString(R.string.track_2_length)} $track2Length\n"
                        content += "${getString(R.string.track_3_length)} $track3Length\n"
                        content += "${getString(R.string.encrypted_tracks)} $encTracks\n"
                        content += "${getString(R.string.encrypted_track_1)} $encTrack1\n"
                        content += "${getString(R.string.encrypted_track_2)} $encTrack2\n"
                        content += "${getString(R.string.encrypted_track_3)} $encTrack3\n"
                        content += "${getString(R.string.partial_track)} $partialTrack\n"
                        content += "${getString(R.string.pinBlock)} $pinBlock\n"
                        content += "encPAN: $encPAN\n"
                        content += "trackRandomNumber: $trackRandomNumber\n"
                        content += "pinRandomNumber: $pinRandomNumber".trimIndent()
                    }
                    statusEditText!!.setText(content)
                    sendMsg(8003)
                } else if (result == DoTradeResult.NFC_DECLINED) {
                    statusEditText!!.setText(getString(R.string.transaction_declined))
                } else if (result == DoTradeResult.NO_RESPONSE) {
                    statusEditText!!.setText(getString(R.string.card_no_response))
                }
            }
        }

        override fun onQposInfoResult(posInfoData: Hashtable<String, String>) {
            TRACE.d("onQposInfoResult$posInfoData")
            runOnUiThread {
                val isSupportedTrack1 =
                    if (posInfoData["isSupportedTrack1"] == null) "" else posInfoData["isSupportedTrack1"]!!
                val isSupportedTrack2 =
                    if (posInfoData["isSupportedTrack2"] == null) "" else posInfoData["isSupportedTrack2"]!!
                val isSupportedTrack3 =
                    if (posInfoData["isSupportedTrack3"] == null) "" else posInfoData["isSupportedTrack3"]!!
                val bootloaderVersion =
                    if (posInfoData["bootloaderVersion"] == null) "" else posInfoData["bootloaderVersion"]!!
                val firmwareVersion =
                    if (posInfoData["firmwareVersion"] == null) "" else posInfoData["firmwareVersion"]!!
                val isUsbConnected =
                    if (posInfoData["isUsbConnected"] == null) "" else posInfoData["isUsbConnected"]!!
                val isCharging =
                    if (posInfoData["isCharging"] == null) "" else posInfoData["isCharging"]!!
                val batteryLevel =
                    if (posInfoData["batteryLevel"] == null) "" else posInfoData["batteryLevel"]!!
                val batteryPercentage =
                    if (posInfoData["batteryPercentage"] == null) "" else posInfoData["batteryPercentage"]!!
                val hardwareVersion =
                    if (posInfoData["hardwareVersion"] == null) "" else posInfoData["hardwareVersion"]!!
                val SUB = if (posInfoData["SUB"] == null) "" else posInfoData["SUB"]!!
                val pciFirmwareVersion =
                    if (posInfoData["PCI_firmwareVersion"] == null) "" else posInfoData["PCI_firmwareVersion"]!!
                val pciHardwareVersion =
                    if (posInfoData["PCI_hardwareVersion"] == null) "" else posInfoData["PCI_hardwareVersion"]!!
                var content = ""
                content += "${getString(R.string.bootloader_version)}$bootloaderVersion\n"
                content += "${getString(R.string.firmware_version)}$firmwareVersion\n"
                content += "${getString(R.string.usb)}$isUsbConnected\n"
                content += "${getString(R.string.charge)}$isCharging\n"
                //			if (batteryPercentage==null || "".equals(batteryPercentage)) {
                content += "${getString(R.string.battery_level)}$batteryLevel\n"
                //			}else {
                content += "${getString(R.string.battery_percentage)}$batteryPercentage\n"
                //			}
                content += "${getString(R.string.hardware_version)}$hardwareVersion\n"
                content += "SUB : $SUB\n"
                content += "${getString(R.string.track_1_supported)}$isSupportedTrack1\n"
                content += "${getString(R.string.track_2_supported)}$isSupportedTrack2\n"
                content += "${getString(R.string.track_3_supported)}$isSupportedTrack3\n"
                content += "PCI FirmwareVresion:$pciFirmwareVersion\n"
                content += "PCI HardwareVersion:$pciHardwareVersion\n"
                statusEditText!!.setText(content)
            }
        }

        /**
         * Request transaction
         *
         * @see com.dspread.xpos.QPOSService.QPOSServiceListener.onRequestTransactionResult
         */
        override fun onRequestTransactionResult(transactionResult: TransactionResult) {
            TRACE.d("onRequestTransactionResult()$transactionResult")
            runOnUiThread {
                if (transactionResult == TransactionResult.CARD_REMOVED) {
                    clearDisplay()
                }
                dismissDialog()
                dialog = Dialog(mContext)
                dialog!!.setContentView(R.layout.alert_dialog)
                dialog!!.setTitle(R.string.transaction_result)
                val messageTextView = dialog!!.findViewById<View>(R.id.messageTextView) as TextView
                if (transactionResult == TransactionResult.APPROVED) {
                    TRACE.d("TransactionResult.APPROVED")
                    var message =
                        "${getString(R.string.transaction_approved)}${getString(R.string.amount)}: ${"$"}$amount".trimIndent()
                    if (cashbackAmount != "") {
                        message += getString(R.string.cashback_amount) + ": INR" + cashbackAmount
                    }
                    messageTextView.text = message
                } else if (transactionResult == TransactionResult.TERMINATED) {
                    clearDisplay()
                    messageTextView.text = getString(R.string.transaction_terminated)
                } else if (transactionResult == TransactionResult.DECLINED) {
                    messageTextView.text = getString(R.string.transaction_declined)
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
                    statusEditText!!.setText("pls clear the trace log and then to begin do trade")
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
                dialog!!.findViewById<View>(R.id.confirmButton)
                    .setOnClickListener { dismissDialog() }
                dialog!!.show()
                amount = ""
                cashbackAmount = ""
            }
        }

        override fun onRequestBatchData(tlv: String) {
            runOnUiThread {
                TRACE.d(getString(R.string.end_transaction))
                var content = getString(R.string.batch_data)
                TRACE.d("onRequestBatchData(String tlv):$tlv")
                content += tlv
                statusEditText!!.setText(content)
                //            autoDoTrade(0);
            }
        }

        override fun onRequestTransactionLog(tlv: String) {
            TRACE.d("onRequestTransactionLog(String tlv):$tlv")
            runOnUiThread {
                dismissDialog()
                var content = getString(R.string.transaction_log)
                content += tlv
                statusEditText!!.setText(content)
            }
        }

        override fun onQposIdResult(posIdTable: Hashtable<String, String>) {
            runOnUiThread {
                TRACE.w("onQposIdResult():$posIdTable")
                val posId = if (posIdTable["posId"] == null) "" else posIdTable["posId"]!!
                val csn = if (posIdTable["csn"] == null) "" else posIdTable["csn"]!!
                val psamId = if (posIdTable["psamId"] == null) "" else posIdTable["psamId"]!!
                val NFCId = if (posIdTable["nfcID"] == null) "" else posIdTable["nfcID"]!!
                var content = ""
                content += "${getString(R.string.posId)}$posId\n"
                content += "csn: $csn\n"
                content += "conn: ${pos!!.bluetoothState}\n"
                content += "psamId: $psamId\n"
                content += "NFCId: $NFCId\n"
                statusEditText!!.setText(content)
            }
        }

        override fun onRequestSelectEmvApp(appList: ArrayList<String>) {
            runOnUiThread {
                TRACE.d("onRequestSelectEmvApp():$appList")
                TRACE.d(getString(R.string.select_app_start))
                dismissDialog()
                dialog = Dialog(mContext)
                dialog!!.setContentView(R.layout.emv_app_dialog)
                dialog!!.setTitle(R.string.please_select_app)
                val appNameList = arrayOfNulls<String>(appList.size)
                for (i in appNameList.indices) {
                    appNameList[i] = appList[i]
                }
                appListView = dialog!!.findViewById<View>(R.id.appList) as ListView
                appListView!!.adapter =
                    ArrayAdapter(mContext, android.R.layout.simple_list_item_1, appNameList)
                appListView!!.onItemClickListener =
                    OnItemClickListener { parent, view, position, id ->
                        pos!!.selectEmvApp(position)
                        TRACE.d(getString(R.string.select_app_end) + position)
                        dismissDialog()
                    }
                dialog!!.findViewById<View>(R.id.cancelButton).setOnClickListener {
                    pos!!.cancelSelectEmvApp()
                    dismissDialog()
                }
                dialog!!.show()
            }
        }

        override fun onRequestSetAmount() {
            TRACE.d("enter amount -- start")
            TRACE.d("onRequestSetAmount()")
            runOnUiThread {
                dismissDialog()
                dialog = Dialog(mContext)
                dialog!!.setContentView(R.layout.amount_dialog)
                dialog!!.setTitle(getString(R.string.set_amount))
                val transactionTypes = arrayOf(
                    "GOODS", "SERVICES", "CASH", "CASHBACK", "INQUIRY",
                    "TRANSFER", "ADMIN", "CASHDEPOSIT",
                    "PAYMENT", "PBOCLOG||ECQ_INQUIRE_LOG", "SALE",
                    "PREAUTH", "ECQ_DESIGNATED_LOAD", "ECQ_UNDESIGNATED_LOAD",
                    "ECQ_CASH_LOAD", "ECQ_CASH_LOAD_VOID", "CHANGE_PIN", "REFOUND", "SALES_NEW"
                )
                (dialog!!.findViewById<View>(R.id.transactionTypeSpinner) as Spinner).adapter =
                    ArrayAdapter(
                        mContext, android.R.layout.simple_spinner_item,
                        transactionTypes
                    )
                dialog!!.findViewById<View>(R.id.setButton).setOnClickListener {
                    val amount =
                        (dialog!!.findViewById<View>(R.id.amountEditText) as EditText).text.toString()
                    val cashbackAmount =
                        (dialog!!.findViewById<View>(R.id.cashbackAmountEditText) as EditText).text.toString()
                    val transactionTypeString =
                        (dialog!!.findViewById<View>(R.id.transactionTypeSpinner) as Spinner).selectedItem as String
                    var transactionType: QPOSService.TransactionType? = null
                    if (transactionTypeString == "GOODS") {
                        transactionType = QPOSService.TransactionType.GOODS
                    } else if (transactionTypeString == "SERVICES") {
                        transactionType = QPOSService.TransactionType.SERVICES
                    } else if (transactionTypeString == "CASH") {
                        transactionType = QPOSService.TransactionType.CASH
                    } else if (transactionTypeString == "CASHBACK") {
                        transactionType = QPOSService.TransactionType.CASHBACK
                    } else if (transactionTypeString == "INQUIRY") {
                        transactionType = QPOSService.TransactionType.INQUIRY
                    } else if (transactionTypeString == "TRANSFER") {
                        transactionType = QPOSService.TransactionType.TRANSFER
                    } else if (transactionTypeString == "ADMIN") {
                        transactionType = QPOSService.TransactionType.ADMIN
                    } else if (transactionTypeString == "CASHDEPOSIT") {
                        transactionType = QPOSService.TransactionType.CASHDEPOSIT
                    } else if (transactionTypeString == "PAYMENT") {
                        transactionType = QPOSService.TransactionType.PAYMENT
                    } else if (transactionTypeString == "PBOCLOG||ECQ_INQUIRE_LOG") {
                        transactionType = QPOSService.TransactionType.PBOCLOG
                    } else if (transactionTypeString == "SALE") {
                        transactionType = QPOSService.TransactionType.SALE
                    } else if (transactionTypeString == "PREAUTH") {
                        transactionType = QPOSService.TransactionType.PREAUTH
                    } else if (transactionTypeString == "ECQ_DESIGNATED_LOAD") {
                        transactionType = QPOSService.TransactionType.ECQ_DESIGNATED_LOAD
                    } else if (transactionTypeString == "ECQ_UNDESIGNATED_LOAD") {
                        transactionType = QPOSService.TransactionType.ECQ_UNDESIGNATED_LOAD
                    } else if (transactionTypeString == "ECQ_CASH_LOAD") {
                        transactionType = QPOSService.TransactionType.ECQ_CASH_LOAD
                    } else if (transactionTypeString == "ECQ_CASH_LOAD_VOID") {
                        transactionType = QPOSService.TransactionType.ECQ_CASH_LOAD_VOID
                    } else if (transactionTypeString == "CHANGE_PIN") {
                        transactionType = QPOSService.TransactionType.UPDATE_PIN
                    } else if (transactionTypeString == "REFOUND") {
                        transactionType = QPOSService.TransactionType.REFUND
                    } else if (transactionTypeString == "SALES_NEW") {
                        transactionType = QPOSService.TransactionType.SALES_NEW
                    }
                    this@OtherActivity.amount = amount
                    this@OtherActivity.cashbackAmount = cashbackAmount
                    pos!!.setAmount(amount, cashbackAmount, "156", transactionType)
                    TRACE.d("enter amount  -- end")
                    dismissDialog()
                }
                dialog!!.findViewById<View>(R.id.cancelButton).setOnClickListener {
                    pos!!.cancelSetAmount()
                    dialog!!.dismiss()
                }
                dialog!!.setCanceledOnTouchOutside(false)
                dialog!!.show()
                //            pos.setAmount("200", cashbackAmount, "156", QPOSService.TransactionType.GOODS);
            }
        }

        /**
         * judge request server is connected or not
         *
         * @see com.dspread.xpos.QPOSService.QPOSServiceListener.onRequestIsServerConnected
         */
        override fun onRequestIsServerConnected() {
            TRACE.d("onRequestIsServerConnected()")
            pos!!.isServerConnected(true)
        }

        override fun onRequestOnlineProcess(tlv: String) {
            runOnUiThread {
                TRACE.d("onRequestOnlineProcess$tlv")
                dismissDialog()
                dialog = Dialog(mContext)
                dialog!!.setContentView(R.layout.alert_dialog)
                dialog!!.setTitle(R.string.request_data_to_server)
                val decodeData = pos!!.anlysEmvIccData(tlv)
                TRACE.d("anlysEmvIccData(tlv):$decodeData")
                if (isPinCanceled) {
                    (dialog!!.findViewById<View>(R.id.messageTextView) as TextView)
                        .setText(R.string.replied_failed)
                } else {
                    (dialog!!.findViewById<View>(R.id.messageTextView) as TextView)
                        .setText(R.string.replied_success)
                }
                try {
//                    analyData(tlv);// analy tlv ,get the tag you need
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                dialog!!.findViewById<View>(R.id.confirmButton).setOnClickListener {
                    if (isPinCanceled) {
                        pos!!.sendOnlineProcessResult(null)
                    } else {
//									String str = "5A0A6214672500000000056F5F24032307315F25031307085F2A0201565F34010182027C008407A00000033301018E0C000000000000000002031F009505088004E0009A031406179C01009F02060000000000019F03060000000000009F0702AB009F080200209F0902008C9F0D05D86004A8009F0E0500109800009F0F05D86804F8009F101307010103A02000010A010000000000CE0BCE899F1A0201569F1E0838333230314943439F21031826509F2608881E2E4151E527899F2701809F3303E0F8C89F34030203009F3501229F3602008E9F37042120A7189F4104000000015A0A6214672500000000056F5F24032307315F25031307085F2A0201565F34010182027C008407A00000033301018E0C000000000000000002031F00";
//									str = "9F26088930C9018CAEBCD69F2701809F101307010103A02802010A0100000000007EF350299F370415B4E5829F360202179505000004E0009A031504169C01009F02060000000010005F2A02015682027C009F1A0201569F03060000000000009F330360D8C89F34030203009F3501229F1E0838333230314943438408A0000003330101019F090200209F410400000001";
                        val str = "8A023030" //Currently the default value,
                        // should be assigned to the server to return data,
                        // the data format is TLV
                        pos!!.sendOnlineProcessResult(str) //Script notification/55domain/ICCDATA
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
                val terminalTime =
                    SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().time)
                pos!!.sendTime(terminalTime)
                statusEditText!!.setText(getString(R.string.request_terminal_time) + " " + terminalTime)
            }
        }

        override fun onRequestDisplay(displayMsg: QPOSService.Display) {
            TRACE.d("onRequestDisplay(Display displayMsg):$displayMsg")
            runOnUiThread {
                dismissDialog()
                var msg: String? = ""
                if (displayMsg == QPOSService.Display.CLEAR_DISPLAY_MSG) {
                    msg = ""
                } else if (displayMsg == QPOSService.Display.MSR_DATA_READY) {
                    val builder = AlertDialog.Builder(mContext)
                    builder.setTitle("Audio")
                    builder.setMessage("Success,Contine ready")
                    builder.setPositiveButton("Confirm", null)
                    builder.show()
                } else if (displayMsg == QPOSService.Display.PLEASE_WAIT) {
                    msg = getString(R.string.wait)
                } else if (displayMsg == QPOSService.Display.REMOVE_CARD) {
                    msg = getString(R.string.remove_card)
                } else if (displayMsg == QPOSService.Display.TRY_ANOTHER_INTERFACE) {
                    msg = getString(R.string.try_another_interface)
                } else if (displayMsg == QPOSService.Display.PROCESSING) {
                    msg = getString(R.string.processing)
                } else if (displayMsg == QPOSService.Display.INPUT_PIN_ING) {
                    msg = "please input pin on pos"
                } else if (displayMsg == QPOSService.Display.INPUT_OFFLINE_PIN_ONLY || displayMsg == QPOSService.Display.INPUT_LAST_OFFLINE_PIN) {
                    msg = "please input offline pin on pos"
                } else if (displayMsg == QPOSService.Display.MAG_TO_ICC_TRADE) {
                    msg = "please insert chip card on pos"
                } else if (displayMsg == QPOSService.Display.CARD_REMOVED) {
                    msg = "card removed"
                }
                statusEditText!!.setText(msg)
            }
        }

        override fun onRequestFinalConfirm() {
            TRACE.d("onRequestFinalConfirm() ")
            TRACE.d("onRequestFinalConfirm+Confirm Amount-- S")
            runOnUiThread {
                dismissDialog()
                if (!isPinCanceled) {
                    dialog = Dialog(mContext)
                    dialog!!.setContentView(R.layout.confirm_dialog)
                    dialog!!.setTitle(getString(R.string.confirm_amount))
                    var message = getString(R.string.amount) + ": $" + amount
                    if (cashbackAmount != "") {
                        message += "${getString(R.string.cashback_amount)}: ${"$"}$cashbackAmount".trimIndent()
                    }
                    (dialog!!.findViewById<View>(R.id.messageTextView) as TextView).text = message
                    dialog!!.findViewById<View>(R.id.confirmButton).setOnClickListener {
                        pos!!.finalConfirm(true)
                        dialog!!.dismiss()
                        TRACE.d("Confirm Amount-- End")
                    }
                    dialog!!.findViewById<View>(R.id.cancelButton).setOnClickListener {
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
                statusEditText!!.setText(getString(R.string.no_device_detected))
            }
        }


        override fun onRequestQposConnected() {
            TRACE.d("onRequestQposConnected()")
            runOnUiThread {
                Toast.makeText(mContext, "onRequestQposConnected", Toast.LENGTH_LONG).show()
                dismissDialog()
                statusEditText!!.setText(getString(R.string.device_plugged))
                doTradeButton!!.isEnabled = true
                btnDisconnect!!.isEnabled = true
            }
        }

        override fun onRequestQposDisconnected() {
            runOnUiThread {
                dismissDialog()
                TRACE.d("onRequestQposDisconnected()")
                statusEditText!!.setText(getString(R.string.device_unplugged))
                btnDisconnect!!.isEnabled = false
                doTradeButton!!.isEnabled = false
            }
        }

        override fun onError(errorState: QPOSService.Error) {
            runOnUiThread {
                updateThread?.concelSelf()
                TRACE.d("onError$errorState")
                dismissDialog()
                if (errorState == QPOSService.Error.CMD_NOT_AVAILABLE) {
                    statusEditText!!.setText(getString(R.string.command_not_available))
                } else if (errorState == QPOSService.Error.TIMEOUT) {
                    statusEditText!!.setText(getString(R.string.device_no_response))
                } else if (errorState == QPOSService.Error.DEVICE_RESET) {
                    statusEditText!!.setText(getString(R.string.device_reset))
                } else if (errorState == QPOSService.Error.UNKNOWN) {
                    statusEditText!!.setText(getString(R.string.unknown_error))
                } else if (errorState == QPOSService.Error.DEVICE_BUSY) {
                    statusEditText!!.setText(getString(R.string.device_busy))
                } else if (errorState == QPOSService.Error.INPUT_OUT_OF_RANGE) {
                    statusEditText!!.setText(getString(R.string.out_of_range))
                } else if (errorState == QPOSService.Error.INPUT_INVALID_FORMAT) {
                    statusEditText!!.setText(getString(R.string.invalid_format))
                } else if (errorState == QPOSService.Error.INPUT_ZERO_VALUES) {
                    statusEditText!!.setText(getString(R.string.zero_values))
                } else if (errorState == QPOSService.Error.INPUT_INVALID) {
                    statusEditText!!.setText(getString(R.string.input_invalid))
                } else if (errorState == QPOSService.Error.CASHBACK_NOT_SUPPORTED) {
                    statusEditText!!.setText(getString(R.string.cashback_not_supported))
                } else if (errorState == QPOSService.Error.CRC_ERROR) {
                    statusEditText!!.setText(getString(R.string.crc_error))
                } else if (errorState == QPOSService.Error.COMM_ERROR) {
                    statusEditText!!.setText(getString(R.string.comm_error))
                } else if (errorState == QPOSService.Error.MAC_ERROR) {
                    statusEditText!!.setText(getString(R.string.mac_error))
                } else if (errorState == QPOSService.Error.APP_SELECT_TIMEOUT) {
                    statusEditText!!.setText(getString(R.string.app_select_timeout_error))
                } else if (errorState == QPOSService.Error.CMD_TIMEOUT) {
                    statusEditText!!.setText(getString(R.string.cmd_timeout))
                } else if (errorState == QPOSService.Error.ICC_ONLINE_TIMEOUT) {
                    if (pos == null) {
                        return@runOnUiThread
                    }
                    pos!!.resetPosStatus()
                    statusEditText!!.setText(getString(R.string.device_reset))
                }
            }
        }

        override fun onReturnReversalData(tlv: String) {
            runOnUiThread {
                var content = getString(R.string.reversal_data)
                content += tlv
                TRACE.d("onReturnReversalData(): $tlv")
                statusEditText!!.setText(content)
            }
        }

        override fun onReturnGetPinResult(result: Hashtable<String, String>) {
            TRACE.d("onReturnGetPinResult(Hashtable<String, String> result):$result")
            runOnUiThread {
                val pinBlock = result["pinBlock"]
                val pinKsn = result["pinKsn"]
                var content = "get pin result\n"
                content += "${getString(R.string.pinKsn)} $pinKsn"
                content += "${getString(R.string.pinBlock)} $pinBlock"
                statusEditText!!.setText(content)
                TRACE.i(content)
            }
        }

        override fun onReturnApduResult(arg0: Boolean, arg1: String, arg2: Int) {
            // TODO Auto-generated method stub
            TRACE.d("onReturnApduResult(boolean arg0, String arg1, int arg2):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2)
        }

        override fun onReturnPowerOffIccResult(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d("onReturnPowerOffIccResult(boolean arg0):$arg0")
        }

        override fun onReturnPowerOnIccResult(
            arg0: Boolean,
            arg1: String,
            arg2: String,
            arg3: Int
        ) {
            // TODO Auto-generated method stub
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
                statusEditText!!.setText(content)
            }
        }

        override fun onGetCardNoResult(cardNo: String) { //get card number result
            TRACE.d("onGetCardNoResult(String cardNo):$cardNo")
            runOnUiThread {
                statusEditText!!.setText("cardNo: $cardNo")
            }
        }

        override fun onRequestCalculateMac(calMac: String) {
            var calMac: String? = calMac
            TRACE.d("onRequestCalculateMac(String calMac):$calMac")
            runOnUiThread {
                if (calMac != null && "" != calMac) {
                    calMac = QPOSUtil.byteArray2Hex(calMac!!.toByteArray())
                }
                statusEditText!!.setText("calMac: $calMac")
                TRACE.d("calMac_result: calMac=> e: $calMac")
            }
        }

        override fun onRequestSignatureResult(arg0: ByteArray) {
            TRACE.d("onRequestSignatureResult(byte[] arg0):$arg0")
        }

        override fun onRequestUpdateWorkKeyResult(result: UpdateInformationResult) {
            TRACE.d("onRequestUpdateWorkKeyResult(UpdateInformationResult result):$result")
            runOnUiThread {
                if (result == UpdateInformationResult.UPDATE_SUCCESS) {
                    statusEditText!!.setText("update work key success")
                } else if (result == UpdateInformationResult.UPDATE_FAIL) {
                    statusEditText!!.setText("update work key fail")
                } else if (result == UpdateInformationResult.UPDATE_PACKET_VEFIRY_ERROR) {
                    statusEditText!!.setText("update work key packet vefiry error")
                } else if (result == UpdateInformationResult.UPDATE_PACKET_LEN_ERROR) {
                    statusEditText!!.setText("update work key packet len error")
                }
            }
        }

        override fun onReturnCustomConfigResult(isSuccess: Boolean, result: String) {
            TRACE.d("onReturnCustomConfigResult(boolean isSuccess, String result):" + isSuccess + TRACE.NEW_LINE + result)
            runOnUiThread {
                statusEditText!!.setText("result: $isSuccess\ndata: $result")
            }
        }

        override fun onRequestSetPin() {
            TRACE.d("onRequestSetPin()")
            runOnUiThread {
                dismissDialog()
                dialog = Dialog(mContext)
                dialog!!.setContentView(R.layout.pin_dialog)
                dialog!!.setTitle(getString(R.string.enter_pin))
                dialog!!.findViewById<View>(R.id.confirmButton).setOnClickListener {
                    val pin =
                        (dialog!!.findViewById<View>(R.id.pinEditText) as EditText).text.toString()
                    if (pin.length >= 4 && pin.length <= 12) {
                        if (pin == "000000") {
                            pos!!.sendEncryptPin("5516422217375116")
                        } else {
                            pos!!.sendPin(pin.toByteArray())
                        }
                        dismissDialog()
                    }
                }
                dialog!!.findViewById<View>(R.id.bypassButton)
                    .setOnClickListener { //					pos.bypassPin();
                        pos!!.sendPin(byteArrayOf())
                        dismissDialog()
                    }
                dialog!!.findViewById<View>(R.id.cancelButton).setOnClickListener {
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
                statusEditText!!.setText("result: $isSuccess")
            }
        }

        override fun onReturnBatchSendAPDUResult(batchAPDUResult: LinkedHashMap<Int, String>) {
            TRACE.d("onReturnBatchSendAPDUResult(LinkedHashMap<Integer, String> batchAPDUResult):$batchAPDUResult")
            runOnUiThread {
                val sb = StringBuilder()
                sb.append("APDU Responses: \n")
                for ((key, value) in batchAPDUResult) {
                    sb.append("[$key]: $value".trimIndent())
                }
                statusEditText!!.setText("$sb".trimIndent())
            }
        }

        override fun onBluetoothBondFailed() {
            TRACE.d("onBluetoothBondFailed()")
            runOnUiThread {
                statusEditText!!.setText("bond failed")
            }
        }

        override fun onBluetoothBondTimeout() {
            TRACE.d("onBluetoothBondTimeout()")
            runOnUiThread {
                statusEditText!!.setText("bond timeout")
            }
        }

        override fun onBluetoothBonded() {
            TRACE.d("onBluetoothBonded()")
            runOnUiThread {
                statusEditText!!.setText("bond success")
            }
        }

        override fun onBluetoothBonding() {
            TRACE.d("onBluetoothBonding()")
            runOnUiThread {
                statusEditText!!.setText("bonding .....")
            }
        }

        override fun onReturniccCashBack(result: Hashtable<String, String>) {
            TRACE.d("onReturniccCashBack(Hashtable<String, String> result):$result")
            runOnUiThread {
                var s = "serviceCode: " + result["serviceCode"]
                s += "\n"
                s += "trackblock: " + result["trackblock"]
                statusEditText!!.setText(s)
            }
        }

        override fun onLcdShowCustomDisplay(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d("onLcdShowCustomDisplay(boolean arg0):$arg0")
        }

        override fun onUpdatePosFirmwareResult(arg0: UpdateInformationResult) {
            TRACE.d("onUpdatePosFirmwareResult(UpdateInformationResult arg0):$arg0")
            runOnUiThread {
                if (arg0 != UpdateInformationResult.UPDATE_SUCCESS) {
                    updateThread!!.concelSelf()
                }
                statusEditText!!.setText("onUpdatePosFirmwareResult$arg0")
            }
        }

        override fun onReturnDownloadRsaPublicKey(map: HashMap<String, String>) {
            TRACE.d("onReturnDownloadRsaPublicKey(HashMap<String, String> map):$map")
            runOnUiThread {
                if (map == null) {
                    TRACE.d("MainActivity++++++++++++++map == null")
                    return@runOnUiThread
                }
                val randomKeyLen = map["randomKeyLen"]
                val randomKey = map["randomKey"]
                val randomKeyCheckValueLen = map["randomKeyCheckValueLen"]
                val randomKeyCheckValue = map["randomKeyCheckValue"]
                TRACE.d("randomKey$randomKey    \n    randomKeyCheckValue$randomKeyCheckValue")
                statusEditText!!.setText("randomKeyLen:$randomKeyLen randomKey:$randomKey randomKeyCheckValueLen:$randomKeyCheckValueLen randomKeyCheckValue:$randomKeyCheckValue ".trimIndent())
            }
        }

        override fun onGetPosComm(mod: Int, amount: String, posid: String) {
            TRACE.d("onGetPosComm(int mod, String amount, String posid):" + mod + TRACE.NEW_LINE + amount + TRACE.NEW_LINE + posid)
        }

        override fun onPinKey_TDES_Result(arg0: String) {
            TRACE.d("onPinKey_TDES_Result(String arg0):$arg0")
            runOnUiThread {
                statusEditText!!.setText("result:$arg0")
            }
        }

        override fun onUpdateMasterKeyResult(arg0: Boolean, arg1: Hashtable<String, String>) {
            // TODO Auto-generated method stub
            TRACE.d("onUpdateMasterKeyResult(boolean arg0, Hashtable<String, String> arg1):" + arg0 + TRACE.NEW_LINE + arg1.toString())
        }

        override fun onEmvICCExceptionData(arg0: String) {
            // TODO Auto-generated method stub
            TRACE.d("onEmvICCExceptionData(String arg0):$arg0")
        }

        override fun onSetParamsResult(arg0: Boolean, arg1: Hashtable<String, Any>) {
            // TODO Auto-generated method stub
            TRACE.d("onSetParamsResult(boolean arg0, Hashtable<String, Object> arg1):" + arg0 + TRACE.NEW_LINE + arg1.toString())
        }

        override fun onGetInputAmountResult(arg0: Boolean, arg1: String) {
            // TODO Auto-generated method stub
            TRACE.d("onGetInputAmountResult(boolean arg0, String arg1):" + arg0 + TRACE.NEW_LINE + arg1)
        }

        override fun onReturnNFCApduResult(arg0: Boolean, arg1: String, arg2: Int) {
            // TODO Auto-generated method stub
            TRACE.d("onReturnNFCApduResult(boolean arg0, String arg1, int arg2):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2)
            runOnUiThread {
                statusEditText!!.setText("onReturnNFCApduResult(boolean arg0, String arg1, int arg2):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2)
            }
        }

        override fun onReturnPowerOffNFCResult(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d(" onReturnPowerOffNFCResult(boolean arg0) :$arg0")
            runOnUiThread {
                statusEditText!!.setText(" onReturnPowerOffNFCResult(boolean arg0) :$arg0")
            }
        }

        override fun onReturnPowerOnNFCResult(
            arg0: Boolean,
            arg1: String,
            arg2: String,
            arg3: Int
        ) {
            runOnUiThread {
                // TODO Auto-generated method stub
                TRACE.d("onReturnPowerOnNFCResult(boolean arg0, String arg1, String arg2, int arg3):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2 + TRACE.NEW_LINE + arg3)
                statusEditText!!.setText("onReturnPowerOnNFCResult(boolean arg0, String arg1, String arg2, int arg3):" + arg0 + TRACE.NEW_LINE + arg1 + TRACE.NEW_LINE + arg2 + TRACE.NEW_LINE + arg3)
            }
            }

        override fun onCbcMacResult(result: String) {
            TRACE.d("onCbcMacResult(String result):$result")
            runOnUiThread {
                if (result == null || "" == result) {
                    statusEditText!!.setText("cbc_mac:false")
                } else {
                    statusEditText!!.setText("cbc_mac: $result")
                }
            }
        }

        override fun onReadBusinessCardResult(arg0: Boolean, arg1: String) {
            // TODO Auto-generated method stub
            TRACE.d(" onReadBusinessCardResult(boolean arg0, String arg1):" + arg0 + TRACE.NEW_LINE + arg1)
        }

        override fun onWriteBusinessCardResult(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d(" onWriteBusinessCardResult(boolean arg0):$arg0")
        }

        override fun onConfirmAmountResult(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d("onConfirmAmountResult(boolean arg0):$arg0")
        }

        override fun onQposIsCardExist(cardIsExist: Boolean) {
            TRACE.d("onQposIsCardExist(boolean cardIsExist):$cardIsExist")
            runOnUiThread {
                if (cardIsExist) {
                    statusEditText!!.setText("cardIsExist:$cardIsExist")
                } else {
                    statusEditText!!.setText("cardIsExist:$cardIsExist")
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
                    statusEditText!!.setText("statuString:$statuString cardTypeString:$cardTypeString cardUidLen:$cardUidLen cardUid:$cardUid cardAtsLen:$cardAtsLen cardAts:$cardAts ATQA:$ATQA SAK:$SAK ".trimIndent())
                } else {
                    statusEditText!!.setText("poll card failed")
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
        }

        override fun onReturnUpdateIPEKResult(arg0: Boolean) {
            TRACE.d("onReturnUpdateIPEKResult(boolean arg0):$arg0")
        }

        override fun onReturnUpdateEMVRIDResult(arg0: Boolean) {
            TRACE.d("onReturnUpdateEMVRIDResult(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText!!.setText("operation RID EMV success")
                } else {
                    statusEditText!!.setText("operation RID EMV fail")
                }
            }
        }

        override fun onReturnUpdateEMVResult(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d("onReturnUpdateEMVResult(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText!!.setText("operation EMV app success")
                } else {
                    statusEditText!!.setText("operation emv app fail~")
                }
            }
        }

        override fun onBluetoothBoardStateResult(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d("onBluetoothBoardStateResult(boolean arg0):$arg0")
        }

        override fun onDeviceFound(arg0: BluetoothDevice) {
            // TODO Auto-generated method stub
        }

        override fun onSetSleepModeTime(arg0: Boolean) {
            TRACE.d("onSetSleepModeTime(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText!!.setText("set the Sleep timee Success")
                } else {
                    statusEditText!!.setText("set the Sleep timee unSuccess")
                }
            }
        }

        override fun onReturnGetEMVListResult(arg0: String) {
            // TODO Auto-generated method stub
            TRACE.d("onReturnGetEMVListResult(String arg0):$arg0")
            runOnUiThread {
                if (arg0 != null && arg0.length > 0) {
                    statusEditText!!.setText("The emv list is : $arg0")
                }
            }
        }

        override fun onWaitingforData(arg0: String) {
            // TODO Auto-generated method stub
            TRACE.d("onWaitingforData(String arg0):$arg0")
        }

        override fun onRequestDeviceScanFinished() {
            // TODO Auto-generated method stub
            TRACE.d("onRequestDeviceScanFinished()")
        }

        override fun onRequestUpdateKey(arg0: String) {
            // TODO Auto-generated method stub
            TRACE.d("onRequestUpdateKey(String arg0):$arg0")
            runOnUiThread {
                statusEditText!!.setText("update checkvalue : $arg0")
            }
        }

        override fun onReturnGetQuickEmvResult(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d("onReturnGetQuickEmvResult(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText!!.setText(getString(R.string.emv_configured))
                    //				isQuickEmv=true;
                    pos!!.isQuickEmv = true
                } else {
                    statusEditText!!.setText(getString(R.string.emv_not_configured))
                }
            }
        }

        override fun onQposDoGetTradeLogNum(arg0: String) {
            TRACE.d("onQposDoGetTradeLogNum(String arg0):$arg0")
            runOnUiThread {
            val a = arg0.toInt(16)

                if (a >= 188) {
                    statusEditText!!.setText("the trade num has become max value!!")
                    return@runOnUiThread
                }
                statusEditText!!.setText("get log num:$a")
            }
        }

        override fun onQposDoTradeLog(arg0: Boolean) {
            TRACE.d("onQposDoTradeLog(boolean arg0) :$arg0")

            // TODO Auto-generated method stub
            runOnUiThread {
                if (arg0) {
                    statusEditText!!.setText("clear log success!")
                } else {
                    statusEditText!!.setText("clear log fail!")
                }
            }
        }

        override fun onAddKey(arg0: Boolean) {
            TRACE.d("onAddKey(boolean arg0) :$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText!!.setText("ksn add 1 success")
                } else {
                    statusEditText!!.setText("ksn add 1 failed")
                }
            }
        }

        override fun onEncryptData(resultTable: Hashtable<String, String>) {
            if (resultTable != null) {
                TRACE.d("onEncryptData(String arg0) :$resultTable")
            }
        }

        override fun onQposKsnResult(arg0: Hashtable<String, String>) {
            TRACE.d("onQposKsnResult(Hashtable<String, String> arg0):$arg0")

            // TODO Auto-generated method stub
            val pinKsn = arg0["pinKsn"]
            val trackKsn = arg0["trackKsn"]
            val emvKsn = arg0["emvKsn"]
            TRACE.d("get the ksn result is :pinKsn$pinKsn\ntrackKsn$trackKsn\nemvKsn$emvKsn")
        }

        override fun onQposDoGetTradeLog(arg0: String, arg1: String) {
            var arg1 = arg1
            TRACE.d("onQposDoGetTradeLog(String arg0, String arg1):" + arg0 + TRACE.NEW_LINE + arg1)
            runOnUiThread {
                // TODO Auto-generated method stub
                arg1 = QPOSUtil.convertHexToString(arg1)
                statusEditText!!.setText("orderId:$arg1\ntrade log:$arg0")
            }
        }

        override fun onRequestDevice() {
            val deviceList: List<UsbDevice> = permissionDeviceList as List<UsbDevice>
            val mManager = mContext!!.getSystemService(USB_SERVICE) as UsbManager
            for (i in deviceList.indices) {
                val usbDevice = deviceList[i]
                if (usbDevice.vendorId == 2965 || usbDevice.vendorId == 0x03EB) {
                    if (mManager.hasPermission(usbDevice)) {
                        pos!!.setPermissionDevice(usbDevice)
                    } else {
                        devicePermissionRequest(mManager, usbDevice)
                    }
                }
            }
        }

        override fun onGetKeyCheckValue(checkValue: Hashtable<String, String>?) {
            super.onGetKeyCheckValue(checkValue)
            runOnUiThread {
                if (checkValue != null) {

                    statusEditText!!.setText(checkValue.toString())
                }
            }
        }

//        override fun onGetKeyCheckValue(checkValue: List<String>) {
//            if (checkValue != null) {
//                val buffer = StringBuffer()
//                buffer.append("{")
//                for (i in checkValue.indices) {
//                    buffer.append(checkValue[i]).append(",")
//                }
//                buffer.append("}")
//                statusEditText!!.setText(buffer.toString())
//            }
//        }

        override fun onGetDevicePubKey(clearKeys: Hashtable<String, String>?) {
            super.onGetDevicePubKey(clearKeys)
            if (clearKeys != null) {
                pubModel = clearKeys.get("modulus")
            }
        }
//        override fun onGetDevicePubKey(clearKeys: String) {
//            TRACE.d("onGetDevicePubKey(clearKeys):$clearKeys")
//            statusEditText!!.setText(clearKeys)
//            val lenStr = clearKeys.substring(0, 4)
//            var sum = 0
//            for (i in 0..3) {
//                val bit = lenStr.substring(i, i + 1).toInt()
//                sum += (bit * Math.pow(16.0, (3 - i).toDouble())).toInt()
//            }
//            pubModel = clearKeys.substring(4, 4 + sum * 2)
//        }

        override fun onTradeCancelled() {
            TRACE.d("onTradeCancelled")
            runOnUiThread {
                dismissDialog()
            }
        }

        override fun onReturnSetAESResult(isSuccess: Boolean, result: String) {}
        override fun onReturnAESTransmissonKeyResult(isSuccess: Boolean, result: String) {}
        override fun onReturnSignature(b: Boolean, signaturedData: String) {
            runOnUiThread {
                if (b) {
                    val base64Encoder = BASE64Encoder()
                    val encode = base64Encoder.encode(signaturedData.toByteArray())
                    statusEditText!!.setText("signature data (Base64 encoding):$encode")
                }
            }
        }

        override fun onReturnConverEncryptedBlockFormat(result: String) {
            runOnUiThread {
                statusEditText!!.setText(result)
            }
        }

        override fun onQposIsCardExistInOnlineProcess(haveCard: Boolean) {}
        override fun onFinishMifareCardResult(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d("onFinishMifareCardResult(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText!!.setText("finish success")
                } else {
                    statusEditText!!.setText("finish fail")
                }
            }
        }

        override fun onVerifyMifareCardResult(arg0: Boolean) {
            TRACE.d("onVerifyMifareCardResult(boolean arg0):$arg0")
            runOnUiThread {
                // TODO Auto-generated method stub
//			String msg = pos.getMifareStatusMsg();
                if (arg0) {
                    statusEditText!!.setText(" onVerifyMifareCardResult success")
                } else {
                    statusEditText!!.setText("onVerifyMifareCardResult fail")
                }
            }
        }

        override fun onReadMifareCardResult(arg0: Hashtable<String, String>) {
            // TODO Auto-generated method stub
//			String msg = pos.getMifareStatusMsg();
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onReadMifareCardResult(Hashtable<String, String> arg0):$arg0")
                    val addr = arg0["addr"]
                    val cardDataLen = arg0["cardDataLen"]
                    val cardData = arg0["cardData"]
                    statusEditText!!.setText("addr:$addr\ncardDataLen:$cardDataLen\ncardData:$cardData")
                } else {
                    statusEditText!!.setText("onReadWriteMifareCardResult fail")
                }
            }
        }

        override fun onWriteMifareCardResult(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d("onWriteMifareCardResult(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText!!.setText("write data success!")
                } else {
                    statusEditText!!.setText("write data fail!")
                }
            }
        }

        override fun onOperateMifareCardResult(arg0: Hashtable<String, String>) {
            // TODO Auto-generated method stub
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onOperateMifareCardResult(Hashtable<String, String> arg0):$arg0")
                    val cmd = arg0["Cmd"]
                    val blockAddr = arg0["blockAddr"]
                    statusEditText!!.setText("Cmd:$cmd\nBlock Addr:$blockAddr")
                } else {
                    statusEditText!!.setText("operate failed")
                }
            }
        }

        override fun getMifareCardVersion(arg0: Hashtable<String, String>) {

            // TODO Auto-generated method stub
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("getMifareCardVersion(Hashtable<String, String> arg0):$arg0")
                    val verLen = arg0["versionLen"]
                    val ver = arg0["cardVersion"]
                    statusEditText!!.setText("versionLen:$verLen\nverison:$ver")
                } else {
                    statusEditText!!.setText("get mafire UL version failed")
                }
            }
        }

        override fun getMifareFastReadData(arg0: Hashtable<String, String>) {
            // TODO Auto-generated method stub
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("getMifareFastReadData(Hashtable<String, String> arg0):$arg0")
                    val startAddr = arg0["startAddr"]
                    val endAddr = arg0["endAddr"]
                    val dataLen = arg0["dataLen"]
                    val cardData = arg0["cardData"]
                    statusEditText!!.setText("startAddr:$startAddr endAddr:$endAddr dataLen:$dataLen cardData:$cardData ".trimIndent())
                } else {
                    statusEditText!!.setText("read fast UL failed")
                }
            }
        }

        override fun getMifareReadData(arg0: Hashtable<String, String>) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("getMifareReadData(Hashtable<String, String> arg0):$arg0")
                    val blockAddr = arg0["blockAddr"]
                    val dataLen = arg0["dataLen"]
                    val cardData = arg0["cardData"]
                    statusEditText!!.setText("blockAddr:$blockAddr\ndataLen:$dataLen\ncardData:$cardData")
                } else {
                    statusEditText!!.setText("read mafire UL failed")
                }
            }
        }

        override fun writeMifareULData(arg0: String) {
            runOnUiThread {
            if (arg0 != null) {
                TRACE.d("writeMifareULData(String arg0):$arg0")
                statusEditText!!.setText("addr:$arg0")
            } else {
                statusEditText!!.setText("write UL failed")
            }
                }
        }

        override fun verifyMifareULData(arg0: Hashtable<String, String>) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("verifyMifareULData(Hashtable<String, String> arg0):$arg0")
                    val dataLen = arg0["dataLen"]
                    val pack = arg0["pack"]
                    statusEditText!!.setText("dataLen:$dataLen\npack:$pack")
                } else {
                    statusEditText!!.setText("verify UL failed")
                }
            }
        }

        override fun onGetSleepModeTime(arg0: String) {
            // TODO Auto-generated method stub
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onGetSleepModeTime(String arg0):$arg0")
                    val time = arg0.toInt(16)
                    statusEditText!!.setText("time is ： $time seconds")
                } else {
                    statusEditText!!.setText("get the time is failed")
                }
            }
        }

        override fun onGetShutDownTime(arg0: String) {
            runOnUiThread {
                if (arg0 != null) {
                    TRACE.d("onGetShutDownTime(String arg0):$arg0")
                    statusEditText!!.setText("shut down time is : " + arg0.toInt(16) + "s")
                } else {
                    statusEditText!!.setText("get the shut down time is fail!")
                }
            }
        }

        override fun onQposDoSetRsaPublicKey(arg0: Boolean) {
            // TODO Auto-generated method stub
            TRACE.d("onQposDoSetRsaPublicKey(boolean arg0):$arg0")
            runOnUiThread {
                if (arg0) {
                    statusEditText!!.setText("set rsa is successed!")
                } else {
                    statusEditText!!.setText("set rsa is failed!")
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
                    statusEditText!!.setText("rsaFileName:$rsaFileName enPinKeyData:$enPinKeyData enKcvPinKeyData:$enKcvPinKeyData enCardKeyData:$enCardKeyData enKcvCardKeyData:$enKcvCardKeyData ".trimIndent())
                } else {
                    statusEditText!!.setText("get key failed,pls try again!")
                }
            }
        }

        override fun transferMifareData(arg0: String) {
            TRACE.d("transferMifareData(String arg0):$arg0")

            // TODO Auto-generated method stub
            runOnUiThread {
                if (arg0 != null) {
                    statusEditText!!.setText("response data:$arg0")
                } else {
                    statusEditText!!.setText("transfer data failed!")
                }
            }
        }

        override fun onReturnRSAResult(arg0: String) {
            TRACE.d("onReturnRSAResult(String arg0):$arg0")
            runOnUiThread {
                if (arg0 != null) {
                    statusEditText!!.setText("rsa data:\n$arg0")
                } else {
                    statusEditText!!.setText("get the rsa failed")
                }
            }
        }

        override fun onRequestNoQposDetectedUnbond() {
            // TODO Auto-generated method stub
            TRACE.d("onRequestNoQposDetectedUnbond()")
        }
    }

    private fun sendMsgDelay(what: Int) {
        val msg = Message()
        msg.what = what
        mHandler.sendMessageDelayed(msg, 500)
    }

    private fun deviceShowDisplay(diplay: String) {
        Log.e("execut start:", "deviceShowDisplay")
        var customDisplayString: String? = ""
        try {
            val paras = diplay.toByteArray(charset("GBK"))
            customDisplayString = QPOSUtil.byteArray2Hex(paras)
            pos!!.lcdShowCustomDisplay(
                QPOSService.LcdModeAlign.LCD_MODE_ALIGNCENTER,
                customDisplayString,
                60
            )
        } catch (e: Exception) {
            e.printStackTrace()
            TRACE.d("gbk error")
            Log.e("execut error:", "deviceShowDisplay")
        }
        Log.e("execut end:", "deviceShowDisplay")
    }

    private fun devicePermissionRequest(mManager: UsbManager, usbDevice: UsbDevice) {
        val mPermissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(
                "com.android.example.USB_PERMISSION"
            ), PendingIntent.FLAG_IMMUTABLE
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(mUsbReceiver, filter)
        mManager.requestPermission(usbDevice, mPermissionIntent)
    }

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device = intent
                        .getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
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
                            pos!!.setPermissionDevice(device)
                        }
                    } else {
                        TRACE.i("usbpermission denied for device $device")
                    }
                    mContext!!.unregisterReceiver(this)
                }
            }
        }
    }

    // check for existing devices
    private val permissionDeviceList: List<*>
        private get() {
            val mManager = this.getSystemService(USB_SERVICE) as UsbManager
            val deviceList: MutableList<UsbDevice> = ArrayList<UsbDevice>()
            // check for existing devices
            for (device in mManager.deviceList.values) {
                deviceList.add(device)
            }
            return deviceList
        }

    private fun clearDisplay() {
        statusEditText!!.setText("")
    }

    internal inner class MyOnClickListener : View.OnClickListener {
        @SuppressLint("NewApi")
        override fun onClick(v: View) {
            statusEditText!!.setText("")
            if (selectBTFlag) {
                statusEditText!!.setText(R.string.wait)
                return
            } else if (v === doTradeButton) { //do trade button
                mhipStatus!!.setTextColor(resources.getColor(R.color.eb_col_34))
                mhipStatus!!.setText("")
                if (pos == null) {
                    statusEditText!!.setText(R.string.scan_bt_pos_error)
                    return
                }
                isPinCanceled = false
                statusEditText!!.setText(R.string.starting)
                //                terminalTime = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
                if (posType == POS_TYPE.UART) { //postype is UART
                    pos!!.setCardTradeMode(QPOSService.CardTradeMode.SWIPE_TAP_INSERT_CARD_NOTUP)
                    //                    pos.doTrade(terminalTime, 0, 30);
                    pos!!.doTrade(20)
                } else {
                    val keyIdex = keyIndex
                    //                    pos.setCardTradeMode(QPOSService.CardTradeMode.SWIPE_TAP_INSERT_CARD_NOTUP);
                    pos!!.doTrade(keyIdex, 30) //start do trade
                }
            } else if (v === btnUSB) {
                val usb = USBClass()
                val deviceList = usb.GetUSBDevices(baseContext)
                if (deviceList == null) {
                    Toast.makeText(mContext, "No Permission", Toast.LENGTH_SHORT).show()
                    return
                }
                val items = deviceList.toTypedArray<CharSequence>()
                val builder = AlertDialog.Builder(mContext)
                builder.setTitle("Select a Reader")
                builder.setSingleChoiceItems(items, -1) { dialog, item ->
                    val selectedDevice = items[item] as String
                    dialog.dismiss()
                    usbDevice = USBClass.getMdevices()[selectedDevice]
                    open(QPOSService.CommunicationMode.USB_OTG_CDC_ACM)
                    posType = POS_TYPE.OTG
                    pos!!.openUsb(usbDevice)
                }
                val alert = builder.create()
                alert.show()
            } else if (v === pollBtn) {
                pos!!.pollOnMifareCard(20)
                //                pos.doMifareCard("01", 20);
            } else if (v === pollULbtn) {
                pos!!.pollOnMifareCard(20)
                //                pos.doMifareCard("01", 20);
            } else if (v === finishBtn) {
                pos!!.finishMifareCard(20)
                //                pos.doMifareCard("0E", 20);
            } else if (v === finishULBtn) {
                pos!!.finishMifareCard(20)
                //                pos.doMifareCard("0E", 20);
            } else if (v === veriftBtn) {
                val keyValue = status!!.text.toString()
                val blockaddr = blockAdd!!.text.toString()
                val keyclass = mafireSpinner!!.selectedItem as String
                pos!!.setBlockaddr(blockaddr)
                pos!!.setKeyValue(keyValue)
                //                pos.doMifareCard("02" + keyclass, 20);
                pos!!.authenticateMifareCard(
                    QPOSService.MifareCardType.CLASSIC,
                    keyclass,
                    blockaddr,
                    keyValue,
                    20
                )
            } else if (v === veriftULBtn) {
                val keyValue = status11!!.text.toString()
                pos!!.setKeyValue(keyValue)
                //                pos.doMifareCard("0D", 20);
                pos!!.authenticateMifareCard(
                    QPOSService.MifareCardType.UlTRALIGHT,
                    "",
                    "",
                    keyValue,
                    20
                )
            } else if (v === readBtn) {
                val blockaddr = blockAdd!!.text.toString()
                pos!!.setBlockaddr(blockaddr)
                //                pos.doMifareCard("03", 20);
                pos!!.readMifareCard(QPOSService.MifareCardType.CLASSIC, blockaddr, 20)
            } else if (v === writeBtn) {
                val blockaddr = blockAdd!!.text.toString()
                val cardData = status!!.text.toString()
                //				SpannableString s = new SpannableString("please input card data");
//		        status.setHint(s);
                pos!!.setBlockaddr(blockaddr)
                pos!!.setKeyValue(cardData)
                //                pos.doMifareCard("04", 20);
                pos!!.writeMifareCard(QPOSService.MifareCardType.CLASSIC, blockaddr, cardData, 20)
            } else if (v === operateCardBtn) {
                val blockaddr = blockAdd!!.text.toString()
                val cardData = status!!.text.toString()
                val cmd = cmdSp!!.selectedItem as String
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
                //                pos.doMifareCard("05" + cmd, 20);
            } else if (v === getULBtn) {
//                pos.doMifareCard("06", 20);
                pos!!.getMifareCardInfo(20)
            } else if (v === readULBtn) {
                val blockaddr = blockAdd!!.text.toString()
                pos!!.setBlockaddr(blockaddr)
                //                pos.doMifareCard("07", 20);
                pos!!.readMifareCard(QPOSService.MifareCardType.CLASSIC, blockaddr, 20)
            } else if (v === fastReadUL) {
                val endAddr = blockAdd!!.text.toString()
                val startAddr = status11!!.text.toString()
                pos!!.setKeyValue(startAddr)
                pos!!.setBlockaddr(endAddr)
                //                pos.doMifareCard("08", 20);
                pos!!.fastReadMifareCardData(startAddr, endAddr, 20)
            } else if (v === writeULBtn) {
                val addr = blockAdd!!.text.toString()
                val data = status11!!.text.toString()
                pos!!.setKeyValue(data)
                pos!!.setBlockaddr(addr)
                //                pos.doMifareCard("0B", 20);
                pos!!.writeMifareCard(QPOSService.MifareCardType.UlTRALIGHT, addr, data, 20)
            } else if (v === transferBtn) {
//                String data = status.getText().toString();
//                String len = blockAdd.getText().toString();
//                pos.setMafireLen(Integer.valueOf(len, 16));
//                pos.setKeyValue(data);
//                pos.transferMifareData(data,20);
            }
        }
    }

    private val keyIndex: Int
        private get() {
            val s = mKeyIndex!!.text.toString()
            if (TextUtils.isEmpty(s)) return 0
            var i = 0
            try {
                i = s.toInt()
                if (i > 9 || i < 0) i = 0
            } catch (e: Exception) {
                i = 0
                return i
            }
            return i
        }

    private fun sendMsg(what: Int) {
        val msg = Message()
        msg.what = what
        mHandler.sendMessage(msg)
    }

    private val selectBTFlag = false
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                8003 -> {
                    try {
                        Thread.sleep(200)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    var content = ""
                    content = if (nfcLog == null) {
                        val h = pos!!.nfcBatchData
                        val tlv = h["tlv"]
                        TRACE.i("nfc batchdata1: $tlv")
                        "${statusEditText!!.text} NFCbatchData: ${h["tlv"]} ".trimIndent()
                    } else {
                        "${statusEditText!!.text} NFCbatchData: $nfcLog".trimIndent()
                    }
                    statusEditText!!.setText(content)
                }

                else -> {
                }
            }
        }
    }

    fun updateEmvConfigByXml() {
        pos?.updateEMVConfigByXml(
            this@OtherActivity.assets.open("emv_profile_tlv_D20.xml").bufferedReader().use {
                val text = it.readText()
                text
            })
    }

    companion object {
        private const val REQUEST_CODE_QRCODE_PERMISSIONS = 1
        private const val REQUEST_WRITE_EXTERNAL_STORAGE = 1001
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

        /*---------------------------------------------*/
        private const val FILENAME = "dsp_axdd"
    }
}