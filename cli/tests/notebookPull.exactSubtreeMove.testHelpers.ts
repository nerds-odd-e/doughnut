import * as fs from 'node:fs'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { commitPortableFile } from './notebookPull.testHelpers.js'

export const FOLDER_README = '---\ntype: Readme\n---\n# Folder\n'
export const PASTA_ORIGINAL = '---\ntype: Note\n---\n# Pasta\n\nBoil water\n'
export const PASTA_LOCAL =
  '---\ntype: Note\n---\n# Pasta\n\nSimmer until al dente\n'
export const SAUCE_BYTES =
  '---\ntype: Note\n---\n# Sauce\n\nIdentical twin body.\n'
export const OUTSIDE_NOTE = '---\ntype: Note\n---\n# Outside\n\nUnmoved note.\n'

export const STRUCTURAL_CHANGE_REFUSAL =
  /^donut: Local main cannot receive the accepted history because accepted history includes a structural change at ".+"\. Divergent structural history is not supported yet\.$/

export function seedRecipesKitchenTree(source: string): void {
  commitPortableFile(
    source,
    'Kitchen/README.md',
    FOLDER_README.replace('# Folder', '# Kitchen'),
    'add Kitchen'
  )
  commitPortableFile(
    source,
    'Recipes/README.md',
    FOLDER_README.replace('# Folder', '# Recipes'),
    'add Recipes'
  )
  commitPortableFile(source, 'Recipes/Pasta.md', PASTA_ORIGINAL, 'add Pasta')
  commitPortableFile(source, 'outside.md', OUTSIDE_NOTE, 'add outside note')
}

export function commitExactFolderMove(
  directory: string,
  sourceFolder: string,
  destFolder: string
): void {
  const slash = destFolder.lastIndexOf('/')
  if (slash >= 0) {
    fs.mkdirSync(join(directory, destFolder.slice(0, slash)), {
      recursive: true,
    })
  }
  runGit(['mv', sourceFolder, destFolder], directory)
  runGit(
    ['commit', '--quiet', '-m', `move ${sourceFolder} to ${destFolder}`],
    directory
  )
}

export function prepareExactSubtreeMoveWithLocalDescendantEdit(
  workDir: string,
  options: {
    localEditPath: string
    localBytes: string
    move: { from: string; to: string }
    seed?: (source: string) => void
  }
): {
  directory: string
  source: string
  localTip: string
  acceptedHead: string
  baseHead: string
} {
  const source = buildSourceRepo(workDir)
  seedRecipesKitchenTree(source)
  options.seed?.(source)
  const baseHead = runGit(['rev-parse', 'main'], source)
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )
  commitPortableFile(
    directory,
    options.localEditPath,
    options.localBytes,
    'unpublished descendant edit'
  )
  const localTip = runGit(['rev-parse', 'HEAD'], directory)
  commitExactFolderMove(source, options.move.from, options.move.to)
  const acceptedHead = runGit(['rev-parse', 'main'], source)
  return { directory, source, localTip, acceptedHead, baseHead }
}

export function listHeadTree(directory: string): string {
  return runGit(['ls-tree', '-r', '--name-only', 'HEAD'], directory)
}

export function headTreeBlobMap(directory: string): Map<string, string> {
  const raw = runGit(['ls-tree', '-r', 'HEAD'], directory)
  const map = new Map<string, string>()
  if (raw === '') return map
  for (const line of raw.split('\n')) {
    const match = /^(\d{6}) blob ([0-9a-f]+)\t(.+)$/.exec(line)
    if (!match) {
      throw new Error(`unexpected ls-tree line: ${line}`)
    }
    map.set(match[3]!, match[2]!)
  }
  return map
}
