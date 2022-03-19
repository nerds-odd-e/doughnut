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

import "@testing-library/cypress/add-commands"
import "cypress-file-upload"
require("cy-verify-downloads").addCustomCommand()

// const compareSnapshotCommand = require('cypress-image-diff-js/dist/command');
// compareSnapshotCommand();

Cypress.Commands.add("pageIsLoaded", () => {
  cy.get(".loading-bar").should("not.exist")
})

Cypress.Commands.add("loginAs", (username) => {
  const password = "password"
  cy.request({
    method: "POST",
    url: "/login",
    form: true,
    body: { username, password },
  }).then((response) => {
    expect(response.status).to.equal(200)
  })
})

Cypress.Commands.add("logout", (username) => {
  cy.request({
    method: "POST",
    url: "/logout",
  }).then((response) => {
    expect(response.status).to.equal(204)
  })
})

Cypress.Commands.add("createLink", (type, fromNoteTitle, toNoteTitle) => {
  cy.get("@seededNoteIdMap").then((seededNoteIdMap) =>
    cy
      .request({
        method: "POST",
        url: "/api/testability/link_notes",
        body: {
          type,
          source_id: seededNoteIdMap[fromNoteTitle],
          target_id: seededNoteIdMap[toNoteTitle],
        },
      })
      .its("body")
      .should("contain", "OK"),
  )
})

Cypress.Commands.add("triggerException", () => {
  cy.request({
    method: "POST",
    url: `/api/testability/trigger_exception`,
    failOnStatusCode: false,
  })
})

Cypress.Commands.add("submitNoteCreationFormWith", (noteAttributes) => {
  const linkTypeToParent = noteAttributes["Link Type To Parent"]
  delete noteAttributes["Link Type To Parent"]
  const { Title, Description, ...remainingAttrs } = noteAttributes

  cy.submitNoteFormWith({ Title, "Link Type To Parent": linkTypeToParent })

  if (!!Title) {
    cy.findByText(Title)
  }

  if (!!Description) {
    cy.inPlaceEdit({ Description })
  }

  if (Object.keys(remainingAttrs).length > 0) {
    cy.clickNoteToolbarButton("edit note")
    cy.submitNoteFormWith(remainingAttrs)
  }
})

Cypress.Commands.add("replaceFocusedText", (text) => {
  // cy.clear for now is an alias of cy.type('{selectall}{backspace}')
  // it doesn't clear the text sometimes.
  // Invoking it twice seems to solve the problem.
  cy.focused().clear().clear().type(text).type("{shift}{enter}")
})

Cypress.Commands.add("inPlaceEdit", (noteAttributes) => {
  for (var propName in noteAttributes) {
    const value = noteAttributes[propName]
    if (value) {
      cy.findByRole(propName.toLowerCase()).click()
      cy.replaceFocusedText(value)
    }
  }
})

Cypress.Commands.add("submitNoteFormWith", (noteAttributes) => {
  for (var propName in noteAttributes) {
    const value = noteAttributes[propName]
    if (value) {
      cy.getFormControl(propName).then(($input) => {
        if ($input.attr("type") === "file") {
          cy.fixture(value).then((img) => {
            cy.wrap($input).attachFile({
              fileContent: Cypress.Blob.base64StringToBlob(img),
              fileName: value,
              mimeType: "image/png",
            })
          })
        } else if ($input.attr("role") === "radiogroup") {
          cy.clickRadioByLabel(value)
        } else {
          cy.wrap($input).clear().type(value)
        }
      })
    }
  }
  cy.get('input[value="Submit"]').click()
})

Cypress.Commands.add("clickAddChildNoteButton", () => {
  cy.pageIsLoaded()
  cy.findAllByRole("button", { name: "Add Child Note" }).first().click()
})

Cypress.Commands.add("clickRadioByLabel", (labelText) => {
  cy.findByText(labelText, { selector: "label" }).click({ force: true })
})

Cypress.Commands.add("submitNoteCreationFormsWith", (notes) => {
  notes.forEach((noteAttributes) => cy.submitNoteCreationFormWith(noteAttributes))
})

Cypress.Commands.add("submitNoteFormsWith", (notes) => {
  notes.forEach((noteAttributes) => cy.submitNoteFormWith(noteAttributes))
})

Cypress.Commands.add("expectNoteCards", (expectedCards) => {
  expectedCards.forEach((elem) => {
    for (var propName in elem) {
      if (propName === "note-title") {
        cy.findByText(elem[propName], { selector: ".card-title a" }).should("be.visible")
      } else {
        cy.findByText(elem[propName])
      }
    }
  })
})

Cypress.Commands.add("navigateToChild", (noteTitle) => {
  cy.findByText(noteTitle, { selector: ".card-title" }).click()
})

Cypress.Commands.add("navigateToNotePage", (noteTitlesDividedBySlash) => {
  cy.visitMyNotebooks()
  noteTitlesDividedBySlash
    .commonSenseSplit("/")
    .forEach((noteTitle) => cy.navigateToChild(noteTitle))
})

