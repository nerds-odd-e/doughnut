import { vi } from 'vitest'

vi.mock('node:child_process', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:child_process')>()
  return {
    ...actual,
    spawn: vi.fn(),
  }
})

import './mineruOutlineSubprocess.testHelpers.js'
import { describeMineruOutlineSubprocessErrors } from './mineruOutlineSubprocess.errors.suite.js'
import { describeMineruOutlineSubprocessJsonValidation } from './mineruOutlineSubprocess.jsonValidation.suite.js'
import { describeMineruOutlineSubprocessSpawn } from './mineruOutlineSubprocess.spawn.suite.js'
import { describeMineruOutlineSubprocessSuccess } from './mineruOutlineSubprocess.success.suite.js'

describeMineruOutlineSubprocessSuccess()
describeMineruOutlineSubprocessJsonValidation()
describeMineruOutlineSubprocessSpawn()
describeMineruOutlineSubprocessErrors()
