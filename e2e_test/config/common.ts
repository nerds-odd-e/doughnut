import { existsSync, rmdir } from 'node:fs'
import mcpClient from '../support/mcp_client'
const {
  addCucumberPreprocessorPlugin,
} = require('@badeball/cypress-cucumber-preprocessor')
import { createEsbuildPlugin } from '@badeball/cypress-cucumber-preprocessor/esbuild'
import createBundler from '@bahmutov/cypress-esbuild-preprocessor'
import fs from 'fs'
import path from 'path'
import AdmZip from 'adm-zip'
import type { ExpectedFile } from '../start/downloadChecker'

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
  backendBaseUrl: 'http://localhost:9081',

  e2e: {
    async setupNodeEvents(
      on: Cypress.PluginEvents,
      config: Cypress.PluginConfigOptions
    ): Promise<Cypress.PluginConfigOptions> {
      await addCucumberPreprocessorPlugin(on, config)

      on(
        'file:preprocessor',
        createBundler({
          plugins: [createEsbuildPlugin(config)],
        })
      )

      on('task', {
        deleteFolder(folderName) {
          console.log('deleting folder %s', folderName)

          return new Promise((resolve, reject) => {
            if (!existsSync(folderName)) {
              resolve(null)
              return
            }
            rmdir(folderName, { maxRetries: 10, recursive: true }, (err) => {
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
        checkDownloadedZipContent(expectedFiles: ExpectedFile[]) {
          const downloadsFolder = config.downloadsFolder
          const files = fs.readdirSync(downloadsFolder)
          const zipFile = files.find((file) => file.endsWith('.zip'))

          if (!zipFile) {
            throw new Error('No zip file found in downloads folder')
          }

          const zip = new AdmZip(path.join(downloadsFolder, zipFile))
          const zipEntries = zip.getEntries()

          const actualFiles = zipEntries.map((entry) => ({
            Filename: entry.entryName,
            Format: path.extname(entry.entryName).slice(1),
            Content: entry.getData().toString('utf8'),
          }))

          const expectedFilesArray = expectedFiles.map((file) => ({
            Filename: file.Filename,
            Format: file.Format,
            Content: file.Content,
            validateMetadata: file.validateMetadata,
          }))

          const mismatches: string[] = []

          // Check for missing files
          expectedFilesArray.forEach((expected) => {
            const actual = actualFiles.find(
              (file) => file.Filename === expected.Filename
            )
            if (!actual) {
              mismatches.push(`Missing file: ${expected.Filename}`)
              return
            }

            if (actual.Format !== expected.Format) {
              mismatches.push(
                `Format mismatch in ${expected.Filename}:\n` +
                  `  Expected: ${expected.Format}\n` +
                  `  Actual  : ${actual.Format}`
              )
            }

            const contentMatch = expected.Content
              ? actual.Content.includes(expected.Content)
              : true

            const hasFrontmatter = expected.validateMetadata
              ? actual.Content.match(
                  /^---\n(?:note_id: \d+\ncreated_at: .+\nupdated_at: .+\n)---\n/
                )
              : true

            if (!(contentMatch && hasFrontmatter)) {
              mismatches.push(
                `Content mismatch in ${expected.Filename}:\n` +
                  `  Expected: ${expected.Content}\n` +
                  `  Actual  : ${actual.Content}`
              )
            }
          })

          // Check for unexpected files
          actualFiles.forEach((actual) => {
            const isExpected = expectedFilesArray.some(
              (expected) => expected.Filename === actual.Filename
            )
            if (!isExpected) {
              mismatches.push(`Unexpected file found: ${actual.Filename}`)
            }
          })

          if (mismatches.length > 0) {
            const errorMessage = [
              'ZIP Archive Content Validation Failed:',
              '----------------------------------------',
              ...mismatches,
              '----------------------------------------',
              'Summary:',
              `Expected files: ${expectedFilesArray.map((f) => f.Filename).join(', ')}`,
              `Actual files  : ${actualFiles.map((f) => f.Filename).join(', ')}`,
            ].join('\n')

            throw new Error(errorMessage)
          }

          return true
        },
        async spawnAndConnectMcpServer({
          baseUrl,
          mcpToken,
        }: {
          baseUrl: string
          mcpToken: string
        }) {
          return await mcpClient.spawnAndConnectMcpServer({ baseUrl, mcpToken })
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
