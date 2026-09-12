import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { resolveSutCheckoutTarget } from '../../scripts/sut-isolated-target.mjs'
import {
  existingNoteIndex,
  existingNoteTitle,
  relatedReferenceIndex,
  sourceReferenceIndex,
} from './notebookPublicationFixture'

export function queryIsolatedSut(repoRoot: string, sql: string) {
  const { isolated, target } = resolveSutCheckoutTarget({
    checkoutRoot: repoRoot,
  })
  assert.ok(isolated, 'Only the allocated isolated database may be observed')
  return execFileSync(
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
    { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }
  ).trim()
}

export function publicationPersistedState(repoRoot: string) {
  const query = (sql: string) => queryIsolatedSut(repoRoot, sql)
  return {
    notes: query('SELECT * FROM note ORDER BY id'),
    noteIdentity: query(
      'SELECT id, notebook_id, folder_id, title, created_at, deleted_at FROM note ORDER BY id'
    ),
    learning: query('SELECT * FROM memory_tracker ORDER BY id'),
    bindings: query(
      'SELECT id, notebook_id, accepted_git_object_id, SHA2(bundle_bytes, 256), created_at, updated_at FROM notebook_git_binding ORDER BY id'
    ),
    aliasIndex: query(
      'SELECT n.title, i.alias_display, i.alias_lookup_key FROM note_alias_index i JOIN note n ON n.id = i.note_id ORDER BY n.title, i.alias_display'
    ),
    propertyIndex: query(
      'SELECT n.title, i.property_key, i.item_index, r.wiki_note_portion FROM note_property_index i JOIN note n ON n.id = i.note_id LEFT JOIN authored_note_reference r ON r.id = i.authored_note_reference_id ORDER BY n.title, i.property_key, i.item_index'
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
  assert.equal(
    after.aliasIndex,
    before.aliasIndex,
    'Every alias-index row is unchanged'
  )
  assert.equal(
    after.propertyIndex,
    before.propertyIndex,
    'Every property-index row is unchanged'
  )
  const processedAdditions = after.nextNoteId - before.nextNoteId
  assert.equal(
    processedAdditions,
    additions - 1,
    'All preceding additions allocated note identities before rollback'
  )
  return processedAdditions
}

function parseTsvRows(raw: string): string[][] {
  return raw === '' ? [] : raw.split('\n').map((line) => line.split('\t'))
}

export function expectPublicationEditsPersisted(
  before: ReturnType<typeof publicationPersistedState>,
  after: ReturnType<typeof publicationPersistedState>,
  parameters: { existing: number; updates: number }
) {
  assert.equal(
    after.noteIdentity,
    before.noteIdentity,
    'Every note identity (id, notebook, folder, title) is unchanged by the accepted edit'
  )
  assert.equal(
    after.learning,
    before.learning,
    'Every learning record is unchanged by the accepted edit'
  )

  const aliasRows = parseTsvRows(after.aliasIndex).filter(([noteTitle]) =>
    noteTitle!.startsWith('Existing-')
  )
  assert.equal(
    aliasRows.length,
    parameters.existing,
    'Every existing note keeps exactly one current alias after publication'
  )
  assert.ok(
    aliasRows.every(([, aliasDisplay]) =>
      aliasDisplay!.startsWith('Updated alias ')
    ),
    'Every existing-note alias reflects the accepted edit content'
  )

  const propertyRows = parseTsvRows(after.propertyIndex).filter(([noteTitle]) =>
    noteTitle!.startsWith('Existing-')
  )
  const relatedRows = propertyRows.filter(
    ([, propertyKey]) => propertyKey === 'related'
  )
  for (const [noteTitle, , itemIndex, targetPortion] of relatedRows) {
    const sourceIndex = existingNoteIndex(noteTitle!)
    assert.equal(
      targetPortion,
      existingNoteTitle(
        relatedReferenceIndex(
          sourceIndex,
          parameters.existing,
          Number(itemIndex)
        )
      ),
      `related reference ${itemIndex} target for ${noteTitle} points at the correct note`
    )
  }
  assert.equal(
    relatedRows.length,
    parameters.updates * 2,
    'Every edited note keeps exactly two related property-reference rows'
  )

  const sourceRows = propertyRows.filter(
    ([, propertyKey]) => propertyKey === 'source'
  )
  for (const [noteTitle, , , targetPortion] of sourceRows) {
    const sourceIndex = existingNoteIndex(noteTitle!)
    assert.equal(
      targetPortion,
      existingNoteTitle(sourceReferenceIndex(sourceIndex, parameters.existing)),
      `source reference target for ${noteTitle} points at the correct note`
    )
  }

  return {
    noteIdentitiesUnchanged: parseTsvRows(after.noteIdentity).length,
    learningUnchanged: true,
    exactUpdatedAliases: aliasRows.length,
    exactPropertyReferenceTargets: relatedRows.length,
  }
}
