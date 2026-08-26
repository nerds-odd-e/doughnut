export function exitCliError(message: string): never {
  console.error(`donut: ${message}`)
  process.exit(1)
}
