import { commonSenseSplit } from '../../support/string_util'
import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'
import audioToolsPage from './audioToolsPage'
import { assumeConversationAboutNotePage } from './conversationAboutNotePage'
import noteCreationForm from './noteForms/noteCreationForm'
import { assumeNoteTargetSearchDialog } from './noteTargetSearchDialog'
import { sidebarChildNotePageMethods } from './noteSidebar'
import { assumeAssociateWikidataDialog } from './associateWikidataDialog'
import { toolbarButton } from './toolbarButton'
import { makeSureNoteMoreOptionsDialogIsOpen } from './noteMoreOptionsDialog'

const noteShowHref = /^\/d\/n\/\d+$/
const noteShowPathInUrl = /\/d\/n\/\d+/

function wikiLinkInDetailsFluent(linkText: string) {
  const locator = () =>
    cy.get('[role=details]').find('a.doughnut-link').contains(linkText)
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
    cy.findByText(title, {
      selector: '[role=title]',
      ...(options?.timeout !== undefined ? { timeout: options.timeout } : {}),
    })

  if (noteTopology) {
    findNoteTitle(noteTopology)
  }
  return {
    expectNoteTitleDisplayed(title: string) {
      findNoteTitle(title)
      return this
    },
    /** Asserts the rendered note details body contains this substring (plain or rich) */
    expectDetailsContaining(fragment: string) {
      this.findNoteDetails(fragment)
      return this
    },
    moreOptions: () => {
      return makeSureNoteMoreOptionsDialogIsOpen()
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
    findNoteDetails: (expected: string, timeout?: number) => {
      const normalized = expected.replace(/\\n/g, '\n')
      const lines = normalized.split('\n').filter((line) => line.length > 0)
      cy.get('[role=details]', timeout ? { timeout } : {}).should(($el) => {
        const text = $el.text()
        for (const line of lines) {
          expect(
            text,
            `expected note details to include ${JSON.stringify(line)}`
          ).to.contain(line)
        }
      })
    },
    expectNoteDetailsContainLineBreak: () => {
      cy.get('[role=details]').within(() => {
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
    editTextContent: (noteAttributes: Record<string, string>) => {
      const parseSpecialKeys = (text: string): string => {
        // Replace <Shift-Enter> with Cypress key sequence {shift}{enter}
        return text.replace(/<Shift-Enter>/g, '{shift}{enter}')
      }

      for (const propName in noteAttributes) {
        const value = noteAttributes[propName]
        if (value) {
          if (propName === 'Details') {
            cy.findByRole('details').within(() => {
              cy.get('.ql-editor[contenteditable="true"], textarea')
                .first()
                .click()
            })
          } else {
            cy.findByRole(propName.toLowerCase()).click()
          }
          // Only call cy.tick if the clock is mocked
          cy.state && cy.state('clock') && cy.tick(5000)
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
      this.toolbarButton('Edit as rich content').click()
      return this
    },
    switchToRichDetails() {
      return this.switchToRichContent()
    },
    flushPendingDetailsSave() {
      cy.get('[role=details]').then(($details) => {
        const $textarea = $details.find('textarea').filter(':visible')
        if ($textarea.length) {
          cy.wrap($textarea.first()).blur()
        }
      })
      cy.get('body').click(0, 0, { force: true })
      cy.get('.dirty').should('not.exist')
      pageIsNotLoading()
      return this
    },
    openMarkdownDetailsEditor() {
      this.toolbarButton('Edit as markdown').click()
      return this
    },
    expectMarkdownDetailsSourceContains(fragment: string) {
      cy.get('textarea').should(($ta) => {
        expect($ta.val()).to.include(fragment)
      })
      return this
    },
    expectMarkdownDetailsSourceDoesNotContain(fragment: string) {
      cy.get('textarea').should(($ta) => {
        expect($ta.val()).to.not.include(fragment)
      })
      return this
    },
    updateDetailsAsMarkdown(markdown: string) {
      this.toolbarButton('Edit as markdown').click()
      cy.get('textarea').clear().type(markdown)
      return this
    },
    expectRichDetails(elements: Record<string, string>[]) {
      for (const element of elements) {
        const tag = element.Tag as string
        const content = element.Content
        cy.get('#main-note-content .note-details .ql-editor').within(() => {
          if (content === '') {
            cy.get(tag).should('exist')
          } else {
            cy.contains(tag, content).should('exist')
          }
        })
      }
    },
    addRichNoteProperty(key: string, value: string) {
      cy.get('[role=details]').within(() => {
        cy.findByRole('button', { name: 'Add note property' }).click()
        cy.findByTestId('rich-note-property-key').clear().type(key)
        cy.findByTestId('rich-note-property-value').clear().type(value)
      })
      cy.findByRole('details').within(() => {
        cy.get('.ql-editor[contenteditable="true"]').first().click()
      })
      return this
    },
    expectRichNotePropertyDisplayed(key: string, value: string) {
      cy.get('[role=details]').within(() => {
        cy.contains('h4', 'Properties')
        cy.get(
          `[data-testid="rich-note-property-row"][data-property-key="${key}"]`
        ).within(() => {
          cy.get('[data-testid="rich-note-property-row-key-input"]').should(
            'have.value',
            key
          )
          cy.get('[data-testid="rich-note-property-row-value-input"]').should(
            ($el) => {
              expect($el.text().trim()).to.eq(value)
            }
          )
        })
      })
      return this
    },
    expectRichNotePropertyAbsent(key: string) {
      cy.get('[role=details]').within(() => {
        cy.contains('h4', 'Properties')
        cy.get(
          `[data-testid="rich-note-property-row"][data-property-key="${key}"]`
        ).should('not.exist')
      })
      return this
    },
    editRichNoteProperty(oldKey: string, newKey: string, newValue: string) {
      // Edit value before key: changing the key updates `data-property-key` on the row,
      // which breaks a single `.within()` chain that queries by `oldKey` then touches both inputs.
      cy.get('[role=details]').within(() => {
        cy.contains('h4', 'Properties')
        cy.get(
          `[data-testid="rich-note-property-row"][data-property-key="${oldKey}"]`,
          { timeout: 15000 }
        ).within(() => {
          cy.get('[data-testid="rich-note-property-row-value-input"]')
            .clear()
            .type(newValue)
        })
      })
      cy.get('[role=details]').within(() => {
        cy.get(
          `[data-testid="rich-note-property-row"][data-property-key="${oldKey}"]`,
          { timeout: 15000 }
        ).within(() => {
          cy.get('[data-testid="rich-note-property-row-key-input"]')
            .clear()
            .type(newKey)
        })
      })
      cy.findByRole('details').within(() => {
        cy.get('.ql-editor[contenteditable="true"]').first().click()
      })
      return this
    },
    followDeadLink(linkTitle: string) {
      cy.get('[role=details]').find('a.dead-link').contains(linkTitle).click()
      return {
        createNote: () => {
          noteCreationForm.submit()
        },
      }
    },
    wikiLinkInDetails(linkText: string) {
      return wikiLinkInDetailsFluent(linkText)
    },
    updateNoteImage(attributes: Record<string, string>) {
      // Before upload, the image should not be visible (simulate new upload)
      cy.get('#note-image').should('not.exist')
      this.moreOptions().editNoteImage(attributes)
      // After upload and dialog closes, the image should be visible
      cy.get('#note-image').should('be.visible')
      return this
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
    aiGenerateImage() {
      this.moreOptions().generateImageWithDALLE()
    },
    deleteNote() {
      this.moreOptions().deleteNote()
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
      toolbarButton('associate wikidata').click()
      return assumeAssociateWikidataDialog()
    },
    setLevel(level: number) {
      form.getField('Level').within(() => {
        cy.findByRole('button', { name: `${level}` }).click()
      })
      return this
    },
    setRememberSpelling() {
      form.getField('Remember Spelling').check()
      pageIsNotLoading()
      return this
    },
  }
}
