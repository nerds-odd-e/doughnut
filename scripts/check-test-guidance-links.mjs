import { readFileSync, statSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('../', import.meta.url))
// This is the authoring route, not a recursive documentation crawl.
const sources = [
  'unit-testing/SKILL.md',
  'frontend-testing/SKILL.md',
  'backend-testing/SKILL.md',
  'cli/SKILL.md',
  'e2e-authoring/SKILL.md',
  'e2e-authoring/references/running-e2e.md',
  'e2e-authoring/references/test-data-and-service-mocking.md',
  'dough-makeme/SKILL.md',
  'dough-page-objects/SKILL.md',
  'dough-execute-plan/references/test-entry-rules.md',
].map((path) => `.agents/skills/${path}`)

const withoutFences = (text) =>
  text.replace(/^(`{3,}|~{3,})[^\n]*\n[\s\S]*?^\1[^\n]*$/gm, '')
const withoutCode = (text) => withoutFences(text).replace(/(`+)[\s\S]*?\1/g, '')

function anchors(text) {
  const result = new Set()
  const counts = new Map()
  for (const [, heading] of withoutFences(text).matchAll(
    /^#{1,6}\s+(.+?)\s*#*$/gm
  )) {
    const slug = heading
      .toLowerCase()
      .replace(/[^\p{L}\p{N}\p{M}_\-\s]/gu, '')
      .replace(/\s/g, '-')
    const count = counts.get(slug) ?? 0
    counts.set(slug, count + 1)
    result.add(count ? `${slug}-${count}` : slug)
  }
  for (const [, id] of text.matchAll(/\b(?:id|name)=["']([^"']+)["']/g))
    result.add(id)
  return result
}

let checked = 0
const failures = []
for (const source of sources) {
  const sourcePath = resolve(root, source)
  const markdown = withoutCode(readFileSync(sourcePath, 'utf8'))
  for (const [, destination] of markdown.matchAll(/\[[^\]]+\]\(([^\s)]+)\)/g)) {
    if (/^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(destination)) continue
    const [path, fragment] = destination.split('#')
    const target = path
      ? resolve(dirname(sourcePath), decodeURIComponent(path))
      : sourcePath
    checked += 1
    try {
      if (!statSync(target).isFile())
        throw new Error('destination is not a file')
      if (
        fragment &&
        !anchors(readFileSync(target, 'utf8')).has(decodeURIComponent(fragment))
      ) {
        throw new Error(`heading anchor #${fragment} not found`)
      }
    } catch (error) {
      failures.push(`${source} -> ${destination}: ${error.message}`)
    }
  }
}

if (failures.length) {
  console.error(failures.join('\n'))
  process.exitCode = 1
} else {
  console.log(
    `Testing guidance: ${checked} local links resolved across ${sources.length} sources`
  )
}
