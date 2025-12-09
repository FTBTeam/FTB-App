const production = import.meta.env.PROD;
const platform = import.meta.env.MODE === 'overwolf' ? 'overwolf' : 'electron';

export const constants = {
  modpacksApi: "https://api.feed-the-beast.com/v1/modpacks",
  metaApi: "https://api.feed-the-beast.com/v1/meta",
  ftbDomain: "https://www.feed-the-beast.com",
  mcHeadApi: "https://api.feed-the-beast.com/v1/lookup/profile/{uuid}/head",
  isProduction: production,
  isDevelopment: !production,
  platform: platform,
}

export const defaultInstanceCategory = "02c2cc8e-0b8d-45d0-ac1d-7e32b1c21cb0";