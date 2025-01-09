import { existsSync, rmdir } from 'node:fs'
import { addCucumberPreprocessorPlugin } from '@badeball/cypress-cucumber-preprocessor'
import { createEsbuildPlugin } from '@badeball/cypress-cucumber-preprocessor/esbuild'
import createBundler from '@bahmutov/cypress-esbuild-preprocessor'
import fs from 'fs'
import path from 'path'
import AdmZip from 'adm-zip'

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
        checkDownloadedZipContent(expectedFiles) {
          const downloadsFolder = config.downloadsFolder || 'cypress/downloads'
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
          }))

          const matchesExpected = expectedFilesArray.every((expected) =>
            actualFiles.some(
              (actual) =>
                actual.Filename === expected.Filename &&
                actual.Format === expected.Format &&
                (expected.Content ? actual.Content === expected.Content : true)
            )
          )

          if (!matchesExpected) {
            throw new Error(
              `File content mismatch. Expected: ${JSON.stringify(expectedFilesArray)}, Got: ${JSON.stringify(actualFiles)}`
            )
          }

          return true
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
