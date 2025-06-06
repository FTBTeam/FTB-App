import fs from 'fs';
import path from 'path';
import zip from 'adm-zip';

const javaApi = `https://api.adoptopenjdk.net/v3/assets/version`

export async function installJre(javaVersion, javaPath) {
  const url = javaApi + `/${javaVersion}?architecture=x64&image_type=jre&jvm_impl=hotspot&os=windows&page=0&page_size=10&project=jdk&release_type=ga&semver=true&sort_method=DEFAULT&sort_order=DESC&vendor=eclipse`;
  console.log("Downloading Java from " + url)
  const response = await fetch(url)
  const data = await response.json()

  const downloadUrl = data[0]?.binaries.find(e => e?.package?.link?.endsWith("zip"))?.package?.link
  if (!downloadUrl) {
    console.error("Failed to find download URL")
    process.exit(1)
  }

  const downloadResponse = await fetch(downloadUrl)
  const downloadBuffer = await downloadResponse.arrayBuffer();

  if (!fs.existsSync(javaPath)) {
    fs.mkdirSync(javaPath, {recursive: true})
  }

  const filePath = path.join(javaPath, "jre.zip")
  fs.writeFileSync(filePath, Buffer.from(downloadBuffer))

  if (!fs.existsSync(filePath)) {
    console.error("Failed to download Java")
    process.exit(1)
  }

  // Extract the zip
  const zipFile = new zip(filePath)
  zipFile.extractAllTo(javaPath, true)

  // Move the contents of the inner folder to the root
  const innerFolder = fs.readdirSync(javaPath).find(e => e.startsWith("jdk-"));
  if (!innerFolder) {
    console.error("Failed to find inner folder")
    process.exit(1)
  }

  const innerPath = path.join(javaPath, innerFolder)
  const files = fs.readdirSync(innerPath)
  for (const file of files) {
    fs.renameSync(path.join(innerPath, file), path.join(javaPath, file))
  }

  fs.rmSync(innerPath, {recursive: true})
  fs.rmSync(filePath)
}

export const javaVersion = "21.0.3+9-jre"