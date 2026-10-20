import assert from 'node:assert/strict'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import { afterEach, describe, test } from 'node:test'
import { mcpClientTasks } from './mcpClientTasks'

describe('bundleMcpServer', () => {
  const roots: string[] = []

  afterEach(() => {
    for (const root of roots.splice(0)) {
      rmSync(root, { recursive: true, force: true })
    }
  })

  function fixtureRepo(): { root: string; valueSource: string } {
    const root = mkdtempSync(join(tmpdir(), 'mcp-bundle-fixture-'))
    roots.push(root)
    const mcpServerDir = join(root, 'mcp-server')
    mkdirSync(join(mcpServerDir, 'src'), { recursive: true })
    writeFileSync(
      join(mcpServerDir, 'package.json'),
      `${JSON.stringify(
        {
          name: 'mcp-bundle-fixture',
          private: true,
          type: 'module',
          scripts: { bundle: 'node build.mjs' },
        },
        null,
        2
      )}\n`
    )
    writeFileSync(
      join(mcpServerDir, 'build.mjs'),
      [
        "import { mkdirSync, writeFileSync } from 'node:fs'",
        "import { dirname, join } from 'node:path'",
        "import { fileURLToPath } from 'node:url'",
        "import { currentValue } from './src/index.mjs'",
        '',
        'const dir = dirname(fileURLToPath(import.meta.url))',
        "mkdirSync(join(dir, 'dist'), { recursive: true })",
        'writeFileSync(',
        "  join(dir, 'dist', 'mcp-server.bundle.mjs'),",
        '  `export const currentValue = ${JSON.stringify(currentValue)}\\n`',
        ')',
        '',
      ].join('\n')
    )
    writeFileSync(
      join(mcpServerDir, 'src/index.mjs'),
      "import { value } from './value.mjs'\n\nexport const currentValue = value\n"
    )
    const valueSource = join(mcpServerDir, 'src/value.mjs')
    writeFileSync(valueSource, "export const value = 'first-value'\n")
    return { root, valueSource }
  }

  test('rebuilds the bundle from current source when an earlier bundle exists', async () => {
    const { root, valueSource } = fixtureRepo()
    const tasks = mcpClientTasks(root)
    const bundlePath = join(root, 'mcp-server/dist/mcp-server.bundle.mjs')
    let importCount = 0
    async function bundledValue(): Promise<string> {
      importCount += 1
      const artifact = (await import(
        `${pathToFileURL(bundlePath).href}?import=${importCount}`
      )) as { currentValue: string }
      return artifact.currentValue
    }

    await tasks.bundleMcpServer()
    assert.equal(await bundledValue(), 'first-value')

    writeFileSync(valueSource, "export const value = 'second-value'\n")

    await tasks.bundleMcpServer()
    assert.equal(await bundledValue(), 'second-value')
  })
})
