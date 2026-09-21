import { existsSync, rm } from 'node:fs'
import { join, resolve } from 'node:path'
import { guardCypressNodeSetup } from '../../scripts/isolated-cypress.mjs'
import {
  applyMcpIsolationCypressExpose,
  mcpIsolationCypressTasks,
} from '../../scripts/mcp-isolation-proof.mjs'
import { runSutHealthcheck } from '../../scripts/sut-healthcheck.mjs'
import {
  appendCucumberExposeTag,
  isolationBarrierAt,
  WORKTREE_RESET_ISOLATION_BARRIER_AT,
  WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK,
  worktreeResetIsolationCypressTasks,
} from '../../scripts/worktree-reset-isolation-barrier.mjs'
const {
  addCucumberPreprocessorPlugin,
} = require('@badeball/cypress-cucumber-preprocessor')
import { createEsbuildPlugin } from '@badeball/cypress-cucumber-preprocessor/esbuild'
import createBundler from '@bahmutov/cypress-esbuild-preprocessor'
import { composeCypressPluginEvents } from './composeCypressPluginEvents.mjs'
import { attachCypressSpecScreenshotSink } from './cypressSpecScreenshotSink'
import { createCliE2ePluginTasks } from './cliE2ePluginTasks'
import { E2E_APP_BASE_URL } from './constants'
import { notebookPublicationProfileTasks } from './notebookPublicationProfile'
import { mcpClientTasks } from './mcpClientTasks'

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
      on = composeCypressPluginEvents(on) as Cypress.PluginEvents
      // Cypress 10+ changes process.cwd() to the config file's directory when using --config-file,
      // so resolve from __dirname to get the repo root regardless of cwd.
      const repoRoot = resolve(__dirname, '..', '..')
      await guardCypressNodeSetup(repoRoot, config, {
        on,
        healthcheckFn: async (checkoutRoot) => {
          const lines: string[] = []
          const result = await runSutHealthcheck({
            checkoutRoot,
            log: (line: string) => lines.push(line),
          })
          if (!result.ok) {
            console.error(lines.join('\n'))
          }
          return result
        },
      })
      if (!config.expose || typeof config.expose !== 'object') {
        config.expose = {}
      }
      const barrierAt = isolationBarrierAt()
      config.expose[WORKTREE_RESET_ISOLATION_BARRIER_AT] = barrierAt
      applyMcpIsolationCypressExpose(config)
      if (barrierAt === WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK) {
        appendCucumberExposeTag(config, 'not @openaiUnavailableWhenPaired')
      }
      await addCucumberPreprocessorPlugin(on, config)
      const generatedBackendPath = join(
        repoRoot,
        'packages',
        'generated',
        'donut-backend-api'
      )
      const frontendRoutesPath = join(repoRoot, 'frontend', 'src', 'routes')
      const frontendUtilsPath = join(repoRoot, 'frontend', 'src', 'utils')
      on(
        'file:preprocessor',
        createBundler({
          plugins: [createEsbuildPlugin(config)],
          alias: {
            '@generated/donut-backend-api': generatedBackendPath,
            '@/routes': frontendRoutesPath,
            '@/utils': frontendUtilsPath,
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
        ...worktreeResetIsolationCypressTasks(),
        ...notebookPublicationProfileTasks(repoRoot, on),
        ...mcpIsolationCypressTasks(),
        ...mcpClientTasks(repoRoot),
        ...createCliE2ePluginTasks(repoRoot, {
          saveBufferToCurrentSpecFolder:
            specScreenshotSink.saveBufferToCurrentSpecFolder.bind(
              specScreenshotSink
            ),
          appBaseUrl: config.baseUrl,
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
