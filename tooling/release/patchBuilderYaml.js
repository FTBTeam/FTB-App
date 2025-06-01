import fs from 'fs'
import locateSubprocess from './locateSubprocess.cjs'
import {parse, stringify} from 'yaml'
import path from 'path'

const builderPath = path.join(process.cwd(), 'electron-builder.yml')
const yamlData = fs.readFileSync(builderPath, 'utf8')

const binaryPath = locateSubprocess();
const yamlParsed = parse(yamlData)

yamlParsed.mac.binaries = [
  binaryPath
]

// Write the updated YAML back to the file
const yamlString = stringify(yamlParsed)
fs.writeFileSync(builderPath, yamlString, 'utf8')