import { execFileSync } from 'node:child_process'

export function createCliE2eNotebookListingTasks() {
  return {
    measureCliNotebookListing(checkoutDir: string) {
      const listing = execFileSync(
        'git',
        ['-C', checkoutDir, 'ls-tree', '-r', '-z', 'HEAD'],
        {
          maxBuffer: 8 * 1024 * 1024,
        }
      )
      const head = execFileSync(
        'git',
        ['-C', checkoutDir, 'rev-parse', 'HEAD'],
        { encoding: 'utf8' }
      ).trim()
      const result = {
        bytes: listing.byteLength,
        entries: listing.toString('utf8').split('\0').filter(Boolean).length,
        head,
      }
      console.log('Accepted notebook listing:', JSON.stringify(result))
      return result
    },
  }
}
