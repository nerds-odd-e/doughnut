import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { render } from 'ink-testing-library'
import { afterEach, beforeEach } from 'vitest'
import { interactiveSlashCommands } from '../src/commands/interactiveSlashCommands.js'
import { InputHistoryProvider } from '../src/inputHistory/index.js'
import { MainInteractivePrompt } from '../src/mainInteractivePrompt/index.js'
import {
  extendInkRenderForInteractiveTests,
  inkCommandLineProbeUndelete,
  stripAnsi,
  waitUntilInkLastFrameStripped,
} from './inkTestHelpers.js'

/** Expected scroll UI copy (private in commonUIComponents/guidanceListWindowInk). */
export const EXPECT_GUIDANCE_MORE_BELOW = '↓ more below'
export const EXPECT_GUIDANCE_ROW_BUDGET = 5

export const MAIN_PROMPT_PLACEHOLDER = '`exit` to quit.'

export { stripAnsi }

/** StripAnsi line that contains the main `→` prompt (inside the bordered box). */
export function lineWithMainPrompt(plain: string): string {
  return plain.split('\n').find((l) => l.includes('→')) ?? ''
}

export function rawLineIncludesBoldMarker(
  raw: string,
  marker: string
): boolean {
  return raw
    .split('\n')
    .some((line) => line.includes(marker) && line.includes('\x1b[1m'))
}

/** Typed buffer after `→`, before Ink right-padding / `│` column border. */
export function mainPromptDraftAfterArrow(plain: string): string {
  const lm = lineWithMainPrompt(plain)
  const idx = lm.indexOf('→')
  if (idx < 0) return ''
  const after = lm.slice(idx + '→'.length).trimStart()
  return after.replace(/\s*│.*$/, '').trimEnd()
}

/** Call once per describe so each split suite gets isolated config-dir hooks. */
export function installMainInteractivePromptConfig(): {
  getPromptConfigDir: () => string
} {
  let promptConfigDir: string
  let prevDonutConfigDir: string | undefined

  beforeEach(() => {
    prevDonutConfigDir = process.env.DONUT_CONFIG_DIR
    promptConfigDir = fs.mkdtempSync(path.join(os.tmpdir(), 'donut-mip-'))
    process.env.DONUT_CONFIG_DIR = promptConfigDir
  })

  afterEach(() => {
    if (prevDonutConfigDir === undefined) {
      delete process.env.DONUT_CONFIG_DIR
    } else {
      process.env.DONUT_CONFIG_DIR = prevDonutConfigDir
    }
    fs.rmSync(promptConfigDir, { recursive: true, force: true })
  })

  return { getPromptConfigDir: () => promptConfigDir }
}

export async function renderMainInteractivePrompt(
  onCommittedLine: (line: string) => void = () => undefined
) {
  const result = render(
    <InputHistoryProvider>
      <MainInteractivePrompt
        onCommittedCommand={() => undefined}
        onCommittedLine={onCommittedLine}
        slashCommands={interactiveSlashCommands}
        placeholder={MAIN_PROMPT_PLACEHOLDER}
      />
    </InputHistoryProvider>
  )
  await waitUntilInkLastFrameStripped(
    result.lastFrame,
    (f) => f.includes('→') && f.includes('/ commands')
  )
  await inkCommandLineProbeUndelete(result, {
    probeChar: '@',
    probeVisible: (f) => lineWithMainPrompt(f).includes('@'),
    probeHidden: (f) => !lineWithMainPrompt(f).includes('@'),
  })
  return { ...result, ...extendInkRenderForInteractiveTests(result) }
}
