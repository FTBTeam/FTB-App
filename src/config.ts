import fs from 'fs';
let appVersion: string = 'Missing Version File';
let webVersion: string = 'Missing Version File';
let dateCompiled: string = 'Missing Version File';
let javaLicenses: object = {};

interface Config {
    apiURL: string;
    appVersion: string;
    webVersion: string;
    dateCompiled: string;
    javaLicenses: object;
}

if (fs.existsSync('./version.json')) {
    const contents = fs.readFileSync('./version.json', 'utf-8');
    const jsonContent = JSON.parse(contents);
    appVersion = jsonContent.jarVersion;
    webVersion = jsonContent.webVersion;
    dateCompiled = jsonContent.timestampBuilt;
    javaLicenses = jsonContent.javaLicense;
}

const config: Config = {
    apiURL: `https://api.modpacks.ch`,
    appVersion,
    webVersion,
    dateCompiled,
    javaLicenses,
};
export default config;
