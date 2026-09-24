/// <reference types="Cypress" />
// @ts-check
import {
  NotebookGitTestabilityController,
  NotebookLfsTestabilityController,
} from '@generated/donut-backend-api/sdk.gen'
import type { AcceptLfsAttachmentTipResponse } from '@generated/donut-backend-api'
import { unwrapData } from './unwrapApi'

/**
 * Test-only setup helper: rebuilds a notebook's `NotebookGitBinding` from its *current*
 * database content after a fixture seeds unsupported structural changes such as folders or
 * readmes. It establishes the fixture's initial accepted baseline; do not use it after the
 * content edit under test.
 */
export const notebookGitTestabilityMethods = {
  resnapshotNotebookGitBindingForTestability(notebookName: string) {
    return cy.wrap(
      NotebookGitTestabilityController.resnapshotNotebookGitBindingForTestability(
        {
          body: { notebookName },
        }
      ),
      { log: false }
    )
  },

  /**
   * Demotes a product-created LFS notebook to legacy RAW so pull-based fixtures keep proving
   * the raw journey explicitly.
   */
  forceRawNotebookGitBindingForTestability(notebookName: string) {
    return cy.wrap(
      NotebookGitTestabilityController.forceRawNotebookGitBindingForTestability(
        {
          body: { notebookName },
        }
      ),
      { log: false }
    )
  },

  /** Runs the startup conversion of a legacy RAW notebook to LFS on demand. */
  convertRawNotebookToLfsForTestability(notebookName: string) {
    return cy.wrap(
      NotebookGitTestabilityController.convertRawNotebookToLfsForTestability({
        body: { notebookName },
      }),
      { log: false }
    )
  },

  /** Stores text as a file at a slash-separated notebook path (creating folders), then resnapshots. */
  putNotebookFileForTestability(
    notebookName: string,
    path: string,
    content: string
  ) {
    const utf8 = new TextEncoder().encode(content)
    return this.putNotebookFileBytesForTestability(
      notebookName,
      path,
      btoa(Array.from(utf8, (byte) => String.fromCharCode(byte)).join(''))
    )
  },

  /** Stores base64-encoded bytes as a file at a slash-separated notebook path, then resnapshots. */
  putNotebookFileBytesForTestability(
    notebookName: string,
    path: string,
    contentBase64: string
  ) {
    return cy.then(() =>
      NotebookGitTestabilityController.putNotebookFileForTestability({
        body: { notebookName, path, contentBase64 },
      })
    )
  },

  /**
   * Stores an accepted LFS file at the notebook root (switching the notebook to LFS) and yields
   * its tip oid. An obsolete payload, when given, is stored first as an unreferenced LFS object.
   */
  acceptLfsAttachmentTipForTestability(
    notebookName: string,
    filename: string,
    payload: string,
    obsoletePayload?: string
  ) {
    return cy
      .wrap(
        NotebookLfsTestabilityController.acceptLfsAttachmentTipForTestability({
          body: { notebookName, filename, payload, obsoletePayload },
        }),
        { log: false }
      )
      .then(
        (response) => unwrapData<AcceptLfsAttachmentTipResponse>(response).oid!
      )
  },
}
