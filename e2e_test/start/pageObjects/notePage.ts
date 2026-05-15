import { commonSenseSplit } from '../../support/string_util'
import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'
import audioToolsPage from './audioToolsPage'
import { assumeConversationAboutNotePage } from './conversationAboutNotePage'
import noteCreationForm from './forms/noteCreationForm'
import { assumeNoteTargetSearchDialog } from './noteTargetSearchDialog'
import { sidebarChildNotePageMethods } from './noteSidebar'
import { assumeAssociateWikidataDialog } from './associateWikidataDialog'
import { toolbarButton } from './toolbarButton'
import { makeSureNoteMoreOptionsFormIsOpen } from './noteMoreOptionsForm'
import { assumeAssimilationPage } from './assimilationPage'

const noteShowHref = /^\/d\/n\/\d+$/
const noteShowPathInUrl = /\/d\/n\/\d+/

const noteContentRegion = { role: 'region' as const, name: 'Note content' }

const mainNoteHeadingTitleSelector =
  '#main-note-content h2.path-name-heading [role=title]'

type TitleRenameReferenceChoice = 'KEEP_VISIBLE_TEXT' | 'UPDATE_VISIBLE_TEXT'

const titleRenameReferenceSaveTestId: Record<
  TitleRenameReferenceChoice,
  string
> = {
  KEEP_VISIBLE_TEXT: 'referenced-title-save-keep-visible-text',
  UPDATE_VISIBLE_TEXT: 'referenced-title-save-update-visible-text',
}

function wikiLinkInNoteContentFluent(linkText: string) {
  const locator = () =>
    cy
      .findByRole(noteContentRegion.role, { name: noteContentRegion.name })
      .find('a.doughnut-link')
      .contains(linkText)
  return {
    expectNoteShowHref() {
      locator().should('have.attr', 'href').and('match', noteShowHref)
      return this
    },
    followAndAssumeNote(noteTitle: string) {
      locator().click()
      cy.url({ timeout: 15000 }).should('match', noteShowPathInUrl)
      return assumeNotePage(noteTitle)
    },
  }
}

