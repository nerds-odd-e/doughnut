import { execFileSync } from 'node:child_process'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { homedir } from 'node:os'
import { join } from 'node:path'
import { runSutHealthcheck } from '../../scripts/sut-healthcheck.mjs'
import { resolveSutCheckoutTarget } from '../../scripts/sut-isolated-target.mjs'
import { getListenerPids } from '../../scripts/sut-listener-pids.mjs'

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
  return {
    async startNotebookPublicationProfile() {
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
