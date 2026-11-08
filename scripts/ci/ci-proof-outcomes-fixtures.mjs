import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import {
  mkdirSync,
  mkdtempSync,
  readdirSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { action } from './workflow-fixtures.mjs'

export const repoRoot = fileURLToPath(new URL('../..', import.meta.url))

export const writeFragment = (t, format, entry, report) => {
  const temp = mkdtempSync(join(tmpdir(), 'ci-proof-'))
  t.after(() => rmSync(temp, { recursive: true, force: true }))
  const reportPath = join(temp, 'report')
  if (typeof report === 'object') {
    mkdirSync(reportPath)
    for (const [name, content] of Object.entries(report)) {
      writeFileSync(join(reportPath, name), content)
    }
  } else {
    writeFileSync(reportPath, report)
  }
  const step = action('ci_proof_fragments').runs.steps.find(
    ({ id }) => id === 'fragments'
  )
  execFileSync(step.shell, ['-e', '-o', 'pipefail', '-c', step.run], {
    cwd: repoRoot,
    env: {
      ...process.env,
      FORMAT: format,
      REPORT: reportPath,
      ENTRY: entry,
      JOB_INDEX: '0',
      PARTS: '2',
      RUNNER_TEMP: temp,
      GITHUB_OUTPUT: join(temp, 'github-output'),
      GITHUB_RUN_ATTEMPT: '1',
      CI_PROOF_REVISION: 'deadbeef',
    },
  })
  const fragments = join(temp, 'ci-proof')
  const files = readdirSync(fragments)
  assert.equal(files.length, 1)
  return {
    times: readFileSync(join(temp, 'ci-proof-times.tsv'), 'utf8'),
    fragment: JSON.parse(readFileSync(join(fragments, files[0]), 'utf8')),
  }
}
