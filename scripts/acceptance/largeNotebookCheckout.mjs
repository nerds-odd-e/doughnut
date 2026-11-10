import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

export function makeLargeNotebookCheckout(root) {
  const directory = join(root, 'large-notebook')
  mkdirSync(directory)
  const git = (args, input) =>
    execFileSync('git', ['-C', directory, ...args], {
      input,
      encoding: 'utf8',
      maxBuffer: 4 * 1024 * 1024,
      env: {
        ...process.env,
        GIT_CONFIG_GLOBAL: '/dev/null',
        GIT_CONFIG_NOSYSTEM: '1',
      },
    }).trim()
  git(['init', '--quiet', '-b', 'main'])
  const paths = Array.from(
    { length: 4200 },
    (_, index) => `${'folder-'.repeat(30)}${String(index).padStart(5, '0')}.md`
  )
  const content = (body) => `---\ntype: Note\n---\n${body}\n`
  const append = (files, body, parent) => {
    let stream =
      'commit refs/heads/main\ncommitter Acceptance <acceptance@example.test> 1 +0000\ndata 6\nchange\n'
    if (parent) stream += `from ${parent}\n`
    for (const path of files) {
      const bytes = content(body)
      stream += `M 100644 inline ${path}\ndata ${Buffer.byteLength(bytes)}\n${bytes}\n`
    }
    git(['fast-import', '--quiet'], stream)
    return git(['rev-parse', 'main'])
  }
  const accepted = append(paths, 'Accepted body')
  const listingBytes = execFileSync(
    'git',
    ['-C', directory, 'ls-tree', '-r', '-z', accepted],
    { maxBuffer: 4 * 1024 * 1024 }
  ).byteLength
  assert.ok(
    listingBytes > 1024 * 1024,
    `Full accepted listing: ${listingBytes}`
  )
  const acceptedBundle = join(root, 'accepted.bundle')
  git(['bundle', 'create', acceptedBundle, 'main'])
  const edits = paths.slice(0, 3)
  const proposed = append(edits, 'Published small edit', accepted)
  git(['reset', '--quiet', '--hard', 'main'])
  return {
    directory,
    accepted,
    proposed,
    acceptedBundle,
    listingBytes,
    bind(origin) {
      git(['config', '--local', 'donut.notebook-id', '42'])
      git(['config', '--local', 'donut.api-origin', origin])
    },
    expectPublication(posts) {
      assert.equal(posts.length, 1)
      assert.equal(posts[0].expectedHead, accepted)
      const submitted = join(root, 'submitted.bundle')
      writeFileSync(submitted, posts[0].bytes)
      assert.equal(
        git(['bundle', 'list-heads', submitted]),
        `${proposed} refs/heads/main`
      )
      assert.equal(git(['rev-parse', 'main']), proposed)
      assert.equal(git(['rev-parse', 'main^']), accepted)
      assert.equal(git(['status', '--porcelain']), '')
      for (const path of edits)
        assert.equal(
          readFileSync(join(directory, path), 'utf8'),
          content('Published small edit')
        )
    },
  }
}
