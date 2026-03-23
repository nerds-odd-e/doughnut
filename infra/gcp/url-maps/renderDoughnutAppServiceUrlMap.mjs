import { readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

export const FRONTEND_GITHUB_SHA_PLACEHOLDER = '__FRONTEND_GITHUB_SHA__'

/**
 * @param {string} templateText
 * @param {string} githubSha 40-char lowercase hex
 * @returns {string}
 */
export function renderDoughnutAppServiceUrlMapTemplate(templateText, githubSha) {
  const sha = String(githubSha).toLowerCase()
  if (!/^[0-9a-f]{40}$/.test(sha)) {
    throw new Error(
      'GITHUB_SHA must be a 40-character lowercase hexadecimal commit id'
    )
  }
  if (!templateText.includes(FRONTEND_GITHUB_SHA_PLACEHOLDER)) {
    throw new Error(
      `URL map template must contain ${FRONTEND_GITHUB_SHA_PLACEHOLDER}`
    )
  }
  return templateText.split(FRONTEND_GITHUB_SHA_PLACEHOLDER).join(sha)
}

const __dirname = path.dirname(fileURLToPath(import.meta.url))

function parseArgs(argv) {
  const out = { writePath: null, sha: null }
  for (let i = 2; i < argv.length; i++) {
    if (argv[i] === '--write' && argv[i + 1]) {
      out.writePath = argv[++i]
      continue
    }
    if (argv[i] === '--sha' && argv[i + 1]) {
      out.sha = argv[++i]
      continue
    }
    throw new Error(`Unknown argument: ${argv[i]}`)
  }
  return out
}

function main() {
  const { writePath, sha: shaArg } = parseArgs(process.argv)
  const sha = shaArg ?? process.env.GITHUB_SHA
  if (!sha) {
    console.error('Usage: renderDoughnutAppServiceUrlMap.mjs --sha <40-hex> [--write <path>]')
    console.error('   or: GITHUB_SHA=<40-hex> node ... [--write <path>]')
    process.exit(1)
  }
  const templatePath = path.join(
    __dirname,
    'doughnut-app-service-map.template.yaml'
  )
  const templateText = readFileSync(templatePath, 'utf8')
  const rendered = renderDoughnutAppServiceUrlMapTemplate(templateText, sha)
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
