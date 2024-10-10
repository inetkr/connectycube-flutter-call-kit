package com.connectycube.flutter.connectycube_flutter_call_kit

import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.connectycube.flutter.connectycube_flutter_call_kit.utils.getPhotoPlaceholderResId
import com.google.android.material.imageview.ShapeableImageView
import com.skyfishjy.library.RippleBackground
import org.json.JSONObject


fun createStartIncomingScreenIntent(
    context: Context, callId: String, callType: Int, callInitiatorId: Int,
    callInitiatorName: String, opponents: ArrayList<Int>, callPhoto: String?, userInfo: String
): Intent {
    val intent = Intent(context, IncomingCallActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.putExtra(EXTRA_CALL_ID, callId)
    intent.putExtra(EXTRA_CALL_TYPE, callType)
    intent.putExtra(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
    intent.putExtra(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
    intent.putIntegerArrayListExtra(EXTRA_CALL_OPPONENTS, opponents)
    intent.putExtra(EXTRA_CALL_PHOTO, callPhoto)
    intent.putExtra(EXTRA_CALL_USER_INFO, userInfo)
    return intent
}

class IncomingCallActivity : Activity() {
    private lateinit var callStateReceiver: BroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private var callId: String? = null
    private var callType = -1
    private var callInitiatorId = -1
    private var callInitiatorName: String? = null
    private var callOpponents: ArrayList<Int>? = ArrayList()
    private var callPhoto: String? = null
    private var callUserInfo: String? = null


    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        processIncomingData(intent)
        if(callType == 2){
            setContentView(resources.getIdentifier("activity_incoming_pluskit", "layout", packageName))
        }else{
            setContentView(resources.getIdentifier("activity_incoming_call", "layout", packageName))
        }
//        setContentView(resources.getIdentifier("activity_incoming_call", "layout", packageName))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setInheritShowWhenLocked(true)
        }

        with(getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestDismissKeyguard(this@IncomingCallActivity, object :
                    KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissError() {
                        Log.d("IncomingCallActivity", "[KeyguardDismissCallback.onDismissError]")
                    }

                    override fun onDismissSucceeded() {
                        Log.d(
                            "IncomingCallActivity",
                            "[KeyguardDismissCallback.onDismissSucceeded]"
                        )
                    }

                    override fun onDismissCancelled() {
                        Log.d(
                            "IncomingCallActivity",
                            "[KeyguardDismissCallback.onDismissCancelled]"
                        )
                    }
                })
            }
        }

        processIncomingData(intent)
        initUi()
        initCallStateReceiver()
        registerCallStateReceiver()
        if(callType == 2){
            val countValue : TextView =  findViewById(resources.getIdentifier("count_down_txt", "id", packageName))
            onStartCall(null)
//            timer = object : CountDownTimer(0, 1000){
//                override fun onTick(remaining: Long) {
//                    val formattedRemaining = String.format("%02d", remaining / 1000)
//                    countValue.text = formattedRemaining
//                }
//                override fun onFinish() {
//                    onStartCall(null)
//                }
//            }
        }
    }

    private fun initCallStateReceiver() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        callStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null || TextUtils.isEmpty(intent.action)) return
                val action: String? = intent.action

                val callIdToProcess: String? = intent.getStringExtra(EXTRA_CALL_ID)
                if (TextUtils.isEmpty(callIdToProcess) || callIdToProcess != callId) {
                    return
                }
                when (action) {
                    ACTION_CALL_NOTIFICATION_CANCELED, ACTION_CALL_REJECT, ACTION_CALL_ENDED -> {
                        finishAndRemoveTask()
                    }

                    ACTION_CALL_ACCEPT -> finishDelayed()
                }
            }
        }
    }

    private fun finishDelayed() {
        Handler(Looper.getMainLooper()).postDelayed({
            finishAndRemoveTask()
        }, 1000)
    }

    private fun registerCallStateReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_CALL_NOTIFICATION_CANCELED)
        intentFilter.addAction(ACTION_CALL_REJECT)
        intentFilter.addAction(ACTION_CALL_ACCEPT)
        intentFilter.addAction(ACTION_CALL_ENDED)
        localBroadcastManager.registerReceiver(callStateReceiver, intentFilter)
    }

    private fun unRegisterCallStateReceiver() {
        localBroadcastManager.unregisterReceiver(callStateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unRegisterCallStateReceiver()
    }

    private fun processIncomingData(intent: Intent) {
        callId = intent.getStringExtra(EXTRA_CALL_ID)
        callType = intent.getIntExtra(EXTRA_CALL_TYPE, -1)
        callInitiatorId = intent.getIntExtra(EXTRA_CALL_INITIATOR_ID, -1)
        callInitiatorName = intent.getStringExtra(EXTRA_CALL_INITIATOR_NAME)
        callOpponents = intent.getIntegerArrayListExtra(EXTRA_CALL_OPPONENTS)
        callPhoto = intent.getStringExtra(EXTRA_CALL_PHOTO)
        callUserInfo = intent.getStringExtra(EXTRA_CALL_USER_INFO)
    }

    private fun initUi() {
        val callTitleTxt: TextView =
            findViewById(resources.getIdentifier("user_name_txt", "id", packageName))
        val price: TextView =
            findViewById(resources.getIdentifier("user_price_txt", "id", packageName))


        val avatar: ImageView =
            findViewById(resources.getIdentifier("user_avatar", "id", packageName))
        var obj = JSONObject(callUserInfo)
        var caller = obj?.getString("caller")!!
        var callerObj = JSONObject(caller);
        var callerAvatar = callerObj?.getString("avatar")!!

        var callHistory = obj?.getString("call_history")!!
        var callHistoryObj = JSONObject(callHistory);
        var callPrice = callHistoryObj?.getString("pay_per_minute")!!

        Glide.with(this).load(callerAvatar).placeholder(R.drawable.default_avatar)
            .into(avatar)
        callTitleTxt.text = callInitiatorName
        if(callType == 1){
            price.text = "수신 시 1분당 ${callPrice}스타가 적립됩니다."
        }
        else if(callType == 2){
            price.text = "연결시 1분당 ${callPrice}스타가 적립됩니다."
        }
    }

    override fun onStart() {
        super.onStart()
//        if(callType == 2)
//            timer.start()
    }

    override fun onStop() {
        super.onStop()
//        if(callType == 2)
//            timer.cancel()
    }

    // calls from layout file
    fun onEndCall(view: View?) {
        val bundle = Bundle()
        bundle.putString(EXTRA_CALL_ID, callId)
        bundle.putInt(EXTRA_CALL_TYPE, callType)
        bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
        bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
        bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
        bundle.putString(EXTRA_CALL_PHOTO, callPhoto)
        bundle.putString(EXTRA_CALL_USER_INFO, callUserInfo)

        val endCallIntent = Intent(this, EventReceiver::class.java)
        endCallIntent.action = ACTION_CALL_REJECT
        endCallIntent.putExtras(bundle)
        applicationContext.sendBroadcast(endCallIntent)
    }

    // calls from layout file
    fun onStartCall(view: View?) {
        val bundle = Bundle()
        bundle.putString(EXTRA_CALL_ID, callId)
        bundle.putInt(EXTRA_CALL_TYPE, callType)
        bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
        bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
        bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
        bundle.putString(EXTRA_CALL_PHOTO, callPhoto)
        bundle.putString(EXTRA_CALL_USER_INFO, callUserInfo)

        val startCallIntent = Intent(this, EventReceiver::class.java)
        startCallIntent.action = ACTION_CALL_ACCEPT
        startCallIntent.putExtras(bundle)
        applicationContext.sendBroadcast(startCallIntent)
    }
}
