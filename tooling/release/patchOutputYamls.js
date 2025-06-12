import fs from 'fs'
import {parse, stringify} from 'yaml'
import path from 'path'
import crypto from 'crypto'

const filesInReleaseDir = fs.readdirSync(path.join(process.cwd(), 'release'))
const yamlFiles = filesInReleaseDir
  .filter(file => file.includes('linux'))
  .filter(file => file.endsWith('.yaml') || file.endsWith('.yml'))

for (const file of yamlFiles) {
  const filePath = path.join(process.cwd(), 'release', file)
  const yamlData = fs.readFileSync(filePath, 'utf8')
  const yamlParsed = parse(yamlData)
  
  if (!yamlParsed.files) {
    throw new Error(`No files section found in ${filePath}`);
  }
  
  const arch = yamlParsed.path.includes('arm64') ? 'arm64' : 'x64';

  // tar.gz
  const locatedTarGz = filesInReleaseDir.find(file => 
    file.endsWith(`${arch}.tar.gz`)
  )
  
  if (!locatedTarGz) {
    throw new Error("No tar.gz file found in release directory");
  }

  const tarGzPath = path.join(process.cwd(), 'release', locatedTarGz)
  if (!fs.existsSync(tarGzPath)) {
    throw new Error(`Tar.gz file not found at ${tarGzPath}`);
  }

  const tarGzContents = fs.readFileSync(tarGzPath, 'utf8')
  const tazGzSize = fs.statSync(tarGzPath).size;
  const tazGzSha512 = crypto.createHash('sha512').update(tarGzContents).digest('base64');
  
  yamlParsed.files.push({
    url: locatedTarGz,
    sha512: tazGzSha512,
    size: tazGzSize,
  })
  
  // Update the yaml file with the new files section
  const yamlString = stringify(yamlParsed);
  fs.writeFileSync(filePath, yamlString, 'utf8');
  
  console.log(`Updated ${filePath} with tar.gz file ${locatedTarGz}`);
}