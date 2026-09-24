import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  childTreeitems,
  expectRowLabels,
  folderTreitemByLabel,
} from './sidebarTreeItems'

export const attachmentPage = () => ({
  expectFileAndSize(filename: string, size: number) {
    cy.get('[data-testid="attachment-page-filename"]').should(
      'have.text',
      filename
    )
    cy.get('[data-testid="attachment-page-size"]').should(
      'contain.text',
      `${size} bytes`
    )
    return this
  },

  reload() {
    cy.reload()
    waitUntilAppIsNotBusy()
    return this
  },

  expectSidebarOpenAtFolderShowing(folderLabel: string, filename: string) {
    folderTreitemByLabel(folderLabel).should(
      'have.attr',
      'aria-expanded',
      'true'
    )
    expectRowLabels(childTreeitems(folderTreitemByLabel(folderLabel)), [
      filename,
    ])
    return this
  },

  expectDownload(filename: string, content: string) {
    cy.get<HTMLAnchorElement>('[data-testid="attachment-download-link"]').then(
      ($link) => {
        cy.request($link[0].href).then((response) => {
          expect(response.body, 'downloaded bytes').to.equal(content)
          expect(response.headers['content-disposition']).to.contain(
            `attachment; filename="${filename}"`
          )
        })
      }
    )
    return this
  },
})
