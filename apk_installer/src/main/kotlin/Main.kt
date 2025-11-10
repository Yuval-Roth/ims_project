fun main() {
    val installer = ApkInstaller()

    while (true) {
        installer.clearConsole()
        println("=== APK Installer ===")
        println()
        println("Loading...")
        val connectedDevices = installer.getConnectedDevices()
        installer.clearConsole()
        println("=== APK Installer ===")
        println()
        println("Connected Devices:")
        println(connectedDevices.joinToString("\n"))
        println()
        println("1. Connect a new device")
        println("2. Install APK on all connected devices")
        println("3. One-time APK install")
        println("4. Exit")
        println()
        print("Select option: ")
        when (readlnOrNull()?.trim()) {
            "1" -> {
                installer.clearConsole()
                installer.connectDevice()
            }
            "2" -> {
                installer.clearConsole()
                installer.installOnAllDevices()
            }
            "3" -> {
                installer.clearConsole()
                installer.oneTimeInstall()
            }
            "4" -> {
                println("\nExiting...")
                break
            }
            else -> {
                println("\nInvalid option. Please try again.")
                installer.waitForEnter()
                installer.clearConsole()
            }
        }
    }
}
