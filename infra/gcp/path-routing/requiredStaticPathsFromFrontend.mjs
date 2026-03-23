import { existsSync, readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'

const HREF_SRC_RE = /\b(?:href|src)=["'](\/[^"'?#]+)/g

/**
 * Root-absolute URL paths referenced from HTML (pathname only).
 * @param {string} html
 * @returns {string[]}
 */
export function extractRootAbsolutePathsFromHtml(html) {
  const out = new Set()
  for (const m of html.matchAll(HREF_SRC_RE)) {
    out.add(m[1])
  }
  return [...out]
}

function walkPublicFiles(publicDir, base, outPaths) {
  for (const ent of readdirSync(publicDir, { withFileTypes: true })) {
    if (ent.name.startsWith('.')) continue
    const full = path.join(publicDir, ent.name)
    if (ent.isDirectory()) {
      walkPublicFiles(full, base, outPaths)
    } else {
      const rel = path.relative(base, full).split(path.sep).join('/')
      outPaths.add(`/${rel}`)
    }
  }
}

/**
 * Paths the built SPA may request at site root (icons, public files, hashed /assets from dist).
 * @param {string} repoRoot
 * @returns {string[]}
 */
export function collectRequiredStaticPathsFromFrontend(repoRoot) {
  const paths = new Set()
  const frontendRoot = path.join(repoRoot, 'frontend')

  const sourceIndex = path.join(frontendRoot, 'index.html')
  if (existsSync(sourceIndex)) {
    const html = readFileSync(sourceIndex, 'utf8')
    for (const p of extractRootAbsolutePathsFromHtml(html)) {
      if (p.startsWith('/src/')) continue
      paths.add(p)
    }
  }

  const distIndex = path.join(frontendRoot, 'dist', 'index.html')
  if (existsSync(distIndex)) {
    const html = readFileSync(distIndex, 'utf8')
    for (const p of extractRootAbsolutePathsFromHtml(html)) {
      paths.add(p)
    }
  }

  const publicDir = path.join(frontendRoot, 'public')
  if (existsSync(publicDir)) {
    walkPublicFiles(publicDir, publicDir, paths)
  }

  return [...paths].sort()
}
