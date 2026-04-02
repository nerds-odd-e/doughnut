import * as fs from 'node:fs'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { renderInkWhenCommandLineReady } from './inkTestHelpers.js'

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

  test('in notebook stage, / shows slash sub-command guidance', async () => {
    const { stdin, waitForFramesToInclude, waitForLastFrameToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/use Top Maths\r')
    await waitForFramesToInclude('Active notebook: Top Maths')
    stdin.write('/')
    await waitForLastFrameToInclude('/exit, exit')
    await waitForLastFrameToInclude('Leave notebook context')
  })

  test('enters notebook stage then /exit clears stage indicator', async () => {
    const {
      stdin,
      lastStrippedFrame,
      waitForLastFrameToInclude,
      waitForFramesToInclude,
    } = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    stdin.write('/use Top Maths\r')
    await waitForFramesToInclude('Active notebook: Top Maths')
    stdin.write('/exit\r')
    await waitForLastFrameToInclude('Left notebook context.')
    await waitForLastFrameToInclude('`exit` to quit.')

    expect(lastStrippedFrame()).not.toContain('Active notebook: Top Maths')
  })
})
