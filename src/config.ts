import fs from 'fs';
import path from 'path';
let appVersion: string = 'Missing Version File';
let webVersion: string = 'Missing Version File';
let dateCompiled: string = 'Missing Version File';
let javaLicenses: object = {};
declare const __static: string;

interface Config {
    apiURL: string;
    appVersion: string;
    webVersion: string;
    dateCompiled: string;
    javaLicenses: object;
}

if (fs.existsSync(path.join(__static, 'version.json'))) {
    const contents = fs.readFileSync(path.join(__static, 'version.json'), 'utf-8');
    const jsonContent = JSON.parse(contents);
    appVersion = jsonContent.jarVersion;
    webVersion = jsonContent.webVersion;
    dateCompiled = jsonContent.timestampBuilt;
    javaLicenses = jsonContent.javaLicense;
}

const config: Config = {
    apiURL: process.env.NODE_ENV === 'production' ? `https://api.modpacks.ch` : `https://api.modpacks.ch`,
    appVersion,
    webVersion,
    dateCompiled,
    javaLicenses,
};
export default config;
