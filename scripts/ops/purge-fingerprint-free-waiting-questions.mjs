import { execFileSync } from 'node:child_process'
import { parseArgs } from 'node:util'

const usage = `Usage:
  node scripts/ops/purge-fingerprint-free-waiting-questions.mjs preview \\
    --host HOST --port PORT --user USER --database DATABASE [--login-path PATH]
  node scripts/ops/purge-fingerprint-free-waiting-questions.mjs apply \\
    --host HOST --port PORT --user USER --database DATABASE [--login-path PATH] \\
    --confirm PREVIEW_CONFIRMATION`

const eligibility = `
  prompt.question_type = 'MCQ'
  AND prompt.answer_id IS NULL
  AND question.is_contested = FALSE
  AND COALESCE(JSON_TYPE(JSON_EXTRACT(
    IF(JSON_VALID(question.raw_json_question), question.raw_json_question, NULL),
    '$.focusNoteFingerprint'
  )), 'NULL') = 'NULL'`

const eligiblePrompts = `
  SELECT prompt.id AS prompt_id, question.id AS question_id
  FROM recall_prompt AS prompt
  JOIN mcq AS question ON question.id = prompt.mcq_id
  WHERE ${eligibility}`

// Hash each ID pair and fold the hashes without concatenating a list of IDs.
// Bind the receipt to the MySQL instance and schema as well as the exact rows.
function recordSelection(source) {
  return `
    SELECT COUNT(*), SHA2(CONCAT_WS('/', @@server_uuid, DATABASE(), COUNT(*),
      HEX(BIT_XOR(UNHEX(SHA2(CONCAT_WS('/', prompt_id, question_id), 256))))
    ), 256)
    INTO @eligible_prompts, @confirmation
    FROM ${source};`
}

const selectionFields = `
  'serverUuid', @@server_uuid,
  'database', DATABASE(),
  'eligiblePrompts', @eligible_prompts`

const previewSql = `
  START TRANSACTION READ ONLY;
  ${recordSelection(`(${eligiblePrompts}) AS candidates`)}
  SELECT JSON_OBJECT(${selectionFields}, 'confirmation', @confirmation);
  COMMIT;`

function applySql(confirmation) {
  return `
    CREATE TEMPORARY TABLE candidates (
      prompt_id INT UNSIGNED PRIMARY KEY,
      question_id INT UNSIGNED NOT NULL
    ) ENGINE=InnoDB;
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;
    INSERT INTO candidates (prompt_id, question_id)
    ${eligiblePrompts}
    FOR UPDATE;
    ${recordSelection('candidates')}
    SET @reviewed = @confirmation = '${confirmation}';

    -- The prompt FK detaches conversations while preserving their content.
    DELETE prompt FROM recall_prompt AS prompt
    JOIN candidates ON candidates.prompt_id = prompt.id
    JOIN mcq AS question ON question.id = candidates.question_id
    WHERE @reviewed AND prompt.mcq_id = question.id AND ${eligibility};
    SET @removed_prompts = ROW_COUNT();
    SET @applied = @reviewed AND @removed_prompts = @eligible_prompts;

    -- Keep any MCQ still referenced by a prompt, including answered siblings.
    DELETE question FROM mcq AS question
    JOIN candidates ON candidates.question_id = question.id
    WHERE @applied AND NOT EXISTS (
      SELECT 1 FROM recall_prompt WHERE recall_prompt.mcq_id = question.id
    );
    SET @removed_questions = ROW_COUNT();
    SET @finish = IF(@applied, 'COMMIT', 'ROLLBACK');
    PREPARE finish_purge FROM @finish;
    EXECUTE finish_purge;

    SELECT JSON_OBJECT(${selectionFields}, 'applied', @applied,
      'confirmation', IF(@applied, @confirmation, NULL),
      'removedPrompts', IF(@applied, @removed_prompts, 0),
      'removedQuestions', IF(@applied, @removed_questions, 0));`
}

function main() {
  const { values, positionals } = parseArgs({
    allowPositionals: true,
    options: {
      host: { type: 'string' },
      port: { type: 'string' },
      user: { type: 'string' },
      database: { type: 'string' },
      'login-path': { type: 'string' },
      confirm: { type: 'string' },
    },
  })
  const [mode] = positionals
  if (
    positionals.length !== 1 ||
    !values.host ||
    !values.port ||
    !values.user ||
    !values.database ||
    (mode !== 'preview' && mode !== 'apply') ||
    (mode === 'preview' && values.confirm !== undefined) ||
    (mode === 'apply' && !/^[a-f0-9]{64}$/.test(values.confirm ?? ''))
  ) {
    throw new Error(usage)
  }

  const connection = []
  for (const option of ['login-path', 'host', 'port', 'user', 'database']) {
    if (values[option] !== undefined) {
      connection.push(`--${option}=${values[option]}`)
    }
  }
  const output = execFileSync(
    'mysql',
    [
      '--no-defaults',
      ...connection,
      '--protocol=TCP',
      '--skip-reconnect',
      '--batch',
      '--raw',
      '--skip-column-names',
    ],
    {
      input: mode === 'preview' ? previewSql : applySql(values.confirm),
      encoding: 'utf8',
    }
  )
  const result = JSON.parse(output)
  console.log(JSON.stringify(result))
  if (mode === 'apply' && !result.applied) {
    throw new Error(
      'Purge rolled back. Review a fresh preview before applying.'
    )
  }
}

try {
  main()
} catch (error) {
  console.error(error.message)
  process.exitCode = 1
}
