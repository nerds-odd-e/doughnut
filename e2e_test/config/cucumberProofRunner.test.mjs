import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { createRequire } from 'node:module'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { test } from 'node:test'
import { stripVTControlCharacters } from 'node:util'
import { proofProject } from './cucumberProofTestProject.mjs'
import { writeRunnerFixture } from './cucumberProofRunnerFixture.mjs'

const require = createRequire(import.meta.url)
const cypress = join(
  dirname(require.resolve('cypress/package.json')),
  'bin/cypress'
)
const reporter = (output) =>
  stripVTControlCharacters(output)
    .split('\n')
    .map((line) =>
      line
        .trim()
        .replaceAll(/\(\d+(?:ms|s)\)/g, '(duration)')
        .replaceAll(/\d+ms|\d+s/g, 'duration')
        .replaceAll(/localhost:\d+/g, 'localhost:port')
    )
    .filter(
      (line) =>
        /^([✔✓]|\d+\)|\d+ passing|\d+ failing|\d+ pending)/.test(line) ||
        line.includes('proof fixture')
    )

test('real full, focused, tagged and dry Cypress runs preserve results and publish scoped proof', {
  timeout: 180000,
}, async (t) => {
  const project = proofProject(t)
  writeRunnerFixture(project.root)
  const logs = mkdtempSync(join(tmpdir(), 'donut-cucumber-proof-runner-'))
  t.diagnostic(`Cypress fixture evidence: ${logs}`)
  let runNumber = 0
  async function run(mode, args = []) {
    rmSync(join(project.root, 'proof'), { recursive: true, force: true })
    rmSync(join(project.root, 'owners.log'), { force: true })
    const label = `${runNumber++}-${mode}`
    const child = spawn(
      process.execPath,
      [
        cypress,
        'run',
        '--project',
        project.root,
        '--config-file',
        join(project.root, 'cypress.config.cjs'),
        '--browser',
        'electron',
        ...args,
      ],
      {
        cwd: project.root,
        env: { ...process.env, PROOF_MODE: mode },
        stdio: ['ignore', 'pipe', 'pipe'],
      }
    )
    let output = ''
    child.stdout.on('data', (chunk) => {
      output += chunk
    })
    child.stderr.on('data', (chunk) => {
      output += chunk
    })
    const [code] = await once(child, 'exit')
    writeFileSync(join(logs, `${label}.log`), output)
    const owners = readFileSync(join(project.root, 'owners.log'), 'utf8')
      .trim()
      .split('\n')
    assert.equal(owners[0], 'start', output)
    assert.equal(owners.at(-1), 'cleanup', output)
    return { code, output, proof: project.proof() }
  }
  const baseline = await run('baseline')
  const observed = await run('enabled')
  assert.equal(baseline.code, 3, baseline.output)
  assert.match(baseline.output, /3 passing/, baseline.output)
  assert.match(
    baseline.output,
    /proof fixture intentional failure/,
    baseline.output
  )
  assert.match(
    baseline.output,
    /proof fixture raw hook failure/,
    baseline.output
  )
  assert.match(
    baseline.output,
    /proof fixture Cucumber hook failure/,
    baseline.output
  )
  assert.equal(observed.code, baseline.code, observed.output)
  assert.deepEqual(reporter(observed.output), reporter(baseline.output))
  assert.doesNotMatch(
    observed.output,
    /Hook failures can't be represented|Due to browser crash, no reports/
  )
  assert.equal(observed.proof.length, 5)
  assert.ok(
    observed.proof.every((row) => row.feature === 'features/normal.feature')
  )
  const retried = observed.proof.find(
    (row) => row.scenario === 'Retry succeeds'
  )
  assert.equal(retried.attempts, 2)
  assert.equal(retried.outcome, 'passed')
  assert.ok(retried.durationMs > 0)
  assert.equal(
    observed.proof.find((row) => row.scenario === 'Cucumber hook fails')
      .outcome,
    'failed'
  )
  assert.deepEqual(
    observed.proof.filter((row) => row.example).map((row) => row.example),
    [['a|b'], ['back\\slash']]
  )
  assert.equal(retried.commit, project.git('rev-parse', 'HEAD'))
  assert.equal(
    retried.featureBlob,
    project.git('hash-object', 'features/normal.feature')
  )
  const dry = await run('dry')
  assert.equal(dry.code, 0, dry.output)
  assert.deepEqual(dry.proof, [])
  const focused = await run('enabled', [
    '--spec',
    `${project.git('rev-parse', '--show-toplevel')}/features/normal.feature`,
  ])
  assert.equal(focused.code, 2, focused.output)
  assert.equal(focused.proof.length, 5)
  const tagged = await run('enabled', ['--expose', 'tags=@selected'])
  assert.equal(tagged.code, 0, tagged.output)
  assert.equal(tagged.proof.length, 2)
  const reported = await run('report')
  assert.equal(reported.code, baseline.code, reported.output)
  assert.equal(reported.proof.length, 5)
  const messages = readFileSync(join(project.root, 'existing.ndjson'), 'utf8')
    .trim()
    .split('\n')
    .map(JSON.parse)
  assert.ok(messages.some((message) => message.testCaseFinished))
})