export const assumeNotePage = (
  noteTopology?: string,
  options?: { timeout?: number }
) => {
  const findNoteTitle = (title: string) =>
    cy
      .get(mainNoteHeadingTitleSelector, {
        ...(options?.timeout !== undefined ? { timeout: options.timeout } : {}),
      })
      .should(($el) => {
        expect($el.text().trim()).to.eq(title)
      })

  if (noteTopology) {
    findNoteTitle(noteTopology)
  }
  return {
    expectNoteTitleDisplayed(title: string) {
      findNoteTitle(title)
      return this
    },
    /** Asserts the rendered note content body contains this substring (plain or rich) */
    expectContentContaining(fragment: string) {
      this.findNoteContent(fragment)
      return this
    },
    moreOptions: () => {
      return makeSureNoteMoreOptionsFormIsOpen()
    },
    expandChildren: () => {
      cy.findByRole('button', { name: 'expand children' }).click()
    },
    addRelationshipTo: (target: string) => {
      return {
        relationType: (relationType: string) => {
          cy.get('#main-note-content').then(($scope) => {
            const cardWithTarget = $scope
              .find('[role=card]')
              .toArray()
              .find((el) => el.textContent?.includes(target))
            if (cardWithTarget) {
              cy.wrap(cardWithTarget).should('contain', relationType)
            } else {
              cy.contains('#main-note-content [role=card]', target).should(
                'contain',
                relationType
              )
            }
          })
        },
      }
    },

    expectRelationshipTopic: function (relationType: string, target: string) {
      this.addRelationshipTo(target).relationType(relationType)
    },
    expectRelationshipChildren: function (
      relationType: string,
      targetNoteTopics: string
    ) {
      cy.get('#main-note-content').then(($main) => {
        const expandBtn = $main.find('button[title="expand children"]')
        if (expandBtn.length) {
          cy.wrap(expandBtn.first()).click()
        }
      })
      commonSenseSplit(targetNoteTopics, ',').forEach((target) => {
        this.expectRelationshipTopic(relationType, target)
      })
    },
    changeRelationType: function (relationType: string) {
      cy.get('[data-property-key="relation"]').within(() => {
        cy.findByRole('button', { name: 'Relation Type' }).click()
      })
      form.getField('Relation Type').clickOption(relationType)
      pageIsNotLoading()
    },

    navigateToReference: (referenceTopic: string) => {
      cy.get('#main-note-content')
        .find('[role=card]')
        .contains(referenceTopic)
        .closest('[role=card]')
        .find('a')
        .first()
        .click({ force: true })
      return assumeNotePage()
    },
    expectBreadcrumb: (items: string) => {
      cy.get('.daisy-breadcrumbs').within(() =>
        commonSenseSplit(items, ', ').forEach((noteTopology: string) =>
          cy.findByText(noteTopology)
        )
      )
    },
    findNoteContent: (expected: string, timeout?: number) => {
      const normalized = expected.replace(/\\n/g, '\n')
      const lines = normalized.split('\n').filter((line) => line.length > 0)
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
        ...(timeout !== undefined ? { timeout } : {}),
      }).should(($el) => {
        const text = $el.text()
        for (const line of lines) {
          expect(
            text,
            `expected note content to include ${JSON.stringify(line)}`
          ).to.contain(line)
        }
      })
    },
    expectNoteContentContainLineBreak: () => {
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
      }).within(() => {
        // Verify that "Hello" is immediately followed by a <br> tag
        cy.get('.ql-editor, [contenteditable]').should(($el) => {
          const html = $el.html()
          // Check that "Hello" is immediately followed by a <br> tag (with optional whitespace/newlines)
          // This ensures the <br> is right after "Hello", not somewhere else
          const match = html.match(/Hello\s*<br[^>]*>/i)
          expect(match).to.not.be.null
          // Also verify "World" comes after the <br>
          expect(html).to.match(/Hello\s*<br[^>]*>\s*World/i)
        })
      })
    },
    toolbarButton: (btnTextOrTitle: string) => {
      return toolbarButton(btnTextOrTitle)
    },
    undo(undoType: string) {
      this.toolbarButton(`undo ${undoType}`).click()
      cy.findByRole('button', { name: 'OK' }).click()
    },
    saveReferencedNoteTitle: (
      newTitle: string,
      choice: TitleRenameReferenceChoice
    ) => {
      cy.findByRole('title').click()
      cy.clearFocusedText().type(newTitle)
      cy.findByTestId('referenced-title-save-panel')
        .find(`[data-testid="${titleRenameReferenceSaveTestId[choice]}"]`)
        .click()
      cy.findByTestId('referenced-title-save-panel').should('not.exist')
      cy.findByText(newTitle, { selector: '[role=title]' })
      pageIsNotLoading()
    },
    editTextContent: (noteAttributes: Record<string, string>) => {
      const parseSpecialKeys = (text: string): string => {
        // Replace <Shift-Enter> with Cypress key sequence {shift}{enter}
        return text.replace(/<Shift-Enter>/g, '{shift}{enter}')
      }

      for (const propName in noteAttributes) {
        const value = noteAttributes[propName]
        if (value) {
          if (propName === 'Content') {
            cy.findByRole(noteContentRegion.role, {
              name: noteContentRegion.name,
            }).within(() => {
              cy.get('.ql-editor[contenteditable="true"], textarea')
                .first()
                .click()
            })
          } else {
            cy.findByRole(propName.toLowerCase()).click()
          }
          const cypressState = cy as unknown as {
            state?: (key: string) => unknown
          }
          if (cypressState.state?.('clock')) {
            cy.tick(5000)
          }
          const parsedValue = parseSpecialKeys(value)
          cy.clearFocusedText().type(parsedValue).blur()
          cy.get('.dirty').should('not.exist')
        }
      }
      pageIsNotLoading()
    },
    audioTools() {
      this.toolbarButton('Audio tools').click()
      return audioToolsPage()
    },
    switchToRichContent() {
      cy.get('body').then(($body) => {
        const toRich = $body.find('button[aria-label="Edit as rich content"]')
        if (toRich.length > 0) {
          cy.wrap(toRich.first()).click()
        }
      })
      return this
    },
    switchToRichContentMode() {
      return this.switchToRichContent()
    },
    flushPendingContentSave() {
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
      }).then(($noteField) => {
        const $textarea = $noteField.find('textarea').filter(':visible')
        if ($textarea.length) {
          cy.wrap($textarea.first()).blur()
        }
      })
      cy.get('body').click(0, 0, { force: true })
      cy.get('.dirty').should('not.exist')
      pageIsNotLoading()
      return this
    },
    openMarkdownContentEditor() {
      this.toolbarButton('Edit as markdown').click()
      return this
    },
    expectMarkdownContentSourceContains(fragment: string) {
      cy.get('textarea').should(($ta) => {
        expect($ta.val()).to.include(fragment)
      })
      return this
    },
    expectMarkdownContentSourceDoesNotContain(fragment: string) {
      cy.get('textarea').should(($ta) => {
        expect($ta.val()).to.not.include(fragment)
      })
      return this
    },
    updateContentAsMarkdown(markdown: string) {
      this.toolbarButton('Edit as markdown').click()
      cy.get('textarea').clear().type(markdown)
      return this.flushPendingContentSave()
    },
    expectRichContent(elements: Record<string, string>[]) {
      for (const element of elements) {
        const tag = element.Tag as string
        const content = element.Content ?? ''
        cy.get('#main-note-content .note-content .ql-editor').within(() => {
          if (content === '') {
            cy.get(tag).should('exist')
          } else {
            cy.contains(tag, content).should('exist')
          }
        })
      }
    },
    addRichNoteProperty(key: string, value: string) {
      cy.get('body').then(($body) => {
        const folderIndexBody = $body.find('[data-testid="folder-index-body"]')
        const scope =
          folderIndexBody.length > 0
            ? cy.wrap(folderIndexBody.first())
            : cy.findByRole(noteContentRegion.role, {
                name: noteContentRegion.name,
              })
        scope.within(() => {
          cy.findByRole('button', { name: 'Add property' }).click()
          cy.findByTestId('rich-note-property-key')
            .clear()
            .type(key, { parseSpecialCharSequences: false })
          cy.findByTestId('rich-note-property-value')
            .clear()
            .type(value, { parseSpecialCharSequences: false })
            .blur()
        })
        const focusScope =
          folderIndexBody.length > 0
            ? cy.wrap(folderIndexBody.first())
            : cy.findByRole(noteContentRegion.role, {
                name: noteContentRegion.name,
              })
        focusScope.within(() => {
          cy.get('.ql-editor[contenteditable="true"]').first().click()
        })
      })
      return this
    },
    uploadRichNoteImagePropertyFromFixture(fixtureRelativePath: string) {
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
      }).within(() => {
        cy.findByRole('button', { name: 'Add property' }).click()
        cy.findByTestId('rich-note-property-key').clear().type('image')
        cy.get('[data-testid="rich-note-image-insert-file-input"]').selectFile(
          `e2e_test/fixtures/${fixtureRelativePath}`,
          { force: true }
        )
      })
      cy.get(
        `[data-testid="rich-note-property-row"][data-property-key="image"]`,
        { timeout: 20000 }
      ).should('exist')
      return this.flushPendingContentSave()
    },
    expectRichNotePropertyDisplayed(key: string, value: string) {
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
      }).within(() => {
        cy.contains('h4', 'Properties')
        cy.get(
          `[data-testid="rich-note-property-row"][data-property-key="${key}"]`
        ).within(() => {
          cy.get('[data-testid="rich-note-property-row-key-input"]').should(
            'have.value',
            key
          )
          const keyNorm = key.trim().toLowerCase()
          const isWikidata =
            keyNorm === 'wikidata_id' || keyNorm === 'wikidataid'
          if (isWikidata) {
            cy.contains('.daisy-font-mono', value).should('exist')
          } else if (keyNorm === 'image') {
            cy.get('[data-testid="rich-note-image-property-path"]').should(
              ($el) => {
                expect($el.text().trim()).to.eq(value.trim())
              }
            )
          } else {
            cy.get('[data-testid="rich-note-property-row-value-input"]').should(
              ($el) => {
                expect($el.text().trim()).to.eq(value)
              }
            )
          }
        })
      })
      return this
    },
    expectRichNoteImagePropertyAttachmentPath(key: string) {
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
      }).within(() => {
        cy.get(
          `[data-testid="rich-note-property-row"][data-property-key="${key}"]`
        ).within(() => {
          cy.get('[data-testid="rich-note-image-property-path"]').should(
            ($el) => {
              expect($el.text().trim()).to.match(
                /^\/attachments\/images\/\d+\/.+/
              )
            }
          )
        })
      })
      return this
    },
    expectRichNotePropertyAbsent(key: string) {
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
      }).within(() => {
        cy.get(
          `[data-testid="rich-note-property-row"][data-property-key="${key}"]`
        ).should('not.exist')
      })
      return this
    },
    expectDeadWikiLink(linkText: string) {
      cy.findByRole(noteContentRegion.role, { name: noteContentRegion.name })
        .find('a.dead-link')
        .contains(linkText)
      return this
    },
    editRichNoteProperty(oldKey: string, newKey: string, newValue: string) {
      // Edit value before key: changing the key updates `data-property-key` on the row,
      // which breaks a single `.within()` chain that queries by `oldKey` then touches both inputs.
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
      }).within(() => {
        cy.contains('h4', 'Properties')
        cy.get(
          `[data-testid="rich-note-property-row"][data-property-key="${oldKey}"]`,
          { timeout: 15000 }
        ).within(() => {
          cy.get('[data-testid="rich-note-property-row-value-input"]')
            .clear()
            .type(newValue)
            .blur()
        })
      })
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
      }).within(() => {
        cy.get(
          `[data-testid="rich-note-property-row"][data-property-key="${oldKey}"]`,
          { timeout: 15000 }
        ).within(() => {
          cy.get('[data-testid="rich-note-property-row-key-input"]')
            .clear()
            .type(newKey)
            .blur()
        })
      })
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
      }).within(() => {
        cy.get('.ql-editor[contenteditable="true"]').first().click()
      })
      return this
    },
    followDeadLink(linkTitle: string) {
      cy.findByRole(noteContentRegion.role, { name: noteContentRegion.name })
        .find('a.dead-link')
        .contains(linkTitle)
        .click()
      return {
        createNote: () => {
          cy.findByRole('button', { name: /Create a new note/ }).click()
          noteCreationForm.submit()
        },
        linkToExistingNote: (
          existingNoteTitle: string,
          displayText: string
        ) => {
          cy.findByRole('button', { name: 'Link to an existing note' }).click()
          assumeNoteTargetSearchDialog()
            .findTarget(existingNoteTitle)
            .insertDeadLinkToTarget(existingNoteTitle, displayText)
        },
      }
    },
    wikiLinkInNoteContent(linkText: string) {
      return wikiLinkInNoteContentFluent(linkText)
    },

    startSearchingAndAddRelationship() {
      this.toolbarButton('Link').click()
      return assumeNoteTargetSearchDialog()
    },
    insertWikiLinkToNote(toNoteTopic: string) {
      this.toolbarButton('Link').click()
      assumeNoteTargetSearchDialog()
        .findTarget(toNoteTopic)
        .insertWikiLinkToTarget(toNoteTopic)
      return this
    },
    ...sidebarChildNotePageMethods(),
    deleteNote() {
      this.moreOptions().deleteNote()
    },
    deleteNoteAndLeaveReferencesAsDeadLinks() {
      this.moreOptions().deleteNoteAndLeaveReferencesAsDeadLinks()
    },
    deleteNoteAndRemoveFromReferenceProperties() {
      this.moreOptions().deleteNoteAndRemoveFromReferenceProperties()
    },
    openQuestionList() {
      return this.moreOptions().openQuestionList()
    },
    addQuestion(row: Record<string, string>) {
      this.openQuestionList().addQuestionPage().addQuestion(row)
    },
    refineQuestion(row: Record<string, string>) {
      this.openQuestionList().addQuestionPage().refineQuestion(row)
    },
    expectQuestionsInList(expectedQuestions: Record<string, string>[]) {
      this.openQuestionList().expectQuestion(expectedQuestions)
    },
    sendMessageToNoteOwner(message: string) {
      this.toolbarButton('Star a conversation about this note').click()
      cy.findByRole('textbox').type(message)
      cy.findByRole('button', { name: 'Send message' }).click()
    },

    startAConversationAboutNote() {
      this.toolbarButton('Star a conversation about this note').click()
      return assumeConversationAboutNotePage()
    },

    sendMessageToAI(message: string) {
      this.startAConversationAboutNote().replyToConversationAndInviteAiToReply(
        message
      )
    },

    associateWikidataDialog() {
      // Notes open in rich (frontmatter) mode by default; only switch when already in markdown mode.
      cy.get('body').then(($body) => {
        const toRich = $body.find('button[aria-label="Edit as rich content"]')
        if (toRich.length > 0) {
          cy.wrap(toRich.first()).click()
        }
      })
      cy.findByRole(noteContentRegion.role, {
        name: noteContentRegion.name,
      }).within(() => {
        cy.root().then(($region) => {
          const editBtn = $region.find(
            '[data-testid="rich-note-wikidata-property-edit"]'
          )
          if (editBtn.length > 0) {
            cy.wrap(editBtn.first()).click()
          } else {
            cy.findByRole('button', { name: 'Add property' }).click()
            cy.findByTestId('rich-note-property-key')
              .clear()
              .type('wikidata_id')
            cy.findByTestId('rich-note-wikidata-property-insert-edit').click()
          }
        })
      })
      return assumeAssociateWikidataDialog()
    },
    openAssimilationSettings() {
      cy.url().then((href) => {
        if (!String(href).includes('/d/assimilate/')) {
          makeSureNoteMoreOptionsFormIsOpen().openAssimilationPage()
        } else {
          assumeAssimilationPage().waitForAssimilationReady()
        }
      })
      cy.url().should('include', '/d/assimilate/')
      pageIsNotLoading()
      return assumeAssimilationPage()
    },
    setLevel(level: number) {
      this.openAssimilationSettings()
      form.getField('Level').within(() => {
        cy.findByRole('button', { name: `${level}` }).click()
      })
      pageIsNotLoading()
      return this
    },
    setRememberSpelling() {
      this.openAssimilationSettings()
      form.getField('Remember Spelling').check()
      pageIsNotLoading()
      return this
    },
  }
}
