import { existsSync, rm } from 'node:fs'
import { join, resolve } from 'node:path'
import mcpClient from '../support/mcp_client'
const {
  addCucumberPreprocessorPlugin,
} = require('@badeball/cypress-cucumber-preprocessor')
import { createEsbuildPlugin } from '@badeball/cypress-cucumber-preprocessor/esbuild'
import createBundler from '@bahmutov/cypress-esbuild-preprocessor'
import { attachCypressSpecScreenshotSink } from './cypressSpecScreenshotSink'
import { createCliE2ePluginTasks } from './cliE2ePluginTasks'
import { CLI_E2E_PNPM_SPAWN_ENV, runShellCommandSync } from './cliE2eRepo'
import { E2E_APP_BASE_URL } from './constants'

const commonConfig = {
  chromeWebSecurity: false,
  screenshotOnRunFailure: true,
  fixturesFolder: 'e2e_test/fixtures',
  screenshotsFolder: 'e2e_test/screenshots',
  downloadsFolder: 'e2e_test/downloads',
  pageLoadTimeout: 100000,
  defaultCommandTimeout: 6000,
  trashAssetsBeforeRuns: true,
  environment: 'ci',
  viewportWidth: 1200,
  viewportHeight: 800,

  e2e: {
    baseUrl: E2E_APP_BASE_URL,
    async setupNodeEvents(
      on: Cypress.PluginEvents,
      config: Cypress.PluginConfigOptions
    ): Promise<Cypress.PluginConfigOptions> {
      await addCucumberPreprocessorPlugin(on, config)

      // Cypress 10+ changes process.cwd() to the config file's directory when using --config-file,
      // so resolve from __dirname to get the repo root regardless of cwd.
      const repoRoot = resolve(__dirname, '..', '..')
      const generatedBackendPath = join(
        repoRoot,
        'packages',
        'generated',
        'doughnut-backend-api'
      )
      on(
        'file:preprocessor',
        createBundler({
          plugins: [createEsbuildPlugin(config)],
          alias: {
            '@generated/doughnut-backend-api': generatedBackendPath,
          },
        })
      )

      const testState: Record<string, unknown> = {}

      // Cypress stores screenshots under screenshotsFolder (relative to projectRoot).
      const projectRootForScreenshots =
        typeof config.projectRoot === 'string' ? config.projectRoot : repoRoot
      const screenshotsFolder =
        typeof config.screenshotsFolder === 'string'
          ? config.screenshotsFolder
          : 'e2e_test/screenshots'
      const screenshotsFolderAbsolute = resolve(
        projectRootForScreenshots,
        screenshotsFolder
      )

      const specScreenshotSink = attachCypressSpecScreenshotSink(
        on,
        screenshotsFolderAbsolute
      )

      on('task', {
        ...createCliE2ePluginTasks(repoRoot, {
          saveBufferToCurrentSpecFolder:
            specScreenshotSink.saveBufferToCurrentSpecFolder.bind(
              specScreenshotSink
            ),
        }),
        setTestState({ key, value }: { key: string; value: unknown }) {
          testState[key] = value
          return null
        },
        getTestState(key: string) {
          return testState[key] ?? null
        },
        clearTestState() {
          Object.keys(testState).forEach((k) => delete testState[k])
          return null
        },
        deleteFolder(folderName) {
          console.log('deleting folder %s', folderName)

          return new Promise((resolve, reject) => {
            if (!existsSync(folderName)) {
              resolve(null)
              return
            }
            rm(folderName, { maxRetries: 10, recursive: true }, (err) => {
              if (err) {
                console.error(err)
                return reject(err)
              }
              resolve(null)
            })
          })
        },

        fileShouldExistSoon(filePath, retryCount = 50): Promise<boolean> {
          const checker = (count: number): Promise<boolean> => {
            return new Promise((resolve) => {
              if (existsSync(filePath)) {
                resolve(true)
                return
              }
              if (count === 0) {
                resolve(false)
                return
              }
              setTimeout(() => {
                checker(count - 1).then((result) => resolve(result))
              }, 100)
            })
          }
          return checker(retryCount)
        },
        async spawnAndConnectMcpServer({
          baseUrl,
          accessToken,
        }: {
          baseUrl: string
          accessToken: string
        }) {
          const apiBaseUrl =
            baseUrl && baseUrl !== 'undefined' ? baseUrl : E2E_APP_BASE_URL
          return await mcpClient.spawnAndConnectMcpServer({
            baseUrl: apiBaseUrl,
            accessToken,
          })
        },
        async callMcpToolWithParams({
          apiName,
          params,
        }: {
          apiName: string
          params: Record<string, any>
        }) {
          return await mcpClient.callMcpToolWithParams({ apiName, params })
        },
        async disconnectMcpServer() {
          return await mcpClient.disconnectMcpServer()
        },
        async ocrCanvasImage(base64Png: string) {
          const { createWorker } = await import('tesseract.js')
          const tessDir = join(repoRoot, 'e2e_test', 'tesseract')
          const worker = await createWorker('eng', 1, {
            langPath: tessDir,
            cachePath: tessDir,
          })
          const {
            data: { text },
          } = await worker.recognize(Buffer.from(base64Png, 'base64'))
          await worker.terminate()
          return text
        },
        async bundleMcpServer() {
          const mcpServerDir = join(repoRoot, 'mcp-server')
          try {
            runShellCommandSync('pnpm bundle', {
              cwd: mcpServerDir,
              env: {
                ...process.env,
                ...CLI_E2E_PNPM_SPAWN_ENV,
              },
            })
            return true
          } catch (error) {
            console.error('Failed to bundle MCP server:', error)
            throw error
          }
        },
      })

      return config
    },
    supportFile: 'e2e_test/support/e2e.ts',
    specPattern: 'e2e_test/features/**/*.feature',
    excludeSpecPattern: [
      '**/*.{js,ts}',
      '**/__snapshots__/*',
      '**/__image_snapshots__/*',
    ],
  },
}

export default commonConfig
