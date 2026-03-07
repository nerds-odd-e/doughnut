import {
  copyFileSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  rmdir,
  writeFileSync,
} from 'node:fs'
import { execSync } from 'node:child_process'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
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

const CLI_BUNDLE_PATH = 'cli/dist/doughnut-cli.bundle.mjs'

function bundleAndCopyCliToBuildOutput(
  repoRoot: string,
  env?: NodeJS.ProcessEnv
) {
  execSync('pnpm cli:bundle', { cwd: repoRoot, stdio: 'inherit', env })
  const src = join(repoRoot, CLI_BUNDLE_PATH)
  const destDir = join(
    repoRoot,
    'backend/build/resources/main/static/doughnut-cli-latest'
  )
  mkdirSync(destDir, { recursive: true })
  copyFileSync(src, join(destDir, 'doughnut'))
}

function ensureCliBundleExists(repoRoot: string): string {
  const bundlePath = join(repoRoot, CLI_BUNDLE_PATH)
  if (!existsSync(bundlePath)) {
    execSync('pnpm cli:bundle', { cwd: repoRoot, stdio: 'inherit' })
  }
  return bundlePath
}

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

      const projectRoot = path.dirname(config.configFile ?? process.cwd())
      const generatedBackendPath = path.join(
        projectRoot,
        'packages',
        'generated-backend'
      )
      on(
        'file:preprocessor',
        createBundler({
          plugins: [createEsbuildPlugin(config)],
          alias: {
            '@generated/backend': generatedBackendPath,
          },
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
          accessToken,
        }: {
          baseUrl: string
          accessToken: string
        }) {
          const apiBaseUrl =
            baseUrl && baseUrl !== 'undefined'
              ? baseUrl
              : commonConfig.backendBaseUrl
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
        async bundleMcpServer() {
          const repoRoot = path.resolve(__dirname, '..', '..')
          const mcpServerDir = path.join(repoRoot, 'mcp-server')
          try {
            execSync('pnpm bundle', {
              cwd: mcpServerDir,
              stdio: 'inherit',
            })
            return true
          } catch (error) {
            console.error('Failed to bundle MCP server:', error)
            throw error
          }
        },
        createCliConfigDir() {
          return mkdtempSync(join(tmpdir(), 'cypress-cli-config-'))
        },
        async bundleAndCopyCli() {
          const repoRoot = path.resolve(__dirname, '..', '..')
          try {
            bundleAndCopyCliToBuildOutput(repoRoot)
            return true
          } catch (error) {
            console.error('Failed to bundle and copy CLI:', error)
            throw error
          }
        },
        async bundleAndCopyCliWithVersion(version: string) {
          const repoRoot = path.resolve(__dirname, '..', '..')
          try {
            bundleAndCopyCliToBuildOutput(repoRoot, {
              ...process.env,
              CLI_VERSION: version,
            })
            return true
          } catch (error) {
            console.error('Failed to bundle and copy CLI:', error)
            throw error
          }
        },
        async installCli(baseUrl: string) {
          const installDir = mkdtempSync(
            join(tmpdir(), 'cypress-doughnut-cli-')
          )
          const installScriptPath = join(installDir, 'install.sh')
          const response = await fetch(`${baseUrl}/install`)
          if (!response.ok) {
            throw new Error(
              `Failed to fetch install script: ${response.status}`
            )
          }
          const script = await response.text()
          writeFileSync(installScriptPath, script, { mode: 0o755 })
          execSync(`bash ${installScriptPath}`, {
            env: {
              ...process.env,
              INSTALL_PREFIX: installDir,
              BASE_URL: baseUrl,
            },
          })
          return join(installDir, 'bin', 'doughnut')
        },
        async runCliDirectWithInput({
          input,
          env,
        }: {
          input: string
          env?: NodeJS.ProcessEnv
        }) {
          const { spawn } = await import('node:child_process')
          const repoRoot = path.resolve(__dirname, '..', '..')
          const bundlePath = ensureCliBundleExists(repoRoot)
          return new Promise<string>((resolve, reject) => {
            const proc = spawn(process.execPath, [bundlePath], {
              cwd: repoRoot,
              env: { ...process.env, ...env },
              stdio: ['pipe', 'pipe', 'pipe'],
            })
            let stdout = ''
            proc.stdout?.on('data', (chunk: Buffer) => {
              stdout += chunk.toString()
            })
            proc.stdin?.write(input)
            proc.stdin?.end()
            proc.on('close', (code) => {
              if (code === 0) resolve(stdout)
              else reject(new Error(`CLI exited with code ${code}`))
            })
            proc.on('error', reject)
          })
        },
        async runCliDirectWithArgs({
          args,
          env,
        }: {
          args: string[]
          env?: NodeJS.ProcessEnv
        }) {
          const { spawn } = await import('node:child_process')
          const repoRoot = path.resolve(__dirname, '..', '..')
          const bundlePath = ensureCliBundleExists(repoRoot)
          return new Promise<string>((resolve, reject) => {
            const proc = spawn(process.execPath, [bundlePath, ...args], {
              cwd: repoRoot,
              env: { ...process.env, ...env },
              stdio: ['pipe', 'pipe', 'pipe'],
            })
            let stdout = ''
            proc.stdout?.on('data', (chunk: Buffer) => {
              stdout += chunk.toString()
            })
            proc.stdin?.end()
            proc.on('close', (code) => {
              if (code === 0) resolve(stdout)
              else reject(new Error(`CLI exited with code ${code}`))
            })
            proc.on('error', reject)
          })
        },
        async runCliDirectWithGmailAdd({
          googleBaseUrl,
        }: {
          googleBaseUrl: string
        }) {
          const { spawn } = await import('node:child_process')
          const repoRoot = path.resolve(__dirname, '..', '..')
          const configDir = mkdtempSync(join(tmpdir(), 'cypress-gmail-add-'))
          const configPath = join(configDir, 'gmail.json')
          const config = {
            clientId: 'e2e-test-client',
            clientSecret: 'e2e-test-secret',
            accounts: [],
          }
          mkdirSync(configDir, { recursive: true })
          writeFileSync(configPath, JSON.stringify(config, null, 2))

          const bundlePath = ensureCliBundleExists(repoRoot)
          return new Promise<{ stdout: string; exitCode: number }>(
            (resolve, reject) => {
              const proc = spawn(process.execPath, [bundlePath], {
                cwd: repoRoot,
                env: {
                  ...process.env,
                  DOUGHNUT_CONFIG_DIR: configDir,
                  DOUGHNUT_NO_BROWSER: '1',
                  GOOGLE_BASE_URL: googleBaseUrl,
                },
                stdio: ['pipe', 'pipe', 'pipe'],
              })

              let stdout = ''
              const append = (chunk: string) => {
                stdout += chunk
                return stdout
              }
              proc.stdout?.on('data', (chunk: Buffer) => {
                const out = append(chunk.toString())
                const authMatch = out.match(
                  /https:\/\/accounts\.google\.com\/[^\s]+/
                )
                if (authMatch) {
                  const redirectUri = new URL(authMatch[0]).searchParams.get(
                    'redirect_uri'
                  )
                  if (redirectUri) {
                    fetch(`${redirectUri}?code=e2e_mock_auth_code`).catch(
                      () => {
                        /* ignore callback errors */
                      }
                    )
                  }
                }
              })
              proc.stderr?.on('data', (chunk: Buffer) =>
                append(chunk.toString())
              )

              proc.stdin?.write('/add gmail\nexit\n')
              proc.stdin?.end()

              proc.on('close', (code) => {
                resolve({ stdout, exitCode: code ?? -1 })
              })
              proc.on('error', reject)
            }
          )
        },
        async runCliDirectWithLastEmail({
          googleBaseUrl,
        }: {
          googleBaseUrl: string
        }) {
          const { spawn } = await import('node:child_process')
          const repoRoot = path.resolve(__dirname, '..', '..')
          const configDir = mkdtempSync(join(tmpdir(), 'cypress-gmail-last-'))
          const configPath = join(configDir, 'gmail.json')
          mkdirSync(configDir, { recursive: true })
          const config = {
            accounts: [
              {
                email: 'e2e@gmail.com',
                accessToken: 'mock_access_token',
                refreshToken: 'mock_refresh_token',
                expiresAt: Date.now() + 3600_000,
              },
            ],
          }
          writeFileSync(configPath, JSON.stringify(config, null, 2))
          const bundlePath = ensureCliBundleExists(repoRoot)
          return new Promise<string>((resolve, reject) => {
            const proc = spawn(process.execPath, [bundlePath], {
              cwd: repoRoot,
              env: {
                ...process.env,
                DOUGHNUT_CONFIG_DIR: configDir,
                GOOGLE_BASE_URL: googleBaseUrl,
              },
              stdio: ['pipe', 'pipe', 'pipe'],
            })
            let stdout = ''
            proc.stdout?.on('data', (chunk: Buffer) => {
              stdout += chunk.toString()
            })
            proc.stdin?.write('/last email\nexit\n')
            proc.stdin?.end()
            proc.on('close', (code) => {
              if (code === 0) resolve(stdout)
              else reject(new Error(`CLI exited with code ${code}`))
            })
            proc.on('error', reject)
          })
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
