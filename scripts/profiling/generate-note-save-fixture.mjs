#!/usr/bin/env node
/**
 * Deterministic no-attachment notebook fixture generator for the note-save
 * measurement (SEED-034#story-4, slice 1). One script for one purpose: given
 * only its counts, it writes the same Portable bytes and paths every time, so
 * the same content can be imported into two isolated setups (an earlier
 * revision and the current one) and their exports compared.
 *
 * It emits Portable content only — Markdown notes, container READMEs and Wiki
 * references. No attachments, no randomness, no timestamps, no network, no
 * service lifecycle. Seeding a running application is the caller's job: the
 * emitted `fixture.json` carries the exact `injectNotes` payload, and the
 * emitted `portable/` tree carries the same content as files for export
 * comparison.
 *
 * Usage:
 *   node scripts/profiling/generate-note-save-fixture.mjs <output-directory>
 *
 * Env vars (all optional, all integers):
 *   NOTE_SAVE_FIXTURE_NOTES       notes in the notebook (default 11000)
 *   NOTE_SAVE_FIXTURE_FOLDERS     folders to distribute them over (default 40)
 *   NOTE_SAVE_FIXTURE_REVISIONS   history increments after the root (default 4)
 *   NOTE_SAVE_FIXTURE_REVISION_EDITS notes edited per increment (default 10)
 *   NOTE_SAVE_FIXTURE_ATTACHMENTS set to `realistic` to also emit root
 *                                 attachment files (default: none)
 *
 * TEMPORARY (slice 13, deleted by slice 14): with `realistic` attachments the
 * notes, `portable/` tree and digests are byte-identical to the no-attachment
 * payload, so the same notebook is measured with and without files. The files
 * land in `attachments/`, deterministic incompressible bytes of realistic
 * sizes; they are published afterward through the product's own Git
 * publication, never seeded as rows.
 *
 * The output directory must be disposable and outside the repository; the
 * generated payload is never tracked source.
 */
