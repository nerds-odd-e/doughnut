declare const CLI_VERSION: string | undefined
const VERSION = typeof CLI_VERSION !== 'undefined' ? CLI_VERSION : '0.1.0'

export function getVersion(): string {
  return VERSION
}

export function formatVersionOutput(): string {
  return `doughnut ${getVersion()}`
}

export function parseVersionFromOutput(output: string): string | null {
  const match = output.match(/doughnut\s+(\d+\.\d+\.\d+)/)
  return match ? match[1] : null
}

export function compareVersions(a: string, b: string): number {
  const [aMajor, aMinor, aPatch] = a.split('.').map(Number)
  const [bMajor, bMinor, bPatch] = b.split('.').map(Number)
  if (aMajor !== bMajor) return aMajor - bMajor
  if (aMinor !== bMinor) return aMinor - bMinor
  return aPatch - bPatch
}
