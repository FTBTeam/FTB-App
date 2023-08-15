using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;

namespace OverwolfShim;

public class OverwolfShim : IDisposable {

    private static readonly string    overwolfDir   = new FileInfo(Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location)!).FullName;
    private static readonly string    dotFtbaDir    = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), ".ftba");
    private static readonly string    subprocessPid = Path.Combine(dotFtbaDir, "subprocess.pid");
    private readonly        List<int> openedWindows = new();

    [DllImport("user32.dll", CharSet = CharSet.Auto)]
    private static extern IntPtr SendMessage(IntPtr hWnd, UInt32 Msg, IntPtr wParam, IntPtr lParam);

    [DllImport("shell32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool IsUserAnAdmin();

    private const UInt32 WM_CLOSE = 0x0010;

    private Process javaProcess;

    public OverwolfShim() {
    }

    #region Events

    public event Action<object> onData;

    #endregion Events

    #region Shim

    /// <summary>
    /// Generate a new random GUID
    /// </summary>
    /// <returns>The random GUID</returns>
    public string RandomUUID() => Guid.NewGuid().ToString();

    /// <returns>If the current process is running with admin permissions.</returns>
    public bool IsRunningAsAdministrator() => IsUserAnAdmin();

    #region IO

    /// <summary>
    /// The directory the Overwolf package exists in.
    /// </summary>
    /// <returns>The overwolf dir</returns>
    public string GetOverwolfDir() => overwolfDir;

    /// <summary>
    /// The directory local FTBApp data is stored in. %LOCALAPPDATA%/.ftba
    /// </summary>
    /// <returns>The .ftba folder.</returns>
    public string GetDotFTBADir() => dotFtbaDir;

    /// <summary>
    /// Extract the provided zip into the given destination directory.
    /// </summary>
    /// <param name="archivePath">The zip archive to extract.</param>
    /// <param name="destinationDir">The destination directory.</param>
    /// <param name="callback">Error/success callback.</param>
    public void ExtractZip(string archivePath, string destinationDir, Action<object> callback) {
        try {
            ZipFile.ExtractToDirectory(archivePath, destinationDir);
            callback(new { success = true });
        } catch (Exception ex) {
            callback(new { success = false, message = "Extraction failed: " + ex });
        }
    }

    /// <summary>
    /// The process id the Shim is currently running in.
    /// </summary>
    /// <returns>The PID of the shim. Probably main overwolf process, or sandbox, idk.</returns>
    public int GetOverwolfPID() => Process.GetCurrentProcess().Id;

    #endregion

    #region Java

    /// <summary>
    /// Checks if the Java sub-process is still running.
    /// </summary>
    /// <returns>If the sub-process is running or not.</returns>
    public bool IsJavaRunning() => javaProcess != null && !javaProcess.HasExited;

    /// <summary>
    /// Checks if the Java sub-process is still running in the background.
    /// </summary>
    /// <returns>If another copy of the sub-process is still running.</returns>
    public bool IsJavaStillRunning() {
        try {
            if (!File.Exists(subprocessPid)) return false;
            
            int pid;
            if (!int.TryParse(File.ReadAllText(subprocessPid), out pid)) return false;

            Process process = Process.GetProcessById(pid);
            return process.StartInfo.FileName.Contains("java");
        } catch (Exception) {
            return false;
        }

    }

    /// <summary>
    /// Try to yeet the old sub-process still running in the background.
    /// </summary>
    public void YeetOldJavaProcess() {
        try {
            if (!File.Exists(subprocessPid)) return;
            
            int pid;
            if (!int.TryParse(File.ReadAllText(subprocessPid), out pid)) return;
            
            Process process = Process.GetProcessById(pid);
            process.Kill();
        } catch (Exception) {
        }
    }

    [Obsolete("Provided for binary compatibility until new method is used by Frontend.")]
    public void LaunchJava(string version, bool dev, Action<object> callback) {
        var args = new List<string>();
        args.Add("--pid");
        args.Add(Process.GetCurrentProcess().Id.ToString());
        args.Add("--overwolf");
        if (dev) {
            args.Add("--dev");
        }

        LaunchJava(
            overwolfDir,
            Path.Combine(overwolfDir, @".\jdk-17.0.1+12-minimal\bin\java.exe"),
            new List<string>(),
            $"launcher-{version}-all.jar",
            args,
            callback
        );
    }

    /// <summary>
    /// Start a Java sub-process.
    ///
    /// Only one Java process may be running at a time.
    /// 
    /// </summary>
    /// <param name="workingDir">The working directory of the process. (where . will be)</param>
    /// <param name="javaPath">The absolute path to the Java executable.</param>
    /// <param name="jvmArgs">List of JVM arguments to add.</param>
    /// <param name="jar">The absolute path to the Jar file to execute.</param>
    /// <param name="arguments">List of Program arguments to add.</param>
    /// <param name="callback">Error/success callback for starting the process.</param>
    public void LaunchJava(string workingDir, string javaPath, List<string> jvmArgs, string jar, List<string> arguments, Action<object> callback) {
        if (IsJavaRunning()) {
            callback(new { success = false, message = "Already running." });
            return;
        }

        List<string> args = new();
        args.AddRange(jvmArgs);
        args.Add("-jar");
        args.Add(jar);
        args.AddRange(arguments);

        for (int i = 0; i < args.Count; i++) {
            if (args[i].Contains(" ")) {
                args[i] = "\"" + args[i] + "\"";
            }
        }

        try {
            javaProcess = new Process();
            javaProcess.StartInfo.UseShellExecute = false;
            javaProcess.StartInfo.FileName = javaPath;
            javaProcess.StartInfo.Arguments = string.Join(" ", args);
            // Strip known problem environment variables. If these env vars have unknown or bad arguments, it can cause crashes.
            javaProcess.StartInfo.Environment.Remove("_JAVA_OPTIONS");
            javaProcess.StartInfo.Environment.Remove("JAVA_TOOL_OPTIONS");
            javaProcess.StartInfo.Environment.Remove("JAVA_OPTIONS");
            javaProcess.StartInfo.CreateNoWindow = true;
            javaProcess.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;
            javaProcess.StartInfo.RedirectStandardInput = true;
            javaProcess.StartInfo.RedirectStandardOutput = true;
            javaProcess.StartInfo.RedirectStandardError = true;
            javaProcess.StartInfo.StandardOutputEncoding = Encoding.UTF8;
            javaProcess.StartInfo.WorkingDirectory = workingDir;
            javaProcess.Exited += JavaProcess_Exited;
            javaProcess.ErrorDataReceived += JavaProcess_ErrorDataReceived;
            javaProcess.OutputDataReceived += JavaProcess_OutputDataReceived;
            onData?.Invoke(new { output = $"Attempting to start process: \"{javaPath} {javaProcess.StartInfo.Arguments}\" in \"{workingDir}\"." });
            bool started = javaProcess.Start();
            javaProcess.BeginErrorReadLine();
            javaProcess.BeginOutputReadLine();
            callback(new { success = true, message = "Started Java with pid " + javaProcess.Id, pid = javaProcess.Id });
            WriteJavaPID(javaProcess.Id);
        } catch (Exception ex) {
            javaProcess = null;
            callback(new { successs = false, message = "Failed to start process: " + ex });
        }
    }

    /// <summary>
    /// Force kill the Java Sub-Process if it is running.
    /// </summary>
    public void KillJava() {
        if (IsJavaRunning()) {
            javaProcess.Kill();
        }
    }

    #endregion

    #region Screen

    /// <summary>
    /// Creates a screenshot of the specified on-screen location saving it to
    /// the specified file location.
    /// </summary>
    /// <param name="left">The left window position.</param>
    /// <param name="top">The top window position.</param>
    /// <param name="width">The width.</param>
    /// <param name="height">The height.</param>
    /// <param name="path">The location on disk to write the file.</param>
    /// <param name="callback">Error/success callback.</param>
    public void CaptureScreenshot(int left, int top, int width, int height, string path, Action<object> callback) {
        try {
            File.WriteAllBytes(path, CaptureScreenshotImpl(left, top, width, height));
            callback(new { success = true });
        } catch (Exception ex) {
            callback(new { success = false, message = ex.ToString() });
        }
    }

    /// <summary>
    /// Creates a screenshot of the specified on-screen location.
    /// </summary>
    /// <param name="left">The left window position.</param>
    /// <param name="top">The top window position.</param>
    /// <param name="width">The width.</param>
    /// <param name="height">The height.</param>
    /// <param name="onError">Callback for when an error occurs.</param>
    /// <returns>An array of bytes representing a PNG capture of the specified location. In the event of an error, will return an empty array.</returns>
    public byte[] CaptureScreenshot(int left, int top, int width, int height, Action<object> onError) {
        try {
            return CaptureScreenshotImpl(left, top, width, height);
        } catch (Exception ex) {
            onError(new { message = ex.ToString() });
        }

        return Array.Empty<byte>();
    }

    private static byte[] CaptureScreenshotImpl(int left, int top, int width, int height) {
        var bmp = new Bitmap(width, height, PixelFormat.Format32bppArgb);
        using (Graphics g = Graphics.FromImage(bmp)) {
            g.CopyFromScreen(left, top, 0, 0, new Size(width, height));
        }

        using (MemoryStream s = new MemoryStream()) {
            bmp.Save(s, ImageFormat.Png);
            return s.ToArray();
        }
    }

    #endregion

    public void WindowOpened(int hwnd) {
        openedWindows.Add(hwnd);
    }

    public void WindowClosed(int hwnd) {
        openedWindows.Remove(hwnd);
    }

    #endregion

    private void JavaProcess_OutputDataReceived(object sender, DataReceivedEventArgs e) {
        if (string.IsNullOrEmpty(e.Data)) return;
        onData?.Invoke(new { output = e.Data });
    }

    private void JavaProcess_ErrorDataReceived(object sender, DataReceivedEventArgs e) {
        if (string.IsNullOrEmpty(e.Data)) return;
        onData?.Invoke(new { error = e.Data });
    }

    private void JavaProcess_Exited(object sender, EventArgs e) {
        openedWindows.ForEach(CloseWindow);
        javaProcess = null;
    }

    private void WriteJavaPID(int pid) {
        try {
            File.WriteAllText(subprocessPid, pid.ToString());
        } catch (Exception) {
            // ignored We don't care about this.
        }
    }

    void CloseWindow(int hwnd) {
        SendMessage(new IntPtr(hwnd), WM_CLOSE, IntPtr.Zero, IntPtr.Zero);
    }

    #region IDisposable

    public void Dispose() {
        KillJava();
    }

    #endregion

}