import { readFileSync, renameSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { createServer } from 'node:http'

export function installPublicationStore(root) {
  const objectStore = join(root, 'object-store')
  const original = join(root, 'bin/gcloud-fixture')
  renameSync(join(root, 'bin/gcloud'), original)
  writeFileSync(
    join(root, 'bin/gcloud'),
    `#!/usr/bin/env node
import { cpSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { spawnSync } from 'node:child_process'
const args = process.argv.slice(2)
const positional = args.slice(2).filter(arg => !arg.startsWith('-'))
if (args[0] === 'storage' && ['cp', 'rsync'].includes(args[1]) && args[2] !== '-') {
  const [source, destination] = positional
  const target = join(${JSON.stringify(objectStore)}, destination.slice('gs://'.length))
  mkdirSync(dirname(target), { recursive: true })
  cpSync(source, target, { recursive: true })
}
const result = spawnSync(${JSON.stringify(original)}, args, { stdio: 'inherit' })
process.exit(result.status ?? 1)
`,
    { mode: 0o755 }
  )
  return {
    publicFile: (path) => join(objectStore, 'public-frontend', path),
    uploadEnv: {
      ...process.env,
      PATH: `${join(root, 'bin')}:${process.env.PATH}`,
      TRACE: join(root, 'trace'),
      GCS_FRONTEND_BUCKET: 'public-frontend',
    },
  }
}

export async function servePublicationStore(t, store, notebook) {
  const posts = []
  const server = createServer(async (request, response) => {
    try {
      const url = new URL(request.url, 'http://localhost')
      if (url.pathname === '/doughnut-cli-latest/doughnut') {
        response.end(
          readFileSync(store.publicFile('doughnut-cli-latest/doughnut'))
        )
      } else if (
        url.pathname === '/api/notebooks/42/git-bundle' &&
        request.headers.authorization === 'Bearer acceptance-token'
      ) {
        if (request.method === 'GET')
          response.end(readFileSync(notebook.acceptedBundle))
        else if (request.method === 'POST') {
          const chunks = []
          for await (const chunk of request) chunks.push(chunk)
          posts.push({
            expectedHead: url.searchParams.get('expectedHead'),
            bytes: Buffer.concat(chunks),
          })
          response.end(notebook.proposed)
        } else response.writeHead(405).end()
      } else response.writeHead(404).end()
    } catch (error) {
      response.writeHead(500).end(String(error))
    }
  })
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  t.after(
    () =>
      new Promise((resolve, reject) =>
        server.close((error) => (error ? reject(error) : resolve()))
      )
  )
  return { origin: `http://127.0.0.1:${server.address().port}`, posts }
}
