import { runWorktreeRetire } from './worktree-retirement.mjs'

export function makeWritable() {
  let content = ''
  return {
    write(chunk) {
      content += chunk
    },
    content() {
      return content
    },
  }
}

export const clearEvidenceDeps = {
  listDatabaseSessionsFn: async () => [],
}

export async function runCheck(checkoutRoot, evidenceDeps = clearEvidenceDeps) {
  const out = makeWritable()
  const err = makeWritable()
  const code = await runWorktreeRetire({
    argv: ['--check'],
    checkoutRoot,
    out,
    err,
    evidenceDeps,
  })
  return { code, out: out.content(), err: err.content() }
}
