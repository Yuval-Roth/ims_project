import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ApkInstaller {

    private val terminalLock = Any()

    private class PairingFailedException: Exception()

    private val adbResource = "/tools/adb.exe"
    private val instructionsResource = "/instructions.txt"
    private val tempDir = createTempDir("adb-installer")

    // Extract adb.exe from inside the JAR to temp
    private val adbFile: File by lazy {
        extractResource(adbResource, "adb.exe")
    }

    // The APK is NOT inside the JAR — it's external.
    private val apkFile: File by lazy {
        resolveApkPath()
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

        val target = handleConnecting() ?: return
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
        println("Installing APK from: ${apkFile.absolutePath}\n")

        val statuses = devices.associateWith { "WAIT" }.toMutableMap()

        // Print static table header
        println("%-3s %-28s | %-10s".format("#", "Device", "Status"))
        println("-".repeat(45))
        devices.forEachIndexed { i, device ->
            println("%-3d %-28s | %-10s".format(i + 1, device, statuses[device]))
        }
        println()

        val tableStartLine = 5 + devices.size // header + spacing (adjust if layout changes)

        fun updateStatusLine(index: Int, status: String) {
            synchronized(terminalLock) {
                statuses[devices[index]] = status
                val line = tableStartLine + index
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
        val hostIp = pairedHost ?: readLineSafe("Device host address")
        val debugPort = readLineSafe("Wireless debugging port")
        val target = "$hostIp:$debugPort"

        println("Connecting to device...")
        val connectOutput = runCommand(listOf(adbFile.absolutePath, "connect", target))
        return if (connectOutput.any { it.contains("connected to") }) {
            println("\nConnected successfully to $target.\n")
            target
        } else {
            println("\nFailed to connect to $target")
            println("Please check the IP and port.")
            waitForEnter()
            null
        }
    }

    private fun handlePairing(): String? {
        val response = readLineSafe("Is the device already paired? (Y/N)").lowercase()
        return if (response != "y") {
            val hostIp = readLineSafe("Device IP address")
            val pairingPort = readLineSafe("Wireless pairing port")
            val pairingCode = readLineSafe("Pairing code")

            println("Pairing device...")
            val pairOutput = runCommand(listOf(adbFile.absolutePath, "pair", "$hostIp:$pairingPort", pairingCode))
            if (pairOutput.any { it.contains("Successfully paired", ignoreCase = true) }) {
                println("\nPairing successful.\n")
                hostIp
            } else {
                println("\nPairing failed. Check IP, port, and pairing code.\n")
                waitForEnter()
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
            println(" Error running command: ${args.joinToString(" ")}")
            println(e.message)
            emptyList()
        }
    }

    private fun readLineSafe(prompt: String): String {
        print("$prompt: ")
        return readlnOrNull()?.trim().takeUnless { it.isNullOrEmpty() } ?: readLineSafe(prompt)
    }

    private fun printInstructions() {
        val stream = this::class.java.getResourceAsStream(instructionsResource)
        if (stream != null) {
            BufferedReader(InputStreamReader(stream)).useLines { lines ->
                lines.forEach(::println)
            }
        } else {
            println(" Instructions file not found in resources at $instructionsResource")
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

    private fun resolveApkPath(): File {
        // Detect where the jar itself is running from
        val jarDir = File(ApkInstaller::class.java.protectionDomain.codeSource.location.toURI()).parentFile
        val apkPath = File(jarDir.parentFile, "apk/ims.apk")
        if (!apkPath.exists()) {
            throw FileNotFoundException("Could not find APK at: ${apkPath.absolutePath}")
        }
        return apkPath
    }

    private fun println(msg: String) {
        synchronized(terminalLock) {
            kotlin.io.println(msg)
        }
    }
}
