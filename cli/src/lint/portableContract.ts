import { dirname, posix } from 'node:path'
import { unsafePathReason } from '../sync/previewPullActions.js'
import type { Finding } from './lintReport.js'

const MARKDOWN_SUFFIX = '.md'
const RESERVED_BASENAMES = new Set(['index.md', 'log.md'])
const MD_LINK = /\[([^\]]*)\]\(([^)\s]+)(?:\s+"[^"]*")?\)/g
const WIKI_LINK = /\[\[([^\]]+)]]/g

function basename(path: string): string {
  const slash = path.lastIndexOf('/')
  return slash === -1 ? path : path.slice(slash + 1)
}

function isConceptPath(path: string): boolean {
  return (
    path.endsWith(MARKDOWN_SUFFIX) && !RESERVED_BASENAMES.has(basename(path))
  )
}

function isRemoteOrIgnoredHref(href: string): boolean {
  const t = href.trim()
  if (/^https?:\/\//i.test(t)) return true
  if (/^\/attachments\//i.test(t)) return true
  return false
}

function stripHash(href: string): string {
  const hash = href.indexOf('#')
  return hash === -1 ? href : href.slice(0, hash)
}

/** Leading `/` that is not remote → workspace-root-relative. */
function localTargetFromHref(href: string): string {
  let t = stripHash(href.trim())
  if (t.startsWith('/')) t = t.slice(1)
  return t
}

function wikiTarget(inner: string): string {
  const pipe = inner.indexOf('|')
  return (pipe === -1 ? inner : inner.slice(0, pipe)).trim()
}

function lineAt(content: string, index: number): number {
  return content.slice(0, index).split('\n').length
}

function resolveCandidates(sourcePath: string, target: string): string[] {
  if (target === '') return []
  const sourceDir = dirname(sourcePath)
  const fromSource =
    sourceDir === '.' ? target : posix.normalize(`${sourceDir}/${target}`)
  const fromRoot = posix.normalize(target)
  const candidates = new Set<string>()
  for (const base of [fromSource, fromRoot]) {
    const cleaned = base.replace(/^\.\/+/, '').replace(/\/+$/, '')
    if (cleaned === '' || cleaned === '.') continue
    candidates.add(cleaned)
    if (!cleaned.endsWith(MARKDOWN_SUFFIX)) {
      candidates.add(`${cleaned}${MARKDOWN_SUFFIX}`)
    }
  }
  return [...candidates]
}

function targetExists(
  notes: ReadonlyMap<string, string>,
  candidates: readonly string[]
): boolean {
  return candidates.some((candidate) => notes.has(candidate))
}

function missingIndexFindings(notes: ReadonlyMap<string, string>): Finding[] {
  const dirsWithConcepts = new Set<string>()
  for (const path of notes.keys()) {
    if (!isConceptPath(path)) continue
    const dir = dirname(path)
    dirsWithConcepts.add(dir === '.' ? '' : dir)
  }
  const findings: Finding[] = []
  for (const dir of dirsWithConcepts) {
    const indexPath = dir === '' ? 'index.md' : `${dir}/index.md`
    if (notes.has(indexPath)) continue
    findings.push({
      path: indexPath,
      severity: 'error',
      message: 'Missing index.md in a directory that contains concepts',
    })
  }
  return findings
}

function unsafeWorkspacePathFindings(
  notes: ReadonlyMap<string, string>
): Finding[] {
  const findings: Finding[] = []
  for (const path of notes.keys()) {
    const reason = unsafePathReason(path)
    if (reason === undefined) continue
    findings.push({ path, severity: 'error', message: reason })
  }
  return findings
}

function reportLocalTarget(
  sourcePath: string,
  targetRaw: string,
  line: number,
  notes: ReadonlyMap<string, string>,
  into: Finding[]
): void {
  const reason = unsafePathReason(targetRaw)
  if (reason !== undefined) {
    into.push({
      path: sourcePath,
      severity: 'error',
      line,
      message: `${reason} (link target ${targetRaw})`,
    })
    return
  }
  const candidates = resolveCandidates(sourcePath, targetRaw)
  if (targetExists(notes, candidates)) return
  into.push({
    path: sourcePath,
    severity: 'error',
    line,
    message: `Broken local link — missing target ${targetRaw}`,
  })
}

function linkFindings(notes: ReadonlyMap<string, string>): Finding[] {
  const findings: Finding[] = []
  for (const [path, content] of notes) {
    if (!path.endsWith(MARKDOWN_SUFFIX)) continue

    for (const match of content.matchAll(MD_LINK)) {
      const href = match[2]?.trim()
      if (href === undefined || isRemoteOrIgnoredHref(href)) continue
      const target = localTargetFromHref(href)
      const line = lineAt(content, match.index ?? 0)
      reportLocalTarget(path, target, line, notes, findings)
    }

    for (const match of content.matchAll(WIKI_LINK)) {
      const inner = match[1]
      if (inner === undefined) continue
      const target = wikiTarget(inner)
      if (target === '' || isRemoteOrIgnoredHref(target)) continue
      const line = lineAt(content, match.index ?? 0)
      const asPath = localTargetFromHref(target)
      reportLocalTarget(path, asPath, line, notes, findings)
    }
  }
  return findings
}

/** Portable knowledge-contract findings on top of OKF per-file rules. */
export function portableContractFindings(
  notes: ReadonlyMap<string, string>
): Finding[] {
  return [
    ...linkFindings(notes),
    ...missingIndexFindings(notes),
    ...unsafeWorkspacePathFindings(notes),
  ]
}
