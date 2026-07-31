/**
 * Cypress task handlers for CLI workspace / config directories (no PTY).
 */

import {
  mkdirSync,
  mkdtempSync,
  readdirSync,
  readFileSync,
  unlinkSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { unzipExportedWorkspace } from './unzipExportedWorkspace'

export function createCliE2ePluginWorkspaceTasks() {
  return {
    createCliConfigDir() {
      return mkdtempSync(join(tmpdir(), 'cypress-cli-config-'))
    },
    /** An empty directory for a command to write into. */
    createCliEmptyDirectory() {
      return mkdtempSync(join(tmpdir(), 'cypress-cli-export-'))
    },
    readCliWorkspaceFile({
      workspace,
      relativePath,
    }: {
      workspace: string
      relativePath: string
    }) {
      return readFileSync(join(workspace, relativePath), 'utf8')
    },
    deleteCliWorkspaceFile({
      workspace,
      relativePath,
    }: {
      workspace: string
      relativePath: string
    }) {
      unlinkSync(join(workspace, relativePath))
      return null
    },
    writeCliWorkspaceFile({
      workspace,
      relativePath,
      content,
    }: {
      workspace: string
      relativePath: string
      content: string
    }) {
      const full = join(workspace, relativePath)
      mkdirSync(dirname(full), { recursive: true })
      writeFileSync(full, content, 'utf8')
      return null
    },
    /**
     * Retype a note's body in the workspace, the way an editor like Obsidian
     * would: the export's frontmatter and `# title` heading stay as they are,
     * so the file still reads as the same note with different content.
     */
    writeCliWorkspaceNoteBody({
      workspace,
      relativePath,
      content,
    }: {
      workspace: string
      relativePath: string
      content: string
    }) {
      const full = join(workspace, relativePath)
      const lines = readFileSync(full, 'utf8').split('\n')
      const heading = lines.findIndex((line) => line.startsWith('# '))
      if (heading === -1) {
        throw new Error(`No "# title" heading in ${relativePath} to keep.`)
      }
      // The export leaves one blank line between the heading and the body.
      const keep = lines.slice(0, heading + 2)
      writeFileSync(full, [...keep, content].join('\n'), 'utf8')
      return null
    },
    /** A workspace holding exactly what a notebook export zip contains. */
    createCliWorkspaceFromZip({ zipBase64 }: { zipBase64: string }) {
      const workspace = mkdtempSync(join(tmpdir(), 'cypress-cli-workspace-'))
      for (const [relativePath, content] of unzipExportedWorkspace(
        Buffer.from(zipBase64, 'base64')
      )) {
        const full = join(workspace, relativePath)
        mkdirSync(dirname(full), { recursive: true })
        writeFileSync(full, content, 'utf8')
      }
      return workspace
    },
    /** Every file in the workspace, as forward-slashed relative paths, sorted. */
    listCliWorkspaceFiles(workspace: string) {
      const found: string[] = []
      const walk = (directory: string, prefix: string) => {
        for (const entry of readdirSync(directory, { withFileTypes: true })) {
          const path = join(directory, entry.name)
          if (entry.isDirectory()) {
            walk(path, `${prefix}${entry.name}/`)
          } else {
            found.push(`${prefix}${entry.name}`)
          }
        }
      }
      walk(workspace, '')
      return found.sort()
    },
    createCliConfigDirWithGmail(gmailConfig: Record<string, unknown>) {
      const configDir = mkdtempSync(join(tmpdir(), 'cypress-cli-gmail-'))
      writeFileSync(
        join(configDir, 'gmail.json'),
        JSON.stringify(gmailConfig, null, 2)
      )
      return configDir
    },
  }
}
