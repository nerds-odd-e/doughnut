// ***********************************************
// custom commands and overwrite existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })

/// <reference types="cypress" />
// @ts-check
import '@testing-library/cypress/add-commands'
import 'cypress-file-upload'
import start from '../start'
import { commonSenseSplit } from './string_util'

Cypress.Commands.add('pageIsNotLoading', () => {
  cy.get('.loading-bar').should('not.exist', { timeout: 10000 })
})

Cypress.Commands.add('dialogDisappeared', () => {
  cy.get('.modal-body').should('not.exist')
})

Cypress.Commands.add('expectBreadcrumb', (items: string) => {
  cy.get('.daisy-breadcrumbs').within(() =>
    commonSenseSplit(items, ', ').forEach((noteTopology: string) =>
      cy.findByText(noteTopology)
    )
  )
})

Cypress.Commands.add('clearFocusedText', () => {
  // cy.clear for now is an alias of cy.type('{selectall}{backspace}')
  // it doesn't clear the text sometimes.
  // Invoking it twice seems to solve the problem.
  cy.focused()
    .should('be.visible')
    .should('match', 'input, textarea')
    .clear()
    .clear()
})

Cypress.Commands.add('replaceFocusedTextAndEnter', (text) => {
  cy.clearFocusedText().type(text).type('{enter}')
})

Cypress.Commands.add(
  'fieldShouldHaveValue',
  { prevSubject: true },
  ($input: JQuery<HTMLElement>, value: string) => {
    cy.wrap($input).should('have.value', value)
  }
)

Cypress.Commands.add(
  'assignFieldValue',
  { prevSubject: true },
  ($input: JQuery<HTMLElement>, value: string) => {
    if ($input.attr('type') === 'file') {
      cy.fixture(value).then((img) => {
        cy.wrap($input).attachFile({
          fileContent: Cypress.Blob.base64StringToBlob(img),
          fileName: value,
          mimeType: 'image/png',
        })
      })
    } else if ($input.attr('role') === 'radiogroup') {
      cy.clickRadioByLabel(value)
    } else if ($input.attr('role') === 'button') {
      cy.wrap($input).click()
      cy.clickRadioByLabel(value)
    } else {
      cy.wrap($input).clear().type(value)
    }
  }
)

Cypress.Commands.add('clickRadioByLabel', (labelText) => {
  cy.findByText(labelText, { selector: 'label' }).click({ force: true })
})

Cypress.Commands.add(
  'expectNoteCards',
  (expectedCards: Record<string, string>[]) => {
    cy.get('.daisy-card-title').should('have.length', expectedCards.length)
    expectedCards.forEach((elem) => {
      for (const propName in elem) {
        if (propName === 'note-title') {
          cy.findCardTitle(elem[propName]!)
        } else {
          cy.findByText(elem[propName]!)
        }
      }
    })
  }
)

interface RouterPushOptions {
  name: string
  params: Record<string, string | number>
  query: Record<string, string | number>
}

interface RouteParams {
  [key: string]: string | number
}

type CustomWindow = Omit<Cypress.AUTWindow, 'Infinity' | 'NaN'> & {
  Infinity: number
  NaN: number
  router?: {
    push: (options: RouterPushOptions) => Promise<unknown>
  }
}

Cypress.Commands.add(
  'routerPush',
  (fallback: string, name: string, params: RouteParams) => {
    cy.get('@firstVisited').then((firstVisited) => {
      // Extract the value from the Cypress subject
      const isFirstVisited =
        (firstVisited as unknown as { valueOf(): string }).valueOf() === 'yes'
      cy.window().then(async (win: CustomWindow) => {
        if (win.router && isFirstVisited) {
          try {
            await win.router.push({
              name,
              params,
              query: { time: Date.now() }, // make sure the route re-render
            })
            cy.dialogDisappeared()
            return
          } catch (error) {
            cy.log('router push failed')
            cy.log(error as string)
          }
        }
        cy.wrap('yes').as('firstVisited')
        cy.visit(fallback)
      })
    })
  }
)

