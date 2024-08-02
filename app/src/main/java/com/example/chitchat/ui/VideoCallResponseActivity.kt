package com.example.chitchat.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.chitchat.R
import com.example.chitchat.databinding.ActivityVideoCallResponseBinding
import com.example.chitchat.model.IceCandidateModel
import com.example.chitchat.model.MessageModel
import com.example.chitchat.model.TYPE
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

class VideoCallResponseActivity : AppCompatActivity(),NewMessageInterface {
    private var binding:ActivityVideoCallResponseBinding?=null

    private var rtcClient: RTCClient?=null
    private var remoteUser:String?=null
    private var localUser:String?=null
    private var sessionDescription:String?=null
    private lateinit var gson:Gson
    private var session:SessionDescription?=null

    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RtcAudioManager.create(applicationContext) }
    private var isSpeakerMode = true



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityVideoCallResponseBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        gson=Gson()

        // Get data from intent
        sessionDescription = intent.getStringExtra("sessionDescription")
        remoteUser = intent.getStringExtra("remoteUserId")
        localUser = intent.getStringExtra("localUserId")

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
            val message = MessageModel(TYPE.CALL_ENDED, localUser, remoteUser, null)
            WebSocketManager.sendMessageToSocket(message)

            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VideoCallResponseActivity, "The call has ended", Toast.LENGTH_LONG)
                        .show()
                    finish()
                }
            }
        }


        rtcClient= RTCClient(application,localUser!!, WebSocketManager,object : PeerConnectionObserver(){

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                rtcClient?.addIceCandidate(p0)

                val candidate= hashMapOf(
                    "sdpMid" to p0?.sdpMid,
                    "sdpMLineIndex" to p0?.sdpMLineIndex,
                    "sdpCandidate" to p0?.sdp
                )
                WebSocketManager.sendMessageToSocket(
                    MessageModel(TYPE.ICE_CANDIDATE,localUser,remoteUser,candidate)
                )
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                binding?.remoteViewLoading?.visibility=View.GONE
                p0?.videoTracks?.get(0)?.addSink(binding?.remoteView)
                Log.d("VIDEOCALLINGWEBRTC", "onAddStream: $p0")
            }

            override fun onRenegotiationNeeded() {

            }

        })





    }


    override fun onResume() {
        super.onResume()
        WebSocketManager.setActiveListener(this)

        Log.d("VIDEOCALLINGWEBRTC","onResume hit of VideoCallResponseActivity")


        rtcClient?.initializeSurfaceView(binding!!.localView)
        rtcClient?.initializeSurfaceView(binding!!.remoteView)
        rtcClient?.startLocalVideo(binding!!.localView)


        if(sessionDescription==null){
            Log.e("VIDEOCALLINGWEBRTC","session description is null")
        }else{
            Log.d("VIDEOCALLINGWEBRTC","session description is not null --- $sessionDescription")
        }


        session = SessionDescription(SessionDescription.Type.OFFER, sessionDescription)
        rtcClient?.onRemoteSessionReceived(session!!)
        rtcClient?.answer(remoteUser!!)

    }

    override fun onPause() {
        Log.d("VIDEOCALLINGWEBRTC","onPause hit of VideoCallResponseActivity")
        super.onPause()
        WebSocketManager.setActiveListener(null)
    }

    override fun onStop() {
        super.onStop()
        Log.d("VIDEOCALLINGWEBRTC","onStop of VideoCallResponseActivity hit..")
    }

    override fun onNewMessage(message: MessageModel) {
        when(message.type){
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
                        Toast.makeText(this@VideoCallResponseActivity, "The call has ended", Toast.LENGTH_LONG)
                            .show()
                        finish()
                    }
                }
            }

            else->{

            }
        }
    }

    override fun onDestroy() {
        Log.d("VIDEOCALLINGWEBRTC","onDestroy of VideoCallResponseActivity hit...")
        super.onDestroy()
        binding = null
        rtcClient?.endCall() // Close any existing WebRTC connections
        rtcClient=null
    }
}