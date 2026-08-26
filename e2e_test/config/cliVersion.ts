/**
 * Pure CLI version helpers for E2E (safe for Cypress browser bundle).
 * Product default version lives in cli/package.json.
 */

export function formatCliVersionBanner(version: string): string {
  return `donut ${version}`
}

/** Next major — always newer than the installed package version for update scenarios. */
export function newerCliVersionThan(version: string): string {
  const major = Number(version.split('.')[0])
  return `${major + 1}.0.0`
}

export function cliUpdateSuccessMessage(
  fromVersion: string,
  toVersion: string
): string {
  return `Updated donut from ${fromVersion} to ${toVersion}`
}
