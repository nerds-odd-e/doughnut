import { execFileSync } from 'node:child_process'
import { isAbsolute, relative, resolve, sep } from 'node:path'

const beneath = (parent, file) => {
  const path = relative(parent, file)
  return path !== '..' && !path.startsWith(`..${sep}`) && !isAbsolute(path)
}

function scenarioNodes(children, nodes = new Map()) {
  for (const child of children) {
    if (child.rule) scenarioNodes(child.rule.children, nodes)
    if (!child.scenario) continue
    const scenario = child.scenario
    nodes.set(scenario.id, { scenario: scenario.name })
    for (const examples of scenario.examples) {
      for (const row of examples.tableBody) {
        nodes.set(row.id, { example: row.cells.map((cell) => cell.value) })
      }
    }
  }
  return nodes
}

/** Resolve each compiled feature once, then attach its stable case identities. */
export function cucumberProofCases(
  messages,
  { repoRoot, projectRoot, canonicalProjectRoot, locations }
) {
  const sources = new Map(
    messages
      .filter((item) => item.source)
      .map(({ source }) => [source.uri, source])
  )
  const documents = new Map(
    messages
      .filter((item) => item.gherkinDocument)
      .map(({ gherkinDocument }) => [gherkinDocument.uri, gherkinDocument])
  )
  const features = new Map()
  const pickles = new Map()
  for (const { pickle } of messages) {
    if (!pickle) continue
    if (!features.has(pickle.uri)) {
      // URI coordinates belong to the runner's original lexical project root.
      const sourcePath = resolve(projectRoot, pickle.uri)
      const absolute = beneath(projectRoot, sourcePath)
        ? resolve(canonicalProjectRoot, relative(projectRoot, sourcePath))
        : sourcePath
      const directories = locations
        .filter((location) => beneath(location.path, absolute))
        .map((location) => location.proof)
      if (!directories.length) continue
      const featureBlob = execFileSync('git', ['hash-object', '--stdin'], {
        cwd: repoRoot,
        input: sources.get(pickle.uri).data,
        encoding: 'utf8',
        stdio: ['pipe', 'pipe', 'ignore'],
      }).trim()
      features.set(pickle.uri, {
        directories,
        featureBlob,
        feature: relative(repoRoot, absolute).split(sep).join('/'),
        nodes: scenarioNodes(documents.get(pickle.uri).feature.children),
      })
    }
    const { nodes, ...feature } = features.get(pickle.uri)
    const identity = Object.assign(
      {},
      ...pickle.astNodeIds.map((id) => nodes.get(id))
    )
    pickles.set(pickle.id, { ...identity, ...feature })
  }
  const cases = new Map()
  for (const { testCase } of messages) {
    if (testCase && pickles.has(testCase.pickleId)) {
      cases.set(testCase.id, pickles.get(testCase.pickleId))
    }
  }
  return cases
}
