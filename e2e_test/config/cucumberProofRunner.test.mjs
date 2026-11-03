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
const reporter = (output) => {
  const specs = new Map()
  const totals = []
  let rows
  for (const line of stripVTControlCharacters(output).split('\n')) {
    const spec = line.match(/^\s*Running:\s+(.*?)\s+\(\d+ of \d+\)/)?.[1]
    if (spec) {
      assert.ok(!specs.has(spec), `Duplicate spec: ${spec}`)
      rows = []
      specs.set(spec, rows)
    }
    if (/^\s*\(Run Finished\)/.test(line)) rows = totals
    if (
      /^\s*([✔✓]|\d+\)|\d+ passing|\d+ failing|\d+ pending)/.test(line) ||
      line.includes('proof fixture')
    ) {
      assert.ok(rows, line)
      rows.push(
        line
          .trim()
          .replaceAll(/\(\d+(?:ms|s)\)/g, '(duration)')
          .replaceAll(/\s*\d+(?:ms|s)\b/g, ' duration')
          .replaceAll(/localhost:\d+/g, 'localhost:port')
      )
    }
  }
  return {
    specs: [...specs.entries()].sort(([left], [right]) =>
      left.localeCompare(right)
    ),
    totals,
  }
}

test('Cypress summary rows compare equal whatever the width of their durations', () => {
  const summary = (duration) =>
    [
      '  Running:  parse.feature  (1 of 1)',
      `  \u2714  All specs passed!${duration.padStart(31)}        1        1        -        -        -`,
    ].join('\n')
  assert.deepEqual(reporter(summary('999ms')), reporter(summary('1s')))
})

test('Cypress run totals compare equal whatever order the specs ran in', () => {
  const run = (...specs) =>
    [
      ...specs.flatMap((spec, index) => [
        `  Running:  ${spec}  (${index + 1} of ${specs.length})`,
        '  1 passing (5ms)',
      ]),
      '  (Run Finished)',
      '    \u2714  All specs passed!    9ms        2        2        -        -        -',
    ].join('\n')
  assert.deepEqual(
    reporter(run('a.feature', 'b.feature')),
    reporter(run('b.feature', 'a.feature'))
  )
})

