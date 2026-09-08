/**
 * Disposable Nix MySQL 8.4 instance for retirement DROP proof.
 * Temporary datadir + unused TCP port; never touches developer databases.
 */
import { spawn, spawnSync } from 'node:child_process'
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
} from 'node:fs'
import net from 'node:net'
import os from 'node:os'
import path from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'
import { listenEphemeralPort } from './sut-e2e-port-listen.mjs'

function resolveMysqlBasedir() {
  if (process.env.MYSQL_BASEDIR) return process.env.MYSQL_BASEDIR
  const which = spawnSync('which', ['mysqld'], { encoding: 'utf8' })
  if (which.status !== 0) {
    throw new Error('mysqld not found; run under CURSOR_DEV=true nix develop')
  }
  return path.dirname(path.dirname(which.stdout.trim()))
}

async function reserveTcpPort() {
  const { server, port } = await listenEphemeralPort(() =>
    net.createServer((socket) => socket.end())
  )
  await new Promise((resolve, reject) => {
    server.close((error) => (error ? reject(error) : resolve()))
  })
  return port
}

function mysqlCli(basedir, port, sql, extraArgs = []) {
  const result = spawnSync(
    path.join(basedir, 'bin', 'mysql'),
    [
      '-u',
      'root',
      '-h',
      '127.0.0.1',
      '-P',
      String(port),
      ...extraArgs,
      '-e',
      sql,
    ],
    { encoding: 'utf8' }
  )
  if (result.status !== 0) {
    throw new Error(
      `mysql failed (${result.status}): ${result.stderr || result.stdout}`
    )
  }
  return result.stdout
}

async function waitForMysql(basedir, port, timeoutMs = 60_000) {
  const admin = path.join(basedir, 'bin', 'mysqladmin')
  const deadline = Date.now() + timeoutMs
  let lastError = ''
  while (Date.now() < deadline) {
    const ping = spawnSync(
      admin,
      ['ping', '-h127.0.0.1', `-P${port}`, '--silent'],
      { encoding: 'utf8' }
    )
    if (ping.status === 0) return
    lastError = ping.stderr || ping.stdout || `status ${ping.status}`
    await delay(200)
  }
  throw new Error(`disposable mysqld on port ${port} not ready: ${lastError}`)
}

/**
 * Boot an isolated mysqld matching the Nix mysql84 tooling version.
 */
export async function startDisposableMysql(t) {
  const basedir = resolveMysqlBasedir()
  const home = mkdtempSync(path.join(os.tmpdir(), 'doughnut-retire-mysql-'))
  const datadir = path.join(home, 'data')
  mkdirSync(datadir, { mode: 0o750 })
  const socket = path.join(home, 'mysql.sock')
  const pidFile = path.join(home, 'mysql.pid')
  const logFile = path.join(home, 'mysql.log')
  const port = await reserveTcpPort()
  const username = os.userInfo().username

  const init = spawnSync(
    path.join(basedir, 'bin', 'mysqld'),
    [
      '--initialize-insecure',
      `--basedir=${basedir}`,
      `--datadir=${datadir}`,
      `--port=${port}`,
      `--user=${username}`,
      '--tls-version=TLSv1.2',
      '--explicit_defaults_for_timestamp',
    ],
    { encoding: 'utf8' }
  )
  if (init.status !== 0) {
    rmSync(home, { recursive: true, force: true })
    throw new Error(
      `mysqld --initialize-insecure failed: ${init.stderr || init.stdout}`
    )
  }

  const child = spawn(
    path.join(basedir, 'bin', 'mysqld'),
    [
      `--basedir=${basedir}`,
      `--datadir=${datadir}`,
      `--port=${port}`,
      `--socket=${socket}`,
      `--pid-file=${pidFile}`,
      `--log-error=${logFile}`,
      '--mysqlx=0',
      '--bind-address=127.0.0.1',
      '--tls-version=TLSv1.2',
      `--user=${username}`,
    ],
    {
      stdio: 'ignore',
      detached: true,
    }
  )
  child.unref()

  let stopped = false
  const stop = () => {
    if (stopped) return
    stopped = true
    try {
      if (existsSync(pidFile)) {
        const pid = Number(readFileSync(pidFile, 'utf8').trim())
        if (Number.isInteger(pid) && pid > 0) {
          try {
            process.kill(pid, 'SIGTERM')
          } catch {
            // already gone
          }
        }
      } else if (child.pid) {
        try {
          process.kill(child.pid, 'SIGTERM')
        } catch {
          // already gone
        }
      }
    } finally {
      rmSync(home, { recursive: true, force: true })
    }
  }
  t.after(stop)

  try {
    await waitForMysql(basedir, port)
  } catch (error) {
    const log = existsSync(logFile) ? readFileSync(logFile, 'utf8') : ''
    stop()
    throw new Error(
      `${error instanceof Error ? error.message : String(error)}${
        log ? `\n${log}` : ''
      }`
    )
  }

  const version = mysqlCli(basedir, port, 'SELECT VERSION();', [
    '-N',
    '-B',
  ]).trim()

  return {
    basedir,
    port,
    version,
    home,
    execSql(sql) {
      return mysqlCli(basedir, port, sql)
    },
    query(sql) {
      return mysqlCli(basedir, port, sql, ['-N', '-B']).trim()
    },
    stop,
  }
}
