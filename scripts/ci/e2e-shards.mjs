import { relative, resolve, sep } from 'node:path'
import { pathToFileURL } from 'node:url'
import {
  discoverE2eSpecs,
  e2eSpecPatterns,
} from '../../e2e_test/config/e2eSpecDiscovery.mjs'

// The sixth shard is always the exact remainder, including new directories.
export const explicitGroups = [
  [
    'assimilation',
    'ai_generated_content',
    'ai_generated_recall_questions',
    'note_view',
  ],
  ['bazaar', 'book_reading', 'folder_organization', 'notebooks'],
  ['cli'],
  ['note_topology', 'relationships', 'circles'],
  ['recall', 'wikidata', 'mcp'],
].map((folders) => folders.map((folder) => `e2e_test/features/${folder}/**`))

export function selectE2eShards(projectRoot) {
  const config = { ...e2eSpecPatterns, projectRoot }
  const specs = discoverE2eSpecs(config)
  const assigned = new Set()
  const groups = explicitGroups.map((specPattern) => {
    const matching = new Set(discoverE2eSpecs({ ...config, specPattern }))
    return specs.filter((spec) => {
      if (!matching.has(spec)) return false
      if (assigned.has(spec)) throw new Error(`E2E shard overlap: ${spec}`)
      assigned.add(spec)
      return true
    })
  })
  groups.push(specs.filter((spec) => !assigned.has(spec)))
  return groups.map((group) =>
    group.map((spec) => relative(projectRoot, spec).replaceAll(sep, '/'))
  )
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  const shard = Number(process.argv[2])
  const groups = selectE2eShards(resolve(process.argv[3] ?? '.'))
  if (!Number.isInteger(shard) || shard < 1 || shard > groups.length) {
    throw new Error(`Expected an E2E shard number from 1 to ${groups.length}`)
  }
  if (!groups[shard - 1].length) throw new Error(`E2E shard ${shard} is empty`)
  process.stdout.write(`${groups[shard - 1].join(',')}\n`)
}
