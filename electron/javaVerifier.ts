import { computeArch, computeOs, getAppHome, jreHome, loadApplicationMetaData } from '../src/utils/nuturalHelpers.ts';
import os from 'os';
import path from 'path';
import fs from 'fs';
import { execSync } from 'child_process';
import { MetaData } from '@platform';
import { retrying } from '../src/utils/helpers/asyncHelpers.ts';
import { prelaunchWindow } from './main.ts';
import AdmZip from 'adm-zip';

export class JavaVerifier {
  private readonly metaData: MetaData;
    
  constructor() {
    this.metaData = loadApplicationMetaData(process.resourcesPath, path.join, (path) => fs.readFileSync(path, "utf-8"))
  }

  async verifyJava() {
    this.emitUpdate("Checking for Java installation...");
    console.log("Checking for Java installation...");
    const installed = await this.isInstalled();
    if (!installed) {
      await this.installJava();
      
      this.emitUpdate("Testing Java installation...");
      const validationResult = await this.testJavaInstallation();
      if (!validationResult) {
        console.error("Java installation failed");
        this.emitUpdate("Java installation failed");
        return false;
      }
      
      console.log("Java installation succeeded");
      this.emitUpdate("Java installation succeeded");
      this.writeJavaVersion(this.metaData.runtime.version)
      return true;
    }
    
    this.emitUpdate("Java is installed and up to date");
    return true;
  }
  
  async installJava() {
    const binaryLocation = await this.downloadJava();
    if (!binaryLocation) {
      console.error("Failed to download Java");
      return;
    }

    this.emitUpdate("Extracting Java...");

    // Extract the binary
    const result = this.extractFile(binaryLocation, this.runtimeHome());
    if (!result) {
      console.error("Failed to extract Java binary");
      return;
    }

    // Move the jdk-numbers to the current directory
    const files = fs.readdirSync(this.runtimeHome())
      .filter(file => file.startsWith("jdk-") && fs.statSync(path.join(this.runtimeHome(), file)).isDirectory());

    if (files.length === 0) {
      console.error("Failed to find jdk directory");
      return;
    }

    const jdkDir = path.join(this.runtimeHome(), files[0]);
    const jdkFiles = fs.readdirSync(jdkDir);
    for (const file of jdkFiles) {
      const oldPath = path.join(jdkDir, file);
      const newPath = path.join(this.runtimeHome(), file);
      fs.renameSync(oldPath, newPath);
    }
    
    fs.rmSync(binaryLocation);
    fs.rmSync(path.join(this.runtimeHome(), files[0]), { recursive: true});
  }
  
  private async downloadJava() {
    const adoptiumBinary = await this.getJavaFromApi();

    const runtimeHome = this.runtimeHome();
    if (fs.existsSync(runtimeHome)) {
      try {
        fs.rmSync(runtimeHome, { recursive: true, force: true });
      } catch (e) {
        // We can ask the user to remove it if we fail
        // TODO: Ask the user to remove it
        console.warn("Failed to remove existing java installation", e);
      }
    }
    
    this.emitUpdate("Downloading Java...");
    let binary: Buffer;
    try {
      binary = await retrying(async () => {
        const response = await fetch(adoptiumBinary);
        if (!response.ok) {
          throw new Error(`Failed to fetch from Adoptium API: ${response.statusText}`);
        }

        return Buffer.from(await response.arrayBuffer());
      }, 10, 3000); // Try 10 times, 3 seconds apart
    } catch (e) {
      console.error("Failed to fetch from Adoptium API", e);
      return;
    }

    // Write the binary to a file
    const binaryPath = path.join(runtimeHome, "java." + ( os.platform() === "win32" ? "zip" : "tar.gz"));
    try {
      if (!fs.existsSync(runtimeHome)) {
        fs.mkdirSync(runtimeHome, { recursive: true });
      }

      fs.writeFileSync(binaryPath, binary);
    } catch (e) {
      console.error("Failed to write java binary", e);
      return;
    }
    
    return binaryPath;
  } 
  
  private async testJavaInstallation() {
    const javaPath = this.jreHome();
    if (!fs.existsSync(javaPath)) {
      console.error("Java is not installed");
      return false;
    }
    
    try {
      execSync(`"${javaPath}" --version`, { stdio: 'ignore' });
      return true;
    } catch (e) {
      console.error("Failed to execute java version command", e);
    }
    
    return false;
  }
  
  private async isInstalled(): Promise<boolean> {
    const javaPath = this.jreHome();
    if (!fs.existsSync(javaPath)) {
      console.log(`${javaPath} does not exist`);
      return false
    }
    
    const javaVersion = await this.tryLoadJavaVersion();
    if (!javaVersion) {
      console.log("Unable to load java version");
      return false
    }
    
    // Check if the java version is actually correct for the installed version of the app
    const runtimeVersion = this.metaData.runtime.version;
    console.log(`Comparing java version ${javaVersion} with runtime version ${runtimeVersion}`);
    
    if (javaVersion !== runtimeVersion) {
      console.log(`Java version mismatch: ${javaVersion} != ${runtimeVersion}`);
      this.emitUpdate(`Java update required: ${javaVersion} -> ${runtimeVersion}`);
      return false;
    }
    
    console.log("Java is installed and up to date");
    return true;
  }
  
