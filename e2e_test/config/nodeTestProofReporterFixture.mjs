// Shared scratch checkouts and node --test child runners for the recorder
// checks. Only the inner runs set RECORD_NODE_TEST_PROOF.
import assert from 'node:assert/strict'
import { spawn, spawnSync } from 'node:child_process'
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const configDir = path.dirname(fileURLToPath(import.meta.url))
export const recorder = path.join(configDir, 'nodeTestProofReporter.mjs')

const childEnvBase = { ...process.env }
delete childEnvBase.NODE_TEST_CONTEXT
delete childEnvBase.RECORD_NODE_TEST_PROOF
delete childEnvBase.NODE_OPTIONS

export function git(cwd, args) {
  const result = spawnSync('git', args, {
    cwd,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  })
  assert.equal(
    result.status,
    0,
    `git ${args.join(' ')} failed: ${result.stderr}`
  )
  return result.stdout.trim()
}

function writeOpenDough(fixture, locations) {
  mkdirSync(path.join(fixture, '.planning'), { recursive: true })
  writeFileSync(
    path.join(fixture, '.planning', 'open-dough.json'),
    `${JSON.stringify({ intent: { locations } }, null, 2)}\n`
  )
}

export function scratchCheckout(
  t,
  { covered = 'scripts', proof = 'scripts/proof', inGit = true } = {}
) {
  const fixture = mkdtempSync(path.join(tmpdir(), 'donut-node-test-proof-'))
  t.after(() => rmSync(fixture, { recursive: true, force: true }))
  mkdirSync(path.join(fixture, covered), { recursive: true })
  writeOpenDough(fixture, [
    {
      path: covered,
      kind: 'javascript',
      include: ['**/*.test.mjs'],
      runner: 'node:test',
      proof,
      proofGeneration: {
        command: 'RECORD_NODE_TEST_PROOF=1 node --test <files>',
      },
    },
    {
      path: 'infra',
      kind: 'javascript',
      include: ['**/*.test.mjs'],
      runner: 'node:test',
      proof: 'infra/proof',
      proofGeneration: {
        command: 'RECORD_NODE_TEST_PROOF=1 node --test <files>',
      },
    },
  ])
  if (inGit) {
    git(fixture, ['init'])
    git(fixture, ['config', 'user.email', 'node-test-proof@example.com'])
    git(fixture, ['config', 'user.name', 'Node Test Proof'])
    git(fixture, ['add', '.'])
    git(fixture, ['commit', '-m', 'seed'])
  }
  return fixture
}

export function writeTest(fixture, relativeFile, source) {
  const absolute = path.join(fixture, relativeFile)
  mkdirSync(path.dirname(absolute), { recursive: true })
  writeFileSync(absolute, source)
  return absolute
}

export function readLatest(fixture, proof = 'scripts/proof') {
  const file = path.join(fixture, proof, 'latest.json')
  if (!existsSync(file)) return
  return JSON.parse(readFileSync(file, 'utf8'))
}

export function seedObservations(
  fixture,
  observations,
  proof = 'scripts/proof'
) {
  const directory = path.join(fixture, proof)
  mkdirSync(directory, { recursive: true })
  writeFileSync(
    path.join(directory, 'latest.json'),
    `${JSON.stringify({ version: 1, observations }, null, 2)}\n`
  )
}

export function passedObservation(file, testNames, commit, fileBlob) {
  return {
    file,
    test: testNames,
    outcome: 'passed',
    attempts: 1,
    durationMs: 1,
    finishedAt: '2026-11-04T12:00:00.000Z',
    commit,
    fileBlob,
  }
}

export function maskTiming(text) {
  return text
    .replace(/\d+(?:\.\d+)?m?s\b/g, '<time>')
    .replace(/duration_ms \d+(?:\.\d+)?/g, 'duration_ms <time>')
    .replace(/\d{4}-\d{2}-\d{2}T[\d:.]+Z/g, '<iso>')
}

export function runNode(
  fixture,
  files,
  { record = false, extraArgs = [], reporters = true, env = {} } = {}
) {
  const envForChild = { ...childEnvBase, ...env }
  if (record) envForChild.RECORD_NODE_TEST_PROOF = '1'
  else delete envForChild.RECORD_NODE_TEST_PROOF
  const args = ['--test']
  if (reporters) {
    args.push(
      '--test-reporter=spec',
      '--test-reporter-destination=stdout',
      `--test-reporter=${recorder}`,
      '--test-reporter-destination=stdout'
    )
  }
  args.push(...extraArgs, ...files.map((file) => path.join(fixture, file)))
  return spawnSync(process.execPath, args, {
    cwd: fixture,
    encoding: 'utf8',
    env: envForChild,
  })
}

export function spawnNode(fixture, relativeFile, { record = false } = {}) {
  const envForChild = { ...childEnvBase }
  if (record) envForChild.RECORD_NODE_TEST_PROOF = '1'
  else delete envForChild.RECORD_NODE_TEST_PROOF
  return spawn(
    process.execPath,
    [
      '--test',
      '--test-reporter=spec',
      '--test-reporter-destination=stdout',
      `--test-reporter=${recorder}`,
      '--test-reporter-destination=stdout',
      path.join(fixture, relativeFile),
    ],
    { cwd: fixture, env: envForChild, stdio: 'ignore' }
  )
}
