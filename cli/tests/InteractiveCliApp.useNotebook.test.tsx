import * as fs from 'node:fs'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { formatVersionOutput } from '../src/commands/version.js'
import {
  renderInkWhenCommandLineReady,
  stripAnsi,
  waitForFrames,
} from './inkTestHelpers.js'

import { tempConfigWithToken } from './tempConfigTestHelpers.js'

describe('InteractiveCliApp /use notebook stage', () => {
  let configDir: string
  let savedConfigDir: string | undefined

  beforeEach(() => {
    configDir = tempConfigWithToken()
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir
  })

  afterEach(() => {
    if (savedConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    }
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('enters notebook stage then /exit clears stage indicator', async () => {
    const { stdin, frames, lastFrame, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)
    expect(stripAnsi(frames.join('\n'))).toContain(formatVersionOutput())

    stdin.write('/use Top Maths\r')
    await waitForFramesToInclude(/Active notebook: Top Maths/)

    stdin.write('/exit\r')

    await waitForFrames(
      () => stripAnsi(lastFrame() ?? ''),
      (c) => c.includes('/exit') && c.includes('Left notebook context.')
    )

    const end = stripAnsi(lastFrame() ?? '')
    expect(end).toContain('`exit` to quit.')
    expect(end).not.toContain('Active notebook: Top Maths')
  })
})