Cypress.Commands.add('clickButtonOnCardBody', (noteTopology, buttonTitle) => {
  cy.findCardTitle(noteTopology).then(($card) => {
    cy.wrap($card)
      .parent()
      .parent()
      .parent()
      .parent()
      .parent()
      .findByText(buttonTitle)
      .then(($button) => {
        cy.wrap($button).click()
      })
  })
})

Cypress.Commands.add('startSearching', () => {
  start.assumeNotePage().toolbarButton('search note').click()
})

Cypress.Commands.add(
  'initialReviewOneNoteIfThereIs',
  ({
    'Review Type': reviewType,
    Title: title,
    'Additional Info': additionalInfo,
    Skip: skip,
  }) => {
    if (reviewType === 'initial done') {
      cy.contains("You've achieved your daily assimilation goal").should(
        'be.visible'
      )
    } else {
      cy.findByText(title, { selector: 'main *' })
      switch (reviewType) {
        case 'single note': {
          if (additionalInfo) {
            cy.get('.note-details').should('contain', additionalInfo)
          }
          break
        }

        case 'image note': {
          if (additionalInfo) {
            const [expectedDetails, expectedImage] = commonSenseSplit(
              additionalInfo,
              '; '
            )
            cy.get('.note-details').should('contain', expectedDetails)
            cy.get('#note-image')
              .find('img')
              .should('have.attr', 'src')
              .should('include', expectedImage)
          }
          break
        }

        case 'link': {
          if (additionalInfo) {
            const [linkType, targetNote] = commonSenseSplit(
              additionalInfo,
              '; '
            )
            if (typeof title === 'string') {
              cy.findByText(title, { selector: 'main *' })
            }

            if (typeof targetNote === 'string') {
              cy.findByText(targetNote)
            }

            if (typeof linkType === 'string') {
              cy.get('.link-type').contains(linkType)
            }
          }
          break
        }

        default:
          expect(reviewType).equal('a known review page type')
      }
      if (skip === 'yes') {
        cy.findByText('Skip repetition').click()
        cy.findByRole('button', { name: 'OK' }).click()
      } else {
        cy.findByText('Keep for repetition').click()
      }
    }
  }
)

Cypress.Commands.add('findCardTitle', (title) =>
  cy.findByText(title, { selector: '.daisy-card-title .title-text' })
)

Cypress.Commands.add('yesIRemember', () => {
  cy.findByRole('button', { name: 'Yes, I remember' })
  cy.tick(11 * 1000).then(() => {
    cy.findByRole('button', { name: 'Yes, I remember' }).click({})
  })
})

Cypress.Commands.add('routerToRoot', () => {
  cy.routerPush('/', 'root', {})
})

Cypress.Commands.add('formField', (label) => {
  return cy.findByLabelText(label)
})

Cypress.Commands.add('failure', () => {
  throw new Error('Deliberate CYPRESS test Failure!!!')
})

Cypress.Commands.add('undoLast', (undoType: string) => {
  cy.findByTitle(`undo ${undoType}`).click()
  cy.pageIsNotLoading()
})

Cypress.Commands.add('noteByTitle', (noteTopology: string) => {
  return cy
    .findCardTitle(noteTopology)
    .parent()
    .invoke('attr', 'href')
    .then(($attr) => {
      const match = /n(\d+)/g.exec($attr as string)?.[1]
      if (match) {
        return match
      }
      throw new Error(`Could not find note ID in href: ${$attr}`)
    })
})

Cypress.Commands.add(
  'expectFieldErrorMessage',
  (field: string, message: string) => {
    cy.formField(field)
      .parent()
      .siblings('.daisy-text-error')
      .findByText(message)
  }
)

Cypress.Commands.add('expectAMapTo', (latitude: string, longitude: string) => {
  cy.findByText(`Location: ${latitude}'N, ${longitude}'E`)
})

Cypress.Commands.add('dismissLastErrorMessage', () => {
  cy.get('.Vue-Toastification__close-button').click()
})
