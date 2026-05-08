import assert from 'node:assert'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  backupLogPath,
  createRotatingLogWriter,
  rotateLogFile,
  rotateLogFileIfNeeded,
  tailLogFile,
} from './log-utils.mjs'

test('rotateLogFile keeps the configured number of backups', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'log-utils-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    await writeFile(logFile, 'current', 'utf8')
    await writeFile(backupLogPath(logFile, 1), 'one', 'utf8')
    await writeFile(backupLogPath(logFile, 2), 'two', 'utf8')

    rotateLogFile(logFile, 2)

    assert.strictEqual(
      await readFile(backupLogPath(logFile, 1), 'utf8'),
      'current'
    )
    assert.strictEqual(await readFile(backupLogPath(logFile, 2), 'utf8'), 'one')
    await assert.rejects(readFile(logFile, 'utf8'), /ENOENT/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('rotateLogFileIfNeeded rotates only when the file reaches the cap', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'log-utils-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    await writeFile(logFile, '12345', 'utf8')

    assert.strictEqual(rotateLogFileIfNeeded(logFile, 6, 1), false)
    assert.strictEqual(rotateLogFileIfNeeded(logFile, 5, 1), true)
    assert.strictEqual(
      await readFile(backupLogPath(logFile, 1), 'utf8'),
      '12345'
    )
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('createRotatingLogWriter rotates before a write would exceed the cap', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'log-utils-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    const writer = createRotatingLogWriter(logFile, { maxBytes: 8, backups: 2 })

    writer.write('1234')
    writer.write('5678')
    writer.write('90')
    writer.close()

    assert.strictEqual(
      await readFile(backupLogPath(logFile, 1), 'utf8'),
      '12345678'
    )
    assert.strictEqual(await readFile(logFile, 'utf8'), '90')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('tailLogFile returns the requested final lines and can filter first', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'log-utils-test-'))
  try {
    const logFile = path.join(dir, 'sut.log')
    await writeFile(
      logFile,
      [
        'frontend ready',
        'mountebank started',
        'backend ready',
        'mb replay',
      ].join('\n'),
      'utf8'
    )

    assert.strictEqual(
      await tailLogFile(logFile, 2),
      'backend ready\nmb replay'
    )
    assert.strictEqual(
      await tailLogFile(logFile, 2, { filter: /\b(mountebank|mb)\b/i }),
      'mountebank started\nmb replay'
    )
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})
