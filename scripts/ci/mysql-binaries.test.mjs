import assert from 'node:assert/strict'
import { execFileSync, spawn } from 'node:child_process'
import {
  chmodSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { createServer } from 'node:http'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { action } from './workflow-fixtures.mjs'

test('MySQL installation recovers a reset archive transfer with complete extracted binaries', {
  timeout: 15000,
}, async (t) => {
  const directory = mkdtempSync(join(tmpdir(), 'donut-mysql-download-'))
  t.after(() => rmSync(directory, { recursive: true, force: true }))
  const bin = join(directory, 'bin')
  const runner = join(directory, 'runner')
  const archiveName = 'mysql-8.4.3-linux-glibc2.17-x86_64-minimal'
  const binaries = join(directory, archiveName, 'bin')
  mkdirSync(bin)
  mkdirSync(runner)
  mkdirSync(binaries, { recursive: true })
  mkdirSync(join(directory, 'scripts'))
  writeFileSync(join(directory, 'scripts/ci-system-packages.sh'), 'exit 0\n')
  const executable = (path, source) => {
    writeFileSync(path, source)
    chmodSync(path, 0o755)
  }
  executable(join(bin, 'sudo'), '#!/bin/bash\nexit 0\n')
  for (const name of ['mysqld', 'mysql']) {
    executable(
      join(binaries, name),
      `#!/bin/bash\necho '${name} 8.4.3 fixture'\n`
    )
  }
  const archivePath = join(directory, 'archive.tar.xz')
  execFileSync('tar', ['-cJf', archivePath, '-C', directory, archiveName])
  const archive = readFileSync(archivePath)
  let requests = 0
  const server = createServer((_request, response) => {
    requests += 1
    response.writeHead(200, { 'content-length': archive.length })
    if (requests === 1) {
      response.write(archive.subarray(0, 32))
      setTimeout(() => response.socket?.resetAndDestroy(), 10)
    } else {
      response.end(archive)
    }
  })
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  t.after(() => new Promise((resolve) => server.close(resolve)))
  executable(
    join(bin, 'curl'),
    `#!/bin/bash
args=()
for arg in "$@"; do
  case "$arg" in
    https://cdn.mysql.com/*) args+=("$MYSQL_TEST_ENDPOINT") ;;
    *) args+=("$arg") ;;
  esac
done
exec "$REAL_CURL" "\${args[@]}"
`
  )
  const realCurl = execFileSync('which', ['curl'], { encoding: 'utf8' }).trim()
  const githubEnvironment = join(directory, 'github-env')
  const result = await new Promise((resolve, reject) => {
    const child = spawn(
      'bash',
      [
        '-e',
        '-o',
        'pipefail',
        '-c',
        action('mysql_binaries').runs.steps[0].run,
      ],
      {
        cwd: directory,
        env: {
          ...process.env,
          PATH: `${bin}:${process.env.PATH}`,
          RUNNER_TEMP: runner,
          GITHUB_ENV: githubEnvironment,
          REAL_CURL: realCurl,
          MYSQL_TEST_ENDPOINT: `http://127.0.0.1:${server.address().port}/archive.tar.xz`,
          NO_PROXY: '127.0.0.1',
        },
      }
    )
    let output = ''
    child.stdout.on('data', (data) => {
      output += data
    })
    child.stderr.on('data', (data) => {
      output += data
    })
    child.once('error', reject)
    child.once('close', (code) => resolve({ code, output }))
  })

  assert.equal(result.code, 0, result.output)
  assert.equal(requests, 2)
  assert.deepEqual(readFileSync(join(runner, 'mysql.tar.xz')), archive)
  assert.match(result.output, /mysqld 8\.4\.3 fixture/)
  assert.match(result.output, /mysql 8\.4\.3 fixture/)
  assert.equal(
    readFileSync(githubEnvironment, 'utf8'),
    `MYSQL_BASEDIR=${runner}/${archiveName}\n`
  )
})
