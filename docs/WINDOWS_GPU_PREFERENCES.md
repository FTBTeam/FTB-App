# Windows GPU Preferences Feature

## Overview

The FTB App automatically configures Windows Graphics settings to run all bundled Java runtimes (javaw.exe) on the high-performance GPU (dGPU) instead of the integrated GPU (iGPU). This prevents common performance issues and crashes that occur when Minecraft runs on an iGPU on systems with both iGPU and dGPU (especially common on laptops).

## Problem Statement

On Windows systems with both integrated and dedicated GPUs, Windows doesn't always recognize Java-based applications like Minecraft as requiring high performance. This causes several issues:

1. **Performance degradation**: Minecraft runs significantly slower on iGPU
2. **Crashes**: Some iGPUs can't handle Minecraft's requirements, causing immediate crashes
3. **User confusion**: Different Java runtimes (for different Minecraft versions) each need to be configured individually
4. **Inconsistent manual configuration**: Users face different experiences with NVIDIA Control Panel, AMD settings, and Windows Graphics settings

## Solution

The FTB App now automatically:

1. Discovers all `javaw.exe` executables in the runtime folder (`%localappdata%\.ftba\bin\runtime`)
2. Adds Windows registry entries to configure GPU preferences
3. Runs on first launch via migration system
4. Configures new Java installations automatically

## Technical Implementation

### Registry Configuration

The feature modifies the Windows registry at:
```
HKEY_CURRENT_USER\SOFTWARE\Microsoft\DirectX\UserGpuPreferences
```

For each javaw.exe, it creates a String Value (REG_SZ) with:
- **Name**: Full path to javaw.exe (e.g., `C:\Users\Username\AppData\Local\.ftba\bin\runtime\OpenJDK21U-jre_x64_windows_hotspot_21.0.4_7\bin\javaw.exe`)
- **Value**: `GpuPreference=2;`

Where GpuPreference values are:
- `0` = Let Windows decide (default)
- `1` = Power saving (iGPU)
- `2` = High performance (dGPU)

### Components

1. **WindowsGpuPreferences.java** - Utility class for managing GPU preferences
2. **MigrateSetupWindowsGpuPreferences.java** - Migration that runs once on existing installations
3. **Settings Integration** - New setting: `workaround.autoConfigureWindowsGpu` (boolean, default: true)
4. **Instance Launch Integration** - Automatically configures GPU preference after Java runtime validation
5. **WebSocket Handler** - Allows frontend to trigger manual configuration

## Usage

### Automatic Configuration

The feature runs automatically:
1. On first launch after update (via migration)
2. When launching an instance with embedded JRE
3. No user action required

### Disabling

Users can disable this feature in Settings by setting `workaround.autoConfigureWindowsGpu` to `false`.

## Testing on Windows

1. Check registry before: `reg query "HKEY_CURRENT_USER\SOFTWARE\Microsoft\DirectX\UserGpuPreferences"`
2. Run the FTB App
3. Check registry after: `reg query "HKEY_CURRENT_USER\SOFTWARE\Microsoft\DirectX\UserGpuPreferences" | findstr javaw.exe`
4. Verify entries exist for all javaw.exe in `%localappdata%\.ftba\bin\runtime`

## Limitations

- **Windows Only**: This feature only works on Windows (10/11)
- **No Wildcards**: Each javaw.exe must be configured individually
- **User Permissions**: Requires standard user permissions (no admin needed)
