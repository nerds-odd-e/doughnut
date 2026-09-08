import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

const LIFECYCLE_SCRIPTS = [
  'preinstall',
  'install',
  'postinstall',
  'prepare',
  'preprepare',
  'postprepare',
]

function workspacePackageDirs(workspaceYaml) {
  const dirs = []
  let inPackages = false
  for (const line of workspaceYaml.split('\n')) {
    if (/^packages:\s*$/.test(line)) {
      inPackages = true
      continue
    }
    if (!inPackages) {
      continue
    }
    const item = line.match(/^\s+-\s+(\S+)\s*$/)
    if (item) {
      dirs.push(item[1])
      continue
    }
    if (line.trim() !== '' && !/^\s/.test(line)) {
      inPackages = false
    }
  }
  return dirs
}

function declaresSyncpack(pkg) {
  return ['dependencies', 'devDependencies', 'optionalDependencies'].some(
    (key) => Object.hasOwn(pkg[key] ?? {}, 'syncpack')
  )
}

function commandHeads(script) {
  return script
    .split(/(?:&&|\|\||;|\n)/)
    .map((part) => part.trim().split(/\s+/)[0])
    .filter(Boolean)
}

async function workspaceState() {
  const workspaceYaml = await readFile(
    path.join(repoRoot, 'pnpm-workspace.yaml'),
    'utf8'
  )
  const dirs = workspacePackageDirs(workspaceYaml)
  const packagePaths = [
    'package.json',
    ...dirs.map((dir) => path.join(dir, 'package.json')),
  ]
  const packages = []
  for (const relativePath of packagePaths) {
    packages.push({
      relativePath,
      pkg: JSON.parse(
        await readFile(path.join(repoRoot, relativePath), 'utf8')
      ),
    })
  }
  return { dirs, packages }
}

function syncpackLifecycleInvocations(packages) {
  const invocations = []
  for (const { relativePath, pkg } of packages) {
    const declared = declaresSyncpack(pkg)
    for (const scriptName of LIFECYCLE_SCRIPTS) {
      const script = pkg.scripts?.[scriptName]
      if (typeof script !== 'string') {
        continue
      }
      if (commandHeads(script).includes('syncpack')) {
        invocations.push({
          relativePath,
          scriptName,
          script,
          declared,
        })
      }
    }
  }
  return invocations
}

test('lifecycle scripts invoke PATH syncpack only where the package declares it', async () => {
  const { dirs, packages } = await workspaceState()
  assert.deepEqual(
    ['cli', 'frontend', 'mcp-server'].filter((dir) => !dirs.includes(dir)),
    []
  )

  const unsafe = syncpackLifecycleInvocations(packages)
    .filter((row) => !row.declared)
    .map((row) => `${row.relativePath} ${row.scriptName}: ${row.script}`)

  assert.deepEqual(unsafe, [])
})

test('root postinstall runs syncpack after declaring it', async () => {
  const { packages } = await workspaceState()
  const root = packages.find((row) => row.relativePath === 'package.json')?.pkg

  assert.equal(typeof root?.devDependencies?.syncpack, 'string')
  assert.equal(root?.scripts?.postinstall, 'syncpack fix')
})
