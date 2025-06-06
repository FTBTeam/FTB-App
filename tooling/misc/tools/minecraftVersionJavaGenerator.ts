// Run with Bun
import dayjs from 'dayjs';
// Add week support
import weekOfYear from 'dayjs/plugin/weekOfYear';
import * as path from 'node:path';
dayjs.extend(weekOfYear);

const minecraftVersionsEndpoint = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

export interface Version {
  id: string
  type: string
  url: string
  time: string
  releaseTime: string
  sha1: string
  complianceLevel: number
}

type JavaJump = {
  version: number
  nextJump: string
}

const javaVersionJumps: JavaJump[] = [
  {
    version: 8,
    nextJump: "21w19a"
  },
  {
    version: 16,
    nextJump: "1.18-pre1"
  },
  {
    version: 17,
    nextJump: "24w14potato"
  },
  {
    version: 21,
    nextJump: "999999" // We don't have anything after java 21
  }
]

console.log("Fetching Minecraft versions...");
const versReq = await fetch(minecraftVersionsEndpoint);
const { versions }: {versions: Version[]} = await versReq.json();

const reversedVersions = versions.reverse();

// Remove anything before 1.0
const firstIndex = reversedVersions.findIndex(version => version.id === "1.0");
reversedVersions.splice(0, firstIndex);

// Enum signature
// MC1_17_1(MAJOR, MINOR, PATCH, STRING_FULL, YEAR, WEEK, RELEASE, JAVA_VERSION)

let currentJavaVersion = javaVersionJumps[0];
let javaClass = "";
for (const version of reversedVersions) {
  if (version.id === "1.RV-Pre1") {
    continue;
  }

  if (version.id.includes("w") || version.id.includes("-pre") || version.id.includes(" Pre-") || version.id.includes("-rc")) {
    // We're in a snapshot, we don't record these but we do need to watch for the next jump.
    currentJavaVersion = jumpJavaVersion(version.id, currentJavaVersion);    
    continue;
  }

  let [major, minor, patch] = version.id.split(".");
  if (!patch) {
    minor = minor.split("-")[0];
    patch = "0";
  }

  patch = patch.split("-")[0];

  const date = dayjs(version.releaseTime);
  const isFirst = javaClass === "";
  javaClass += `${!isFirst ? '\n' : ''}    MC${major}_${minor}_${patch}(make(${major}, ${minor}, ${patch}, "${version.id}", ${currentJavaVersion.version}, ${date.format("YY")}, ${date.week()}, ${date.unix()}L)),`;

  currentJavaVersion = jumpJavaVersion(version.id, currentJavaVersion);
}

// Remove the trailing comma
javaClass = javaClass.slice(0, -1);

const targetClass = path.join(__dirname, "..", "..", "..", "subprocess", "src", "main", "java", "dev", "ftb", "app", "util", "mc", "MinecraftVersionPresets.java");
const file = Bun.file(targetClass);
if (!file.exists()) {
  throw new Error(`File ${targetClass} does not exist`);
}

const classData = await file.text();
const replaceRegex = /MinecraftVersionPresets {\n([^;]+);/g;

// Replace the matched group with the new data
const newClassData = classData.replace(replaceRegex, `MinecraftVersionPresets {\n${javaClass};`);

await file.write(newClassData);

function jumpJavaVersion(versionId: string, currentJump: JavaJump): JavaJump {
  if (currentJump.nextJump === "999999") {
    return currentJump;
  }

  if (versionId === currentJump.nextJump) {
    const currentIndex = javaVersionJumps.findIndex(jump => jump.version === currentJump.version);
    return javaVersionJumps[currentIndex + 1];
  }

  return currentJump;
}