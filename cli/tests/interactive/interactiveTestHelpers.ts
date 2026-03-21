import { Readable } from 'node:stream'
import { mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { expect, vi } from 'vitest'
import type { AccessTokenEntry } from '../../src/accessToken.js'
import {
  resetRecallStateForTesting,
  runInteractive,
} from '../../src/interactive.js'
import {
  CLEAR_SCREEN,
  getTerminalWidth,
  renderPastInput,
} from '../../src/renderer.js'

export const tick = () => new Promise<void>((r) => setImmediate(r))

export const BOLD_CYAN = '\x1b[1;36m'
export const ANSI_RESET = '\x1b[0m'
export const GREY_BG_PAST_INPUT = '\x1b[48;5;236m'

export function createMockStdin(input: string): NodeJS.ReadableStream {
  const stream = new Readable({
    read() {
      /* no-op */
    },
  })
  stream.push(input)
  stream.push(null)
  return Object.assign(stream, { isTTY: false })
}

export function createMockTTYStdin() {
  const stream = new Readable({
    read() {
      /* no-op */
    },
  }) as Readable & {
    push: (chunk: string) => void
  }
  return Object.assign(stream, {
    isTTY: true,
    setRawMode: () => {
      /* no-op */
    },
    resume: () => {
      /* no-op */
    },
    setEncoding: () => {
      /* no-op */
    },
  })
}

export type TTYStdin = ReturnType<typeof createMockTTYStdin>

export function typeString(stdin: TTYStdin, str: string) {
  for (const ch of str) {
    stdin.emit('keypress', ch, {
      name: ch === ' ' ? 'space' : ch === '/' ? undefined : ch,
      ctrl: false,
      meta: false,
    })
  }
}

export function pressEnter(stdin: TTYStdin) {
  stdin.emit('keypress', '\r', {
    name: 'return',
    shift: false,
    ctrl: false,
    meta: false,
  })
}

export function pressKey(
  stdin: TTYStdin,
  name: string,
  extra: Record<string, unknown> = {}
) {
  stdin.emit('keypress', undefined, {
    name,
    ctrl: false,
    meta: false,
    ...extra,
  })
}

export async function submitTTYCommand(stdin: TTYStdin, command: string) {
  typeString(stdin, `${command} `)
  await tick()
  pressEnter(stdin)
  await tick()
}

export function ttyOutput(writeSpy: ReturnType<typeof vi.spyOn>) {
  return writeSpy.mock.calls.map((c: [string]) => c[0]).join('')
}

/** Recall-session y/n answers append to scrollback without a grey input row or full-screen clear. */
export function expectTtyRecallYesNoReplyScrollback(
  writeSpy: ReturnType<typeof vi.spyOn>,
  answerLine: string
) {
  const out = ttyOutput(writeSpy)
  expect(out).not.toContain(renderPastInput(answerLine, getTerminalWidth()))
  expect(out).not.toContain(CLEAR_SCREEN)
}

/** Latest line in captured TTY stdout containing `needle` (successive repaints overwrite the same logical rows). */
export function lastStdoutLineContaining(
  output: string,
  needle: string
): string | undefined {
  let found: string | undefined
  for (const line of output.split('\n')) {
    if (line.includes(needle)) found = line
  }
  return found
}

export function makeTempConfigDir(tokens: AccessTokenEntry[]) {
  const configDir = mkdtempSync(join(tmpdir(), 'doughnut-test-'))
  writeFileSync(
    join(configDir, 'access-tokens.json'),
    JSON.stringify({ tokens })
  )
  return configDir
}

export function withConfigDir(configDir: string): () => void {
  const original = process.env.DOUGHNUT_CONFIG_DIR
  process.env.DOUGHNUT_CONFIG_DIR = configDir
  return () => {
    if (original === undefined) delete process.env.DOUGHNUT_CONFIG_DIR
    else process.env.DOUGHNUT_CONFIG_DIR = original
  }
}

export function spyConsoleLogNoop() {
  return vi.spyOn(console, 'log').mockImplementation(() => undefined)
}

export function spyStdoutWriteTrue() {
  return vi.spyOn(process.stdout, 'write').mockImplementation(() => true)
}

export function spyExitNoop(): ReturnType<typeof vi.spyOn> {
  return vi
    .spyOn(process, 'exit')
    .mockImplementation((() => undefined) as unknown as typeof process.exit)
}

export async function startInteractiveOnStdin(stdin: TTYStdin) {
  runInteractive(stdin as unknown as Parameters<typeof runInteractive>[0])
  await tick()
}

export async function ttySessionWithSpies(): Promise<{
  stdin: TTYStdin
  writeSpy: ReturnType<typeof vi.spyOn>
}> {
  resetRecallStateForTesting()
  return startTTYSessionWithoutRecallReset()
}

export async function startTTYSessionWithoutRecallReset(): Promise<{
  stdin: TTYStdin
  writeSpy: ReturnType<typeof vi.spyOn>
}> {
  spyConsoleLogNoop()
  const writeSpy = spyStdoutWriteTrue()
  spyExitNoop()
  const stdin = createMockTTYStdin()
  await startInteractiveOnStdin(stdin)
  return { stdin, writeSpy }
}

export function endTTYSession(stdin: TTYStdin) {
  pressKey(stdin, 'c', { ctrl: true })
  vi.restoreAllMocks()
}

export async function runPipedInteractive(input: string) {
  const stdin = createMockStdin(input)
  runInteractive(stdin as unknown as Parameters<typeof runInteractive>[0])
  await tick()
}
