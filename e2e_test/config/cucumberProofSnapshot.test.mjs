import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { test } from 'node:test'
import { proofProject } from './cucumberProofTestProject.mjs'

const source = 'Feature: Proof\n  Scenario: Selected\n    Given saved\n'

function legacy(project, name, observations) {
  const file = join(project.root, 'proof', name)
  mkdirSync(join(file, '..'), { recursive: true })
  writeFileSync(file, JSON.stringify({ version: 1, observations }))
}

test('refresh reduces nested legacy observations latest-first and preserves unrelated provenance', (t) => {
  const p = proofProject(t)
  p.recorder.start()
  const [selected] = p.source(source)
  p.attempt(selected)
  const [original] = p.proof()
  const unrelated = { ...original, scenario: 'Unexecuted' }
  const newer = { ...original, finishedAt: '2099-01-01T00:00:00.000Z' }
  const invalidated = { ...original, scenario: 'Invalidated' }
  legacy(p, 'nested/earlier.json', [unrelated, invalidated])
  legacy(p, 'later.json', [
    newer,
    {
      ...invalidated,
      outcome: 'failed',
      finishedAt: '2026-11-02T00:03:11.500Z',
    },
  ])
  p.attempt(selected, { seconds: 1793577792 })
  assert.deepEqual(p.files(), ['latest.json'])
  assert.deepEqual(p.proof(), [newer, unrelated])
})

for (const failureFirst of [true, false]) {
  test(`an unsuccessful legacy timestamp tie wins with failure ${failureFirst ? 'first' : 'last'}`, (t) => {
    const p = proofProject(t)
    p.recorder.start()
    const [selected] = p.source(source)
    p.attempt(selected)
    const [original] = p.proof()
    const passed = { ...original, scenario: 'Tied' }
    const failed = { ...passed, outcome: 'failed' }
    legacy(p, 'a.json', [failureFirst ? failed : passed])
    legacy(p, 'nested/z.json', [failureFirst ? passed : failed])
    p.attempt(selected, { seconds: 1793577791 })
    assert.deepEqual(p.proof(), [
      { ...original, finishedAt: '2026-11-02T00:03:11.500Z' },
    ])
  })
}

test('a completed observation respects a newer legacy failure and an unsuccessful equal-time result', (t) => {
  const p = proofProject(t)
  p.recorder.start()
  const [selected] = p.source(source)
  p.attempt(selected)
  const [original] = p.proof()
  legacy(p, 'nested/failed.json', [
    {
      ...original,
      outcome: 'failed',
      finishedAt: '2026-11-02T00:03:11.500Z',
    },
  ])
  p.attempt(selected)
  assert.deepEqual(p.proof(), [])
  legacy(p, 'equal.json', [{ ...original, outcome: 'failed' }])
  p.attempt(selected)
  assert.deepEqual(p.proof(), [])
})
