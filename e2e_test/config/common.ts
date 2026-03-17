import {
  appendFileSync,
  copyFileSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  rm,
  writeFileSync,
} from 'node:fs'
import { spawnSync } from 'node:child_process'
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
import { runCliInPty } from './cliPtyRunner'

const CLI_BUNDLE_PATH = 'cli/dist/doughnut-cli.bundle.mjs'
const CLI_E2E_BACKEND_URL = 'http://localhost:9081'

function cliEnv(overrides?: NodeJS.ProcessEnv): NodeJS.ProcessEnv {
  return {
    DOUGHNUT_API_BASE_URL: CLI_E2E_BACKEND_URL,
    ...overrides,
  }
}

function runSync(
  cmd: string,
  opts: { cwd?: string; env?: NodeJS.ProcessEnv } = {}
) {
  const result = spawnSync(cmd, [], {
    ...opts,
    shell: true,
    stdio: 'pipe',
    encoding: 'utf8',
  })
  if (result.stdout) console.log(result.stdout)
  if (result.stderr) console.error(result.stderr)
  if (result.status !== 0) {
    throw new Error(`Command failed with exit code ${result.status}`)
  }
}

function bundleAndCopyCliToBuildOutput(
  repoRoot: string,
  env?: NodeJS.ProcessEnv
) {
  runSync('pnpm cli:bundle', { cwd: repoRoot, env })
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
    runSync('pnpm cli:bundle', { cwd: repoRoot })
  }
  return bundlePath
}

