# Windows GPU Preferences - Flow Diagram

This document illustrates how the Windows GPU preferences feature works throughout the FTB App lifecycle.

## Overview Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                       FTB App Launch                            │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              Check: Is this Windows?                            │
│         (OS.CURRENT == OS.WIN)                                  │
└─────────────────────┬───────────────────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼ Yes                   ▼ No
┌─────────────────────┐    ┌─────────────────────┐
│ Continue to         │    │ Skip GPU config     │
│ Migration System    │    │ (No-op)             │
└─────────┬───────────┘    └─────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│         Migrations Manager (First Launch Only)                  │
│  - Run: MigrateSetupWindowsGpuPreferences                       │
│  - Check: autoConfigureWindowsGpu setting enabled?              │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│         WindowsGpuPreferences.configureAllJavaRuntimes()        │
│  1. Find runtime directory: %localappdata%\.ftba\bin\runtime    │
│  2. Walk directory tree recursively                             │
│  3. Find all javaw.exe files                                    │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│         For each javaw.exe found:                               │
│  - Get absolute path                                            │
│  - Call: setGpuPreference(path)                                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│         setGpuPreference(executablePath)                        │
│  - Execute: reg add "HKCU\...\UserGpuPreferences"               │
│             /v "C:\path\to\javaw.exe"                           │
│             /t REG_SZ                                           │
│             /d "GpuPreference=2;"                               │
│             /f                                                  │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│         Windows Registry Updated                                │
│  HKEY_CURRENT_USER\SOFTWARE\Microsoft\DirectX\                  │
│    UserGpuPreferences\                                          │
│      "C:\...\javaw.exe" = "GpuPreference=2;"                    │
└─────────────────────────────────────────────────────────────────┘
```

## Instance Launch Flow

```
┌─────────────────────────────────────────────────────────────────┐
│               User Launches Modpack Instance                    │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│         InstanceLauncher.prepareProcess()                       │
│  - Validate Java Runtime                                        │
│  - Provision JDK if needed (download new Java version)          │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│         Check Conditions:                                       │
│  1. Using embedded JRE? (instance.props.embeddedJre)            │
│  2. Windows? (OS.CURRENT == OS.WIN)                             │
│  3. Setting enabled? (autoConfigureWindowsGpu)                  │
└─────────────────────┬───────────────────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼ All Yes               ▼ Any No
┌─────────────────────┐    ┌─────────────────────┐
│ Configure GPU       │    │ Skip GPU config     │
│ for this javaw.exe  │    │                     │
└─────────┬───────────┘    └─────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│  WindowsGpuPreferences.configureJavaRuntime(javaExecutable)     │
│  - Add/update registry entry for this specific javaw.exe        │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│         Continue Instance Launch                                │
│  - Minecraft now configured to use dGPU                         │
└─────────────────────────────────────────────────────────────────┘
```

## Manual Configuration Flow (WebSocket API)

```
┌─────────────────────────────────────────────────────────────────┐
│         Frontend sends "configureWindowsGpu" message            │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│         ConfigureWindowsGpuHandler.handle()                     │
│  - Check: Is Windows?                                           │
└─────────────────────┬───────────────────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼ Yes                   ▼ No
┌─────────────────────┐    ┌─────────────────────┐
│ Call:               │    │ Return error:       │
│ configureAllJava    │    │ "Not on Windows"    │
│ Runtimes()          │    └─────────────────────┘
└─────────┬───────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│  Configure all Java installations                               │
│  (Same as migration flow)                                       │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│         Return success/failure to Frontend                      │
│  ConfigureWindowsGpuData.Reply                                  │
│  - success: true/false                                          │
│  - message: "Successfully configured..." or error               │
└─────────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
%localappdata%\.ftba\
├── bin\
│   └── runtime\
│       ├── OpenJDK8U-jre_x64_windows_hotspot_8u312b07\
│       │   └── bin\
│       │       └── javaw.exe  ← Configured
│       │
│       ├── OpenJDK17U-jre_x64_windows_hotspot_17.0.10_7\
│       │   └── bin\
│       │       └── javaw.exe  ← Configured
│       │
│       └── OpenJDK21U-jre_x64_windows_hotspot_21.0.4_7\
│           └── bin\
│               └── javaw.exe  ← Configured
│
└── storage\
    └── settings.json
        └── workaround.autoConfigureWindowsGpu: true
```

## Registry Structure

```
HKEY_CURRENT_USER\
└── SOFTWARE\
    └── Microsoft\
        └── DirectX\
            └── UserGpuPreferences\
                ├── "C:\...\OpenJDK8U-...\bin\javaw.exe" = "GpuPreference=2;"
                ├── "C:\...\OpenJDK17U-...\bin\javaw.exe" = "GpuPreference=2;"
                └── "C:\...\OpenJDK21U-...\bin\javaw.exe" = "GpuPreference=2;"
```

## Error Handling

```
┌─────────────────────────────────────────────────────────────────┐
│         Error Scenarios                                         │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ├─► Runtime directory doesn't exist
                      │   └─► Log debug message, return empty list
                      │
                      ├─► javaw.exe doesn't exist
                      │   └─► Log warning, return false
                      │
                      ├─► reg.exe command fails
                      │   └─► Log error with output, return false
                      │
                      ├─► IOException during directory walk
                      │   └─► Log error, return empty list
                      │
                      └─► InterruptedException
                          └─► Log error, restore interrupt flag, return false
```

## Platform Safety

```
┌─────────────────────────────────────────────────────────────────┐
│         All Entry Points Check:                                 │
│  if (OS.CURRENT != OS.WIN) {                                    │
│      return true; // No-op on non-Windows                       │
│  }                                                              │
└─────────────────────────────────────────────────────────────────┘

Ensures:
✅ No errors on Linux
✅ No errors on macOS
✅ No unnecessary processing
✅ Safe to call from any platform
```

## State Transitions

```
User State Flow:

[Fresh Install]
    ↓
[First Launch] → Migration runs → All Java configured → [Ready]
    ↓
[Launch Instance] → New Java downloaded → GPU configured → [Instance Running]
    ↓
[Setting Disabled] → No new configurations → Existing entries kept → [Manual Control]
    ↓
[Manual Trigger] → Re-configure all → All entries updated → [Ready]
```

## Integration Points

1. **Migration System**: Automatically configures on first launch
2. **Instance Launcher**: Configures new Java installations on demand
3. **Settings System**: Respects user preferences
4. **WebSocket API**: Allows manual triggering from frontend
5. **Windows Registry**: Direct integration with Windows GPU preferences

## Performance Characteristics

- **Migration**: Runs once, O(n) where n = number of Java installations
- **Instance Launch**: O(1) - single registry entry per launch
- **Manual Trigger**: O(n) - reconfigures all Java installations
- **Registry Access**: Fast, uses native Windows reg.exe command
- **Directory Walking**: Recursive, but limited to runtime folder only
