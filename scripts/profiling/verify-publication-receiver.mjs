/**
 * Bulk, read-only verification that a publication receiver checkout holds
 * every expected file with exactly matching bytes — one filesystem walk
 * instead of one Cypress `cy.readFile` browser command per file.
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * @typedef {{ relativePath: string, content: string }} ExpectedPublicationFile
 * @typedef {{ relativePath: string, reason: 'missing' | 'changed' }} PublicationReceiverMismatch
 */

/**
 * Checks every expected file against the receiver checkout in order, stopping
 * at the first mismatch so its path is reported. Returns `null` when every
 * file exists and matches byte-for-byte.
 *
 * @param {string} checkoutDir
 * @param {ExpectedPublicationFile[]} files
 * @returns {PublicationReceiverMismatch | null}
 */
export function findPublicationReceiverMismatch(checkoutDir, files) {
  for (const { relativePath, content } of files) {
    let actual
    try {
      actual = readFileSync(join(checkoutDir, relativePath), 'utf8')
    } catch (error) {
      if (error && error.code === 'ENOENT') {
        return { relativePath, reason: 'missing' }
      }
      throw error
    }
    if (actual !== content) {
      return { relativePath, reason: 'changed' }
    }
  }
  return null
}
