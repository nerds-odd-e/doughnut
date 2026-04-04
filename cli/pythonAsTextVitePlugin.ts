import { readFileSync } from 'node:fs'
import { dirname, isAbsolute, resolve } from 'node:path'
import type { Plugin } from 'vite'

const virtualPrefix = '\0pytext:'

/** Vitest/Vite: serve `.py` imports as string literals (esbuild bundle uses `--loader:.py=text`). */
export function pythonAsTextVitePlugin(): Plugin {
  return {
    name: 'python-as-text',
    enforce: 'pre',
    resolveId(source, importer) {
      if (!source.endsWith('.py') || source.includes('\0')) {
        return undefined
      }
      const absolute = isAbsolute(source)
        ? source
        : resolve(dirname(importer ?? '.'), source)
      return virtualPrefix + absolute
    },
    load(id) {
      if (!id.startsWith(virtualPrefix)) {
        return undefined
      }
      const absolutePath = id.slice(virtualPrefix.length)
      const text = readFileSync(absolutePath, 'utf8')
      return `export default ${JSON.stringify(text)}`
    },
  }
}
