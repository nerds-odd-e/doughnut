import { spawnSync } from 'node:child_process'
import * as fs from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { NotebookController } from 'donut-api'
import type { Report } from '../../../../packages/generated/donut-backend-api/types.gen.js'
import {
  donutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/donutBackendClient.js'
import { resolveNotebookBinding } from './notebookBinding.js'

export const notebookDiagnosisDoc = {
  usage: 'notebook diagnose <directory>',
  description:
    'Review accepted/live notebook differences without changing local work',
}

export async function diagnoseNotebookCheckout(
  directory: string
): Promise<void> {
  const { notebookId } = resolveNotebookBinding(directory)
  const report = await runDefaultBackendJson<Report>(() =>
    NotebookController.diagnoseNotebookGit({
      ...donutSdkOptions(),
      path: { notebook: Number(notebookId) },
    })
  )
  console.log(`Accepted head: ${report.acceptedHead}`)
  console.log(`Accepted tree: ${report.acceptedTree}`)
  console.log(`Live tree: ${report.liveTree}`)
  if (!report.differences?.length) {
    console.log('Live Portable content matches accepted main.')
    return
  }

  const temporary = fs.mkdtempSync(join(tmpdir(), 'donut-notebook-diagnosis-'))
  try {
    for (const difference of report.differences) {
      const kind =
        difference.acceptedBlobId == null
          ? 'live-only'
          : difference.liveBlobId == null
            ? 'accepted-only'
            : 'changed'
      console.log(`\n${kind}: ${difference.path}`)
      console.log(`Accepted blob: ${difference.acceptedBlobId ?? '(absent)'}`)
      console.log(`Live blob: ${difference.liveBlobId ?? '(absent)'}`)
      const before = Buffer.from(difference.acceptedContent ?? '', 'base64')
      const after = Buffer.from(difference.liveContent ?? '', 'base64')
      console.log(
        `Accepted bytes: ${before.length}; live bytes: ${after.length}`
      )
      const isText = [before, after].every(
        (bytes) =>
          !bytes.includes(0) &&
          Buffer.from(bytes.toString('utf8')).equals(bytes)
      )
      if (!isText) {
        console.log(
          `Accepted content (base64): ${difference.acceptedContent ?? '(absent)'}`
        )
        console.log(
          `Live content (base64): ${difference.liveContent ?? '(absent)'}`
        )
        continue
      }
      for (const [side, bytes] of [
        ['accepted', before],
        ['live', after],
      ] as const) {
        const file = join(temporary, side, difference.path!)
        fs.mkdirSync(dirname(file), { recursive: true })
        fs.writeFileSync(file, bytes)
      }
      const diff = spawnSync(
        'git',
        [
          'diff',
          '--no-index',
          '--no-ext-diff',
          '--no-textconv',
          '--no-color',
          '--',
          join('accepted', difference.path!),
          join('live', difference.path!),
        ],
        { cwd: temporary, encoding: 'utf8' }
      )
      if (diff.error) throw diff.error
      if (diff.status !== 0 && diff.status !== 1) throw new Error(diff.stderr)
      if (diff.stdout) console.log(diff.stdout)
    }
  } finally {
    fs.rmSync(temporary, { recursive: true, force: true })
  }
  console.log(
    '\nReview the exact differences with the notebook owner before server recovery. Genuine live edits require an explicitly reviewed forward commit; projection defects require a cause-specific correction. Pull alone cannot repair server projection drift. Your local work is unchanged.'
  )
}
