export function getAppHome(os: string, homeDir: string, joiner: (...paths: string[]) => string) {
  if (os === "darwin") {
    return joiner(homeDir, 'Library', 'Application Support', '.ftba');
  } else if (os === "win32") {
    return joiner(homeDir, 'AppData', 'Local', '.ftba');
  } else {
    return joiner(homeDir, '.ftba');
  }
}