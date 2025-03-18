package com.imsproject.watch.utils

class PacketTracker() {

    // callbacks
    var onOutOfOrderPacket: (() -> Unit)? = null

    private var counter = 0L
    private var myLastReceivedPacket = -1L
    private var otherLastReceivedPacket = -1L

    /**
     * @return packet sequence number
     */
    fun newPacket() : Long {
        return counter++
    }

    fun receivedMyPacket(packetNum: Long) {
        val lastReceived = myLastReceivedPacket
        myLastReceivedPacket = packetNum
        if (packetNum != lastReceived + 1) {
            onOutOfOrderPacket?.invoke()
        }
    }

    /**
     * @return true if the packet is out of order
     */
    fun receivedOtherPacket(packetNum: Long) : Boolean {
        val lastReceived = otherLastReceivedPacket
        otherLastReceivedPacket = packetNum
        if (packetNum != lastReceived + 1) {
            onOutOfOrderPacket?.invoke()
            return true
        }
        return false
    }
}