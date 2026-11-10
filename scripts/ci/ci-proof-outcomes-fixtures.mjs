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

const tempDir = (t) => {
  const temp = mkdtempSync(join(tmpdir(), 'ci-proof-'))
  t.after(() => rmSync(temp, { recursive: true, force: true }))
  return temp
}

const runStep = (step, env) =>
  execFileSync(step.shell, ['-e', '-o', 'pipefail', '-c', step.run], {
    cwd: repoRoot,
    env: { ...process.env, GITHUB_RUN_ATTEMPT: '1', ...env },
    stdio: ['ignore', 'pipe', 'pipe'],
  })

// Runs the fragment action's conversion for one part; `identity` overrides
// the matrix-derived part (inputs `part` and `of` set PART and PARTS). Parts
// of one job share its `temp` (RUNNER_TEMP).
export const writeFragment = (
  t,
  format,
  entry,
  report,
  identity = {},
  temp = tempDir(t)
) => {
  const reportPath = join(mkdtempSync(join(temp, 'report-')), 'report')
  // An undefined report means the runner wrote none.
  if (typeof report === 'object') {
    mkdirSync(reportPath)
    for (const [name, content] of Object.entries(report)) {
      writeFileSync(join(reportPath, name), content)
    }
  } else if (report !== undefined) {
    writeFileSync(reportPath, report)
  }
  const step = action('ci_proof_fragments').runs.steps.find(
    ({ id }) => id === 'fragments'
  )
  runStep(step, {
    UNIT: '',
    FORMAT: format,
    REPORT: reportPath,
    ENTRY: entry,
    JOB_INDEX: '0',
    JOB_TOTAL: '2',
    ...identity,
    RUNNER_TEMP: temp,
    GITHUB_OUTPUT: join(temp, 'github-output'),
    CI_PROOF_REVISION: 'deadbeef',
  })
  const fragments = join(temp, 'ci-proof')
  const files = readdirSync(fragments)
  assert.equal(files.length, 1)
  const fragmentText = readFileSync(join(fragments, files[0]), 'utf8')
  return {
    times: readFileSync(join(temp, 'ci-proof-times.tsv'), 'utf8'),
    fragmentText,
    fragment: JSON.parse(fragmentText),
  }
}

// Runs the summary action's collector over downloaded fragment texts.
export const summarizeFragments = (t, fragmentTexts, expect) => {
  const temp = tempDir(t)
  const downloaded = join(temp, 'ci-proof-fragments')
  mkdirSync(downloaded)
  fragmentTexts.forEach((text, index) => {
    mkdirSync(join(downloaded, `artifact-${index}`))
    writeFileSync(join(downloaded, `artifact-${index}`, 'fragment.json'), text)
  })
  const step = action('ci_proof_summary').runs.steps.find(
    ({ name }) => name === 'Summarize CI proof'
  )
  const log = runStep(step, {
    EXPECT: expect,
    RUNNER_TEMP: temp,
    GITHUB_REPOSITORY: 'nerds-odd-e/doughnut',
    GITHUB_RUN_ID: '42',
    CI_PROOF_REVISION: 'deadbeef',
  })
  return {
    log: log.toString(),
    summary: JSON.parse(
      readFileSync(join(temp, 'ci-proof-summary.json'), 'utf8')
    ),
  }
}
