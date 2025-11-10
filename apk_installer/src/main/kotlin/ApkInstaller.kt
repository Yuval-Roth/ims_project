import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ApkInstaller {

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
        println(getConnectedDevices().joinToString("\n"))
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
        var installOutput = runCommand(listOf(adbFile.absolutePath, "-s", target, "install", "-r", apkFile.absolutePath))
        var success = installOutput.any { it.contains("Success", ignoreCase = true) }
        val updateIncompatible = installOutput.any { it.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") }

        if (!success) {
            if (updateIncompatible) {
                println("Existing incompatible app detected. Uninstalling...")
                runCommand(listOf(adbFile.absolutePath, "-s", target, "uninstall", "com.imsproject.watch"))
                println("Reinstalling...")
                installOutput = runCommand(listOf(adbFile.absolutePath, "-s", target, "install", apkFile.absolutePath))
                success = installOutput.any { it.contains("Success", ignoreCase = true) }
            }
        }

        if (success) {
            println("\n App installed successfully.\n")
        } else {
            println(" App install failed. Output:")
            installOutput.forEach{ println("> $it") }
        }

        waitForEnter()
    }

    fun installOnAllDevices() {
        clearConsole()
        println("=== Install on All Connected Devices ===\n")

        val devices = getConnectedDevices()
        if (devices.isEmpty()) {
            println("No connected devices found. Please connect devices first.")
            waitForEnter()
            return
        }

        println("Detected ${devices.size} device(s):")
        devices.forEachIndexed { i, d -> println("  ${i + 1}. $d") }
        println("\nInstalling APK: ${apkFile.absolutePath}")
        println()

        for (device in devices) {
            println("---- Installing on $device ----")
            var installOutput = runCommand(
                listOf(adbFile.absolutePath, "-s", device, "install", "-r", apkFile.absolutePath)
            )

            var success = installOutput.any { it.contains("Success", ignoreCase = true) }
            val updateIncompatible = installOutput.any { it.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") }

            if (!success) {
                if (updateIncompatible) {
                    println("Existing incompatible app detected on $device. Uninstalling...")
                    runCommand(listOf(adbFile.absolutePath, "-s", device, "uninstall", "com.imsproject.watch"))
                    println("Reinstalling on $device...")
                    installOutput = runCommand(
                        listOf(adbFile.absolutePath, "-s", device, "install", apkFile.absolutePath)
                    )
                    success = installOutput.any { it.contains("Success", ignoreCase = true) }
                }
            }

            if (success) {
                println(" Installed successfully on $device\n")
            } else {
                println(" Installation failed on $device. Output:")
                installOutput.forEach { println("  > $it") }
                println()
            }
        }

        println("=== Installation complete for all devices ===\n")
        waitForEnter()
    }


    private fun handleConnecting(): String? {
        val pairedHost = try { handlePairing() } catch (_: PairingFailedException) { return null }
        val hostIp = pairedHost ?: readLineSafe("Device host address")
        val debugPort = readLineSafe("Wireless debugging port")
        val target = "$hostIp:$debugPort"

        println("Connecting to device...")
        val connectOutput = runCommand(listOf(adbFile.absolutePath, "connect", target))
        return if (connectOutput.any { it.contains("connected to") }) {
            println("\n Connected successfully to $target.\n")
            target
        } else {
            println("\n Failed to connect to $target")
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
                println("\n Pairing successful.\n")
                hostIp
            } else {
                println("\n Pairing failed. Check IP, port, and pairing code.\n")
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
}
