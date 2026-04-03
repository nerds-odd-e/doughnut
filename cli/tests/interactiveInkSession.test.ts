import { PassThrough, Writable } from 'node:stream'
import { describe, expect, test } from 'vitest'
import { formatVersionOutput } from '../src/commands/version.js'
import { runInteractive } from '../src/interactiveInkSession.js'

describe('interactiveInkSession', () => {
  test('prints version on stdout before Ink blocks on exit', async () => {
    let out = ''
    const stdout = new Writable({
      write(chunk, _enc, cb) {
        out += String(chunk)
        cb()
      },
    }) as unknown as NodeJS.WriteStream
    stdout.columns = 80
    stdout.rows = 24

    const stdin = new PassThrough() as PassThrough & {
      isTTY?: boolean
      ref: () => void
      unref: () => void
      setRawMode: (mode: boolean) => PassThrough
    }
    stdin.isTTY = true
    stdin.ref = () => undefined
    stdin.unref = () => undefined
    stdin.setRawMode = () => stdin

    const session = runInteractive(stdin, stdout)
    expect(out).toContain(formatVersionOutput())

    stdin.write('/exit\r')
    await session
  })
})
