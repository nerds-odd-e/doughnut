import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const payloadCommand = fileURLToPath(
  new URL('./application-release-payload.mjs', import.meta.url)
)

export function runPayloadAdmission({
  backend,
  frontend,
  cli,
  publicationTrace,
}) {
  const result = spawnSync(
    'bash',
    [
      '-c',
      'node "$1" && printf published > "$2"',
      'payload-admission',
      payloadCommand,
      publicationTrace,
    ],
    {
      encoding: 'utf8',
      env: {
        ...process.env,
        DEPLOY_JAR_PATH: backend,
        FRONTEND_STATIC_DIR: frontend,
        CLI_BUNDLE_SOURCE: cli,
      },
    }
  )
  return { ...result, publicationReached: existsSync(publicationTrace) }
}
