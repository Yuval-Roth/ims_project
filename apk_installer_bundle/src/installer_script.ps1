Clear-Host

Write-Host ""
Write-Host "Instructions:`n"
Get-Content -Path "src\instructions.txt"
Write-Host ""

$paired = $false
$connected = $false

$response = Read-Host "Is the device already paired? (Y/N)"
$response = $response.ToLower()
if ($response -ne 'y') {
    # Pair device
    $host_ip = Read-Host "Device IP address"
    $pairing_port = Read-Host "Wireless pairing port"
    $pairing_code = Read-Host "Pairing code"

    Write-Host "Pairing device..."
    $pairOutput = & .\tools\adb pair "$host_ip`:$pairing_port" $pairing_code 2>&1
    if ($pairOutput -imatch "Successfully paired") {
        Write-Host ""
        Write-Host "Pairing successful."
        Write-Host ""
        $paired = $true
    } else {
        Write-Host ""
        Write-Host "Pairing failed - please check the IP, port, and pairing code."
        Write-Host ""
        Pause
        exit
    }
} else {
    $host_ip = Read-Host "Device host address"
}

# Connect to device
$debug_port = Read-Host "Wireless debugging port"
$s = "$host_ip`:$debug_port"

Write-Host "Connecting to device..."
$connectOutput = & .\tools\adb connect $s 2>&1
if ($connectOutput -imatch "connected to") {
    Write-Host ""
    Write-Host "Connected successfully."
    Write-Host ""
    $connected = $true
} else {
    Write-Host ""
    Write-Host "Failed to connect to the device at $s"
    Write-Host "Please check the IP, port, and whether the device is online and authorized."
    Write-Host ""
    Pause
    exit
}

# Install APK
Write-Host "`Installing the app..."
$installOutput = & .\tools\adb -s $s install -r "apk\ims.apk" 2>&1
$installSucceeded = $false
$updateIncompatible = $false

foreach ($line in $installOutput) {
    if ($line -imatch "Success") {
        $installSucceeded = $true
    }
    if ($line -imatch "INSTALL_FAILED_UPDATE_INCOMPATIBLE") {
        $updateIncompatible = $true
    }
}

if (-not $installSucceeded) {
    if ($updateIncompatible) {
        & .\tools\adb -s $s uninstall com.imsproject.watch | Out-Null
        $reinstallOutput = & .\tools\adb -s $s install "apk\ims.apk" 2>&1

        $installSucceeded = $reinstallOutput -imatch "Success"
        if (-not $installSucceeded) {
            Write-Host "App install failed. Output:"
            $reinstallOutput | ForEach-Object { Write-Host $_ }
            Pause
            exit
        }
    } else {
        Write-Host "App install failed. Output:"
        $installOutput | ForEach-Object { Write-Host $_ }
        Pause
        exit
    }
}

Write-Host "`nApp installed successfully."
Write-Host ""
Pause
