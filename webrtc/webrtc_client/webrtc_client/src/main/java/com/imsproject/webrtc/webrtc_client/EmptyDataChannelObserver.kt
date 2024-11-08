package com.imsproject.webrtc.webrtc_client

import org.webrtc.DataChannel

open class EmptyDataChannelObserver : DataChannel.Observer {
    override fun onBufferedAmountChange(p0: Long) {}

    override fun onStateChange() {}

    override fun onMessage(p0: DataChannel.Buffer?) {}
}