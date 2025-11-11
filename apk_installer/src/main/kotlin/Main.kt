private val installer = ApkInstaller()

fun main() {
    while (true) {
        try {
            verifyAPK()
            installer.clearConsole()
            println("=== APK Installer ===")
            println()
            println("Loading...")
            val connectedDevices = installer.getConnectedDevices()
            installer.clearConsole()
            println("=== APK Installer ===")
            println()
            println("Connected Devices:")
            if(connectedDevices.isEmpty()) {
                println("No devices connected.")
            } else {
                println(connectedDevices.joinToString("\n"))
            }
            println()
            println("1. Connect a new device")
            println("2. Install APK on all connected devices")
            println("3. One-time APK install")
            println("4. Refresh")
            println("5. Exit")
            println()
            print("Select option: ")
            when (readlnOrNull()?.trim()) {
                "1" -> {
                    installer.clearConsole()
                    installer.connectDevice()
                }
                "2" -> {
                    verifyAPK()
                    installer.clearConsole()
                    installer.installOnAllDevices()
                }
                "3" -> {
                    verifyAPK()
                    installer.clearConsole()
                    installer.oneTimeInstall()
                }
                "4" -> {
                    // Just refreshes the menu
                }
                "5" -> {
                    println("\nExiting...")
                    break
                }
                else -> {
                    println("\nInvalid option. Please try again.")
                    installer.waitForEnter()
                    installer.clearConsole()
                }
            }
        } catch (e: Exception) {
            println("An error occurred: ${e.message}")
            println()
            installer.waitForEnter()
        }
    }
}

private fun verifyAPK() {
    installer.clearConsole()
    if (!installer.verifyApkExists()) {
        println("=== APK Installer ===")
        println()
        println("APK file not found. Please place the APK in the 'apk' directory")
        println("in the path: ${installer.getApkPath()}")
    }
    while (!installer.verifyApkExists()) {
        Thread.sleep(1000)
    }
}
