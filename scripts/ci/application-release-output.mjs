import { appendFileSync } from 'node:fs'

export function writeReleaseOutput(result) {
  console.log(JSON.stringify(result))
  if (process.env.GITHUB_OUTPUT) {
    appendFileSync(
      process.env.GITHUB_OUTPUT,
      Object.entries(result)
        .map(([key, value]) => `${key}=${value}\n`)
        .join('')
    )
  }
}
