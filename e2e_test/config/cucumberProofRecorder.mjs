import { execFileSync } from 'node:child_process'
import { readFileSync, realpathSync } from 'node:fs'
import { resolve } from 'node:path'
import { cucumberProofCases } from './cucumberProofCases.mjs'
import { maintainScenarioProof } from './cucumberProofStore.mjs'

const statuses = [
  'failed',
  'ambiguous',
  'undefined',
  'pending',
  'skipped',
  'passed',
  'unknown',
]
const milliseconds = (duration) =>
  Number(duration.seconds) * 1000 + duration.nanos / 1e6
const silently = (action) => {
  try {
    return action()
  } catch {
    // Proof is deliberately best effort; runner failures keep their own path.
    return
  }
}

/** Node-side observer of the pinned preprocessor's Cucumber task transport. */
export function createCucumberProofRecorder({
  repoRoot,
  projectRoot = repoRoot,
  env = process.env,
}) {
  const disabled = {
    configured: false,
    start: () => undefined,
    observe: () => undefined,
  }
  if (env.RECORD_CUCUMBER_PROOF !== '1') return disabled
  return (
    silently(() => {
      projectRoot = resolve(projectRoot)
      const canonicalProjectRoot = realpathSync(projectRoot)
      repoRoot = execFileSync('git', ['rev-parse', '--show-toplevel'], {
        cwd: repoRoot,
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'ignore'],
      }).trim()
      const configuration = JSON.parse(
        readFileSync(resolve(repoRoot, '.planning/open-dough.json'), 'utf8')
      )
      const locations = configuration.intent.locations
        .filter(
          (location) =>
            location.kind === 'gherkin' && typeof location.proof === 'string'
        )
        .map((location) => ({
          path: resolve(repoRoot, location.path),
          proof: resolve(repoRoot, location.proof),
        }))
      if (!locations.length) return disabled
      let commit
      const cases = new Map()
      const attempts = new Map()
      const retries = new Map()
      function finish(data) {
        const attempt = attempts.get(data.testCaseStartedId)
        if (!attempt) return
        attempts.delete(data.testCaseStartedId)
        const results = [...attempt.steps.values()]
        if (
          !results.length ||
          results.every((result) => result.status === 'SKIPPED')
        )
          return
        const execution = retries.get(attempt.testCaseId)
        const outcome = statuses.find((status) =>
          results.some((result) => result.status.toLowerCase() === status)
        )
        execution.set(attempt.attempt, {
          outcome,
          durationMs: results.reduce(
            (total, result) => total + milliseconds(result.duration),
            0
          ),
        })
        if (data.willBeRetried) return
        const { directories, ...identity } = cases.get(attempt.testCaseId)
        const observation = {
          ...identity,
          outcome,
          attempts: execution.size,
          durationMs: [...execution.values()].reduce(
            (total, result) => total + result.durationMs,
            0
          ),
          finishedAt: new Date(milliseconds(data.timestamp)).toISOString(),
          commit,
        }
        for (const directory of new Set(directories))
          silently(() => maintainScenarioProof(directory, observation))
        retries.delete(attempt.testCaseId)
      }

      return {
        configured: true,
        start() {
          silently(() => {
            commit = undefined
            cases.clear()
            attempts.clear()
            retries.clear()
            commit = execFileSync('git', ['rev-parse', 'HEAD'], {
              cwd: repoRoot,
              encoding: 'utf8',
              stdio: ['ignore', 'pipe', 'ignore'],
            }).trim()
          })
        },
        observe(name, data) {
          if (!commit) return
          silently(() => {
            switch (name) {
              case 'cypress-cucumber-preprocessor:spec-envelopes':
                for (const [id, identity] of cucumberProofCases(data.messages, {
                  repoRoot,
                  projectRoot,
                  canonicalProjectRoot,
                  locations,
                })) {
                  cases.set(id, identity)
                }
                break
              case 'cypress-cucumber-preprocessor:test-case-started':
                if (!cases.has(data.testCaseId)) break
                if (data.attempt === 0) retries.set(data.testCaseId, new Map())
                attempts.set(data.id, { ...data, steps: new Map() })
                break
              case 'cypress-cucumber-preprocessor:test-step-finished':
                attempts
                  .get(data.testCaseStartedId)
                  ?.steps.set(data.testStepId, data.testStepResult)
                break
              case 'cypress-cucumber-preprocessor:test-case-finished':
                finish(data)
            }
          })
        },
      }
    }) ?? disabled
  )
}
