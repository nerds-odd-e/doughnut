import {
  existsSync,
  readFileSync,
  readdirSync,
  renameSync,
  watch,
  writeFileSync,
} from 'node:fs'
import { join } from 'node:path'

const eventFilePattern = /^(\d{12})\.json$/

function publishJson(directory, name, value) {
  const temporary = join(directory, `${name}.tmp`)
  writeFileSync(temporary, JSON.stringify(value), { mode: 0o600 })
  renameSync(temporary, join(directory, name))
}

export function readMailboxEvents(directory, after = 0) {
  return readdirSync(join(directory, 'events'))
    .filter((name) => eventFilePattern.test(name))
    .sort()
    .map((name) => JSON.parse(readFileSync(join(directory, 'events', name))))
    .filter(({ sequence }) => sequence > after)
}

export function publishMailboxEvent(directory, event) {
  const sequence = (readMailboxEvents(directory).at(-1)?.sequence ?? 0) + 1
  publishJson(
    join(directory, 'events'),
    `${String(sequence).padStart(12, '0')}.json`,
    { sequence, event }
  )
  return sequence
}

export function readDeliveryProgress(directory) {
  const path = join(directory, 'delivery.json')
  return existsSync(path)
    ? JSON.parse(readFileSync(path, 'utf8'))
    : { deliveredThrough: 0 }
}

export function recordDeliveryProgress(directory, deliveredThrough) {
  publishJson(directory, 'delivery.json', { deliveredThrough })
}

function terminalResult(directory, request, status) {
  if (!(request.mode === 'execution' && status === 'stopped')) return { status }
  const records = readMailboxEvents(directory)
  const recordedThrough = records.at(-1)?.sequence ?? 0
  const { deliveredThrough } = readDeliveryProgress(directory)
  const unread = records.filter(
    ({ sequence }) => sequence > deliveredThrough
  ).length
  return {
    status,
    coverage: { state: 'ended', pendingCi: 'unobserved' },
    evidence: { recordedThrough, deliveredThrough, unread },
  }
}

export async function waitForTerminalResult(directory) {
  const path = join(directory, 'result.json')
  if (!existsSync(path))
    await new Promise((resolve) => {
      const finished = () => {
        if (!existsSync(path)) return
        subscription.close()
        resolve()
      }
      const subscription = watch(directory, finished)
      finished()
    })
  return JSON.parse(readFileSync(path, 'utf8'))
}

export function recordTerminalResult(directory, request, status) {
  publishJson(
    directory,
    'result.json',
    terminalResult(directory, request, status)
  )
}