  private emitUpdate(message: string) {
    prelaunchWindow?.webContents.send("java/message", message)
  }
  
  private async getJavaFromApi() {
    const platformData = {
      arch: os.arch(),
      platform: os.platform(),
    };

    const ourOs = computeOs(platformData.platform);
    const ourArch = computeArch(platformData.arch)

    const javaVersion = this.metaData.runtime.version;

    // Attempt to get the java
    const url = `https://api.adoptium.net/v3/assets/latest/${javaVersion}/hotspot?architecture=${ourArch}&image_type=jre&os=${ourOs}`;

    let adopiumRes;
    try {
      adopiumRes = await retrying(async () => {
        const adopiumRequest = await fetch(url);
        if (!adopiumRequest.ok) {
          // Hacks the retrying method to keep trying
          throw new Error(`Failed to fetch from Adoptium API: ${adopiumRequest.statusText}`);
        }
        
        return await adopiumRequest.json();
      }, 10, 3000); // Try 10 times, 3 seconds apart
    } catch (e) {
      console.error("Failed to fetch from Adoptium API", e);
    }
    
    if (!adopiumRes) {
      return null;
    }

    const binary = adopiumRes.find((e: any) => e.binary)?.binary;
    if (!binary) {
      console.error("Failed to find binary in Adoptium API response");
      return null;
    }
    
    return binary.package.link;
  }
  
  private async tryLoadJavaVersion(): Promise<string | null> {
    const runtimePath = this.runtimeHome();
    const versionIdentifier = path.join(runtimePath, ".java-version");
    
    if (fs.existsSync(versionIdentifier)) {
      try {
        const data = fs.readFileSync(versionIdentifier, "utf-8");
        return data.trim();
      } catch (e) {
        console.warn("Failed to read java version file", e);
      }
    }
    
    // If the file doesn't exist, we might have java, so let's try it
    try {
      // Run java --version
      const result = execSync(`"${this.jreHome()}" --version`, {
        stdio: 'pipe', // Capture the output
      });

      /**
       * openjdk 23.0.2 2025-01-21
       * OpenJDK Runtime Environment Temurin-23.0.2+7 (build 23.0.2+7)
       * OpenJDK 64-Bit Server VM Temurin-23.0.2+7 (build 23.0.2+7, mixed mode, sharing)
       */

      const versionOutput = result.toString();
      const versionMatch = versionOutput.match(/openjdk\s+(\d+\.\d+\.\d+)/i);
      
      if (versionMatch) {
        const version = versionMatch[1]?.split(".")[0]
        this.writeJavaVersion(version);
        return version;
      }
    } catch (e) {
      console.warn("Failed to execute java version command", e);
    }
    
    // It's not installed
    return null;
  }
  
  private writeJavaVersion(version: string) {
    const runtimePath = this.runtimeHome();
    const versionIdentifier = path.join(runtimePath, ".java-version");
    
    try {
      if (!fs.existsSync(runtimePath)) {
        fs.mkdirSync(runtimePath, { recursive: true });
      }
      
      fs.writeFileSync(versionIdentifier, version, "utf-8");
    } catch (e) {
      console.warn("Failed to write java version file", e);
    }
  }

  extractFile(filePath: string, outputPath: string) {
    if (!fs.existsSync(filePath)) {
      console.error("File does not exist", filePath);
      return false;
    }
    
    if (!fs.existsSync(outputPath)) {
      try {
        fs.mkdirSync(outputPath, { recursive: true });
      } catch (e) {
        console.error("Failed to create output directory", e);
        return false;
      }
    }
    
    if (filePath.endsWith(".zip")) {
      return this.extractZip(filePath, outputPath);
    } else if (filePath.endsWith("tar.gz")) {
      return this.extractTarball(filePath, outputPath);
    }
    
    throw new Error("Unsupported file type");
  }
  
  extractTarball(tarballPath: string, outputPath: string) {
    // It looks like tar is available on all platforms, so we can just use that `tar -xzf`
    // But let's redirect the contents to the output path
    // And capture the output so we can log it
    const command = `tar -xzf "${tarballPath}" -C "${outputPath}"`;
    console.debug("Extracting tarball", command)
    const output = execSync(command).toString();
    console.debug("Tarball extraction output", output)

    return true;
  }

  extractZip(zipPath: string, outputPath: string) {
    const zip = new AdmZip(zipPath);
    zip.extractAllTo(outputPath, true);

    return true;
  }
  
  private jreHome() {
    return jreHome(this.appHome(), path.join, os.platform() === "win32", os.platform() === "darwin");
  }
  
  private runtimeHome() {
    return path.join(this.appHome(), 'runtime');
  }
  
  private appHome() {
    return getAppHome(os.platform(), os.homedir(), path.join);
  }
}