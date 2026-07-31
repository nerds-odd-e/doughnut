import { cliAssertTask } from './cliAssertTask'
import { interactiveCli } from './interactiveCli'
import { resolveWorkspaceDir } from './workspace'

/**
 * `/sync` pulling a notebook down into a workspace: the command a scenario
 * runs, and how long it is allowed to take. The files it lands in live in
 * `workspace`.
 */
export function pullIntoWorkspace(workspaceName: string) {
  return interactiveCli().enterSlashCommandInInteractiveCli(
    `/sync ${resolveWorkspaceDir(workspaceName)}`
  )
}

export function pullIntoWorkspaceWithinSeconds(
  workspaceName: string,
  seconds: number
) {
  const startedAt = Date.now()
  const dir = resolveWorkspaceDir(workspaceName)
  return interactiveCli()
    .enterSlashCommandInInteractiveCli(`/sync ${dir}`)
    .then(() =>
      cliAssertTask({
        strict: false,
        needle: 'note updated.',
        surface: 'strippedTranscript',
        messagePrefix: 'Past CLI assistant messages (pull timing).',
        timeoutMs: 120_000,
      })
    )
    .then(() => {
      const elapsedMs = Date.now() - startedAt
      expect(
        elapsedMs,
        `sync should finish within ${seconds}s but took ${elapsedMs}ms`
      ).to.be.lessThan(seconds * 1000)
    })
}
