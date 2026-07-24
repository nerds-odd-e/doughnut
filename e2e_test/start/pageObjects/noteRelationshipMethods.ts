import { waitUntilAppIsNotBusy } from '../pageBase'
import { form } from '../forms'
import { commonSenseSplit } from '../../support/string_util'
import { assumeNoteTargetSearchDialog } from './noteTargetSearchDialog'
import { toolbarButton } from './toolbarButton'

export const noteRelationshipMethods = () => ({
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
    waitUntilAppIsNotBusy()
  },

  startSearchingAndAddRelationship() {
    toolbarButton('Link').click()
    return assumeNoteTargetSearchDialog()
  },
  insertWikiLinkToNote(toNoteTopic: string) {
    toolbarButton('Link').click()
    assumeNoteTargetSearchDialog()
      .findTarget(toNoteTopic)
      .insertWikiLinkToTarget(toNoteTopic)
    return this
  },
})
