#!/usr/bin/env node
/**
 * Refresh `.nix/jdk` → JAVA_HOME from `nix develop`, for Cursor / VS Code when the IDE is not
 * started from an active nix shell. Run: `pnpm setupCursorDev` (or prefixed with CURSOR_DEV + nix develop).
 */

import { existsSync, mkdirSync, rmSync, symlinkSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

function repoRoot() {
  return dirname(dirname(fileURLToPath(import.meta.url)))
}

function resolveJavaHomeViaNix(cwd, env) {
  const resolved = spawnSync(
    'nix',
    [
      'develop',
      '--accept-flake-config',
      '-c',
      'bash',
      '-lc',
      'printf "%s" "${JAVA_HOME}"',
    ],
    {
      cwd,
      encoding: 'utf8',
      env: {
        ...env,
        CURSOR_DEV: 'true',
      },
      shell: false,
    }
  )
  if (resolved.status !== 0) {
    const err = `${resolved.stderr || ''}${resolved.stdout || ''}`.trim()
    throw new Error(
      `nix develop failed (${resolved.status}). Is Nix installed and is this flake valid?\n${err}`
    )
  }
  const stdout = `${resolved.stdout}`.trim()
  const candidates = stdout
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean)
  const pick = [...candidates]
    .reverse()
    .find((l) => /^\/nix\/store\/.+/.test(l))
  if (!pick) {
    throw new Error(`Could not find JAVA_HOME in nix shell output:\n${stdout}`)
  }
  return pick
}

function ensureJdkBins(home) {
  const javaBin = join(home, 'bin', 'java')
  if (!existsSync(javaBin)) {
    throw new Error(`JAVA_HOME invalid (missing ${javaBin}): ${home}`)
  }
}

function atomicSymlink({ target, linkPath }) {
  mkdirSync(dirname(linkPath), { recursive: true })
  if (existsSync(linkPath)) {
    rmSync(linkPath)
  }
  symlinkSync(target, linkPath)
}

function refreshNixJavaLink(cwd = repoRoot()) {
  const javaHome = resolveJavaHomeViaNix(cwd, process.env)
  ensureJdkBins(javaHome)
  const linkPath = join(cwd, '.nix', 'jdk')
  atomicSymlink({ target: javaHome, linkPath })
  return { linkPath, javaHome }
}

try {
  const { linkPath, javaHome } = refreshNixJavaLink()
  console.error(`${linkPath} -> ${javaHome}`)
  process.exit(0)
} catch (error) {
  console.error(`${error}`)
  console.error('')
  console.error('Fix: ensure Nix is installed, then run again from repo root.')
  console.error(
    'Equivalent: CURSOR_DEV=true nix develop -c \'printf "%s" "${JAVA_HOME}"\''
  )
  process.exit(1)
}
