import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { test } from 'node:test'
import { proofProject } from './cucumberProofTestProject.mjs'

test('interrupted concurrent processes preserve independent completed observations', async (t) => {
  const project = proofProject(t)
  project.recorder.start()
  project.source('Feature: Observe\n  Scenario: Saved\n    Given saved\n')
  const script = `
    import { createCucumberProofRecorder } from ${JSON.stringify(new URL('./cucumberProofRecorder.mjs', import.meta.url).href)}
    const recorder = createCucumberProofRecorder({ repoRoot: process.env.PROOF_ROOT })
    recorder.start()
    recorder.observe('cypress-cucumber-preprocessor:spec-envelopes', { messages: [
      { source: { uri: 'features/proof.feature', data: 'Feature: Observe\\n  Scenario: Saved\\n    Given saved\\n' } },
      { gherkinDocument: { uri: 'features/proof.feature', feature: { children: [{ scenario: { id: 'scenario', name: 'Saved', examples: [] } }] } } },
      { pickle: { id: 'pickle', uri: 'features/proof.feature', astNodeIds: ['scenario'] } },
      { testCase: { id: 'case', pickleId: 'pickle' } },
    ] })
    const send = (name, data) => recorder.observe('cypress-cucumber-preprocessor:' + name, data)
    send('test-case-started', { id: 'done', testCaseId: 'case', attempt: 0 })
    send('test-step-finished', { testCaseStartedId: 'done', testStepId: 'step', testStepResult: { status: 'PASSED', duration: { seconds: 0, nanos: 1000000 } } })
    send('test-case-finished', { testCaseStartedId: 'done', willBeRetried: false, timestamp: { seconds: 1793577790, nanos: 0 } })
    recorder.observe('cypress-cucumber-preprocessor:spec-envelopes', { messages: [
      { source: { uri: 'features/proof.feature', data: 'Feature: Observe\\n  Scenario: Independent ' + process.env.PROOF_WRITER + '\\n    Given saved\\n' } },
      { gherkinDocument: { uri: 'features/proof.feature', feature: { children: [{ scenario: { id: 'independent', name: 'Independent ' + process.env.PROOF_WRITER, examples: [] } }] } } },
      { pickle: { id: 'independent', uri: 'features/proof.feature', astNodeIds: ['independent'] } },
      { testCase: { id: 'independent', pickleId: 'independent' } },
    ] })
    send('test-case-started', { id: 'own', testCaseId: 'independent', attempt: 0 })
    send('test-step-finished', { testCaseStartedId: 'own', testStepId: 'step', testStepResult: { status: 'PASSED', duration: { seconds: 0, nanos: 1000000 } } })
    send('test-case-finished', { testCaseStartedId: 'own', willBeRetried: false, timestamp: { seconds: 1793577791, nanos: 0 } })
    send('test-case-started', { id: 'unfinished', testCaseId: 'case', attempt: 0 })
    process.send('durable')
    setInterval(() => {}, 1000)
  `
  const children = [0, 1].map((writer) =>
    spawn(process.execPath, ['--input-type=module', '-e', script], {
      env: {
        ...process.env,
        PROOF_ROOT: project.root,
        PROOF_WRITER: String(writer),
        RECORD_CUCUMBER_PROOF: '1',
      },
      stdio: ['ignore', 'ignore', 'pipe', 'ipc'],
    })
  )
  t.after(() => {
    for (const child of children) child.kill('SIGTERM')
  })
  await Promise.all(children.map((child) => once(child, 'message')))
  assert.deepEqual(project.files(), ['latest.json'])
  const exited = children.map((child) => once(child, 'exit'))
  for (const child of children) child.kill('SIGTERM')
  await Promise.all(exited)
  assert.deepEqual(
    project.proof().map((observation) => observation.scenario),
    ['Independent 0', 'Independent 1', 'Saved']
  )
})
