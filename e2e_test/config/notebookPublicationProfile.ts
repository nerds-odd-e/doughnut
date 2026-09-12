import { publishNotebookProfileHttp } from './notebookPublicationHttp'
import assert from 'node:assert/strict'
import {
  publicationPersistedState,
  expectPublicationStatePreserved,
} from './notebookPublicationState'
import { createHash } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { homedir } from 'node:os'
import { join } from 'node:path'
import { runSutHealthcheck } from '../../scripts/sut-healthcheck.mjs'
import { resolveSutCheckoutTarget } from '../../scripts/sut-isolated-target.mjs'
import { getListenerPids } from '../../scripts/sut-listener-pids.mjs'
import { findPublicationReceiverMismatch } from '../../scripts/profiling/verify-publication-receiver.mjs'

export interface PublicationProfileParameters {
  existing: number
  additions: number
  updates: number
  folders: number
}

export function notebookPublicationProfileTasks(
  repoRoot: string,
  on: Cypress.PluginEvents
) {
  let capture:
    | {
        pid: number
        directory: string
        started: string
        recordingActive: boolean
      }
    | undefined
  function jcmd(pid: number, ...args: string[]) {
    return execFileSync('jcmd', [String(pid), ...args], { encoding: 'utf8' })
  }
  function stopRecording(filename: string) {
    if (!capture) throw new Error('No publication capture exists')
    const { pid, directory, started } = capture
    const stopped = new Date().toISOString()
    const output = jcmd(
      pid,
      'JFR.stop',
      'name=publication',
      `filename="${filename}"`
    )
    capture.recordingActive = false
    writeFileSync(
      join(directory, 'result.json'),
      JSON.stringify(
        {
          outcome: 'incomplete',
          recordingStarted: started,
          recordingStopped: stopped,
          recordingIntervalMs: Date.parse(stopped) - Date.parse(started),
          command: `jcmd ${pid} JFR.stop name=publication 'filename="${filename}"'`,
          output,
        },
        null,
        2
      )
    )
    writeFileSync(
      join(directory, 'jfr-summary.txt'),
      execFileSync('jfr', ['summary', filename], { encoding: 'utf8' })
    )
    return directory
  }
  on('after:run', () => {
    if (capture?.recordingActive)
      stopRecording(join(capture.directory, 'incomplete.jfr'))
  })
  const parameters: PublicationProfileParameters = {
    existing: Number(process.env.PUBLICATION_PROFILE_EXISTING ?? 20),
    additions: Number(process.env.PUBLICATION_PROFILE_ADDITIONS ?? 20),
    updates: Number(process.env.PUBLICATION_PROFILE_UPDATES ?? 20),
    folders: Number(process.env.PUBLICATION_PROFILE_FOLDERS ?? 20),
  }
  function git(checkoutDir: string, ...args: string[]) {
    return execFileSync('git', ['-C', checkoutDir, ...args], {
      encoding: 'utf8',
      maxBuffer: 64 * 1024 * 1024,
    }).trim()
  }
  return {
    publishNotebookPublicationHttp({
      checkoutDir,
      configDir,
    }: {
      checkoutDir: string
      configDir: string
    }) {
      assert.ok(
        capture?.recordingActive,
        'HTTP publication requires an active owned capture'
      )
      return publishNotebookProfileHttp(
        repoRoot,
        capture.directory,
        checkoutDir,
        configDir
      )
    },
    notebookPublicationProfileParameters() {
      if (
        Object.values(parameters).some(
          (value) => !Number.isInteger(value) || value < 1
        )
      )
        throw new Error('Profile counts must be positive integers')
      return parameters
    },
    invalidateLastNotebookPublicationDocument(checkoutDir: string) {
      const paths = git(
        checkoutDir,
        'diff',
        '--name-only',
        '--diff-filter=A',
        'HEAD^',
        'HEAD'
      ).split('\n')
      const invalidPath = paths.at(-1)!
      const path = join(checkoutDir, invalidPath)
      writeFileSync(
        path,
        readFileSync(path, 'utf8').replace(
          /^aliases:.*$/m,
          'aliases: {invalid: shape}'
        )
      )
      execFileSync('git', ['-C', checkoutDir, 'add', '--', invalidPath])
      return invalidPath
    },
    normalizeNotebookPublicationProposal(checkoutDir: string) {
      execFileSync(
        'git',
        [
          '-C',
          checkoutDir,
          '-c',
          'user.name=Donut E2E',
          '-c',
          'user.email=donut-e2e@example.com',
          'commit',
          '--amend',
          '--no-edit',
          '--date=2000-01-01T00:00:00Z',
        ],
        {
          env: { ...process.env, GIT_COMMITTER_DATE: '2000-01-01T00:00:00Z' },
        }
      )
      return git(checkoutDir, 'rev-parse', 'HEAD')
    },
    recordNotebookPublicationTiming(timing: {
      started: string
      stopped: string
      boundary: string
    }) {
      if (!capture) throw new Error('No publication capture exists')
      writeFileSync(
        join(capture.directory, 'timing.json'),
        JSON.stringify(
          {
            ...timing,
            elapsedMs: Date.parse(timing.stopped) - Date.parse(timing.started),
          },
          null,
          2
        )
      )
      return null
    },
    async startNotebookPublicationProfile({
      checkoutDir,
      learning,
    }: {
      checkoutDir: string
      learning: unknown
    }) {
      const { isolated, target } = resolveSutCheckoutTarget({
        checkoutRoot: repoRoot,
      })
      if (
        !(isolated && (await runSutHealthcheck({ checkoutRoot: repoRoot })).ok)
      ) {
        throw new Error(
          'Publication profiling requires a healthy owned isolated SUT'
        )
      }
      const pids = await getListenerPids(target.backendPort)
      if (pids.length !== 1)
        throw new Error('Expected exactly one owned backend listener')
      const pid = pids[0]!
      const started = new Date().toISOString()
      const directory = join(
        homedir(),
        'Library',
        'Application Support',
        'Donut',
        'publication-profiles',
        started.replaceAll(':', '-')
      )
      mkdirSync(directory, { recursive: true })
      const metadata = {
        parameters,
        persistedState: publicationPersistedState(repoRoot),
        learning,
        baselineHead: git(checkoutDir, 'rev-parse', 'HEAD^'),
        proposalHead: git(checkoutDir, 'rev-parse', 'HEAD'),
        baselineFingerprint: createHash('sha256')
          .update(git(checkoutDir, 'ls-tree', '-r', 'HEAD^'))
          .digest('hex'),
        proposalFingerprint: createHash('sha256')
          .update(git(checkoutDir, 'ls-tree', '-r', 'HEAD'))
          .digest('hex'),
        baselineTree: git(checkoutDir, 'rev-parse', 'HEAD^:'),
        proposalTree: git(checkoutDir, 'rev-parse', 'HEAD:'),
        mysql: execFileSync(
          'mysql',
          [
            '--protocol=TCP',
            '-h127.0.0.1',
            '-P3309',
            '-uroot',
            '--batch',
            '--skip-column-names',
            '-e',
            'SELECT VERSION()',
          ],
          { encoding: 'utf8' }
        ),
        loggingConfiguration: readFileSync(
          join(repoRoot, 'backend/src/main/resources/application.yml'),
          'utf8'
        ),
        warmup:
          'No explicit warm-up; reused healthy SUT process, fixture setup precedes capture',
        revision: execFileSync('git', ['-C', repoRoot, 'rev-parse', 'HEAD'], {
          encoding: 'utf8',
        }).trim(),
        checkout: repoRoot,
        target,
        pid,
        started,
        process: execFileSync(
          'ps',
          ['-p', String(pid), '-o', 'pid=,lstart=,command='],
          { encoding: 'utf8' }
        ),
        vm: jcmd(pid, 'VM.version'),
        flags: jcmd(pid, 'VM.flags'),
        command: `jcmd ${pid} JFR.start name=publication settings=profile`,
        recording: jcmd(
          pid,
          'JFR.start',
          'name=publication',
          'settings=profile'
        ),
      }
      capture = { pid, directory, started, recordingActive: true }
      writeFileSync(
        join(directory, 'capture.json'),
        JSON.stringify(metadata, null, 2)
      )
      return directory
    },
    stopNotebookPublicationProfile() {
      if (!capture) throw new Error('No publication recording is active')
      return stopRecording(join(capture.directory, 'publication.jfr'))
    },
    confirmRejectedNotebookPublicationProfile({
      checkoutDir,
      invalidPath,
    }: {
      checkoutDir: string
      invalidPath: string
    }) {
      assert.ok(capture && !capture.recordingActive, 'Expected stopped capture')
      const metadata = JSON.parse(
        readFileSync(join(capture.directory, 'capture.json'), 'utf8')
      )
      const after = publicationPersistedState(repoRoot)
      const processedAdditions = expectPublicationStatePreserved(
        metadata.persistedState,
        after,
        parameters.additions
      )
      assert.equal(git(checkoutDir, 'rev-parse', 'HEAD'), metadata.baselineHead)
      assert.equal(
        git(checkoutDir, 'rev-parse', 'HEAD:'),
        metadata.baselineTree
      )
      assert.equal(git(checkoutDir, 'status', '--porcelain'), '')
      const resultPath = join(capture.directory, 'result.json')
      writeFileSync(
        resultPath,
        JSON.stringify(
          {
            ...JSON.parse(readFileSync(resultPath, 'utf8')),
            outcome: 'rejected',
            invalidPath,
            processedAdditions,
            acceptedHead: metadata.baselineHead,
            preservedState: after,
            rejectionEvidence:
              'Publication rejection and Invalid authored property at the final added path asserted before pull; timing.json identifies transport and response',
          },
          null,
          2
        )
      )
      capture = undefined
      return null
    },
    verifyPublicationReceiverFiles({
      checkoutDir,
      files,
    }: {
      checkoutDir: string
      files: { relativePath: string; content: string }[]
    }) {
      const mismatch = findPublicationReceiverMismatch(checkoutDir, files)
      if (mismatch) {
        throw new Error(
          `Publication receiver file ${mismatch.reason}: ${mismatch.relativePath}`
        )
      }
      return null
    },
    confirmNotebookPublicationProfile({
      acceptedHead,
      documentCount,
    }: {
      acceptedHead: string
      documentCount: number
    }) {
      if (!capture || capture.recordingActive)
        throw new Error('Expected a stopped publication recording')
      const resultPath = join(capture.directory, 'result.json')
      const result = JSON.parse(readFileSync(resultPath, 'utf8'))
      writeFileSync(
        resultPath,
        JSON.stringify(
          {
            ...result,
            outcome: 'accepted',
            acceptedHead,
            verifiedAuthoredDocuments: documentCount,
          },
          null,
          2
        )
      )
      capture = undefined
      return null
    },
  }
}
