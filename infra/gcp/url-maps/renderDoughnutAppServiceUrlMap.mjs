import { writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import {
  defaultDoughnutRoutingPath,
  FRONTEND_GITHUB_SHA_PLACEHOLDER,
  loadDoughnutRouting,
  renderDoughnutAppServiceUrlMapTemplate,
  renderDoughnutAppServiceUrlMapYamlFromRouting,
} from '../path-routing/doughnutRouting.mjs'

export {
  FRONTEND_GITHUB_SHA_PLACEHOLDER,
  renderDoughnutAppServiceUrlMapTemplate,
} from '../path-routing/doughnutRouting.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

function parseArgs(argv) {
  const out = {
    writePath: null,
    sha: null,
    routingPath: defaultDoughnutRoutingPath(),
  }
  for (let i = 2; i < argv.length; i++) {
    if (argv[i] === '--write' && argv[i + 1]) {
      out.writePath = argv[++i]
      continue
    }
    if (argv[i] === '--sha' && argv[i + 1]) {
      out.sha = argv[++i]
      continue
    }
    if (argv[i] === '--routing' && argv[i + 1]) {
      out.routingPath = path.resolve(argv[++i])
      continue
    }
    throw new Error(`Unknown argument: ${argv[i]}`)
  }
  return out
}

function main() {
  const { writePath, sha: shaArg, routingPath } = parseArgs(process.argv)
  const sha = shaArg ?? process.env.GITHUB_SHA
  if (!sha) {
    console.error(
      'Usage: renderDoughnutAppServiceUrlMap.mjs --sha <40-hex> [--write <path>] [--routing <doughnut-routing.json>]'
    )
    console.error('   or: GITHUB_SHA=<40-hex> node ... [--write <path>]')
    process.exit(1)
  }
  const routing = loadDoughnutRouting(routingPath)
  const rendered = renderDoughnutAppServiceUrlMapYamlFromRouting(routing, sha)
  if (writePath) {
    writeFileSync(writePath, rendered, 'utf8')
  } else {
    process.stdout.write(rendered)
  }
}

const entry = process.argv[1] && path.resolve(process.argv[1])
if (entry && import.meta.url === pathToFileURL(entry).href) {
  main()
}
