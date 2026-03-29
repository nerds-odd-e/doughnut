import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { render } from 'ink-testing-library'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { ListAccessTokenStage } from '../src/commands/listAccessTokenSlashCommand.js'
import {
  formatNumberedListForTerminal,
  resolvedTerminalWidth,
} from '../src/terminalColumns.js'

async function waitForFrames(
  getCombined: () => string,
  predicate: (combined: string) => boolean,
  maxTicks = 5000
): Promise<void> {
  for (let i = 0; i < maxTicks; i++) {
    if (predicate(getCombined())) return
    await new Promise<void>((resolve) => {
      setImmediate(resolve)
    })
  }
  throw new Error(
    `Output condition not met within ${maxTicks} event-loop turns. Last frames:\n${getCombined()}`
  )
}

describe('ListAccessTokenStage', () => {
  let configDir: string
  let savedConfigDir: string | undefined

  beforeEach(() => {
    configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'doughnut-cli-lat-'))
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
  })

  afterEach(() => {
    if (savedConfigDir === undefined) delete process.env.DOUGHNUT_CONFIG_DIR
    else process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('Escape settles with full assistant text when tokens exist', async () => {
    fs.writeFileSync(
      path.join(configDir, 'access-tokens.json'),
      JSON.stringify({
        tokens: [
          { label: 'Alpha', token: 't1' },
          { label: 'Beta', token: 't2' },
        ],
      }),
      'utf-8'
    )

    let settled: string | null = null
    const { stdin, frames } = render(
      <ListAccessTokenStage
        onSettled={(t) => {
          settled = t
        }}
      />
    )

    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('1. Alpha') && c.includes('2. Beta')
    )

    stdin.write('\u001b')
    await waitForFrames(
      () => frames.join('\n'),
      () => settled !== null
    )

    expect(settled).toBe(
      `Stored access tokens:\n\n${formatNumberedListForTerminal(
        ['Alpha', 'Beta'],
        resolvedTerminalWidth()
      )}`
    )
  })

  test('Escape settles with empty message when no tokens', async () => {
    let settled: string | null = null
    const { stdin, frames } = render(
      <ListAccessTokenStage
        onSettled={(t) => {
          settled = t
        }}
      />
    )

    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('No access tokens stored.')
    )

    stdin.write('\u001b')
    await waitForFrames(
      () => frames.join('\n'),
      () => settled !== null
    )

    expect(settled).toBe('No access tokens stored.')
  })
})
