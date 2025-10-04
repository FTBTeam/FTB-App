# Windows GPU Preferences - Verification Guide

This guide helps verify that the Windows GPU preferences feature is working correctly.

## Prerequisites

- Windows 10 or Windows 11
- FTB App installed with the GPU preferences feature
- At least one Java runtime installed in `%localappdata%\.ftba\bin\runtime`

## Verification Steps

### 1. Check Registry Entries

Open Command Prompt or PowerShell and run:

```cmd
reg query "HKEY_CURRENT_USER\SOFTWARE\Microsoft\DirectX\UserGpuPreferences"
```

You should see entries like:
```
HKEY_CURRENT_USER\SOFTWARE\Microsoft\DirectX\UserGpuPreferences
    C:\Users\YourUsername\AppData\Local\.ftba\bin\runtime\OpenJDK21U-jre_x64_windows_hotspot_21.0.4_7\bin\javaw.exe    REG_SZ    GpuPreference=2;
    C:\Users\YourUsername\AppData\Local\.ftba\bin\runtime\OpenJDK17U-jre_x64_windows_hotspot_17.0.10_7\bin\javaw.exe    REG_SZ    GpuPreference=2;
```

### 2. Check Windows Graphics Settings

1. Open Windows Settings (Win + I)
2. Go to **System** > **Display** > **Graphics**
3. Scroll through the list of applications
4. Look for entries that contain "javaw.exe"
5. Each should show "High performance" preference

### 3. Verify Java Installations

List all Java installations in runtime folder:

```cmd
dir /s /b "%localappdata%\.ftba\bin\runtime\javaw.exe"
```

Each of these paths should have a corresponding registry entry.

### 4. Test Manual Configuration

You can manually trigger GPU configuration via the FTB App's developer tools (if available) or by calling the WebSocket API:

```javascript
// Example WebSocket message (adjust for actual implementation)
{
  "type": "configureWindowsGpu",
  "requestId": "test-123",
  "secret": "your-secret-here"
}
```

### 5. Verify Setting

Check that the setting is enabled:

1. Open FTB App settings
2. Look for `autoConfigureWindowsGpu` under workarounds
3. Should be `true` by default

Or check the settings.json file directly:

```cmd
type "%localappdata%\.ftba\storage\settings.json" | findstr autoConfigureWindowsGpu
```

Should show:
```json
"autoConfigureWindowsGpu": true
```

## Testing Scenarios

### Scenario 1: Fresh Installation

1. Install FTB App with this feature
2. Launch the app
3. Migration should run automatically
4. Verify registry entries exist for all Java runtimes

### Scenario 2: Existing Installation

1. Update FTB App to version with this feature
2. Launch the app
3. Migration should configure existing Java runtimes
4. Verify registry entries exist

### Scenario 3: New Java Installation

1. Install/update a modpack that requires a new Java version
2. App downloads new Java runtime
3. When launching the instance, GPU preference should be configured
4. Verify new registry entry exists

### Scenario 4: Disabled Feature

1. Disable the feature in settings: `"autoConfigureWindowsGpu": false`
2. Install a new Java runtime
3. No new registry entries should be created
4. Existing entries remain unchanged

## PowerShell Verification Script

Save as `verify-gpu-preferences.ps1`:

```powershell
# Verify Windows GPU Preferences for FTB App

$runtimePath = "$env:LOCALAPPDATA\.ftba\bin\runtime"
$registryKey = "HKCU:\SOFTWARE\Microsoft\DirectX\UserGpuPreferences"

Write-Host "Checking Java installations..." -ForegroundColor Cyan
$javawFiles = Get-ChildItem -Path $runtimePath -Recurse -Filter "javaw.exe" -ErrorAction SilentlyContinue

if ($javawFiles.Count -eq 0) {
    Write-Host "No Java installations found in $runtimePath" -ForegroundColor Yellow
    exit
}

Write-Host "Found $($javawFiles.Count) Java installation(s)" -ForegroundColor Green
Write-Host ""

Write-Host "Checking registry entries..." -ForegroundColor Cyan
$registryValues = Get-ItemProperty -Path $registryKey -ErrorAction SilentlyContinue

$configured = 0
$missing = 0

foreach ($javaw in $javawFiles) {
    $fullPath = $javaw.FullName
    $propName = $fullPath
    
    if ($registryValues.PSObject.Properties.Name -contains $propName) {
        $value = $registryValues.$propName
        if ($value -eq "GpuPreference=2;") {
            Write-Host "[OK] $fullPath" -ForegroundColor Green
            $configured++
        } else {
            Write-Host "[WARN] $fullPath - Unexpected value: $value" -ForegroundColor Yellow
        }
    } else {
        Write-Host "[MISSING] $fullPath" -ForegroundColor Red
        $missing++
    }
}

Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  Configured: $configured" -ForegroundColor Green
Write-Host "  Missing:    $missing" -ForegroundColor $(if ($missing -gt 0) { "Red" } else { "Green" })

if ($missing -eq 0) {
    Write-Host "`nAll Java installations are configured correctly!" -ForegroundColor Green
} else {
    Write-Host "`nSome Java installations are not configured. Try running the FTB App to configure them." -ForegroundColor Yellow
}
```

Run with:
```powershell
powershell -ExecutionPolicy Bypass -File verify-gpu-preferences.ps1
```

## Troubleshooting

### No Registry Entries

If no entries exist:
1. Check if the feature is enabled in settings
2. Launch the FTB App to trigger migration
3. Launch an instance with embedded JRE
4. Check app logs for errors

### Partial Configuration

If some Java installations are not configured:
1. Manually trigger configuration via WebSocket API
2. Re-launch the FTB App
3. Check app logs for specific errors

### GPU Still Not Used

If Minecraft still runs on iGPU after configuration:
1. Restart the system (registry changes may require restart)
2. Check NVIDIA/AMD control panel settings
3. Ensure laptop is plugged in (some systems force iGPU on battery)
4. Verify Windows Graphics settings show "High performance"
5. Check if other GPU management software is overriding settings

## Expected Behavior

✅ **Correct Behavior:**
- All javaw.exe files in runtime folder have registry entries
- All entries have `GpuPreference=2;`
- Windows Graphics settings show "High performance" for all Java executables
- Minecraft launches using dedicated GPU
- No errors in FTB App logs

❌ **Incorrect Behavior:**
- Missing registry entries for some Java installations
- Wrong GpuPreference value (0 or 1)
- Minecraft still runs on integrated GPU
- Errors in app logs related to GPU configuration
