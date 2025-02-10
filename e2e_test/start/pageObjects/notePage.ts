import { commonSenseSplit } from '../../support/string_util'
import submittableForm from '../submittableForm'
import audioToolsPage from './audioToolsPage'
import { assumeConversationAboutNotePage } from './conversationAboutNotePage'
import noteCreationForm from './noteForms/noteCreationForm'
import { questionListPage } from './questionListPage'
import { assumeNoteTargetSearchDialog } from './noteTargetSearchDialog'
import { noteSidebar } from './noteSidebar'

function filterAttributes(
  attributes: Record<string, string>,
  keysToKeep: string[]
) {
  return Object.keys(attributes)
    .filter((key) => keysToKeep.includes(key))
    .reduce(
      (obj, key) => {
        const val = attributes[key]
        if (val) {
          obj[key] = val
        }
        return obj
      },
      {} as Record<string, string>
    )
}

export const assumeNotePage = (noteTopology?: string) => {
  const findNoteTitle = (title) =>
    cy.findByText(title, { selector: '[role=title]' })

  if (noteTopology) {
    findNoteTitle(noteTopology)
  }

  const privateToolbarButton = (btnTextOrTitle: string) => {
    const getButton = () => cy.findByRole('button', { name: btnTextOrTitle })
    return {
      click: () => {
        getButton().click()
        return { ...submittableForm }
      },
      shouldNotExist: () => getButton().should('not.exist'),
    }
  }

  const notePageMoreOptionsButton = (btnTextOrTitle: string) => {
    privateToolbarButton('more options').click()
    return privateToolbarButton(btnTextOrTitle)
  }

  const clickNotePageMoreOptionsButton = (btnTextOrTitle: string) => {
    return notePageMoreOptionsButton(btnTextOrTitle).click()
  }

  return {
    navigateToChild: (noteTopology: string) => {
      cy.get('main').within(() => {
        cy.findCardTitle(noteTopology).click()
      })
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
        cy.expectNoteCards(children)
      })
    },

    linkNoteTo: (target: string) => {
      const findLink = () =>
        cy
          .findByText(target, { selector: 'main .title-text' })
          .parent()
          .parent()
          .parent()
      return {
        linkType: (linkType: string) => {
          findLink().findAllByText(linkType, {
            selector: '.link-type',
          })
        },
        goto: () => {
          findLink().find('.link-type').click()
        },
      }
    },

    expectLinkingTopic: function (linkType: string, target: string) {
      this.linkNoteTo(target).linkType(linkType)
    },

    navigateToLinkingChild: function (targetNoteTopic: string) {
      this.linkNoteTo(targetNoteTopic).goto()
      return assumeNotePage()
    },
    expectLinkingChildren: function (
      linkType: string,
      targetNoteTopics: string
    ) {
      cy.get('main').within(() => {
        commonSenseSplit(targetNoteTopics, ',').forEach((target) => {
          this.expectLinkingTopic(linkType, target)
        })
      })
    },
    changeLinkType: function (linkType: string, target: string) {
      cy.findByRole('title').within(() => {
        cy.get('.link-type').click()
      })
      cy.clickRadioByLabel(linkType)
      cy.pageIsNotLoading()
      this.expectLinkingTopic(linkType, target)
    },

    navigateToReference: (referenceTopic: string) => {
      cy.get('main').within(() => {
        cy.findByText(referenceTopic, {
          selector: '.link-container .title-text',
        }).click()
      })
      return assumeNotePage()
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
    toolbarButton: (btnTextOrTitle: string) => {
      return privateToolbarButton(btnTextOrTitle)
    },
    editTextContent: (noteAttributes: Record<string, string>) => {
      for (const propName in noteAttributes) {
        const value = noteAttributes[propName]
        if (value) {
          cy.findByRole(propName.toLowerCase()).click()
          cy.clearFocusedText().type(value).blur()
          cy.get('.dirty').should('not.exist')
        }
      }
      cy.pageIsNotLoading()
    },
    editNoteImage() {
      return notePageMoreOptionsButton('Edit Note Image')
    },
    audioTools() {
      this.toolbarButton('Audio tools').click()
      return audioToolsPage()
    },
    switchToRichContent() {
      this.toolbarButton('Edit as rich content').click()
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

    updateNoteImage(attributes: Record<string, string>) {
      this.editNoteImage()
        .click()
        .submitWith(
          filterAttributes(attributes, [
            'Upload Image',
            'Image Url',
            'Use Parent Image',
          ])
        )
      return this
    },
    updateNoteUrl(attributes: Record<string, string>) {
      clickNotePageMoreOptionsButton('Edit Note URL').submitWith(
        filterAttributes(attributes, ['Url'])
      )
      return this
    },

    startSearchingAndLinkNote() {
      this.toolbarButton('search and link note').click()
      return assumeNoteTargetSearchDialog()
    },
    addingChildNoteButton() {
      cy.pageIsNotLoading()
      return this.toolbarButton('Add Child Note')
    },
    addingChildNote() {
      this.addingChildNoteButton().click()
      return noteCreationForm
    },
    addingNextSiblingNote() {
      cy.pageIsNotLoading()
      this.toolbarButton('Add Next Sibling Note').click()
      return noteCreationForm
    },
    aiGenerateImage() {
      clickNotePageMoreOptionsButton('Generate Image with DALL-E')
    },
    deleteNote() {
      clickNotePageMoreOptionsButton('Delete note')
      cy.findByRole('button', { name: 'OK' }).click()
      cy.pageIsNotLoading()
    },
    openQuestionList() {
      clickNotePageMoreOptionsButton('Questions for the note')
      return questionListPage()
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
      cy.pageIsNotLoading()
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
      cy.pageIsNotLoading()
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
    memoryTracker() {
      clickNotePageMoreOptionsButton('Note Recall Settings')
      return {
        expectMemoryTrackerInfo(attrs: { [key: string]: string }) {
          for (const k in attrs) {
            cy.contains(k)
              .findByText(attrs[k] ?? '')
              .should('be.visible')
          }
        },
        removeMemoryTrackerFromReview() {
          cy.findByRole('button', {
            name: 'remove this note from review',
          }).click()
          cy.findByRole('button', { name: 'OK' }).click()
          cy.findByText('This memory tracker has been removed from tracking.')
        },
      }
    },
    wikidataOptions() {
      const openWikidataOptions = () =>
        privateToolbarButton('wikidata options').click()

      return {
        associate(wikiID: string) {
          privateToolbarButton('associate wikidata').click()
          cy.replaceFocusedTextAndEnter(wikiID)
        },
        reassociationWith(wikiID: string) {
          openWikidataOptions()
          privateToolbarButton('Edit Wikidata ID').click()
          cy.replaceFocusedTextAndEnter(wikiID)
        },
        hasAssociation() {
          openWikidataOptions()
          const elm = () => {
            return cy.findByRole('button', { name: 'Go to Wikidata' })
          }
          elm()

          return {
            expectALinkThatOpensANewWindowWithURL(url: string) {
              cy.window().then((win) => {
                const popupWindowStub = {
                  location: { href: undefined },
                  focus: cy.stub(),
                }
                cy.stub(win, 'open').as('open').returns(popupWindowStub)
                elm().click()
                cy.get('@open').should('have.been.calledWith', '')
                // using a callback so that cypress can wait until the stubbed value is assigned
                cy.wrap(() => popupWindowStub.location.href)
                  .should((cb) => expect(cb()).equal(url))
                  .then(() => {
                    expect(popupWindowStub.focus).to.have.been.called
                  })
              })
            },
          }
        },
      }
    },
    importObsidianData(filename: string) {
      clickNotePageMoreOptionsButton('more options')
      // Find the label containing "Import from Obsidian" text
      cy.contains('label', 'Import from Obsidian').within(() => {
        cy.get('input[type="file"]').selectFile(
          `e2e_test/fixtures/${filename}`,
          { force: true }
        )
      })
      cy.pageIsNotLoading()
      return this
    },
  }
}
