import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { render } from 'ink-testing-library'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { ListAccessTokenStage } from '../src/commands/accessToken/listAccessToken/ListAccessTokenStage.js'
import { pressEscapeAndWait, waitForFrames } from './inkTestHelpers.js'

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

  test('Escape settles with picker abort message when tokens exist', async () => {
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
        onAbortWithError={() => undefined}
      />
    )

    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('1. Alpha') && c.includes('2. Beta')
    )

    await pressEscapeAndWait(
      stdin,
      () => frames.join('\n'),
      () => settled !== null
    )

    expect(settled).toBe('Cancelled.')
  })

  test('empty list auto-settles without Escape (past assistant message path)', async () => {
    let settled: string | null = null
    render(
      <ListAccessTokenStage
        onSettled={(t) => {
          settled = t
        }}
        onAbortWithError={() => undefined}
      />
    )

    await waitForFrames(
      () => (settled !== null ? settled : ''),
      () => settled !== null
    )

    expect(settled).toBe('No access tokens stored.')
  })
})