function getCliRunConfig(repoRoot: string): {
  command: string
  baseArgs: string[]
} {
  if (process.env.CI === '1') {
    return {
      command: process.execPath,
      baseArgs: [join(repoRoot, CLI_BUNDLE_PATH)],
    }
  }
  return {
    command: 'pnpm',
    baseArgs: ['-C', join(repoRoot, 'cli'), 'exec', 'tsx', 'src/index.ts'],
  }
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

      // Cypress 10+ changes process.cwd() to the config file's directory when using --config-file,
      // so resolve from __dirname to get the repo root regardless of cwd.
      const repoRoot = path.resolve(__dirname, '..', '..')
      const generatedBackendPath = path.join(
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

      const timingLogPath = join(repoRoot, 'e2e_test', 'timing_log.jsonl')

      on('before:run', () => {
        if (process.env.RECORD_E2E_TIMING && existsSync(timingLogPath)) {
          writeFileSync(timingLogPath, '')
        }
      })

      on('task', {
        recordTiming({
          label,
          duration,
          note,
        }: {
          label: string
          duration: number
          note?: string
        }) {
          if (!process.env.RECORD_E2E_TIMING) return null
          const line = `${JSON.stringify({
            label,
            duration,
            note: note ?? null,
            ts: Date.now(),
          })}\n`
          appendFileSync(timingLogPath, line)
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
            runSync('pnpm bundle', { cwd: mcpServerDir })
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
              `installCli: failed to fetch install script from ${baseUrl}/install: ${response.status}`
            )
          }
          const script = await response.text()
          writeFileSync(installScriptPath, script, { mode: 0o755 })
          runSync(`bash ${installScriptPath}`, {
            env: {
              ...process.env,
              INSTALL_PREFIX: installDir,
              BASE_URL: baseUrl,
            },
          })
          const doughnutPath = join(installDir, 'bin', 'doughnut')
          if (!existsSync(doughnutPath)) {
            throw new Error(
              `installCli: doughnut binary not found at ${doughnutPath} after install. Check that ${baseUrl}/doughnut-cli-latest/doughnut is served.`
            )
          }
          return doughnutPath
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
          const config = getCliRunConfig(repoRoot)
          const normalizedInput = input.endsWith('\n') ? input : `${input}\n`
          return new Promise<string>((resolve, reject) => {
            const proc = spawn(config.command, [...config.baseArgs], {
              cwd: repoRoot,
              env: { ...process.env, ...cliEnv(env) },
              stdio: ['pipe', 'pipe', 'pipe'],
            })
            let stdout = ''
            proc.stdout?.on('data', (chunk: Buffer) => {
              stdout += chunk.toString()
            })
            const timeout = setTimeout(() => {
              proc.kill('SIGKILL')
              reject(
                new Error(
                  `Piped CLI timed out after 25s. stdout tail: ${stdout.slice(-300)}`
                )
              )
            }, 25_000)
            proc.stdin?.write(normalizedInput)
            proc.stdin?.end()
            proc.on('close', (code) => {
              clearTimeout(timeout)
              if (code === 0) resolve(stdout)
              else reject(new Error(`CLI exited with code ${code}`))
            })
            proc.on('error', (err) => {
              clearTimeout(timeout)
              reject(err)
            })
          })
        },
        async runCliDirectWithInputAndPty({
          input,
          inputChunks,
          env,
        }: {
          input?: Buffer | string
          inputChunks?: { text: string; delayAfterMs?: number }[]
          env?: NodeJS.ProcessEnv
        }) {
          const repoRoot = path.resolve(__dirname, '..', '..')
          const config = getCliRunConfig(repoRoot)
          const cliInput: string | { text: string; delayAfterMs?: number }[] =
            inputChunks ??
            (input
              ? typeof input === 'string'
                ? input
                : input.toString('utf8')
              : '')
          return runCliInPty({
            command: config.command,
            args: config.baseArgs,
            cwd: repoRoot,
            env,
            input: cliInput,
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
          const config = getCliRunConfig(repoRoot)
          return new Promise<string>((resolve, reject) => {
            const proc = spawn(config.command, [...config.baseArgs, ...args], {
              cwd: repoRoot,
              env: { ...process.env, ...cliEnv(env) },
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
        async runInstalledCli({
          doughnutPath,
          input,
          args,
          env,
        }: {
          doughnutPath: string
          input?: string
          args?: string[]
          env?: NodeJS.ProcessEnv
        }) {
          if (!doughnutPath || typeof doughnutPath !== 'string') {
            throw new Error(
              `runInstalledCli: doughnutPath required, got ${JSON.stringify(doughnutPath)}`
            )
          }
          if (!existsSync(doughnutPath)) {
            throw new Error(
              `runInstalledCli: doughnut binary not found at ${doughnutPath}. Ensure prior step "I install the CLI" succeeded.`
            )
          }
          const cwd = path.dirname(doughnutPath)
          if (input !== undefined) {
            return runCliInPty({
              executablePath: doughnutPath,
              args: args ?? [],
              input,
              cwd,
              env,
            })
          }
          const { spawn } = await import('node:child_process')
          return new Promise<string>((resolve, reject) => {
            const proc = spawn(
              process.execPath,
              [doughnutPath, ...(args ?? [])],
              {
                cwd,
                env: { ...process.env, ...cliEnv(env) },
                stdio: ['pipe', 'pipe', 'pipe'],
              }
            )
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

          const cliConfig = getCliRunConfig(repoRoot)
          return new Promise<{ stdout: string; exitCode: number }>(
            (resolve, reject) => {
              const proc = spawn(cliConfig.command, [...cliConfig.baseArgs], {
                cwd: repoRoot,
                env: {
                  ...process.env,
                  ...cliEnv({
                    DOUGHNUT_CONFIG_DIR: configDir,
                    DOUGHNUT_NO_BROWSER: '1',
                    GOOGLE_BASE_URL: googleBaseUrl,
                  }),
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
          const cliConfig = getCliRunConfig(repoRoot)
          return new Promise<string>((resolve, reject) => {
            const proc = spawn(cliConfig.command, [...cliConfig.baseArgs], {
              cwd: repoRoot,
              env: {
                ...process.env,
                ...cliEnv({
                  DOUGHNUT_CONFIG_DIR: configDir,
                  GOOGLE_BASE_URL: googleBaseUrl,
                }),
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
