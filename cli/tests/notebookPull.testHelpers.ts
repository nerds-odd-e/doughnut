import { afterEach, beforeEach, vi } from 'vitest'
import {
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'

export function installNotebookPullAcceptedHistoryTest(workDirPrefix: string) {
  const base = installNotebookCliRunFixture(workDirPrefix)
  let fetchMock: ReturnType<typeof vi.fn>
  let logSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    logSpy.mockRestore()
  })

  return {
    ...base,
    getFetchMock: () => fetchMock,
    getLogSpy: () => logSpy,
  }
}

export function checkoutState(directory: string) {
  return {
    head: runGit(['rev-parse', 'HEAD'], directory),
    branch: runGit(['rev-parse', '--abbrev-ref', 'HEAD'], directory),
    refs: runGit(
      ['for-each-ref', '--format=%(refname) %(objectname)'],
      directory
    ),
    indexTree: runGit(['write-tree'], directory),
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
