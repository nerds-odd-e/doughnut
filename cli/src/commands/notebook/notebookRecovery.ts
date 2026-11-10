import { NotebookController } from 'donut-api'
import {
  donutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/donutBackendClient.js'
import { resolveNotebookBinding } from './notebookBinding.js'

export const notebookRecoveryDoc = {
  usage: 'notebook recover <directory> <accepted-head> <live-tree>',
  description:
    'Preserve explicitly reviewed live edits in accepted notebook history',
}

export async function recoverNotebookCheckout(
  directory: string,
  acceptedHead: string,
  liveTree: string
): Promise<void> {
  const { notebookId } = resolveNotebookBinding(directory)
  const recoveredHead = await runDefaultBackendJson<string>(() =>
    NotebookController.recoverNotebookGit({
      ...donutSdkOptions(),
      path: { notebook: Number(notebookId) },
      body: { acceptedHead, liveTree },
    })
  )
  console.log(`Recovered notebook. Accepted head: ${recoveredHead}.`)
  console.log(
    `Your local work is unchanged. When your checkout is ready, run "donut notebook pull ${directory}", review and resolve any rebase conflicts, then run "donut notebook publish ${directory}".`
  )
}