// jumptoNotePage is faster than navigateToNotePage
//    it uses the note id memorized when creating them with testability api
Cypress.Commands.add("jumpToNotePage", (noteTitle, forceLoadPage) => {
  cy.get("@firstVisited").then((firstVisited) => {
    cy.get("@seededNoteIdMap").then((seededNoteIdMap) =>
      cy.window().then((win) => {
        if (!!win.router && !forceLoadPage && !firstVisited) {
          const noteId = seededNoteIdMap[noteTitle]
          const r = win.router.push({
            name: "noteShow",
            params: { rawNoteId: noteId },
            query: { time: Date.now() },
          })
          cy.get(".modal-body").should("not.exist")
          return
        }
        cy.wrap(true).as("firstVisited")
        cy.visit(`/notes/${seededNoteIdMap[noteTitle]}`)
      }),
    )
  })
  cy.expectNoteTitle(noteTitle)
})

Cypress.Commands.add("clickButtonOnCardBody", (noteTitle, buttonTitle) => {
  const card = cy.findByText(noteTitle, { selector: ".card-title a" })
  const button = card.parent().parent().findByText(buttonTitle)
  button.click()
})

Cypress.Commands.add("visitMyNotebooks", (noteTitle) => {
  cy.visit("/notebooks")
})

Cypress.Commands.add("startSearching", (noteTitle) => {
  cy.clickNoteToolbarButton("link note")
})

Cypress.Commands.add("clickNotePageButton", (noteTitle, btnTextOrTitle, forceLoadPage) => {
  cy.jumpToNotePage(noteTitle, forceLoadPage)
  cy.clickNoteToolbarButton(btnTextOrTitle)
})

Cypress.Commands.add("clickNotePageMoreOptionsButton", (noteTitle, btnTextOrTitle) => {
  cy.jumpToNotePage(noteTitle)
  cy.clickNotePageMoreOptionsButtonOnCurrentPage(btnTextOrTitle)
})

Cypress.Commands.add("clickNotePageMoreOptionsButtonOnCurrentPage", (btnTextOrTitle) => {
  cy.clickNoteToolbarButton("more options")
  cy.clickNoteToolbarButton(btnTextOrTitle)
})

Cypress.Commands.add("expectExactLinkTargets", (targets) => {
  cy.get(".search-result .card-title a").then(elms=>Cypress._.map(elms, 'innerText')).should('deep.equal', targets)
})

Cypress.Commands.add("findNoteCardButton", (noteTitle, btnTextOrTitle) => {
  return cy
    .findByText(noteTitle)
    .parent()
    .parent()
    .parent()
    .parent()
    .findByRole("button", { name: btnTextOrTitle })
})

Cypress.Commands.add("findNoteCardEditButton", (noteTitle) => {
  return cy.findNoteCardButton(noteTitle, "edit note")
})

Cypress.Commands.add("updateCurrentUserSettingsWith", (hash) => {
  cy.request({
    method: "POST",
    url: "/api/testability/update_current_user",
    body: hash,
  })
    .its("body")
    .should("contain", "OK")
})

Date.prototype.addDays = function (days) {
  var date = new Date(this.valueOf())
  date.setDate(date.getDate() + days)
  return date
}

Cypress.Commands.add(
  "initialReviewOneNoteIfThereIs",
  ({ review_type, title, additional_info, skip }) => {
    if (review_type == "initial done") {
      cy.findByText("You have achieved your daily new notes goal.").should("be.visible")
    } else {
      cy.findByText(title)
      switch (review_type) {
        case "single note": {
          if (additional_info) {
            cy.get(".note-body").should("contain", additional_info)
          }
          break
        }

        case "picture note": {
          if (additional_info) {
            const [expectedDescription, expectedPicture] = additional_info.commonSenseSplit("; ")
            cy.get(".note-body").should("contain", expectedDescription)
            cy.get("#note-picture")
              .find("img")
              .should("have.attr", "src")
              .should("include", expectedPicture)
          }
          break
        }

        case "link": {
          if (additional_info) {
            const [linkType, targetNote] = additional_info.commonSenseSplit("; ")
            cy.findByText(title)
            cy.findByText(targetNote)
            cy.get(".badge").contains(linkType)
          }
          break
        }

        default:
          expect(review_type).equal("a known review page type")
      }
      if (skip) {
        cy.findByText("Skip repetition").click()
        cy.findByRole("button", { name: "OK" }).click()
      } else {
        cy.findByText("Keep for repetition").click()
      }
    }
  },
)

Cypress.Commands.add("expectNoteTitle", (title) =>
  cy.findByText(title, { selector: "[role=title] *" }),
)

Cypress.Commands.add("repeatReviewOneNoteIfThereIs", ({ review_type, title, additional_info }) => {
  if (review_type == "repeat done") {
    cy.findByText("You have reviewed all the old notes for today.").should("be.visible")
  } else {
    cy.findByText(title, { selector: "h2" })
    switch (review_type) {
      case "single note": {
        if (additional_info) {
          cy.get(".note-body").should("contain", additional_info)
        }
        break
      }

      default:
        expect(review_type).equal("a known review page type")
    }
    cy.get("#repeat-satisfied").click()
  }
})

