// A `node --test --test-reporter=<this file>` recorder for `node:test` proof.
// With RECORD_NODE_TEST_PROOF unset it drains events and yields nothing. With
// the option set it maintains each covered location's latest.json through
// proofStore.mjs. Errors stay contained so the run's exit and output are
// unchanged. Ordinary `pnpm test:*` scripts do not load this reporter.
import { execFileSync } from 'node:child_process'
import { readFileSync, realpathSync } from 'node:fs'
import path from 'node:path'
import { createSession } from './nodeTestProofSession.mjs'

function git(args, cwd) {
  return execFileSync('git', args, {
    cwd,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'ignore'],
  }).trim()
}

function repositoryRoot(cwd) {
  try {
    return realpathSync(git(['rev-parse', '--show-toplevel'], cwd))
  } catch {
    return
  }
}

function coveredLocations(root) {
  try {
    const settings = JSON.parse(
      readFileSync(path.join(root, '.planning/open-dough.json'), 'utf8')
    )
    return (settings.intent?.locations ?? []).flatMap((location) =>
      location.runner === 'node:test' && location.proof && location.path
        ? [
            {
              files: `${String(location.path).replace(/\/$/, '')}/`,
              proof: location.proof,
            },
          ]
        : []
    )
  } catch {
    return []
  }
}

function startRun(cwd) {
  const root = repositoryRoot(cwd)
  if (!root) return
  const locations = coveredLocations(root)
  if (!locations.length) return
  return {
    root,
    commit: git(['rev-parse', 'HEAD'], root),
    locations,
  }
}

async function drain(source) {
  for await (const _event of source) {
    // Drain so the run completes; the recorder prints nothing.
  }
}

// Node reporters are async generators; this one never yields (quiet).
// biome-ignore lint/correctness/useYield: silent node --test reporter API
export default async function* nodeTestProofReporter(source) {
  try {
    if (process.env.RECORD_NODE_TEST_PROOF !== '1') {
      await drain(source)
      return
    }

    let run
    try {
      run = startRun(process.cwd())
    } catch {
      run = undefined
    }
    if (!run) {
      await drain(source)
      return
    }

    const session = createSession(run)
    for await (const event of source) {
      try {
        if (event.type === 'test:start') {
          session.onStart(event.data)
        } else if (event.type === 'test:pass' || event.type === 'test:fail') {
          session.onFinished(event.type, event.data)
        }
      } catch {
        // A recording failure must not change the run's output or exit.
      }
    }
  } catch {
    // Never fail the run or print from the recorder.
  }
}
