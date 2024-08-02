package com.example.chitchat.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.chitchat.R
import com.example.chitchat.databinding.ActivityVideoBinding
import com.example.chitchat.model.IceCandidateModel
import com.example.chitchat.model.MessageModel
import com.example.chitchat.model.TYPE
import com.example.chitchat.model.User
import com.example.chitchat.webrtc.PeerConnectionObserver
import com.example.chitchat.webrtc.RTCClient
import com.example.chitchat.webrtc.RtcAudioManager
import com.example.chitchat.websocket.videosocketclient.NewMessageInterface
import com.example.chitchat.websocket.videosocketclient.WebSocketManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class VideoActivity : AppCompatActivity(),NewMessageInterface {

    private var rtcClient: RTCClient?=null
    private val gson = Gson()
    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RtcAudioManager.create(applicationContext) }
    private var isSpeakerMode = true

    private var currentUser:User?=null
    private var targetUser:User?=null

    private lateinit var uid:String
    private lateinit var targetUID:String

    private var binding:ActivityVideoBinding?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding?.root)


        currentUser=intent.getParcelableExtra("CurrentUserKey")
        targetUser=intent.getParcelableExtra("TargetUserKey")

        if (currentUser != null && targetUser != null) {
            uid=currentUser!!.userName
            targetUID=targetUser!!.userName
            Log.d("WebSocket", "sender--$uid receiver--$targetUID")
        }





        rtcAudioManager.setDefaultAudioDevice(RtcAudioManager.AudioDevice.SPEAKER_PHONE)

        binding?.switchCameraButton?.setOnClickListener {
            rtcClient?.switchCamera()
        }

        binding?.micButton?.setOnClickListener {
            if (isMute){
                isMute = false
                binding!!.micButton.setImageResource(R.drawable.ic_baseline_mic_off_24)
            }else{
                isMute = true
                binding!!.micButton.setImageResource(R.drawable.ic_baseline_mic_24)
            }
            rtcClient?.toggleAudio(isMute)
        }

        binding?.videoButton?.setOnClickListener {
            if (isCameraPause){
                isCameraPause = false
                binding!!.videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24)
            }else{
                isCameraPause = true
                binding!!.videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
            }
            rtcClient?.toggleCamera(isCameraPause)
        }

        binding?.audioOutputButton?.setOnClickListener {
            if (isSpeakerMode){
                isSpeakerMode = false
                binding!!.audioOutputButton.setImageResource(R.drawable.ic_baseline_hearing_24)
                rtcAudioManager.setDefaultAudioDevice(RtcAudioManager.AudioDevice.EARPIECE)
            }else{
                isSpeakerMode = true
                binding!!.audioOutputButton.setImageResource(R.drawable.ic_baseline_speaker_up_24)
                rtcAudioManager.setDefaultAudioDevice(RtcAudioManager.AudioDevice.SPEAKER_PHONE)

            }
        }


        binding?.endCallButton?.setOnClickListener {
            val message = MessageModel(TYPE.CALL_ENDED, uid, targetUID, null)
            WebSocketManager.sendMessageToSocket(message)

            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VideoActivity, "The call has ended", Toast.LENGTH_LONG)
                        .show()
                    finish()
                }
            }
        }


        rtcClient= RTCClient(application,uid, WebSocketManager,object : PeerConnectionObserver(){

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                rtcClient?.addIceCandidate(p0)

                val candidate= hashMapOf(
                    "sdpMid" to p0?.sdpMid,
                    "sdpMLineIndex" to p0?.sdpMLineIndex,
                    "sdpCandidate" to p0?.sdp
                )
                WebSocketManager.sendMessageToSocket(
                    MessageModel(TYPE.ICE_CANDIDATE,uid,targetUID,candidate)
                )
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                p0?.videoTracks?.get(0)?.addSink(binding?.remoteView)
                Log.d("VIDEOCALLINGWEBRTC", "onAddStream: $p0")
            }

            override fun onRenegotiationNeeded() {

            }

        })





    }



    override fun onResume() {
        super.onResume()
        Log.d("VIDEOCALLINGWEBRTC","onResume hit of VideoActivity")
        WebSocketManager.setActiveListener(this)

        WebSocketManager.sendMessageToSocket(
            MessageModel(TYPE.START_CALL, currentUser?.userName, targetUser?.userName, null
            )
        )
    }

    override fun onPause() {
        super.onPause()
        Log.d("VIDEOCALLINGWEBRTC","onPause hit of VideoActivity")
        WebSocketManager.setActiveListener(null)
    }



    override fun onNewMessage(message: MessageModel) {
        Log.d("VIDEOCALLINGWEBRTC","onNewMessage of VideoActivity hit....")
        Log.d("VIDEOCALLINGWEBRTC","received onNewMessage type is ----${message.type}")
        when (message.type) {


            TYPE.CALL_RESPONSE -> {
                if (message.data == "user is not online") {
                    //user is not reachable
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                applicationContext,
                                "user is not reachable",
                                Toast.LENGTH_LONG
                            ).show()

                        }
                    }
                } else {
                    //we are ready for call, we started a call
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            try {
                                Log.d("VIDEOCALLINGWEBRTC","going to start call....")
                                Log.d("VIDEOCALLINGWEBRTC", "Visibility set to visible")
                                binding?.callLayout?.visibility = View.VISIBLE
                                Log.d("VIDEOCALLINGWEBRTC", "Initializing local view")
                                rtcClient?.initializeSurfaceView(binding!!.localView)
                                Log.d("VIDEOCALLINGWEBRTC", "Initializing remote view")
                                rtcClient?.initializeSurfaceView(binding!!.remoteView)
                                Log.d("VIDEOCALLINGWEBRTC", "Starting local video")
                                Log.d("VIDEOCALLINGWEBRTC","at this stage value of rtcClient is $rtcClient")
                                rtcClient?.startLocalVideo(binding!!.localView)
                                Log.d("VIDEOCALLINGWEBRTC", "Calling target UID")
                                rtcClient?.call(targetUID)
                            }catch (e: Exception){
                                Log.e("VIDEOCALLINGWEBRTC", "Exception in starting call: ${e.message}", e)
                                Toast.makeText(
                                    applicationContext,
                                    "Failed to start call: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        }
                    }
                }
            }



            TYPE.ANSWER_RECEIVED -> {
                Log.d("VIDEOCALLINGWEBRTC","Answer Received by VideoCall Activity...")
                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
                rtcClient?.onRemoteSessionReceived(session)
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        binding?.remoteViewLoading?.visibility = View.GONE
                    }
                }
            }


            TYPE.ICE_CANDIDATE -> {
                try {
                    val receivingCandidate =
                        gson.fromJson(gson.toJson(message.data), IceCandidateModel::class.java)

                    rtcClient?.addIceCandidate(
                        IceCandidate(
                            receivingCandidate.sdpMid,
                            Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),
                            receivingCandidate.sdpCandidate
                        )
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            TYPE.CALL_ENDED -> {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@VideoActivity, "The call has ended", Toast.LENGTH_LONG)
                            .show()
                        finish()
                    }
                }
            }

            else -> {}
        }

    }

    override fun onStop() {
        super.onStop()
        Log.d("VIDEOCALLINGWEBRTC","onStop of VideoActivity hit..")
    }

    override fun onDestroy() {
        Log.d("VIDEOCALLINGWEBRTC","onDestroy of VideoActivity hit...")
        super.onDestroy()
        binding = null
        rtcClient?.endCall() // Close any existing WebRTC connections
        rtcClient=null
    }

}