Cypress.Commands.add("navigateToCircle", (circleName) => {
  cy.visit("/circles")
  cy.findByText(circleName).click()
})

Cypress.Commands.add("initialReviewInSequence", (reviews) => {
  cy.visit("/reviews/initial")
  reviews.forEach((initialReview) => {
    cy.initialReviewOneNoteIfThereIs(initialReview)
  })
})

Cypress.Commands.add("initialReviewNotes", (noteTitles) => {
  cy.initialReviewInSequence(
    noteTitles.commonSenseSplit(", ").map((title) => {
      return {
        review_type: title === "end" ? "initial done" : "single note",
        title,
      }
    }),
  )
})

Cypress.Commands.add("repeatReviewNotes", (noteTitles) => {
  cy.visit("/reviews/repeat")
  noteTitles.commonSenseSplit(",").forEach((title) => {
    const review_type = title === "end" ? "repeat done" : "single note"
    cy.repeatReviewOneNoteIfThereIs({ review_type, title })
  })
})

Cypress.Commands.add("shouldSeeQuizWithOptions", (questionParts, options) => {
  questionParts.forEach((part) => {
    cy.get(".quiz-instruction").contains(part)
  })
  options.commonSenseSplit(",").forEach((option) => cy.findByText(option).should("be.visible"))
})

Cypress.Commands.add("getFormControl", (label) => {
  return cy.findByLabelText(label)
})

Cypress.Commands.add("subscribeToNote", (noteTitle, dailyLearningCount) => {
  cy.findNoteCardButton(noteTitle, "Add to my learning").click()
  cy.get("#subscription-dailyTargetOfNewNotes").clear().type(dailyLearningCount)
  cy.findByRole("button", { name: "Submit" }).click()
})

Cypress.Commands.add("unsubscribeFromNotebook", (noteTitle) => {
  cy.visitMyNotebooks()
  cy.findNoteCardButton(noteTitle, "Unsubscribe").click()
})

Cypress.Commands.add("searchNote", (searchKey) => {
  cy.getFormControl("Search Globally").check()
  cy.findByPlaceholderText("Search").clear().type(searchKey)
  cy.tick(500)
})

Cypress.Commands.add("visitBlog", () => {
  cy.visit("/index.html")
})

Cypress.Commands.add("assertBlogPostInWebsiteByTitle", (article) => {
  cy.get("#article-container").within(() => {
    cy.get(".article")
      .first()
      .within(() => {
        cy.get(".title").first().should("have.text", article.title)
        cy.get(".content").first().should("have.text", article.description)
        cy.get(".authorName").first().should("have.text", article.authorName)
        cy.get(".createdAt").first().should("have.text", article.createdAt)
      })
  })
})

Cypress.Commands.add("failure", () => {
  throw new Error("Deliberate CYPRESS test Failure!!!")
})

Cypress.Commands.add("expectCurrentNoteDescription", (expectedDescription) => {
  cy.findByText(expectedDescription, { selector: ".note-description *" })
})

Cypress.Commands.add("withinMindmap", () => {
  cy.pageIsLoaded()
  cy.wrap(
    new Promise((resolve, reject) => {
      cy.get(`.box .content .inner-box .content`).then((mindmap) => {
        const rect = mindmap[0].getBoundingClientRect()
        cy.get("[role='card']").then(($elms) => {
          const cards = Object.fromEntries(
            Cypress.$.makeArray($elms).map((el) => [el.innerText, el.getBoundingClientRect()]),
          )
          cards.mindmapRect = rect
          resolve(cards)
        })
      })
    }),
  )
})

Cypress.Commands.add(
  "distanceBetweenCardsGreaterThan",
  { prevSubject: true },
  (cards, note1, note2, min) => {
    const rect1 = cards[note1]
    const rect2 = cards[note2]
    const xd = (rect1.right + rect1.left) / 2 - (rect2.right + rect2.left) / 2
    const yd = (rect1.top + rect1.bottom) / 2 - (rect2.top + rect2.bottom) / 2
    expect(Math.sqrt(xd * xd + yd * yd)).greaterThan(min)
  },
)

Cypress.Commands.add(
  "expectText",
  (text) => cy.findByText(text), //.should("be.visible")
  // Add should be visible back when the link view page is remade.
)

Cypress.Commands.add("clickNoteToolbarButton", (btnTextOrTitle) => {
  cy.get(".toolbar").findByRole("button", { name: btnTextOrTitle }).click()
})

Cypress.Commands.add("deleteNoteViaAPI", { prevSubject: true }, (subject) => {
  cy.request({
    method: "POST",
    url: `/api/notes/${subject}/delete`,
  }).then((response) => {
    expect(response.status).to.equal(200)
  })
})

Cypress.Commands.add("noteByTitle", (noteTitle) => {
  return cy
    .get("a.card-title")
    .invoke("attr", "href")
    .then(($attr) => /notes\/(\d+)/g.exec($attr)[1])
})
