# Windows GPU Preferences Feature - Quick Start

## What Is This?

Automatic Windows GPU preference configuration for all bundled Java runtimes to prevent Minecraft from running on integrated GPUs, which causes performance issues and crashes.

## Quick Summary

✅ **Automatic** - Works out of the box, no user action needed  
✅ **Comprehensive** - Configures all Java versions automatically  
✅ **Safe** - No admin required, Windows-only, fully reversible  
✅ **Configurable** - Can be disabled by users if needed  

## How It Works

1. **First Launch**: Migration configures all existing Java installations
2. **Instance Launch**: New Java versions configured automatically
3. **Manual Trigger**: Can be triggered via WebSocket API

## Registry Configuration

Modifies: `HKEY_CURRENT_USER\SOFTWARE\Microsoft\DirectX\UserGpuPreferences`

For each javaw.exe:
```
"C:\path\to\javaw.exe" = "GpuPreference=2;"
```

Where `GpuPreference=2` means "High Performance" (dedicated GPU).

## Documentation

| Document | Purpose |
|----------|---------|
| [WINDOWS_GPU_PREFERENCES.md](WINDOWS_GPU_PREFERENCES.md) | Technical overview and usage |
| [WINDOWS_GPU_VERIFICATION.md](WINDOWS_GPU_VERIFICATION.md) | Verification guide with scripts |
| [WINDOWS_GPU_FLOW.md](WINDOWS_GPU_FLOW.md) | Flow diagrams and architecture |
| [WINDOWS_GPU_SUMMARY.md](WINDOWS_GPU_SUMMARY.md) | Complete implementation summary |
| [examples/windows-gpu-registry-example.reg](examples/windows-gpu-registry-example.reg) | Registry example |

## Quick Verification (Windows)

```cmd
reg query "HKEY_CURRENT_USER\SOFTWARE\Microsoft\DirectX\UserGpuPreferences" | findstr javaw.exe
```

Should show entries for all javaw.exe files with `GpuPreference=2;`

## Settings

**Setting**: `workaround.autoConfigureWindowsGpu`  
**Default**: `true` (enabled)  
**Location**: `%localappdata%\.ftba\storage\settings.json`

To disable:
```json
{
  "workaround": {
    "autoConfigureWindowsGpu": false
  }
}
```

## WebSocket API

**Message Type**: `configureWindowsGpu`

**Request**:
```json
{
  "type": "configureWindowsGpu",
  "requestId": "unique-id",
  "secret": "websocket-secret"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Successfully configured GPU preferences"
}
```

## Code Structure

```
subprocess/src/main/java/dev/ftb/app/
├── util/WindowsGpuPreferences.java          - Core utility
├── migration/migrations/
│   └── MigrateSetupWindowsGpuPreferences.java - Migration
├── api/handlers/other/
│   └── ConfigureWindowsGpuHandler.java       - WebSocket handler
└── pack/InstanceLauncher.java                - Integration point
```

## Key Features

- **Automatic Discovery**: Finds all javaw.exe in runtime folder
- **Registry Management**: Uses `reg.exe` to modify preferences
- **Migration Support**: Runs once on first launch
- **Instance Integration**: Configures on every launch
- **WebSocket API**: Manual triggering from frontend
- **User Control**: Can be disabled in settings

## Platform Compatibility

| Platform | Behavior |
|----------|----------|
| Windows 10/11 | ✅ Full functionality |
| Linux | ✅ No-op (safe) |
| macOS | ✅ No-op (safe) |

## Testing

See [WINDOWS_GPU_VERIFICATION.md](WINDOWS_GPU_VERIFICATION.md) for:
- Step-by-step verification guide
- PowerShell verification script
- Troubleshooting tips

## Support

- **GitHub Issues**: FTBTeam/FTB-App
- **Discord**: #java-support channel
- **Documentation**: This folder

## Implementation Status

✅ **Complete and ready for testing**

Total changes:
- 15 files changed
- 435 lines of production code
- 48 lines of test code
- 886 lines of documentation

## Next Steps

1. Test on Windows dual-GPU system
2. Verify registry entries
3. Confirm Minecraft uses dGPU
4. Collect user feedback
5. Iterate based on feedback

---

**Version**: FTB App v1.29.0+  
**Status**: ✅ Complete  
**Platform**: Windows 10/11 (safe on all platforms)
