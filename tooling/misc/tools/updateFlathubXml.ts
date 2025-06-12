import fs from 'node:fs';
import path from 'node:path';
import dayjs from "dayjs";

// Pull the working dir from the args
const workingDir = process.argv[2];

if (!fs.existsSync(workingDir)) {
  console.error("Working directory does not exist:", workingDir);
  process.exit(1);
}

const packageJsonPath = path.join(workingDir, 'package.json');
if (!fs.existsSync(packageJsonPath)) {
  console.error("package.json file does not exist in the working directory:", packageJsonPath);
  process.exit(1);
}

const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
if (!packageJson.version) {
  console.error("package.json does not contain a version field:", packageJsonPath);
  process.exit(1);
}

const flathubXmlPath = path.join(workingDir, "flathub", 'dev.ftb.ftb-app.metainfo.xml');
if (!fs.existsSync(flathubXmlPath)) {
  console.error("Flathub XML file does not exist:", flathubXmlPath);
  process.exit(1);
}

const flathubXml = fs.readFileSync(flathubXmlPath, 'utf-8');

const lines = flathubXml.split('\n');

const outputLines: string[] = [];
for (const line of lines) {
  if (!line.trim().startsWith("<releases>")) {
    outputLines.push(line);
    continue;
  }

  outputLines.push(line)
  
  const release = `    <release version="${packageJson.version}" date="${dayjs().format("YYYY-MM-DD")}" />`
  
  outputLines.push(release);
}

const outputXml = outputLines.join('\n');

await Bun.write(flathubXmlPath, outputXml);

console.log("Flathub XML file updated successfully with version:", packageJson.version);