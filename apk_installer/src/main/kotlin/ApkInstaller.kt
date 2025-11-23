import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ApkInstaller {

    private val terminalLock = Any()

    private class PairingFailedException: Exception()

    private val adbResource = "/tools/adb.exe"
    private val instructionsResource = "/instructions.txt"
    private val tempDir = createTempDir("apk-installer")

    // Extract adb.exe from inside the JAR to temp
    private val adbFile: File by lazy {
        extractResource("/tools/adbWinApi.dll", "adbWinApi.dll")
        extractResource("/tools/adbWinUsbApi.dll", "adbWinUsbApi.dll")
        extractResource("/tools/etc1tool.exe","etc1tool.exe")
        extractResource("/tools/fastboot.exe","fastboot.exe")
        extractResource("/tools/hprof-conv.exe","hprof-conv.exe")
        extractResource("/tools/libwinpthread-1.dll","libwinpthread-1.dll")
        extractResource("/tools/make_f2fs.exe","make_f2fs.exe")
        extractResource("/tools/make_f2fs_casefold.exe","make_f2fs_casefold.exe")
        extractResource("/tools/mke2fs.conf","mke2fs.conf")
        extractResource("/tools/mke2fs.exe","mke2fs.exe")
        extractResource("/tools/source.properties","source.properties")
        extractResource("/tools/sqlite3.exe","sqlite3.exe")
        extractResource(adbResource, "adb.exe")
    }

    // The APK is NOT inside the JAR — it's external.
    private val apkFile: File by lazy {
        val apkFile = apkFile()
        if (!apkFile.exists()) {
            throw FileNotFoundException("Could not find APK at: ${apkFile.absolutePath}")
        }
        apkFile
    }

    fun verifyApkExists(): Boolean {
        return apkFile().exists()
    }

    fun getApkPath(): String {
        return apkFile().absolutePath
    }

    fun connectDevice() {
        clearConsole()
        println("=== Connect New Device ===\n")
        handleConnecting()
        println()
        waitForEnter()
    }

    fun oneTimeInstall() {
        clearConsole()
        println()
        println("Instructions:\n")
        printInstructions()
        println()

        val target = handleConnecting() ?: run {
            waitForEnter()
            return
        }
        println("Installing the app from ${apkFile.absolutePath} ...")
        installOnDevice(target)
        waitForEnter()
    }

    fun installOnAllDevices() {
        val devices = getConnectedDevices()
        if (devices.isEmpty()) {
            println("No connected devices found.")
            waitForEnter()
            return
        }

        println("=== Install on All Devices ===\n")

        val statuses = devices.associateWith { "WAIT" }.toMutableMap()

        // Print static table header
        println("%-3s %-28s | %-10s".format("#", "Device", "Status"))
        println("-".repeat(45))
        devices.forEachIndexed { i, device ->
            println("%-3d %-28s | %-10s".format(i + 1, device, statuses[device]))
        }
        println()

        val tableStartLine = 4 //+ devices.size // header + spacing (adjust if layout changes)

        fun updateStatusLine(index: Int, status: String) {
            synchronized(terminalLock) {
                statuses[devices[index]] = status
                val line = tableStartLine + index + 1
                // Move cursor to specific table line and overwrite
                print("\u001B7") // save cursor
                print("\u001B[${line};1H") // move to start of that table line
                print("\u001B[2K") // clear the line
                print("%-3d %-28s | %-10s".format(index + 1, devices[index], status))
                print("\u001B8") // restore cursor
                System.out.flush()
            }
        }

        val threads = devices.mapIndexed { i, device ->
            Thread {
                updateStatusLine(i, "RUNNING")
                val success = installOnDevice(device)
                updateStatusLine(i, if (success) "OK" else "FAIL")
            }.apply { start() }
        }

        threads.forEach { it.join() }

        println("\nAll installations completed.")
        waitForEnter()
    }

    private fun installOnDevice(target: String): Boolean {
        println("Installing APK on $target...")
        var installOutput = runCommand(listOf(adbFile.absolutePath, "-s", target, "install", "-r", apkFile.absolutePath))
        var success = installOutput.any { it.contains("Success", ignoreCase = true) }
        val updateIncompatible = installOutput.any { it.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") }

        if (!success && updateIncompatible) {
            println("Existing incompatible app detected on $target. Uninstalling...")
            runCommand(listOf(adbFile.absolutePath, "-s", target, "uninstall", "com.imsproject.watch"))
            println("Reinstalling on $target...")
            installOutput = runCommand(listOf(adbFile.absolutePath, "-s", target, "install", apkFile.absolutePath))
            success = installOutput.any { it.contains("Success", ignoreCase = true) }
        }

        println()
        if (success) {
            println("APK installed successfully on $target")
        } else {
            println("APK install failed on $target. Output:")
            installOutput.forEach{ println("> $it") }
        }
        println()
        return success
    }


    private fun handleConnecting(): String? {
        val pairedHost = try { handlePairing() } catch (_: PairingFailedException) { return null }
        val hostIp = pairedHost ?: readLineSafe("Device host address", hostRegex, hostHint)
        val debugPort = readLineSafe("Wireless debugging port", portRegex, portHint)
        val target = "$hostIp:$debugPort"

        println("Connecting to device...")
        val connectOutput = runCommand(listOf(adbFile.absolutePath, "connect", target))
        return if (connectOutput.any { it.contains("connected to") }) {
            println("\nConnected successfully to $target.\n")
            target
        } else {
            println("\nFailed to connect to $target")
            println("Please check the IP and port.")
            null
        }
    }

    private fun handlePairing(): String? {
        val response = readLineSafe("Is the device already paired? (y/n)", Regex("^[yYnN]$")).lowercase()
        return if (response == "n") {
            val hostIp = readLineSafe("Device IP address", hostRegex, hostHint)
            val pairingPort = readLineSafe("Wireless pairing port", portRegex, portHint)
            val pairingCode = readLineSafe("Pairing code", pairingCodeRegex, pairingCodeHint)

            println("Pairing device...")
            val pairOutput = runCommand(listOf(adbFile.absolutePath, "pair", "$hostIp:$pairingPort", pairingCode))
            if (pairOutput.any { it.contains("Successfully paired", ignoreCase = true) }) {
                println("\nPairing successful.\n")
                hostIp
            } else {
                println("\nPairing failed. Check IP, port, and pairing code.\n")
                throw PairingFailedException()
            }
        } else {
            null
        }
    }

    private fun runCommand(args: List<String>): List<String> {
        return try {
            val process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()

            val output = mutableListOf<String>()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lines().forEach(output::add)
            }
            process.waitFor()
            output
        } catch (e: Exception) {
            println("Error running command: ${args.joinToString(" ")}")
            println(e.message)
            emptyList()
        }
    }

    private fun readLineSafe(prompt: String, matching: Regex? = null, hint: String? = null): String {
        print("$prompt: ")
        val response = readlnOrNull()?.trim().takeUnless { it.isNullOrEmpty() } ?: readLineSafe(prompt)
        if (matching != null && !matching.matches(response)) {
            println("Invalid input format. Please try again.${hint?.let { " Expected format: $it" } ?: ""}")
            return readLineSafe(prompt, matching, hint)
        }
        return response
    }

    private fun printInstructions() {
        val stream = this::class.java.getResourceAsStream(instructionsResource)
        if (stream != null) {
            BufferedReader(InputStreamReader(stream)).useLines { lines ->
                lines.forEach(::println)
            }
        } else {
            println("Instructions file not found in resources at $instructionsResource")
        }
    }

    fun waitForEnter() {
        println("Press ENTER to continue...")
        readlnOrNull()
    }

    fun clearConsole() {
        try {
            ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor()
        } catch (_: Exception) { }
    }

    fun getConnectedDevices(): List<String> {
        val devices = mutableListOf<String>()
        val devicesOutput = runCommand(listOf(adbFile.absolutePath, "devices"))
        val iterator = devicesOutput.iterator()
        iterator.next() // Skip the first line "List of devices attached"
        while (iterator.hasNext()) {
            val line = iterator.next()
            if (!line.contains("emulator") && line.isNotBlank()) {
                // regex to extract device info
                val deviceInfo = Regex("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}:[0-9]{5}").find(line)
                if (deviceInfo != null) {
                    devices.add(deviceInfo.groupValues[0])
                }
            }
        }
        return devices
    }

    private fun extractResource(resourcePath: String, outputName: String): File {
        val outputFile = File(tempDir, outputName)
        val stream = this::class.java.getResourceAsStream(resourcePath)
            ?: throw FileNotFoundException("Resource not found: $resourcePath")
        Files.copy(stream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return outputFile
    }

    private fun createTempDir(prefix: String): File {
        val dir = Files.createTempDirectory(prefix).toFile()
        dir.deleteOnExit()
        return dir
    }

    private fun apkFile(): File {
        // Detect where the jar itself is running from
        val jarDir = File(ApkInstaller::class.java.protectionDomain.codeSource.location.toURI()).parentFile
        val apkFile = File(jarDir.parentFile, "apk/ims.apk")
        return apkFile
    }

    private fun println(msg: String) {
        synchronized(terminalLock) {
            kotlin.io.println(msg)
        }
    }

    companion object {
        private val portRegex = Regex("^[0-9]{5}$")
        private val portHint = "5-digit number"
        private val hostRegex = Regex("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$")
        private val hostHint = "IPv4, e.g. 192.168.1.101"
        private val pairingCodeRegex = Regex("^[0-9]{6}$")
        private val pairingCodeHint = "6-digit number"
    }
}
