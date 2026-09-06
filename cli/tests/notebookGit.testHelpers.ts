import * as fs from 'node:fs'
import { join } from 'node:path'
import { runGit } from './notebookClone.testHelpers.js'

// Sets the test-only Git identity a fresh local repo needs before it can commit.
export function configureTestGitIdentity(dir: string): void {
  runGit(['config', 'user.email', 'test@example.com'], dir)
  runGit(['config', 'user.name', 'Test'], dir)
}

// Initializes a fresh repo on `main` with one committed note.md — the common starting state
// shared by every checkout and source repo these tests build. `label` is folded into the note
// content so two independently-created repos never coincidentally commit identical content
// (which, combined with an identical author/committer timestamp, would produce the same SHA and
// make an "unrelated history" checkout indistinguishable from the accepted head it should differ
// from).
export function initGitRepoWithInitialNote(dir: string, label: string): void {
  fs.mkdirSync(dir)
  runGit(['init', '--quiet', '-b', 'main'], dir)
  configureTestGitIdentity(dir)
  fs.writeFileSync(join(dir, 'note.md'), `# hello notebook (${label})\n`)
  runGit(['add', 'note.md'], dir)
  runGit(['commit', '--quiet', '-m', 'initial notebook commit'], dir)
}

// Records the local-only Git config binding that `notebook clone` would have recorded.
export function bindNotebookCheckout(dir: string, apiOrigin: string): void {
  runGit(['config', '--local', 'donut.notebook-id', '42'], dir)
  runGit(['config', '--local', 'donut.api-origin', apiOrigin], dir)
}

// Produces a bound checkout that is also clean and committed on `main` — the
// baseline eligible state for notebook commands.
export function initBoundCheckout(workDir: string, apiOrigin: string): string {
  const dir = join(workDir, 'checkout')
  initGitRepoWithInitialNote(dir, 'checkout')
  bindNotebookCheckout(dir, apiOrigin)
  return dir
}
