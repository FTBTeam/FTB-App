const {execSync} = require("child_process");
const requiredEnv = [
  "AZURE_KEY_VAULT_URL",
  "AZURE_KEY_VAULT_CLIENT_ID",
  "AZURE_KEY_VAULT_CLIENT_SECRET",
  "AZURE_KEY_VAULT_CERTIFICATE_NAME",
  "AZURE_KEY_VAULT_TENANT_ID"
]

/**
 * Signs any given file using the `AzureSignTool` https://github.com/vcsjones/AzureSignTool/
 * Currently using version `5.0.0`
 */
exports.default = async (config) => {
  // Check for required environment variables
  for (const env of requiredEnv) {
    if (!process.env[env]) {
      throw new Error(`Missing required environment variable: ${env}`)
    }
  }

  const args = [
    'azuresigntool', 'sign',
    '-kvu', process.env.AZURE_KEY_VAULT_URL,
    '-kvi', process.env.AZURE_KEY_VAULT_CLIENT_ID,
    '-kvs', process.env.AZURE_KEY_VAULT_CLIENT_SECRET,
    '-kvc', process.env.AZURE_KEY_VAULT_CERTIFICATE_NAME,
    '-kvt', process.env.AZURE_KEY_VAULT_TENANT_ID,
    '-tr', 'http://timestamp.digicert.com',
    '-v',
    `"${config.path}"`
  ]

  const output = execSync(args.join(' ')).toString().trim();
  if (!output.includes("Failed operations: 0")) {
    throw new Error(`Failed to sign ${config.path}`);
  }
}