test('ordinary and explicit full, focused, tagged, dry and reported Cypress runs preserve results and proof ownership', {
  timeout: 180000,
}, async (t) => {
  const project = proofProject(t)
  writeRunnerFixture(project.root)
  const logs = mkdtempSync(join(tmpdir(), 'donut-cucumber-proof-runner-'))
  t.diagnostic(`Cypress fixture evidence: ${logs}`)
  let runNumber = 0
  async function run(mode, args = [], { reset = true, record = true } = {}) {
    if (reset)
      rmSync(join(project.root, 'proof'), { recursive: true, force: true })
    rmSync(join(project.root, 'owners.log'), { force: true })
    const before = project.proofState()
    const label = `${runNumber++}-${mode}-${record ? 'record' : 'ordinary'}`
    const env = { ...process.env, PROOF_MODE: mode }
    delete env.RECORD_CUCUMBER_PROOF
    if (record) env.RECORD_CUCUMBER_PROOF = '1'
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
        env,
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
    if (!record) assert.deepEqual(project.proofState(), before, output)
    return { code, output, owners, proof: project.proof() }
  }
  const baseline = await run('enabled', [], { record: false })
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
  assert.deepEqual(
    reporter(baseline.output).specs.map(([spec]) => spec),
    [
      'features/hooks.feature',
      'features/normal.feature',
      'outside/outside.feature',
    ]
  )
  assert.equal(observed.code, baseline.code, observed.output)
  assert.deepEqual(reporter(observed.output), reporter(baseline.output))
  assert.deepEqual(observed.owners, baseline.owners)
  assert.doesNotMatch(
    observed.output,
    /Hook failures can't be represented|Due to browser crash, no reports/
  )
  assert.equal(observed.proof.length, 3)
  assert.ok(
    observed.proof.every((row) => row.feature === 'features/normal.feature')
  )
  const retried = observed.proof.find(
    (row) => row.scenario === 'Retry succeeds'
  )
  assert.equal(retried.attempts, 2)
  assert.equal(retried.outcome, 'passed')
  assert.ok(retried.durationMs > 0)
  assert.deepEqual(
    observed.proof.filter((row) => row.example).map((row) => row.example),
    [['a|b'], ['back\\slash']]
  )
  assert.equal(retried.commit, project.git('rev-parse', 'HEAD'))
  assert.equal(
    retried.featureBlob,
    project.git('hash-object', 'features/normal.feature')
  )
  const ordinaryReported = await run('report', [], {
    reset: false,
    record: false,
  })
  assert.equal(ordinaryReported.code, baseline.code, ordinaryReported.output)
  const ordinaryMessageTypes = readFileSync(
    join(project.root, 'existing.ndjson'),
    'utf8'
  )
    .trim()
    .split('\n')
    .flatMap((line) => Object.keys(JSON.parse(line)))
    .sort()
  const dry = await run('dry')
  assert.equal(dry.code, 0, dry.output)
  assert.deepEqual(dry.proof, [])
  const focused = await run('enabled', [
    '--spec',
    `${project.git('rev-parse', '--show-toplevel')}/features/normal.feature`,
  ])
  assert.equal(focused.code, 2, focused.output)
  assert.equal(focused.proof.length, 3)
  const ordinaryTagged = await run('enabled', ['--expose', 'tags=@selected'], {
    record: false,
  })
  assert.equal(ordinaryTagged.code, 0, ordinaryTagged.output)
  const tagged = await run('enabled', ['--expose', 'tags=@selected'])
  assert.equal(tagged.code, 0, tagged.output)
  assert.equal(tagged.proof.length, 2)
  assert.deepEqual(reporter(tagged.output), reporter(ordinaryTagged.output))
  assert.deepEqual(tagged.owners, ordinaryTagged.owners)
  const reported = await run('report')
  assert.equal(reported.code, baseline.code, reported.output)
  assert.equal(reported.proof.length, 3)
  assert.deepEqual(reporter(reported.output), reporter(ordinaryReported.output))
  assert.deepEqual(reported.owners, ordinaryReported.owners)
  const messages = readFileSync(join(project.root, 'existing.ndjson'), 'utf8')
    .trim()
    .split('\n')
    .map(JSON.parse)
  assert.ok(messages.some((message) => message.testCaseFinished))
  assert.deepEqual(messages.flatMap(Object.keys).sort(), ordinaryMessageTypes)
  const selection = ['--expose', 'tags=@permanent']
  const ordinaryPassed = await run('recover', selection, {
    reset: false,
    record: false,
  })
  assert.equal(ordinaryPassed.code, 0, ordinaryPassed.output)
  const retained = reported.proof
  const passed = await run('recover', selection, { reset: false })
  assert.equal(passed.code, 0, passed.output)
  assert.deepEqual(reporter(passed.output), reporter(ordinaryPassed.output))
  assert.deepEqual(passed.owners, ordinaryPassed.owners)
  assert.deepEqual(
    passed.proof.map((row) => row.scenario),
    ['Parsed <value>', 'Parsed <value>', 'Permanent failure', 'Retry succeeds']
  )
  const failed = await run('enabled', selection, { reset: false })
  assert.equal(failed.code, 1, failed.output)
  assert.deepEqual(failed.proof, retained)
  const recovered = await run('recover', selection, { reset: false })
  assert.equal(recovered.code, 0, recovered.output)
  assert.deepEqual(
    recovered.proof
      .filter((row) => row.scenario === 'Permanent failure')
      .map((row) => row.outcome),
    ['passed']
  )
  assert.deepEqual(
    recovered.proof.filter((row) => row.scenario !== 'Permanent failure'),
    retained
  )
})
