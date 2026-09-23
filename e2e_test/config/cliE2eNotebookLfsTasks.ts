/**
 * Cypress tasks: standard Git LFS client against a notebook LFS endpoint,
 * plus clone-checkout observations for hydrated LFS tips.
 */

import { createHash } from 'node:crypto'
import {
  existsSync,
  mkdtempSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'

export type NotebookLfsClientInput = {
  lfsUrl: string
  token: string
  payload: string
}

function runOrThrow(
  command: string,
  args: string[],
  cwd: string,
  env?: NodeJS.ProcessEnv
): string {
  const result = spawnSync(command, args, {
    cwd,
    env: { ...process.env, ...env },
    encoding: 'utf8',
  })
  if (result.status !== 0) {
    throw new Error(
      `${command} ${args.join(' ')} failed (status ${result.status}):\n` +
        `${result.stdout ?? ''}\n${result.stderr ?? ''}`
    )
  }
  return `${result.stdout ?? ''}${result.stderr ?? ''}`
}

function gitLfsVersion(): string {
  const result = spawnSync('git', ['lfs', 'version'], { encoding: 'utf8' })
  if (result.status !== 0) {
    throw new Error(
      `git lfs is not available (status ${result.status}): ${result.stderr ?? result.stdout ?? ''}`
    )
  }
  return (result.stdout || result.stderr || '').trim()
}

function sha256Hex(bytes: Buffer): string {
  return createHash('sha256').update(bytes).digest('hex')
}

function prepareLfsRepo(payload: string): {
  dir: string
  oid: string
  bytes: Buffer
} {
  const dir = mkdtempSync(join(tmpdir(), 'donut-lfs-'))
  const bytes = Buffer.from(payload, 'utf8')
  const oid = sha256Hex(bytes)
  runOrThrow('git', ['init'], dir)
  runOrThrow('git', ['lfs', 'install', '--local'], dir)
  runOrThrow('git', ['config', 'user.email', 'lfs-e2e@example.com'], dir)
  runOrThrow('git', ['config', 'user.name', 'LFS E2E'], dir)
  writeFileSync(join(dir, 'payload.bin'), bytes)
  runOrThrow('git', ['lfs', 'track', '*.bin'], dir)
  runOrThrow('git', ['add', '.gitattributes', 'payload.bin'], dir)
  runOrThrow('git', ['commit', '-m', 'lfs payload'], dir)
  return { dir, oid, bytes }
}

function configureLfsEndpoint(
  dir: string,
  lfsUrl: string,
  token: string
): void {
  runOrThrow('git', ['config', 'lfs.url', lfsUrl], dir)
  runOrThrow(
    'git',
    ['config', 'http.extraHeader', `Authorization: Bearer ${token}`],
    dir
  )
  runOrThrow(
    'git',
    ['remote', 'add', 'origin', 'http://example.invalid/repo.git'],
    dir
  )
}

function listLfsObjectOids(checkoutDir: string): string[] {
  const objectsRoot = join(checkoutDir, '.git', 'lfs', 'objects')
  if (!existsSync(objectsRoot)) {
    return []
  }
  const oids: string[] = []
  for (const a of readdirSync(objectsRoot)) {
    const levelA = join(objectsRoot, a)
    for (const b of readdirSync(levelA)) {
      const levelB = join(levelA, b)
      for (const name of readdirSync(levelB)) {
        if (/^[a-f0-9]{64}$/.test(name)) {
          oids.push(name)
        }
      }
    }
  }
  return oids.sort()
}

export function createCliE2eNotebookLfsTasks() {
  return {
    notebookLfsStandardClientRoundTrip(input: NotebookLfsClientInput) {
      const version = gitLfsVersion()
      const { dir, oid, bytes } = prepareLfsRepo(input.payload)
      try {
        configureLfsEndpoint(dir, input.lfsUrl, input.token)
        runOrThrow('git', ['lfs', 'push', '--object-id', 'origin', oid], dir)
        rmSync(join(dir, '.git', 'lfs', 'objects'), {
          recursive: true,
          force: true,
        })
        rmSync(join(dir, 'payload.bin'), { force: true })
        runOrThrow('git', ['lfs', 'fetch', 'origin'], dir)
        runOrThrow('git', ['lfs', 'checkout', 'payload.bin'], dir)
        const downloaded = readFileSync(join(dir, 'payload.bin'))
        const downloadedDigest = sha256Hex(downloaded)
        if (!downloaded.equals(bytes) || downloadedDigest !== oid) {
          throw new Error(
            `downloaded bytes/digest mismatch: expected oid ${oid} (${bytes.length} bytes), ` +
              `got ${downloadedDigest} (${downloaded.length} bytes)`
          )
        }
        return { gitLfsVersion: version, oid, size: bytes.length }
      } finally {
        rmSync(dir, { recursive: true, force: true })
      }
    },
    notebookLfsStandardClientAuthDenial(input: NotebookLfsClientInput) {
      const version = gitLfsVersion()
      const { dir, oid } = prepareLfsRepo(input.payload)
      try {
        configureLfsEndpoint(dir, input.lfsUrl, input.token)
        const result = spawnSync(
          'git',
          ['lfs', 'push', '--object-id', 'origin', oid],
          { cwd: dir, encoding: 'utf8' }
        )
        return {
          gitLfsVersion: version,
          oid,
          output: `${result.stdout ?? ''}\n${result.stderr ?? ''}`,
          status: result.status,
        }
      } finally {
        rmSync(dir, { recursive: true, force: true })
      }
    },
    listCliNotebookCheckoutLfsObjectOids(checkoutDir: string): string[] {
      return listLfsObjectOids(checkoutDir)
    },
    readCliNotebookCheckoutGitConfig({
      checkoutDir,
      key,
    }: {
      checkoutDir: string
      key: string
    }): string {
      return runOrThrow(
        'git',
        ['config', '--local', '--get', key],
        checkoutDir
      ).trim()
    },
    cliNotebookCheckoutTracksToken({
      checkoutDir,
      token,
    }: {
      checkoutDir: string
      token: string
    }): { tracked: boolean; matchedPath?: string } {
      const tracked = runOrThrow('git', ['ls-files', '-z'], checkoutDir)
        .split('\0')
        .filter((path) => path.length > 0)
      for (const relativePath of tracked) {
        const bytes = readFileSync(join(checkoutDir, relativePath))
        if (bytes.toString('utf8').includes(token)) {
          return { tracked: true, matchedPath: relativePath }
        }
      }
      return { tracked: false }
    },
    createCliNotebookCloneDestinationWithFile({
      relativePath,
      content,
    }: {
      relativePath: string
      content: string
    }): string {
      const parent = mkdtempSync(join(tmpdir(), 'cypress-cli-clone-'))
      const destination = join(parent, 'checkout')
      mkdirSync(destination, { recursive: true })
      writeFileSync(join(destination, relativePath), content)
      return destination
    },
  }
}
