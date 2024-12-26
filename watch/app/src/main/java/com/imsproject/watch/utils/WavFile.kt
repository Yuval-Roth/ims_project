package com.imsproject.watch.utils

data class WavFile(
    val chunkId: String,
    val fileSize: Int,
    val format: String,
    val fmtChunkId: String,
    val fmtChunkSize: Int,
    val audioFormat: Int,
    val numChannels: Int,
    val sampleRate: Int,
    val byteRate: Int,
    val blockAlign: Int,
    val bitsPerSample: Int,
    val dataChunkId: String,
    val dataSize: Int,
    val audioData: ByteArray
){
    companion object{
        fun parse(data: ByteArray) : WavFile {
            var offset = 0

            // RIFF Header
            val riffHeader = data.copyOfRange(offset, offset + 12)
            offset += 12
            val chunkId = riffHeader.copyOfRange(0, 4).toString(Charsets.US_ASCII) // "RIFF"
            val fileSize = riffHeader.copyOfRange(4, 8).toLittleEndianInt()
            val format = riffHeader.copyOfRange(8, 12).toString(Charsets.US_ASCII) // "WAVE"

            // fmt Subchunk
            val fmtHeader = data.copyOfRange(offset, offset + 24)
            offset += 24
            val fmtChunkId = fmtHeader.copyOfRange(0, 4).toString(Charsets.US_ASCII) // "fmt "
            val fmtChunkSize = fmtHeader.copyOfRange(4, 8).toLittleEndianInt()
            val audioFormat = fmtHeader.copyOfRange(8, 10).toLittleEndianShort()
            val numChannels = fmtHeader.copyOfRange(10, 12).toLittleEndianShort()
            val sampleRate = fmtHeader.copyOfRange(12, 16).toLittleEndianInt()
            val byteRate = fmtHeader.copyOfRange(16, 20).toLittleEndianInt()
            val blockAlign = fmtHeader.copyOfRange(20, 22).toLittleEndianShort()
            val bitsPerSample = fmtHeader.copyOfRange(22, 24).toLittleEndianShort()

            // data Subchunk
            val dataHeader = data.copyOfRange(offset, offset + 8)
            offset += 8
            val dataChunkId = dataHeader.copyOfRange(0, 4).toString(Charsets.US_ASCII) // "data"
            val dataSize = dataHeader.copyOfRange(4, 8).toLittleEndianInt()

            // Audio data
            val audioData = data.copyOfRange(offset, offset + dataSize)

            return WavFile(
                chunkId,
                fileSize,
                format,
                fmtChunkId,
                fmtChunkSize,
                audioFormat,
                numChannels,
                sampleRate,
                byteRate,
                blockAlign,
                bitsPerSample,
                dataChunkId,
                dataSize,
                audioData
            )
        }
        fun ByteArray.toLittleEndianInt(): Int {
            return (this[0].toInt() and 0xFF) or
                    ((this[1].toInt() and 0xFF) shl 8) or
                    ((this[2].toInt() and 0xFF) shl 16) or
                    ((this[3].toInt() and 0xFF) shl 24)
        }
        fun ByteArray.toLittleEndianShort(): Int {
            return (this[0].toInt() and 0xFF) or
                    ((this[1].toInt() and 0xFF) shl 8)
        }
    }
}