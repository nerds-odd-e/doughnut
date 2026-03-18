import {
  copyFileSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readdirSync,
  rm,
  writeFileSync,
} from 'node:fs'
import { spawnSync } from 'node:child_process'
import { tmpdir } from 'node:os'
import { dirname, extname, join, resolve } from 'node:path'
import mcpClient from '../support/mcp_client'
const {
  addCucumberPreprocessorPlugin,
} = require('@badeball/cypress-cucumber-preprocessor')
import { createEsbuildPlugin } from '@badeball/cypress-cucumber-preprocessor/esbuild'
import createBundler from '@bahmutov/cypress-esbuild-preprocessor'
import AdmZip from 'adm-zip'
import type { ExpectedFile } from '../start/downloadChecker'
import {
  runCliInPty,
  startInteractiveCli as startInteractiveCliProcess,
  sendToInteractiveCli as sendToInteractiveCliInput,
  stopInteractiveCli as stopInteractiveCliProcess,
} from './cliPtyRunner'
import { cliEnv } from './cliEnv'
import { E2E_BACKEND_BASE_URL } from './constants'

const CLI_BUNDLE_PATH = 'cli/dist/doughnut-cli.bundle.mjs'

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

const CLI_SPAWN_TIMEOUT_MS = 25_000

async function spawnCliFromRepo(opts: {
  repoRoot: string
  args?: string[]
  stdin?: string
  env?: NodeJS.ProcessEnv
  simulateOAuthCallback?: boolean
  timeoutMs?: number
}): Promise<string> {
  const { spawn } = await import('node:child_process')
  const config = getCliRunConfig(opts.repoRoot)
  const args = [...config.baseArgs, ...(opts.args ?? [])]
  return new Promise<string>((resolve, reject) => {
    const proc = spawn(config.command, args, {
      cwd: opts.repoRoot,
      env: { ...process.env, ...cliEnv(opts.env) },
      stdio: ['pipe', 'pipe', 'pipe'],
    })
    let stdout = ''
    const append = (chunk: string) => {
      stdout += chunk
      return stdout
    }
    proc.stdout?.on('data', (chunk: Buffer) => {
      const out = append(chunk.toString())
      if (opts.simulateOAuthCallback) {
        const authMatch = out.match(/https:\/\/accounts\.google\.com\/[^\s]+/)
        if (authMatch) {
          const redirectUri = new URL(authMatch[0]).searchParams.get(
            'redirect_uri'
          )
          if (redirectUri) {
            fetch(`${redirectUri}?code=e2e_mock_auth_code`).catch(() => {
              /* ignore OAuth callback errors */
            })
          }
        }
      }
    })
    if (opts.simulateOAuthCallback) {
      proc.stderr?.on('data', (chunk: Buffer) => append(chunk.toString()))
    }
    const timeout =
      opts.timeoutMs &&
      setTimeout(() => {
        proc.kill('SIGKILL')
        reject(
          new Error(
            `CLI timed out after ${opts.timeoutMs}ms. stdout tail: ${stdout.slice(-300)}`
          )
        )
      }, opts.timeoutMs)
    if (opts.stdin !== undefined) {
      proc.stdin?.write(
        opts.stdin.endsWith('\n') ? opts.stdin : `${opts.stdin}\n`
      )
      proc.stdin?.end()
    } else {
      proc.stdin?.end()
    }
    proc.on('close', (code) => {
      if (timeout) clearTimeout(timeout)
      if (code === 0) resolve(stdout)
      else reject(new Error(`CLI exited with code ${code}`))
    })
    proc.on('error', (err) => {
      if (timeout) clearTimeout(timeout)
      reject(err)
    })
  })
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
  backendBaseUrl: E2E_BACKEND_BASE_URL,

  e2e: {
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

      on('task', {
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
        checkDownloadedZipContent(expectedFiles: ExpectedFile[]) {
          const downloadsFolder = config.downloadsFolder
          const files = readdirSync(downloadsFolder)
          const zipFile = files.find((file) => file.endsWith('.zip'))

          if (!zipFile) {
            throw new Error('No zip file found in downloads folder')
          }

          const zip = new AdmZip(join(downloadsFolder, zipFile))
          const zipEntries = zip.getEntries()

          const actualFiles = zipEntries.map((entry) => ({
            Filename: entry.entryName,
            Format: extname(entry.entryName).slice(1),
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
          const repoRoot = resolve(__dirname, '..', '..')
          const mcpServerDir = join(repoRoot, 'mcp-server')
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
        createCliConfigDirWithGmail(gmailConfig: Record<string, unknown>) {
          const configDir = mkdtempSync(join(tmpdir(), 'cypress-cli-gmail-'))
          writeFileSync(
            join(configDir, 'gmail.json'),
            JSON.stringify(gmailConfig, null, 2)
          )
          return configDir
        },
        async bundleAndCopyCli() {
          const repoRoot = resolve(__dirname, '..', '..')
          try {
            bundleAndCopyCliToBuildOutput(repoRoot)
            return true
          } catch (error) {
            console.error('Failed to bundle and copy CLI:', error)
            throw error
          }
        },
        async bundleAndCopyCliWithVersion(version: string) {
          const repoRoot = resolve(__dirname, '..', '..')
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
          simulateOAuthCallback,
        }: {
          input: string
          env?: NodeJS.ProcessEnv
          simulateOAuthCallback?: boolean
        }) {
          const repoRoot = resolve(__dirname, '..', '..')
          return spawnCliFromRepo({
            repoRoot,
            stdin: input,
            env,
            simulateOAuthCallback,
            timeoutMs: CLI_SPAWN_TIMEOUT_MS,
          })
        },
        async startInteractiveCli({ env }: { env?: NodeJS.ProcessEnv }) {
          const repoRoot = resolve(__dirname, '..', '..')
          const config = getCliRunConfig(repoRoot)
          await startInteractiveCliProcess({
            command: config.command,
            args: config.baseArgs,
            cwd: repoRoot,
            env: { ...cliEnv(env) },
          })
          return true
        },
        async sendToInteractiveCli({ input }: { input: string }) {
          return sendToInteractiveCliInput(input)
        },
        async stopInteractiveCli() {
          await stopInteractiveCliProcess()
          return null
        },
        async runCliDirectWithArgs({
          args,
          env,
        }: {
          args: string[]
          env?: NodeJS.ProcessEnv
        }) {
          const repoRoot = resolve(__dirname, '..', '..')
          return spawnCliFromRepo({ repoRoot, args, env })
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
          const cwd = dirname(doughnutPath)
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
