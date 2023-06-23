# FTB-App

You can find the java backend side of the FTB launcher [here](https://github.com/CreeperHost/modpacklauncher)

## Running MacOS / Linux

**The backend**

Start the backend, it's Java, use Intellij, it's just simpler.

**The frontend**

```bash
yarn install
yarn serve
```

## Running Windows

### The Overwolf step

- Enable developer tools in Overwolf. See here https://overwolf.github.io/docs/topics/enable-dev-tools#command-line
- Go to Settings > About > Developer options (at the bottom)
- Click `Load unpacked extensions`
- Navigate to the `overwolf` folder of this repo and press open
- Run the [commands below](#the-app)
- Then press the launch button next to the app titled `FTB App Preview`

### The app

**The backend**

Start the backend, it's Java, use Intellij, it's just simpler.

**The frontend**

**For developer mode**

```bash
yarn vue:serve:overwolf
```

**To build**

```bash
yarn vue:build:overwolf
```