import { run } from './run.js'

export async function main(): Promise<void> {
  const rawArgs = process.argv.slice(2)
  const production = rawArgs.includes('-P') || rawArgs.includes('--production')
  const args = rawArgs.filter((a) => a !== '-P' && a !== '--production')

  if (production) {
    delete process.env.DONUT_CONFIG_DIR
    process.env.DONUT_API_BASE_URL = 'https://doughnut.odd-e.com'
  }

  await run(args)
}
