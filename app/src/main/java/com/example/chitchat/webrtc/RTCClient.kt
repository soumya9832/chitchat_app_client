package com.example.chitchat.webrtc

import android.app.Application
import android.os.Build
import android.util.Log
import com.example.chitchat.model.MessageModel
import com.example.chitchat.model.TYPE
import com.example.chitchat.websocket.videosocketclient.WebSocketManager
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

class RTCClient(
    private val application: Application,
    private val username:String,
    private val webSocketManager: WebSocketManager,
    private val observer: PeerConnection.Observer
) {

    init {
        initPeerConnectionFactory(application)
    }

    /*
      The code initializes essential components of WebRTC, including the
      EglBase for rendering, PeerConnectionFactory for managing peer
      connections, ICE servers for STUN and TURN, and local media sources.
    */

    private val eglContext = EglBase.create()
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }

    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),

        PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
            .setUsername("30abbe56fd9b44aef23fbf7d")
            .setPassword("kkvp4odVfFZHGab2")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
            .setUsername("30abbe56fd9b44aef23fbf7d")
            .setPassword("kkvp4odVfFZHGab2")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
            .setUsername("30abbe56fd9b44aef23fbf7d")
            .setPassword("kkvp4odVfFZHGab2")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443?transport=tcp")
            .setUsername("30abbe56fd9b44aef23fbf7d")
            .setPassword("kkvp4odVfFZHGab2")
            .createIceServer()
        )

    private val localVideoSource = peerConnectionFactory.createVideoSource(false)
    private val localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
    private val peerConnection by lazy { createPeerConnection(observer) }
    private var videoCapturer: CameraVideoCapturer? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null


    /*
     These next three functions handle the initialization and creation of the
     PeerConnectionFactory and PeerConnection instances with appropriate
     configurations.
   */

    private fun initPeerConnectionFactory(application: Application){
        val peerConnectionOptions = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(peerConnectionOptions)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglContext.eglBaseContext,
                    true,
                    true)
            )
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption=true
                disableNetworkMonitor=true
            })
            .setAudioDeviceModule(
                JavaAudioDeviceModule.builder(application)
                    .setUseHardwareAcousticEchoCanceler(Build.VERSION.SDK_INT>= Build.VERSION_CODES.Q)
                    .setUseHardwareNoiseSuppressor(Build.VERSION.SDK_INT>= Build.VERSION_CODES.Q)
                    .createAudioDeviceModule().also {
                        it.setMicrophoneMute(false)
                        it.setSpeakerMute(false)
                    }
            )
            .createPeerConnectionFactory()
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection?{
        return peerConnectionFactory.createPeerConnection(iceServer,observer)
    }


    /*
   These next three functions handle the initialization of a SurfaceViewRenderer
   for local video and starting the local video capture.
  */

    fun initializeSurfaceView(surfaceViewRenderer: SurfaceViewRenderer){
        surfaceViewRenderer.run {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglContext.eglBaseContext,null)
        }
    }


    fun startLocalVideo(surfaceViewRenderer: SurfaceViewRenderer){
        try {
            Log.d("VIDEOCALLINGWEBRTC", "Initializing SurfaceTextureHelper")
            val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglContext.eglBaseContext)
            videoCapturer = getVideoCapturer(application)

            if (videoCapturer == null) {
                Log.e("VIDEOCALLINGWEBRTC", "Video capturer is null")
                return
            }

            Log.d("VIDEOCALLINGWEBRTC", "Initializing video capturer")
            videoCapturer?.initialize(
                surfaceTextureHelper,
                surfaceViewRenderer.context,
                localVideoSource.capturerObserver
            )

            Log.d("VIDEOCALLINGWEBRTC", "Starting video capture")
            videoCapturer?.startCapture(320, 240, 30)

            Log.d("VIDEOCALLINGWEBRTC", "Creating local video track")
            localVideoTrack = peerConnectionFactory.createVideoTrack("local_track", localVideoSource)
            localVideoTrack?.addSink(surfaceViewRenderer)

            Log.d("VIDEOCALLINGWEBRTC", "Creating local audio track")
            localAudioTrack = peerConnectionFactory.createAudioTrack("local_track_audio", localAudioSource)

            Log.d("VIDEOCALLINGWEBRTC", "Creating local media stream")
            val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
            localStream.addTrack(localVideoTrack)
            localStream.addTrack(localAudioTrack)

            Log.d("VIDEOCALLINGWEBRTC", "Adding local stream to peer connection while peerConnection is -->$peerConnection")
            if (localStream.videoTracks.isEmpty() || localStream.audioTracks.isEmpty()) {
                Log.e("VIDEOCALLINGWEBRTC", "Local stream has no tracks for video or audio")
            }
            Log.d("VIDEOCALLINGWEBRTC", "Local stream video tracks: ${localStream.videoTracks} audio tracks: ${localStream.audioTracks}")

            try {
                peerConnection?.addStream(localStream)
            }catch (e:Exception){
                e.printStackTrace()
                Log.e("VIDEOCALLINGWEBRTC", "Exception during startLocalVideo: ${e.message}")
            }


            Log.d("VIDEOCALLINGWEBRTC", "Local video started successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("VIDEOCALLINGWEBRTC", "Exception during startLocalVideo: ${e.message}")
        }

    }

    private fun getVideoCapturer(application: Application): CameraVideoCapturer? {
        return try {
            Camera2Enumerator(application).run {
                deviceNames.find {
                    isFrontFacing(it)
                }?.let {
                    createCapturer(it,null)
                } ?:null
            }
        }catch (e:Exception){
            Log.e("VIDEOCALLINGWEBRTC", "Error creating video capturer: ${e.message}")
            null
        }
    }

    /*
      These functions are responsible for initiating a call, handling the
      reception of a remote session description, and answering an incoming
      call.
    */

    fun call(target:String){
        val mediaConstraints= MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                Log.d("VIDEOCALLINGWEBRTC","local sdp is --- ${description?.description}")

                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        val offer= hashMapOf(
                            "sdp" to description?.description,
                            "type" to description?.type
                        )

                        webSocketManager.sendMessageToSocket(
                            MessageModel(
                                TYPE.CREATE_OFFER,
                                username,
                                target,
                                offer
                            )
                        )

                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.d("VIDEOCALLINGWEBRTC","local description create failure of create offer due to $p0")
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.d("VIDEOCALLINGWEBRTC","set local description failure of create offer due to $p0")
                    }

                },description)
            }

            override fun onSetSuccess() {

            }

            override fun onCreateFailure(p0: String?) {
                Log.d("VIDEOCALLINGWEBRTC","create offer failure due to $p0")
            }

            override fun onSetFailure(p0: String?) {

            }

        },mediaConstraints)
    }


    fun onRemoteSessionReceived(description: SessionDescription){
        Log.d("VIDEOCALLINGWEBRTC", "Remote session description type: ${description.type}")
        Log.d("VIDEOCALLINGWEBRTC", "Remote session description SDP: ${description.description}")

        if (peerConnection == null) {
            Log.d("VIDEOCALLINGWEBRTC", "PeerConnection is null")
            return
        }

        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                Log.d("VIDEOCALLINGWEBRTC", "Remote Description create success....")
            }

            override fun onSetSuccess() {
                Log.d("VIDEOCALLINGWEBRTC", "Remote Description set success....")
            }

            override fun onCreateFailure(p0: String?) {
                Log.d("VIDEOCALLINGWEBRTC", "Remote Description create failure due to $p0....")
            }

            override fun onSetFailure(p0: String?) {
                Log.d("VIDEOCALLINGWEBRTC", "Remote Description set failure due to $p0")
            }
        }, description)
    }


    fun answer(target: String) {
        Log.d("VIDEOCALLINGWEBRTC","answer creation start...")
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {

                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Log.d("VIDEOCALLINGWEBRTC","answer creation  success....")
                    }


                    override fun onSetSuccess() {
                        val answer = hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type
                        )
                        webSocketManager.sendMessageToSocket(
                            MessageModel(
                                TYPE.CREATE_ANSWER, username, target, answer
                            )
                        )
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.d("VIDEOCALLINGWEBRTC","local description create failure of create answer due to $p0")
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.d("VIDEOCALLINGWEBRTC","local description set failure of create answer due to $p0")
                    }

                }, desc)
            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }

        }, constraints)
    }


    /*
      These functions handle the addition of ICE candidates, camera switching,
      audio toggling, video toggling, and ending a call.
    */

    fun addIceCandidate(p0: IceCandidate?) {
        peerConnection?.addIceCandidate(p0)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun toggleAudio(mute: Boolean) {
        localAudioTrack?.setEnabled(mute)
    }

    fun toggleCamera(cameraPause: Boolean) {
        localVideoTrack?.setEnabled(cameraPause)
    }

    fun endCall() {
        peerConnection?.close()
        peerConnection?.dispose()
    }



}
