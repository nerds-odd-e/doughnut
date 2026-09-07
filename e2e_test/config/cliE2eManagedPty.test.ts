import assert from 'node:assert/strict'
import { describe, test } from 'node:test'
import { waitForPtyExit } from './cliE2eManagedPty'

function ptyExitingWith(exitCode: number, signal?: number) {
  return {
    onExit(listener: (e: { exitCode: number; signal?: number }) => void) {
      queueMicrotask(() => listener({ exitCode, signal }))
      return {
        dispose() {
          // Stub subscription: the listener already ran on the microtask.
        },
      }
    },
  }
}

describe('waitForPtyExit', () => {
  test('unexpected exit includes the PTY transcript so a pull rejection is diagnosable', async () => {
    await assert.rejects(
      () =>
        waitForPtyExit(
          ptyExitingWith(1, 0),
          0,
          1000,
          () =>
            'donut: Local main cannot receive the accepted history because accepted history includes a structural change at "README.md".\n'
        ),
      {
        message:
          'CLI exited with code 1 (signal 0)\n\nCLI output:\ndonut: Local main cannot receive the accepted history because accepted history includes a structural change at "README.md".',
      }
    )
  })

  test('names empty output instead of omitting the transcript block', async () => {
    await assert.rejects(
      () => waitForPtyExit(ptyExitingWith(1, 0), 0, 1000, () => '  \n'),
      {
        message: 'CLI exited with code 1 (signal 0)\n\nCLI output:\n(empty)',
      }
    )
  })
})
