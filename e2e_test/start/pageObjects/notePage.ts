import { commonSenseSplit } from '../../support/string_util'
import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'
import audioToolsPage from './audioToolsPage'
import { assumeConversationAboutNotePage } from './conversationAboutNotePage'
import noteCreationForm from './noteForms/noteCreationForm'
import { assumeNoteTargetSearchDialog } from './noteTargetSearchDialog'
import { noteSidebar, sidebarChildNotePageMethods } from './noteSidebar'
import { assumeAssociateWikidataDialog } from './associateWikidataDialog'
import { toolbarButton } from './toolbarButton'
import { makeSureNoteMoreOptionsDialogIsOpen } from './noteMoreOptionsDialog'

const findChildNoteCard = (title: string) =>
  cy.findByText(title, { selector: '.daisy-card-title .title-text' })

export const assumeNotePage = (noteTopology?: string) => {
  const findNoteTitle = (title) =>
    cy.findByText(title, { selector: '[role=title]' })

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
    navigateToChild: (noteTopology: string) => {
      cy.get('main').within(() => findChildNoteCard(noteTopology).click())
      return assumeNotePage(noteTopology)
    },
    collapseChildren: () => {
      cy.get('main').within(() => {
        cy.findByRole('button', { name: 'collapse children' }).click()
      })
    },
    expandChildren: () => {
      cy.findByRole('button', { name: 'expand children' }).click()
    },
    expectChildren: (children: Record<string, string>[]) => {
      cy.get('main').within(() => {
        cy.get('.daisy-card-title').should('have.length', children.length)
        children.forEach((elem) => {
          for (const propName in elem) {
            if (propName === 'note-title') {
              findChildNoteCard(elem[propName]!)
            } else {
              cy.findByText(elem[propName]!)
            }
          }
        })
      })
    },
    addRelationshipTo: (target: string) => {
      // Related notes as child cards use .daisy-card-title .title-text; the note title
      // predicate (NoteTitleAsPredicate) uses .title-text inside h2[role=title] when
      // viewing the target side of a link — both must match for changeRelationType etc.
      const findRelationshipScope = () =>
        cy
          .get('main')
          .find('.title-text')
          .contains(target)
          .closest('[role=card], [role=title]')
      return {
        relationType: (relationType: string) => {
          findRelationshipScope().findAllByText(relationType, {
            selector: '.relation-type',
          })
        },
        goto: () => {
          findRelationshipScope().find('.relation-type').click()
        },
      }
    },

    expectRelationshipTopic: function (relationType: string, target: string) {
      this.addRelationshipTo(target).relationType(relationType)
    },

    navigateToRelationshipChild: function (targetNoteTopic: string) {
      this.addRelationshipTo(targetNoteTopic).goto()
      return assumeNotePage()
    },
    expectRelationshipChildren: function (
      relationType: string,
      targetNoteTopics: string
    ) {
      commonSenseSplit(targetNoteTopics, ',').forEach((target) => {
        this.expectRelationshipTopic(relationType, target)
      })
    },
    changeRelationType: function (relationType: string, target: string) {
      cy.findByRole('title').within(() => {
        cy.get('.relation-type').click()
      })
      form.getField('Relation Type').clickOption(relationType)
      pageIsNotLoading()
      this.expectRelationshipTopic(relationType, target)
    },

    navigateToReference: (referenceTopic: string) => {
      cy.get('main').within(() => {
        cy.findByText(referenceTopic, {
          selector: '.relationship-container .title-text',
        }).click()
      })
      return assumeNotePage()
    },
    expectBreadcrumb: (items: string) => {
      cy.get('.daisy-breadcrumbs').within(() =>
        commonSenseSplit(items, ', ').forEach((noteTopology: string) =>
          cy.findByText(noteTopology)
        )
      )
    },
    collapsedChildrenWithCount: (count: number) => {
      cy.findByText(count, { selector: '[role=collapsed-children-count]' })
    },
    findNoteDetails: (expected: string, timeout?: number) => {
      expected
        .split('\\n')
        .forEach((line) =>
          timeout
            ? cy.get('[role=details]', { timeout }).should('contain', line)
            : cy.get('[role=details]').should('contain', line)
        )
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
        } else {
          cy.wrap($details).find('.ql-editor').first().blur()
        }
      })
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
      elements.forEach((element) => {
        cy.get(element.Tag as string).should('contain', element.Content)
      })
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
            'have.value',
            value
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
    updateNoteImage(attributes: Record<string, string>) {
      // Before upload, the image should not be visible (simulate new upload)
      cy.get('#note-image').should('not.exist')
      this.moreOptions().editNoteImage(attributes)
      // After upload and dialog closes, the image should be visible
      cy.get('#note-image').should('be.visible')
      return this
    },
    updateNoteUrl(attributes: Record<string, string>) {
      this.moreOptions().editNoteUrl(attributes)
      return this
    },

    startSearchingAndAddRelationship() {
      this.toolbarButton('search and add relationship').click()
      return assumeNoteTargetSearchDialog()
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

    moveUpAmongSiblings() {
      pageIsNotLoading()
      noteSidebar()
      // Find current note in sidebar
      cy.findByRole('title')
        .invoke('text')
        .then((currentTopic) => {
          // Find the note in sidebar
          cy.get('.daisy-list-group-item')
            .contains(currentTopic)
            .as('currentNote')
          // Find previous sibling
          cy.get('@currentNote')
            .parents('li')
            .prev('li')
            .prev('li')
            .find('.note-content')
            .first()
            .as('targetNote')

          // Perform drag and drop
          cy.get('@currentNote').trigger('dragstart')
          cy.get('@targetNote')
            .trigger('dragenter')
            .trigger('dragover', { clientX: 0 })
            .trigger('drop')
          cy.get('@currentNote').trigger('dragend')
        })
    },
    moveDownAmongSiblings() {
      pageIsNotLoading()
      noteSidebar()
      // Find current note in sidebar
      cy.findByRole('title')
        .invoke('text')
        .then((currentTopic) => {
          // Find the note in sidebar
          cy.get('.daisy-list-group-item')
            .contains(currentTopic)
            .as('currentNote')
          // Find next sibling
          cy.get('@currentNote')
            .parents('li')
            .next('li')
            .find('.note-content')
            .first()
            .as('targetNote')

          // Perform drag and drop
          cy.get('@currentNote').trigger('dragstart')
          cy.get('@targetNote')
            .trigger('dragenter')
            .trigger('dragover', { clientX: 0 })
            .trigger('drop')
          cy.get('@currentNote').trigger('dragend')
        })
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
