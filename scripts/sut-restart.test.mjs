import assert from 'node:assert'
import { spawn } from 'node:child_process'
import { once } from 'node:events'
import { createInterface } from 'node:readline'
import net from 'node:net'
import { test } from 'node:test'
import {
  parsePidsFromLsofStdout,
  terminateTcpListenersOnPort,
} from './sut-restart.mjs'

test('parsePidsFromLsofStdout empty', () => {
  assert.deepStrictEqual(parsePidsFromLsofStdout(''), [])
})

test('parsePidsFromLsofStdout single PID', () => {
  assert.deepStrictEqual(parsePidsFromLsofStdout('12345\n'), [12345])
})

test('parsePidsFromLsofStdout dedupes and filters junk', () => {
  assert.deepStrictEqual(
    parsePidsFromLsofStdout('100\n100\n0\n-1\nabc\n200\r\n'),
    [100, 200]
  )
})

function checkTcpConnects(host, port, timeoutMs = 2_000) {
  return new Promise((resolve, reject) => {
    const socket = net.createConnection({ host, port })
    const t = setTimeout(() => {
      socket.destroy()
      reject(new Error('timeout'))
    }, timeoutMs)
    socket.once('connect', () => {
      clearTimeout(t)
      socket.destroy()
      resolve(true)
    })
    socket.once('error', (err) => {
      clearTimeout(t)
      reject(err)
    })
  })
}

function spawnEphemeralTcpListener() {
  return new Promise((resolve, reject) => {
    const child = spawn(
      process.execPath,
      [
        '-e',
        `const net = require('node:net');
        const s = net.createServer((c) => c.end()).listen(0, '127.0.0.1', () => {
          process.stdout.write(String(s.address().port) + '\\n');
        });`,
      ],
      { stdio: ['ignore', 'pipe', 'pipe'] }
    )
    child.once('error', reject)
    const onEarlyExit = (code) => {
      reject(new Error(`listener child exited early: ${code}`))
    }
    child.once('exit', onEarlyExit)
    const rl = createInterface({ input: child.stdout })
    rl.once('line', (line) => {
      child.removeListener('exit', onEarlyExit)
      const port = Number(line.trim())
      if (!Number.isInteger(port) || port <= 0) {
        child.kill('SIGKILL')
        reject(new Error('bad port from child'))
        return
      }
      resolve({ child, port })
    })
  })
}

test('terminateTcpListenersOnPort ends a child TCP listener', async () => {
  const { child, port } = await spawnEphemeralTcpListener()
  try {
    await checkTcpConnects('127.0.0.1', port)
    await terminateTcpListenersOnPort(port)
    await once(child, 'exit')
    await assert.rejects(
      checkTcpConnects('127.0.0.1', port, 500),
      /ECONNREFUSED|timeout/
    )
  } finally {
    if (!child.killed) child.kill('SIGKILL')
  }
})
