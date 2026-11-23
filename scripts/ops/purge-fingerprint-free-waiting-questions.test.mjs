import assert from 'node:assert/strict'
import { execFileSync, spawnSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import { startDisposableMysql } from '../worktree-retirement-disposable-mysql.mjs'

const operator = fileURLToPath(
  new URL('./purge-fingerprint-free-waiting-questions.mjs', import.meta.url)
)
const dueAt = '2027-01-10 12:00:00'
const historicalStem = 'Which landmark was built first?'
const historicalChoices = ['The clock', 'The fountain']

const fixture = `
  INSERT INTO \`user\` (id, name, external_identifier) VALUES (1, 'Learner', 'rehearsal');
  INSERT INTO ownership (id, user_id) VALUES (1, 1);
  INSERT INTO note (id, title) VALUES (1, 'Landmarks');
  INSERT INTO memory_tracker (id, user_id, note_id, next_recall_at)
    VALUES (1, 1, 1, '${dueAt}');
  INSERT INTO answer (id, result, choice_index) VALUES (1, 1, 1);
  INSERT INTO recall_log (id, memory_tracker_id, recorded_at, elapsed_hours, product_outcome, answer_id)
    VALUES (1, 1, '2027-01-09 11:00:00', 24, 'GOOD', 1);

  INSERT INTO mcq (id, raw_json_question, is_contested, correct_answer_index) VALUES
    (1, JSON_OBJECT('questionStem', '${historicalStem}',
                    'responseChoices', CAST('${JSON.stringify(historicalChoices)}' AS JSON)), FALSE, 1),
    (2, '{"focusNoteFingerprint":null}', FALSE, 0),
    (3, '{unreadable', FALSE, 0),
    (4, JSON_OBJECT('focusNoteFingerprint', REPEAT('a', 64)), FALSE, 0),
    (5, '{}', TRUE, 0),
    (6, '{}', FALSE, 0),
    (7, '{"focusNoteFingerprint":"null"}', FALSE, 0),
    (8, NULL, FALSE, 0),
    (9, '{}', FALSE, 0);
  INSERT INTO recall_prompt (id, memory_tracker_id, question_type, mcq_id, answer_id) VALUES
    (101, 1, 'MCQ', 1, NULL),
    (102, 1, 'MCQ', 1, 1),
    (103, 1, 'MCQ', 2, NULL),
    (104, 1, 'MCQ', 3, NULL),
    (105, 1, 'MCQ', 4, NULL),
    (106, 1, 'MCQ', 5, NULL),
    (107, 1, 'SPELLING', 6, NULL),
    (108, 1, 'MCQ', 7, NULL),
    (109, 1, 'MCQ', 8, NULL);
  INSERT INTO conversation (id, subject_ownership_id, conversation_initiator_id, recall_prompt_id)
    VALUES (1, 1, 1, 103), (2, 1, 1, 102);
  INSERT INTO conversation_message (id, conversation_id, message, sender)
    VALUES (1, 1, 'Discuss the old question', 1);
  INSERT INTO conversation_note_correction
    (id, source_message_id, note_id, response_id, tool_call_id, field, proposed_value, reason)
    VALUES (1, 1, 1, 'response', 'tool', 'CONTENT', 'Landmark details', 'Clarify the note');
`

function migratedSchema() {
  const config = new URL('../../.worktree.local.json', import.meta.url)
  const { id } = JSON.parse(readFileSync(config, 'utf8'))
  assert.match(id, /^wt_[a-z0-9_]{1,32}$/)
  return execFileSync(
    'mysqldump',
    [
      '--no-defaults',
      '--host=127.0.0.1',
      '--port=3309',
      '--user=root',
      '--no-data',
      '--no-tablespaces',
      '--set-gtid-purged=OFF',
      `doughnut_${id}_test`,
    ],
    { encoding: 'utf8' }
  )
}

function seedDatabase(mysql, database, schema) {
  mysql.execSql(`CREATE DATABASE ${database}`)
  execFileSync(
    'mysql',
    [
      '--no-defaults',
      '--host=127.0.0.1',
      `--port=${mysql.port}`,
      '--user=root',
      `--database=${database}`,
    ],
    { input: `${schema}\n${fixture}` }
  )
}

function runOperator(mysql, database, mode, ...args) {
  const command = spawnSync(
    process.execPath,
    [
      operator,
      mode,
      '--host',
      '127.0.0.1',
      '--port',
      String(mysql.port),
      '--user',
      'root',
      '--database',
      database,
      ...args,
    ],
    { encoding: 'utf8' }
  )
  assert.ifError(command.error)
  return command
}

function operatorResult(command, status = 0) {
  assert.equal(command.status, status, command.stderr)
  return JSON.parse(command.stdout)
}

test('waiting question purge', async (t) => {
  const schema = migratedSchema()
  const mysql = await startDisposableMysql(t)
  assert.match(mysql.version, /^8\.4\./)

  const query = (sql, database = 'rehearsal') =>
    mysql.query(`USE ${database}; ${sql}`)
  const promptIds = (database) =>
    query('SELECT id FROM recall_prompt ORDER BY id', database)
      .split('\n')
      .map(Number)
  const originalPrompts = [101, 102, 103, 104, 105, 106, 107, 108, 109]

  await t.test('refuses confirmation from a different database', () => {
    seedDatabase(mysql, 'preview_target', schema)
    seedDatabase(mysql, 'different_target', schema)
    const reviewed = operatorResult(
      runOperator(mysql, 'preview_target', 'preview')
    )
    const wrongTarget = operatorResult(
      runOperator(
        mysql,
        'different_target',
        'apply',
        '--confirm',
        reviewed.confirmation
      ),
      1
    )
    assert.equal(wrongTarget.applied, 0)
    assert.equal(wrongTarget.confirmation, null)
    assert.equal(wrongTarget.eligiblePrompts, reviewed.eligiblePrompts)
    assert.deepEqual(promptIds('different_target'), originalPrompts)
  })

  await t.test(
    'purges only the reviewed waiting prompts and their unreferenced questions',
    (purge) => {
      seedDatabase(mysql, 'rehearsal', schema)
      const preview = () =>
        operatorResult(runOperator(mysql, 'rehearsal', 'preview'))
      const reviewed = preview()
      assert.equal(reviewed.database, 'rehearsal')
      assert.equal(reviewed.serverUuid, query('SELECT @@server_uuid'))
      assert.equal(reviewed.eligiblePrompts, 4)
      assert.deepEqual(promptIds(), originalPrompts)

      // A reviewed prompt gets an answer, with a different legacy prompt keeping the total the same.
      query(`
      INSERT INTO answer (id, result, choice_index) VALUES (2, 1, 1);
      UPDATE recall_prompt SET answer_id = 2 WHERE id = 101;
      INSERT INTO recall_prompt (id, memory_tracker_id, question_type, mcq_id)
        VALUES (110, 1, 'MCQ', 1)`)
      const staleReview = operatorResult(
        runOperator(
          mysql,
          'rehearsal',
          'apply',
          '--confirm',
          reviewed.confirmation
        ),
        1
      )
      assert.equal(staleReview.applied, 0)
      assert.equal(staleReview.confirmation, null)
      assert.equal(staleReview.eligiblePrompts, 4)
      assert.deepEqual(promptIds(), [...originalPrompts, 110])

      const fresh = preview()
      const applied = operatorResult(
        runOperator(
          mysql,
          'rehearsal',
          'apply',
          '--confirm',
          fresh.confirmation
        )
      )
      assert.equal(applied.confirmation, fresh.confirmation)
      assert.equal(applied.applied, 1)
      assert.equal(applied.removedPrompts, 4)
      assert.equal(applied.removedQuestions, 3)
      assert.deepEqual(promptIds(), [101, 102, 105, 106, 107, 108])
      assert.equal(query('SELECT id FROM mcq ORDER BY id'), '1\n4\n5\n6\n7\n9')
      assert.equal(query('SELECT is_contested FROM mcq WHERE id = 5'), '1')
      assert.equal(
        query('SELECT answer_id FROM recall_prompt WHERE id = 101'),
        '2'
      )

      const history = query(`
      SELECT question.raw_json_question, question.correct_answer_index,
             answer.choice_index, answer.result, log.product_outcome
      FROM recall_prompt AS prompt
      JOIN mcq AS question ON question.id = prompt.mcq_id
      JOIN answer ON answer.id = prompt.answer_id
      JOIN recall_log AS log
        ON log.answer_id = answer.id AND log.memory_tracker_id = prompt.memory_tracker_id
      WHERE prompt.id = 102`)
      const [json, answerKey, choice, correct, grade] = history.split('\t')
      assert.deepEqual(JSON.parse(json), {
        questionStem: historicalStem,
        responseChoices: historicalChoices,
      })
      assert.deepEqual(
        [answerKey, choice, correct, grade],
        ['1', '1', '1', 'GOOD']
      )
      assert.equal(
        query('SELECT next_recall_at FROM memory_tracker WHERE id = 1'),
        dueAt
      )
      assert.equal(
        query('SELECT recall_prompt_id FROM conversation ORDER BY id'),
        'NULL\n102'
      )
      assert.equal(
        query(`
        SELECT message.message, correction.proposed_value
        FROM conversation AS discussion
        JOIN conversation_message AS message ON message.conversation_id = discussion.id
        JOIN conversation_note_correction AS correction ON correction.source_message_id = message.id
        WHERE discussion.id = 1`),
        'Discuss the old question\tLandmark details'
      )
      const finalPreview = preview()
      assert.equal(finalPreview.eligiblePrompts, 0)
      purge.diagnostic(
        `MySQL ${mysql.version}: preview ${reviewed.eligiblePrompts}, removed ${applied.removedPrompts} prompts and ${applied.removedQuestions} questions, repeat preview ${finalPreview.eligiblePrompts}`
      )
    }
  )
})
