const production = process.env.NODE_ENV === "production";
const platform = process.env.VUE_APP_PLATFORM
  
const useStaging = false;

export const constants = {
  modpacksApi: useStaging ? "https://staging.api.modpacks.ch" : "https://api.modpacks.ch",
  metaApi: "https://meta.feed-the-beast.com",
  ftbDomain: "https://www.feed-the-beast.com",
  mcHeadApi: "https://api.feed-the-beast.com/v1/lookup/profile/{uuid}/head",
  isProduction: production,
  isDevelopment: !production,
  platform: platform,
}