import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { test } from 'node:test'
import { createCucumberProofRecorder } from './cucumberProofRecorder.mjs'
import { proofProject } from './cucumberProofTestProject.mjs'

const source =
  'Feature: Proof\n  Scenario: Selected\n    Given saved\n  Scenario: Retained\n    Given saved\n'

test('a final failure clears its previous successes and a later pass restores proof', (t) => {
  const p = proofProject(t)
  p.recorder.start()
  const [selected, retained] = p.source(source)
  p.attempt(selected)
  p.attempt(retained)
  const other = p.proof().find((row) => row.scenario === 'Retained')
  p.recorder.start()
  p.source(source)
  p.attempt(selected, { results: ['FAILED'], seconds: 1793577791 })
  assert.deepEqual(p.proof(), [other])
  p.attempt(selected, { seconds: 1793577792 })
  assert.deepEqual(
    p
      .proof()
      .map((row) => [row.scenario, row.outcome])
      .sort(),
    [
      ['Retained', 'passed'],
      ['Selected', 'passed'],
    ]
  )
})

test('a final failure clears only its outline example', (t) => {
  const p = proofProject(t)
  p.recorder.start()
  const rows = p.source(
    'Feature: Rows\n  Scenario Outline: Save <row>\n    Given <row>\n    Examples:\n      | row |\n      | first |\n      | second |\n'
  )
  rows.forEach((row) => p.attempt(row))
  p.attempt(rows[0], { results: ['FAILED'], seconds: 1793577791 })
  assert.deepEqual(
    p.proof().map((row) => row.example),
    [['second']]
  )
})

test('cleanup reaches nested files and preserves proof newer than the unsuccessful attempt', (t) => {
  const p = proofProject(t)
  p.recorder.start()
  const [selected] = p.source(source)
  p.attempt(selected)
  const [earlier] = p.proof()
  mkdirSync(join(p.root, 'proof/nested'))
  writeFileSync(
    join(p.root, 'proof/nested/earlier.json'),
    JSON.stringify({ version: 1, observations: [earlier] })
  )
  const newer = { ...earlier, finishedAt: '2099-01-01T00:00:00.000Z' }
  writeFileSync(
    join(p.root, 'proof/newer.json'),
    JSON.stringify({ version: 1, observations: [newer] })
  )
  p.attempt(selected, { results: ['FAILED'], seconds: 1793577791 })
  assert.deepEqual(p.proof(), [newer])
})

test('a continuing writer keeps another recorder’s completed identity cleanup', (t) => {
  const p = proofProject(t)
  p.recorder.start()
  const [selected, retained] = p.source(source)
  p.attempt(selected)
  const peer = createCucumberProofRecorder({
    repoRoot: p.root,
    env: { RECORD_CUCUMBER_PROOF: '1' },
  })
  peer.start()
  peer.observe('cypress-cucumber-preprocessor:spec-envelopes', {
    messages: p.messages,
  })
  p.attempt(
    selected,
    { results: ['FAILED'], seconds: 1793577791 },
    (event, data) =>
      peer.observe(`cypress-cucumber-preprocessor:${event}`, data)
  )
  p.attempt(retained, { seconds: 1793577792 })
  assert.deepEqual(
    p.proof().map((row) => row.scenario),
    ['Retained']
  )
})

test('a non-run keeps its earlier successful proof', (t) => {
  const p = proofProject(t)
  p.recorder.start()
  const [selected] = p.source(source)
  p.attempt(selected)
  const earlier = p.proof()
  p.attempt(selected, { results: ['SKIPPED'], seconds: 1793577791 })
  assert.deepEqual(p.proof(), earlier)
})

test('recording recovers after an interrupted lock owner', async (t) => {
  const p = proofProject(t)
  const owner = spawn(
    process.execPath,
    [
      '-e',
      "require('node:fs').symlinkSync(String(process.pid), process.argv[1]); process.send('locked'); process.stdin.resume()",
      join(p.root, 'proof.lock'),
    ],
    { stdio: ['pipe', 'ignore', 'pipe', 'ipc'] }
  )
  t.after(() => {
    if (owner.exitCode === null && owner.signalCode === null)
      owner.kill('SIGKILL')
  })
  await once(owner, 'message')
  const closed = once(owner, 'exit')
  owner.kill('SIGKILL')
  await closed
  p.recorder.start()
  const [selected] = p.source(source)
  p.attempt(selected)
  assert.deepEqual(
    p.proof().map((row) => row.outcome),
    ['passed']
  )
})
