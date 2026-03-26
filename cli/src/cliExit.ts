export function exitCliError(message: string): never {
  console.error(`doughnut: ${message}`)
  process.exit(1)
}
