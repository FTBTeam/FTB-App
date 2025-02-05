const fs = require("fs");
const assert = require("assert");

const ref = process.env.GITHUB_REF;
assert(ref, "GITHUB_REF is required");

const packageJson = JSON.parse(fs.readFileSync("package.json", "utf8"));
assert(packageJson?.version, "package.json version is required");

const version = packageJson.version;

// // Are we a tag? If yes, are we a `v` tag
if (ref.startsWith("refs/tags/")) {
  const tagName = ref.replace("refs/tags/", "");
  assert(tagName === `v${version}`, `Tag name ${tagName} does not match package.json version ${version}`);
  console.log(`Tag name ${tagName} matches package.json version ${version}`);
  process.exit(0);
}

assert(process.env.GITHUB_TOKEN, "GITHUB_TOKEN is required");

// If we're not a tag, have we correctly updated the package.json version?
// This is for new versions, if we've released `1.24.0` and we try and push a 
// `1.24.0-beta.1` tag, we should fail as this is an old version.

assert(process.env.BUILD_NUMBER, "BUILD_NUMBER is required");
const buildNumber = parseInt(process.env.BUILD_NUMBER, 10);

// Find all the tags so we can see if we're trying to push an old version
(async () => {
  const tags = [];
  let page = 1;
  while (true) {
    const repoTagsReq = await fetch("https://api.github.com/repos/ftbteam/ftb-app/tags?per-page=100&page=" + page, {
      headers: {
        Accept: "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        Authorization: `Bearer ${process.env.GITHUB_TOKEN}`,
      },
    });

    const repoTags = await repoTagsReq.json();
    if (repoTags.length === 0) {
      break
    }
    
    tags.push(...repoTags.map(e => e.name.replace("v", "")))
    page++;
  }
  
  assert(!tags.includes(version), `Version ${version} has already been released`);
  
  // We now know that the base version is safe, we can attach a beta / alpha tag to the version
  // What branch are we on?
  let newVersion = version;
  const branch = process.env.GITHUB_REF.replace("refs/heads/", "");
  if (branch === "main") {
    newVersion = version + "-beta." + buildNumber;
  } else if (branch === "dev") {
    newVersion = version + "-alpha." + buildNumber;
  } else {
    throw new Error(`Unknown branch ${branch}`);
  }
  
  // Update the package.json version
  packageJson.version = newVersion;
  fs.writeFileSync("package.json", JSON.stringify(packageJson, null, 2));
  
  console.log("✅ Updated package.json version to", newVersion);
})().catch((e) => console.error(e));