// Run with: CURSOR_DEV=true nix develop -c node scripts/acceptance/cli-publication-update.mjs --predecessor <authentic-0.5.9-binary>
// The required predecessor is a private input, never downloaded or installed on the host.
import assert from 'node:assert/strict'
import { execFile, execFileSync } from 'node:child_process'
import {
  chmodSync,
  copyFileSync,
  mkdirSync,
  readFileSync,
  writeFileSync,
} from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { parseArgs, promisify } from 'node:util'
import { test } from 'node:test'
import { parse } from 'yaml'
import {
  hash,
  makePublication,
  readApplicationRecords,
} from '../ci/application-release-publication-fixtures.mjs'
import {
  installPublicationStore,
  servePublicationStore,
} from './cliPublicationStore.mjs'
import { makeLargeNotebookCheckout } from './largeNotebookCheckout.mjs'

const { values } = parseArgs({ options: { predecessor: { type: 'string' } } })
assert.ok(values.predecessor, 'Required --predecessor <authentic-0.5.9-binary>')
const predecessorHash =
  '5b868908f33576604389ad9b41f26a21eaa6efef982c570e2a60488d3365525c'
assert.equal(hash(readFileSync(values.predecessor)), predecessorHash)
const repositoryRoot = fileURLToPath(new URL('../../', import.meta.url))
const run = promisify(execFile)

test('authentic installation updates to a usable tag-versioned CLI after application publication', async (t) => {
  const publication = makePublication(t)
  const { root } = publication
  const store = installPublicationStore(root)
  const notebook = makeLargeNotebookCheckout(root)
  const { origin, posts } = await servePublicationStore(t, store, notebook)
  notebook.bind(origin)
  const configDir = join(root, 'config')
  mkdirSync(configDir)
  writeFileSync(
    join(configDir, 'access-tokens.json'),
    JSON.stringify({ token: 'acceptance-token' })
  )
  const env = {
    ...process.env,
    BASE_URL: origin,
    DONUT_API_BASE_URL: origin,
    DONUT_CONFIG_DIR: configDir,
    TMPDIR: root,
  }
  const workflow = parse(
    readFileSync(
      join(repositoryRoot, '.github/workflows/cli-release.yml'),
      'utf8'
    )
  )
  const steps = workflow.jobs['build-and-publish'].steps
  const tag = 'refs/tags/cli-v0.5.28'
  const versionEnv = join(root, 'release.env')
  execFileSync(
    'bash',
    ['-c', steps.find((step) => step.name === 'Set CLI version from tag').run],
    {
      env: { ...env, GITHUB_REF: tag, GITHUB_ENV: versionEnv },
    }
  )
  const version = readFileSync(versionEnv, 'utf8').trim().split('=')[1]
  assert.equal(version, '0.5.28')
  const bundle = join(root, 'cli/dist/donut-cli.bundle.mjs')
  mkdirSync(join(root, 'cli/dist'), { recursive: true })
  execFileSync(
    'bash',
    ['-c', steps.find((step) => step.name === 'Build CLI').run],
    {
      cwd: repositoryRoot,
      env: { ...env, CLI_VERSION: version, CLI_BUNDLE_OUTFILE: bundle },
      stdio: 'pipe',
    }
  )
  const builtDigest = hash(readFileSync(bundle))
  assert.equal(
    (await run(process.execPath, [bundle, 'version'], { env })).stdout.trim(),
    `donut ${version}`
  )
  const upload = steps.find(
    (step) => step.name === 'Upload CLI binary to GCS (public read)'
  ).run
  execFileSync('bash', [join(repositoryRoot, upload)], {
    cwd: root,
    env: store.uploadEnv,
    stdio: 'pipe',
  })
  const publicBinary = store.publicFile('doughnut-cli-latest/doughnut')
  assert.equal(hash(readFileSync(publicBinary)), builtDigest)

  const application = publication.publish()
  assert.equal(application.status, 0, application.stderr)
  assert.equal(
    readFileSync(
      store.publicFile(`frontend/${publication.sha}/index.html`),
      'utf8'
    ),
    'selected SPA'
  )
  assert.equal(
    readApplicationRecords(publication.applicationRecords).at(-1).outcome,
    'succeeded'
  )
  assert.equal(hash(readFileSync(publicBinary)), builtDigest)

  const installed = join(root, 'installed-donut')
  copyFileSync(values.predecessor, installed)
  chmodSync(installed, 0o755)
  assert.equal(
    (
      await run(process.execPath, [installed, 'version'], { env })
    ).stdout.trim(),
    'donut 0.5.9'
  )
  const update = await run(process.execPath, [installed, 'update'], { env })
  assert.match(update.stdout, /Updated donut from 0\.5\.9 to 0\.5\.28/)
  assert.equal(hash(readFileSync(installed)), builtDigest)
  assert.equal(
    (
      await run(process.execPath, [installed, 'version'], { env })
    ).stdout.trim(),
    `donut ${version}`
  )
  const published = await run(
    process.execPath,
    [installed, 'notebook', 'publish', notebook.directory],
    { env }
  )
  assert.match(
    published.stdout,
    new RegExp(`Published notebook. Accepted head: ${notebook.proposed}`)
  )
  notebook.expectPublication(posts)
  console.log(
    JSON.stringify({
      predecessorHash,
      tag,
      version,
      builtDigest,
      listingBytes: notebook.listingBytes,
      accepted: notebook.accepted,
      proposed: notebook.proposed,
      boundary:
        'Real build/uploader/application publication/HTTP/update/executed replacement and native Git; external GCS/GCP/backend transport fixtures, not production backend acceptance',
    })
  )
})
