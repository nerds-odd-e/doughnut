import { execFileSync } from 'node:child_process'
import {
  mkdirSync,
  mkdtempSync,
  readdirSync,
  readFileSync,
  realpathSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { createRequire } from 'node:module'
import { tmpdir } from 'node:os'
import { join, relative } from 'node:path'
import { createCucumberProofRecorder } from './cucumberProofRecorder.mjs'

const require = createRequire(import.meta.url)
const dependencyRequire = createRequire(
  require.resolve('@badeball/cypress-cucumber-preprocessor')
)
const { generateMessages } = dependencyRequire('@cucumber/gherkin')
const { IdGenerator, SourceMediaType } = dependencyRequire('@cucumber/messages')

export function proofProject(t) {
  const root = mkdtempSync(join(tmpdir(), 'donut-cucumber-proof-test-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const git = (...args) =>
    execFileSync('git', args, {
      cwd: root,
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'ignore'],
    }).trim()
  git('init', '--quiet')
  mkdirSync(join(root, '.planning'))
  writeFileSync(
    join(root, '.planning/open-dough.json'),
    JSON.stringify({
      intent: {
        locations: [{ path: 'features', kind: 'gherkin', proof: 'proof' }],
      },
    })
  )
  git('add', '.')
  git(
    '-c',
    'user.name=Proof Fixture',
    '-c',
    'user.email=proof@example.test',
    'commit',
    '--quiet',
    '-m',
    'Starting point'
  )
  const recorder = createCucumberProofRecorder({ repoRoot: root })
  const send = (event, data) =>
    recorder.observe(`cypress-cucumber-preprocessor:${event}`, data)
  let sequence = 0
  return {
    root,
    git,
    recorder,
    send,
    source(content, uri = 'features/proof.feature') {
      mkdirSync(join(root, uri, '..'), { recursive: true })
      writeFileSync(join(root, uri), content)
      const sourceUri = relative(root, realpathSync(join(root, uri)))
      const messages = generateMessages(
        content,
        sourceUri,
        SourceMediaType.TEXT_X_CUCUMBER_GHERKIN_PLAIN,
        {
          includeSource: true,
          includeGherkinDocument: true,
          includePickles: true,
          newId: IdGenerator.incrementing(),
        }
      )
      const pickles = messages
        .filter((message) => message.pickle)
        .map(({ pickle }) => pickle)
      messages.push(
        ...pickles.map((pickle) => ({
          testCase: { id: `case-${pickle.id}`, pickleId: pickle.id },
        }))
      )
      this.messages = messages
      send('spec-envelopes', { messages })
      return pickles
    },
    attempt(
      pickle,
      {
        index = 0,
        results = ['PASSED'],
        durations = [100],
        retry = false,
        seconds = 1793577790,
      } = {},
      observe = send
    ) {
      const id = `attempt-${sequence++}`
      observe('test-case-started', {
        id,
        testCaseId: `case-${pickle.id}`,
        attempt: index,
        timestamp: { seconds, nanos: 0 },
      })
      results.forEach((status, step) =>
        observe('test-step-finished', {
          testCaseStartedId: id,
          testStepId: `step-${step}`,
          testStepResult: {
            status,
            duration: { seconds: 0, nanos: (durations[step] ?? 0) * 1e6 },
          },
        })
      )
      observe('test-case-finished', {
        testCaseStartedId: id,
        willBeRetried: retry,
        timestamp: { seconds, nanos: 500000000 },
      })
    },
    files() {
      try {
        return readdirSync(join(root, 'proof'), {
          recursive: true,
          encoding: 'utf8',
        }).filter((name) => name.endsWith('.json'))
      } catch {
        return []
      }
    },
    proof() {
      return this.files().flatMap(
        (name) =>
          JSON.parse(readFileSync(join(root, 'proof', name), 'utf8'))
            .observations
      )
    },
  }
}
