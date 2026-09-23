/**
 * Testability inspect seam for notebook LFS attachment row + content-store state.
 */
import { e2eAppBaseUrl } from '../../../support/e2eAppUrl'

export type InspectNotebookLfsAttachmentResponse = {
  attachmentPresent: boolean
  acceptedGitContentLength?: number
  acceptedGitContentUtf8?: string
  objectStored: boolean
  storedObjectSize?: number
}

export function inspectNotebookLfsAttachment(body: {
  notebookName: string
  filename: string
  oid: string
}) {
  return cy.request<InspectNotebookLfsAttachmentResponse>({
    method: 'POST',
    url: `${e2eAppBaseUrl()}/api/testability/inspect_notebook_lfs_attachment_for_testability`,
    body,
  })
}
