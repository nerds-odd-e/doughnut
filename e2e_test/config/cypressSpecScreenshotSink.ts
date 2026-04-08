import { randomBytes } from 'node:crypto'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

export function attachCypressSpecScreenshotSink(
  on: Cypress.PluginEvents,
  screenshotsFolderAbsolute: string,
  fallbackSpecFolderName = 'cli-pty'
) {
  let currentSpecScreenshotFolderName: string | undefined
  on('before:spec', (spec: Cypress.Spec) => {
    currentSpecScreenshotFolderName = spec.name
  })
  on('after:spec', () => {
    currentSpecScreenshotFolderName = undefined
  })

  return {
    saveBufferToCurrentSpecFolder(
      stemPrefix: string,
      extensionWithDot: string,
      data: Buffer
    ): string {
      const specFolder =
        currentSpecScreenshotFolderName?.trim() || fallbackSpecFolderName
      const dir = join(screenshotsFolderAbsolute, specFolder)
      mkdirSync(dir, { recursive: true })
      const fileName = `${stemPrefix}-${Date.now()}-${randomBytes(4).toString('hex')}${extensionWithDot}`
      const filePath = join(dir, fileName)
      writeFileSync(filePath, data)
      return filePath
    },
  }
}
