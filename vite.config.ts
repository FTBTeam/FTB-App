import {defineConfig} from 'vite'
import path from 'node:path'
import electron from 'vite-plugin-electron/simple'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const isOverwolf = mode === 'overwolf'
  
  console.log(`Building for ${isOverwolf ? 'Overwolf' : 'Electron'} platform`)
  
  return {
    base: isOverwolf ? '/dist/desktop' : undefined,
    // Define @ alias for src/ directory
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
        "@platform": path.resolve(__dirname, `src/utils/interface/impl/${isOverwolf ? 'overwolf' : 'electron'}.ts`),
      },
    },
    plugins: [
      vue(),
      tailwindcss(),
      !isOverwolf && electron({
        main: {
          // Shortcut of `build.lib.entry`.
          entry: 'electron/main.ts',

          onstart: async (args) => {
            const runArgs = ['.', '--no-sandbox']
            if (process.env.SHOW_ADS !== 'true') {
              runArgs.push('--test-ad')
            }

            await args.startup(runArgs, {}, '@overwolf/ow-electron')
          }
        },
        preload: {
          // Shortcut of `build.rollupOptions.input`.
          // Preload scripts may contain Web assets, so use the `build.rollupOptions.input` instead `build.lib.entry`.
          entry: path.join(__dirname, 'electron/preload.ts') ,
        } as any, // Hack
        // Ployfill the Electron and Node.js API for Renderer process.
        // If you want use Node.js in Renderer process, the `nodeIntegration` needs to be enabled in the Main process.
        // See 👉 https://github.com/electron-vite/vite-plugin-electron-renderer
        renderer: process.env.NODE_ENV === 'test'
          // https://github.com/electron-vite/vite-plugin-electron-renderer/issues/78#issuecomment-2053600808
          ? undefined
          : {},
      }),
    ],
    build: {
      // Modify the output directory
      outDir: isOverwolf ? './overwolf/dist/desktop' : './dist',
      rollupOptions: {
        input: {
          index: path.join(__dirname, isOverwolf ? 'index-overwolf.html' : 'index.html'),
          ...(!isOverwolf ? {
            prelaunch: path.join(__dirname, 'prelaunch.html')
          } : {}),
        },
      },
    },
    server: {
      watch: {
        ignored: [
          "**/node_modules/**",
          "**/subprocess/**",
        ]
      }
    },
  }
})
