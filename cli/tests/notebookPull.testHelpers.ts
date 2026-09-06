import { runGit } from './notebookClone.testHelpers.js'

export function checkoutState(directory: string) {
  return {
    head: runGit(['rev-parse', 'HEAD'], directory),
    branch: runGit(['rev-parse', '--abbrev-ref', 'HEAD'], directory),
    branches: runGit(
      ['for-each-ref', '--format=%(refname) %(objectname)', 'refs/heads'],
      directory
    ),
    status: runGit(['status', '--porcelain=v1'], directory),
    staged: runGit(['diff', '--cached'], directory),
    unstaged: runGit(['diff'], directory),
    notebookId: runGit(
      ['config', '--local', '--get', 'donut.notebook-id'],
      directory
    ),
    apiOrigin: runGit(
      ['config', '--local', '--get', 'donut.api-origin'],
      directory
    ),
  }
}
