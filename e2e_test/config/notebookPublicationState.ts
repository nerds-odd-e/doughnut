import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { resolveSutCheckoutTarget } from '../../scripts/sut-isolated-target.mjs'

export function publicationPersistedState(repoRoot: string) {
  const { isolated, target } = resolveSutCheckoutTarget({
    checkoutRoot: repoRoot,
  })
  assert.ok(isolated, 'Only the allocated isolated database may be observed')
  const query = (sql: string) =>
    execFileSync(
      'mysql',
      [
        '--protocol=TCP',
        '-h127.0.0.1',
        '-P3309',
        '-uroot',
        '--batch',
        '--skip-column-names',
        target.database,
        '-e',
        sql,
      ],
      { encoding: 'utf8' }
    ).trim()
  return {
    notes: query('SELECT * FROM note ORDER BY id'),
    learning: query('SELECT * FROM memory_tracker ORDER BY id'),
    bindings: query(
      'SELECT id, notebook_id, accepted_git_object_id, SHA2(bundle_bytes, 256), created_at, updated_at FROM notebook_git_binding ORDER BY id'
    ),
    nextNoteId: Number(
      query(
        "SET SESSION information_schema_stats_expiry=0; SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='note'"
      )
    ),
  }
}

export function expectPublicationStatePreserved(
  before: ReturnType<typeof publicationPersistedState>,
  after: ReturnType<typeof publicationPersistedState>,
  additions: number
) {
  assert.equal(
    after.notes,
    before.notes,
    'Every stored note row, identity and content is unchanged'
  )
  assert.equal(
    after.learning,
    before.learning,
    'Every learning record is unchanged'
  )
  assert.equal(after.bindings, before.bindings, 'Accepted binding is unchanged')
  const processedAdditions = after.nextNoteId - before.nextNoteId
  assert.equal(
    processedAdditions,
    additions - 1,
    'All preceding additions allocated note identities before rollback'
  )
  return processedAdditions
}