import { createHash } from 'node:crypto'
import { mkdirSync, writeFileSync, rmSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'

const NOTEBOOK = 'Note Save Measurement'

function countSetting(name, fallback) {
  const value = Number(process.env[name] ?? fallback)
  if (!Number.isInteger(value) || value < 1)
    throw new Error(`${name} must be a positive integer`)
  return value
}

const notes = countSetting('NOTE_SAVE_FIXTURE_NOTES', 11000)
const folders = countSetting('NOTE_SAVE_FIXTURE_FOLDERS', 40)
const revisions = countSetting('NOTE_SAVE_FIXTURE_REVISIONS', 4)
const revisionEdits = countSetting('NOTE_SAVE_FIXTURE_REVISION_EDITS', 10)
const withAttachments =
  process.env.NOTE_SAVE_FIXTURE_ATTACHMENTS === 'realistic'

const noteTitle = (i) => `Note-${String(i).padStart(5, '0')}`
const folderName = (i) => `Topic-${String(i).padStart(2, '0')}`
const folderOf = (i) => folderName(i % folders)

/** Wiki reference targets for note `i`: one property source, two property
 * related, two body links. Every target is another note in the same notebook,
 * so every reference resolves. */
const references = (i) => [
  noteTitle((i * 7 + 1) % notes),
  noteTitle((i * 13 + 2) % notes),
  noteTitle((i * 29 + 3) % notes),
  noteTitle((i + 1) % notes),
  noteTitle((i + folders) % notes),
]
const referencesPerNote = references(0).length

const PROSE = 'Deterministic authored prose for save measurement. '.repeat(16)

function noteContent(i, revision) {
  const [source, related0, related1, body0, body1] = references(i)
  return `---
type: Note
meaning: 'Measured concept ${i}'
source: '[[${source}]]'
related:
  - '[[${related0}]]'
  - '[[${related1}]]'
---
Measured concept ${i}, revision ${revision}. ${PROSE}
See [[${body0}]] and [[${body1}]].
`
}

const landingPage = (name) => `---
type: Note
---
Landing page for ${name}.
`

const folderReadme = (i) => landingPage(folderName(i))
const notebookReadme = landingPage(NOTEBOOK)

/** Note indexes edited by history increment `r` (1-based). Disjoint per
 * increment so every increment changes real content. */
const editedIn = (r) =>
  Array.from(
    { length: revisionEdits },
    (_, k) => ((r - 1) * revisionEdits + k) % notes
  )

/** The one authority for how far each note has been edited at a given history
 * depth: note index -> its revision number. Unedited notes are absent. */
function revisionsAt(depth) {
  const edited = new Map()
  for (let r = 1; r <= depth; r += 1)
    for (const i of editedIn(r)) edited.set(i, r)
  return edited
}

/** The Portable tree at a given history depth: path -> exact file content. */
function portableTree(depth) {
  const edited = revisionsAt(depth)
  const tree = new Map()
  tree.set('README.md', notebookReadme)
  for (let f = 0; f < folders; f += 1)
    tree.set(`${folderName(f)}/README.md`, folderReadme(f))
  for (let i = 0; i < notes; i += 1)
    tree.set(
      `${folderOf(i)}/${noteTitle(i)}.md`,
      noteContent(i, edited.get(i) ?? 0)
    )
  return new Map([...tree].sort(([a], [b]) => (a < b ? -1 : 1)))
}

function digest(tree) {
  const hash = createHash('sha256')
  for (const [path, content] of tree) hash.update(`${path}\0${content}\0`)
  return hash.digest('hex')
}

if (!process.argv[2]) throw new Error('An output directory is required')
const outputDirectory = resolve(process.argv[2])

const finalTree = portableTree(revisions)
rmSync(outputDirectory, { recursive: true, force: true })
for (const [path, content] of finalTree) {
  const file = join(outputDirectory, 'portable', path)
  mkdirSync(dirname(file), { recursive: true })
  writeFileSync(file, content)
}

/** The seeding payload: the notebook's root README, its folder READMEs and
 * every note, as the application's existing note injection understands them. */
const finalRevisions = revisionsAt(revisions)
const fixture = {
  notebook: NOTEBOOK,
  readme: notebookReadme,
  folders: Array.from({ length: folders }, (_, f) => ({
    Folder: folderName(f),
    Readme: folderReadme(f),
  })),
  notes: Array.from({ length: notes }, (_, i) => ({
    Title: noteTitle(i),
    Folder: folderOf(i),
    Content: noteContent(i, finalRevisions.get(i) ?? 0),
  })),
  revisions: Array.from({ length: revisions }, (_, r) =>
    editedIn(r + 1).map((i) => ({
      relativePath: `${folderOf(i)}/${noteTitle(i)}.md`,
      content: noteContent(i, r + 1),
    }))
  ),
}
writeFileSync(
  join(outputDirectory, 'fixture.json'),
  `${JSON.stringify(fixture, null, 2)}\n`
)

/** A mix a note-taker keeps at a notebook root: icons, screenshots, photos
 * and a couple of PDFs. Sizes vary within each kind. */
const KB = 1024
const attachmentSizes = withAttachments
  ? [
      ...Array.from({ length: 8 }, (_, k) => [
        `icon-${k + 1}.png`,
        (4 + k) * KB,
      ]),
      ...Array.from({ length: 12 }, (_, k) => [
        `screenshot-${String(k + 1).padStart(2, '0')}.png`,
        (80 + k * 20) * KB,
      ]),
      ...Array.from({ length: 6 }, (_, k) => [
        `photo-${k + 1}.jpg`,
        (400 + k * 160) * KB,
      ]),
      ['reference-paper.pdf', 2048 * KB],
      ['lecture-slides.pdf', 3072 * KB],
    ]
  : []

/** Deterministic incompressible bytes, like real image/PDF payloads. */
function attachmentBytes(name, size) {
  const chunks = []
  for (let block = 0; block * 32 < size; block += 1)
    chunks.push(createHash('sha256').update(`${name}\0${block}`).digest())
  return Buffer.concat(chunks).subarray(0, size)
}

for (const [name, size] of attachmentSizes) {
  const file = join(outputDirectory, 'attachments', name)
  mkdirSync(dirname(file), { recursive: true })
  writeFileSync(file, attachmentBytes(name, size))
}

const counts = {
  notes,
  folders,
  containerReadmes: folders + 1,
  portableFiles: finalTree.size,
  wikiReferencesPerNote: referencesPerNote,
  wikiReferences: notes * referencesPerNote,
  historyCommits: revisions + 1,
  attachments: attachmentSizes.length,
  bytes: [...finalTree.values()].reduce((sum, c) => sum + c.length, 0),
}
const historyDigests = Array.from({ length: revisions + 1 }, (_, depth) =>
  digest(depth === revisions ? finalTree : portableTree(depth))
)
const manifest = {
  notebook: NOTEBOOK,
  counts,
  portableDigest: historyDigests[revisions],
  historyDigests,
  ...(withAttachments && {
    rootAttachments: {
      totalBytes: attachmentSizes.reduce((sum, [, size]) => sum + size, 0),
      files: Object.fromEntries(attachmentSizes),
    },
  }),
}
const manifestJson = `${JSON.stringify(manifest, null, 2)}\n`
writeFileSync(join(outputDirectory, 'manifest.json'), manifestJson)
process.stdout.write(manifestJson)
