import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'
import bookReadingPage from './bookReadingPage'
import { sidebarChildNotePageMethods } from './sidebarChildNotePageMethods'

const notebookPage = () => {
  const clickButton = (name: string) =>
    cy.findByRole('button', { name }).click()

  const openSettingsTab = () => {
    cy.get('[data-testid="notebook-workspace-tab-settings"]').click()
    cy.get('[data-testid="notebook-workspace-settings"]').should('be.visible')
  }

  const openHealthTab = () => {
    cy.get('[data-testid="notebook-workspace-tab-health"]').click()
    cy.get('[data-testid="notebook-workspace-health"]').should('be.visible')
  }

  const expectAdminSettingsAbsent = () => {
    cy.get('[data-testid="notebook-workspace-settings"]').should('not.exist')
    cy.contains('Notebook Management').should('not.exist')
    cy.contains('Notebook Settings').should('not.exist')
    cy.contains('Notebook Indexing').should('not.exist')
    cy.contains('Move to ...').should('not.exist')
    cy.contains('Share notebook to bazaar').should('not.exist')
    cy.contains('Skip Memory Tracking').should('not.exist')
    cy.contains('Update index').should('not.exist')
    cy.contains('Reset notebook index').should('not.exist')
  }

  return {
    openSettingsTab() {
      openSettingsTab()
      return this
    },

    openHealthTab() {
      openHealthTab()
      return this
    },

    runLint() {
      cy.get('[data-testid="notebook-health-run"]').click()
      pageIsNotLoading()
      return this
    },

    expectHealthIdle() {
      cy.get('[data-testid="notebook-health-idle"]').should('be.visible')
      return this
    },

    checkRemoveEmptyFolders() {
      cy.get(
        '[data-testid="notebook-health-remove-empty-folders"] input[type="checkbox"]'
      ).check({ force: true })
      return this
    },

    expectFindingGroupsExpandable() {
      cy.get('[data-testid="notebook-health-findings"]').should('be.visible')
      for (const ruleId of [
        'empty_folders',
        'readme_only_folders',
        'dead_wiki_links',
      ]) {
        cy.get(`[data-testid="notebook-health-group-${ruleId}"]`).within(() => {
          cy.get('input[type="checkbox"]').should('exist')
          cy.get('.daisy-collapse-title').should('be.visible')
        })
      }
      return this
    },

    expectFindingGroupIncludes(ruleId: string, label: string) {
      cy.get(`[data-testid="notebook-health-group-${ruleId}"]`).should(
        'contain.text',
        label
      )
      return this
    },

    expectDeadWikiLinkFinding(noteTitle: string, token: string) {
      cy.get('[data-testid="notebook-health-group-dead_wiki_links"]').within(
        () => {
          cy.contains('.daisy-collapse-title', noteTitle).should('be.visible')
          cy.contains('li', token).should('be.visible')
        }
      )
      return this
    },

    expectReadmeLandmarks(name: string) {
      cy.get('[data-testid="notebook-page-kind-label"]').should(
        'contain.text',
        'Notebook'
      )
      cy.get('[data-testid="notebook-page-summary"]')
        .find('h1')
        .should('contain.text', name)
      cy.get('[data-testid="notebook-workspace-readme"]').should('be.visible')
      cy.get('[data-testid="notebook-readme-editor"]').should('be.visible')
      expectAdminSettingsAbsent()
      return this
    },

    expectAdminSettingsAbsent() {
      expectAdminSettingsAbsent()
      return this
    },

    expectSettingsSectionsVisible() {
      cy.get('[data-testid="notebook-workspace-settings"]').should('be.visible')
      cy.get('[data-testid="notebook-workspace-settings"]').within(() => {
        cy.contains('Notebook Management').should('exist')
        cy.contains('Notebook Settings').should('exist')
        cy.contains('Notebook Indexing').should('exist')
        cy.contains('Share notebook to bazaar').should('exist')
        cy.contains('Skip Memory Tracking').should('exist')
        cy.contains('Update index').should('exist')
        cy.contains('Reset notebook index').should('exist')
      })
      return this
    },

    assertNoteHasSettingWithValue(setting: string, value: string) {
      openSettingsTab()
      form.getField(setting).shouldHaveValue(value)
    },

    skipMemoryTracking() {
      openSettingsTab()
      form.getField('Skip Memory Tracking').check()
      clickButton('Update Settings')
      pageIsNotLoading()
    },

    attachEpubFixture(relativePath: string) {
      openSettingsTab()
      cy.get('[data-testid="notebook-no-book"]')
        .find('input[type="file"]')
        .selectFile(`e2e_test/fixtures/${relativePath}`, { force: true })
      pageIsNotLoading()
      cy.get('[data-testid="notebook-attached-book"]').should('be.visible')
      return this
    },
    attemptAttachEpubFixture(relativePath: string) {
      openSettingsTab()
      cy.get('[data-testid="notebook-no-book"]')
        .find('input[type="file"]')
        .selectFile(`e2e_test/fixtures/${relativePath}`, { force: true })
      pageIsNotLoading()
      return this
    },
    expectEpubAttachErrorContaining(messageSubstring: string) {
      cy.contains('.Vue-Toastification__toast--error', messageSubstring, {
        timeout: 10000,
      }).should('be.visible')
      cy.get('[data-testid="notebook-no-book"]').should('be.visible')
      cy.get('[data-testid="notebook-attached-book"]').should('not.exist')
      return this
    },
    reindexNotebook() {
      openSettingsTab()
      cy.findByRole('button', { name: 'Update index' }).click()
      // Wait for the indexing to complete - toast notification will appear
      pageIsNotLoading()
      return this
    },
    shareNotebookToBazaar() {
      openSettingsTab()
      cy.findByRole('button', { name: 'Share notebook to bazaar' }).click()
      cy.findByRole('button', { name: 'OK' }).click()
      pageIsNotLoading()
      return this
    },
    moveNotebookToCircle() {
      openSettingsTab()
      cy.findByRole('button', { name: 'Move to ...' }).click()
      return this
    },
    ...sidebarChildNotePageMethods(),
    typeNotebookReadmeDraftAndSave(text: string) {
      cy.get('[data-testid="notebook-readme-editor"] .ql-editor')
        .should('be.visible')
        .click()
        .type(text, { delay: 0 })
        .blur()
      pageIsNotLoading()
      return this
    },
    expectNotebookReadmeBodyContains(fragment: string) {
      pageIsNotLoading()
      cy.get('[data-testid="notebook-readme-body"] .ql-editor').should(
        'contain.text',
        fragment
      )
      return this
    },
    readBook(bookTitle: string) {
      pageIsNotLoading()
      openSettingsTab()
      cy.get('[data-testid="notebook-attached-book"]').within(() => {
        cy.contains(bookTitle)
        cy.findByRole('button', { name: /^Read$/i }).click()
      })
      return bookReadingPage()
    },
  }
}

export default notebookPage
