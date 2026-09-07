#!/usr/bin/env node
/**
 * Collapse two-or-more trailing newlines to a single POSIX newline.
 * Cursor/VS Code "insert final newline" and some apply/undo paths write a
 * blank last line; files.trimFinalNewlines does not remove just one.
 */
import { Buffer } from 'node:buffer'
import fs from 'node:fs'
import path from 'node:path'

const LF = 0x0a
const MAX_BYTES = 8 * 1024 * 1024

function readStdin() {
  return new Promise((resolve, reject) => {
    const chunks = []
    process.stdin.on('data', (c) => chunks.push(c))
    process.stdin.on('end', () =>
      resolve(Buffer.concat(chunks).toString('utf8'))
    )
    process.stdin.on('error', reject)
  })
}

function resolveEditedPath(payload, cwd) {
  const raw = payload.file_path || payload.filePath || payload.path
  if (typeof raw !== 'string' || raw.length === 0) return null
  const roots = Array.isArray(payload.workspace_roots)
    ? payload.workspace_roots.filter((r) => typeof r === 'string')
    : []
  const base = roots[0] || cwd
  return path.resolve(base, raw)
}

function isInside(root, candidate) {
  const rel = path.relative(root, candidate)
  return rel === '' || (!rel.startsWith(`..${path.sep}`) && rel !== '..')
}

function collapseExtraTrailingNewlines(buf) {
  if (buf.length === 0 || buf.includes(0)) return null
  let end = buf.length
  while (end > 0 && buf[end - 1] === LF) end -= 1
  const trailing = buf.length - end
  if (trailing <= 1) return null
  return Buffer.concat([buf.subarray(0, end), Buffer.from([LF])])
}

const cwd = process.cwd()
const payload = JSON.parse((await readStdin()) || '{}')
const filePath = resolveEditedPath(payload, cwd)
if (!(filePath && isInside(cwd, filePath))) process.exit(0)

let buf
try {
  const stat = fs.statSync(filePath)
  if (!stat.isFile() || stat.size > MAX_BYTES) process.exit(0)
  buf = fs.readFileSync(filePath)
} catch {
  process.exit(0)
}

const next = collapseExtraTrailingNewlines(buf)
if (next) fs.writeFileSync(filePath, next)
process.exit(0)
