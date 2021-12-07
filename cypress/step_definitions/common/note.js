/// <reference types="cypress" />
// @ts-check

import {
  And,
  Before,
  Given,
  Then,
  When,
} from "cypress-cucumber-preprocessor/steps";

Given("there are some notes for the current user", (data) => {
  cy.seedNotes(data.hashes());
});

Given(
  "there are some notes for existing user {string}",
  (externalIdentifier, data) => {
    cy.seedNotes(data.hashes(), externalIdentifier);
  }
);

Given("there are notes from Note {int} to Note {int}", (from, to) => {
  const notes = Array(to - from + 1)
    .fill(0)
    .map((_, i) => {
      return { title: `Note ${i + from}` };
    });
  cy.seedNotes(notes);
});

When("I create top level note with:", (data) => {
  cy.visitMyNotebooks();
  cy.findByText("Add New Notebook").click();
  cy.submitNoteFormsWith(data.hashes());
});

When(
  "I am editing note {string} the field should be pre-filled with",
  (noteTitle, data) => {
    cy.clickNotePageButton(noteTitle, "edit note");
    const expects = data.hashes()[0];
    for (var field in expects) {
      cy.getFormControl(field).should("have.value", expects[field]);
    }
  }
);

When("I update it to become:", (data) => {
  cy.submitNoteFormsWith(data.hashes());
});

When(
  "I update note {string} with the description {string}",
  (noteTitle, newDescription) => {
    cy.clickNotePageButton(noteTitle, "edit note");
    cy.submitNoteFormsWith([{ Description: newDescription }]);
  }
);

When("I create note belonging to {string}:", (noteTitle, data) => {
  cy.jumpToNotePage(noteTitle);
  cy.findByText(noteTitle);
  cy.clickAddChildNoteButton();
  cy.submitNoteFormsWith(data.hashes());
});

When("I am creating note under {string}", (noteTitles) => {
  cy.navigateToNotePage(noteTitles);
  cy.clickAddChildNoteButton();
});

Then("I should see {string} in breadcrumb", (noteTitles) => {
  cy.pageIsLoaded();
  cy.get(".breadcrumb").within(() =>
    noteTitles
      .commonSenseSplit(", ")
      .forEach((noteTitle) => cy.findByText(noteTitle, { timeout: 2000 }))
  );
});

Then(
  "I should see these notes belonging to the user at the top level of all my notes",
  (data) => {
    cy.visitMyNotebooks();
    cy.expectNoteCards(data.hashes());
  }
);

Then("I should see these notes as children", (data) => {
  cy.expectNoteCards(data.hashes());
});

When("I delete top level note {string}", (noteTitle) => {
  cy.clickNotePageMoreOptionsButton(noteTitle, "delete note");
  cy.findByRole("button", { name: "OK" }).click();
});

When("I create a sibling note of {string}:", (noteTitle, data) => {
  cy.navigateToChild(noteTitle);
  cy.findByText(noteTitle);
  cy.findByRole("button", { name: "Add Sibling Note" }).click();
  cy.submitNoteFormsWith(data.hashes());
});

When(
  "I should see that the note creation is not successful",
  (noteTitle, data) => {
    cy.findByText("must not be empty");
  }
);

Then("I should see {string} in note title", (noteTitle) => {
  cy.expectNoteTitle(noteTitle);
});

Then(
  "I should not see note {string} at the top level of all my notes",
  (noteTitle) => {
    cy.pageIsLoaded();
    cy.findByText("Notebooks");
    cy.findByText(noteTitle).should("not.exist");
  }
);

When("I open {string} note from top level", (noteTitles) => {
  cy.navigateToNotePage(noteTitles);
});

When("I click the child note {string}", (noteTitle) => {
  cy.navigateToChild(noteTitle);
});

When("I move note {string} left", (noteTitle) => {
  cy.jumpToNotePage(noteTitle);
  cy.findByText("Move This Note").click();
  cy.findByRole("button", { name: "Move Left" }).click();
});

When(
  "I double click {string} and edit the description to {string}",
  (noteTitle, newDescription) => {
    cy.findByText(noteTitle).dblclick();
    cy.submitNoteFormsWith([{ Description: newDescription }]);
  }
);

When("I should see the screenshot matches", () => {
  // cy.get('.content').compareSnapshot('page-snapshot', 0.001);
});

When("I move note {string} right", (noteTitle) => {
  cy.jumpToNotePage(noteTitle);
  cy.findByText("Move This Note").click();
  cy.findByRole("button", { name: "Move Right" }).click();
});

When(
  "I should see {string} is before {string} in {string}",
  (noteTitle1, noteTitle2, parentNoteTitle) => {
    cy.jumpToNotePage(parentNoteTitle);
    var matcher = new RegExp(noteTitle1 + ".*" + noteTitle2, "g");

    cy.get(".card-title").then(($els) => {
      const texts = Array.from($els, (el) => el.innerText);
      expect(texts).to.match(matcher);
    });
  }
);

// This step definition is for demo purpose
Then(
  "*for demo* I should see there are {int} descendants",
  (numberOfDescendants) => {
    cy.findByText("" + numberOfDescendants, {
      selector: ".descendant-counter",
    });
  }
);

When(
  "I should be asked to log in again when I click the link {string}",
  (noteTitle) => {
    cy.on("window:alert", (str) => {
      expect(str).to.equal(`Your login session has expired, let's refresh.`);
    }).then(() => {
      cy.findByText(noteTitle).click();
    });
  }
);

When("I open the {string} view of note {string}", (viewType, noteTitle) => {
  cy.clickNotePageButton(noteTitle, `${viewType} view`);
});

When(
  "I should see the note {string} is {int}px * {int}px offset the center of the map",
  (noteTitle, dx, dy) => {
    cy.withinMindmap().then((cards) => {
      const rect = cards.mindmapRect;
      const frameCenterX = rect.left + rect.width / 2;
      const frameCenterY = rect.top + rect.height / 2;
      const cardRect = cards[noteTitle];
      const cardCenterX = cardRect.left + cardRect.width / 2;
      const cardCenterY = cardRect.top + cardRect.height / 2;
      expect(cardCenterX - frameCenterX).to.closeTo(dx, 10);
      expect(cardCenterY - frameCenterY).to.closeTo(dy, 30);
    });
  }
);

When("I click note {string}", (noteTitle) => {
  cy.findByRole("card", { name: noteTitle }).click();
});

When("I click note title {string}", (noteTitle) => {
  cy.findByText(noteTitle).click();
});

When(
  "The note {string} {string} have the description indicator",
  (noteTitle, shouldOrNot) => {
    cy.findByRole("card", { name: noteTitle }).within(() =>
      cy
        .get(".description-indicator")
        .should(shouldOrNot === "should" ? "exist" : "not.exist")
    );
  }
);

When(
  "I should see the note {string} is {string}",
  (noteTitle, highlightedOrNot) => {
    cy.findByRole("card", { name: noteTitle }).should(
      `${highlightedOrNot === "highlighted" ? "" : "not."}have.class`,
      "highlighted"
    );
  }
);

When("I drag the map by {int}px * {int}px", (dx, dy) => {
  cy.get(".mindmap-event-receiver")
    .trigger("pointerdown", "topLeft")
    .trigger("pointermove", "topLeft", { clientX: dx, clientY: dy })
    .trigger("pointerup", { force: true });
});

When(
  "I drag the map by {int}px * {int}px when holding the shift button",
  (dx, dy) => {
    cy.get(".mindmap-event-receiver")
      .trigger("pointerdown", "topLeft")
      .trigger("pointermove", "topLeft", {
        shiftKey: true,
        clientX: dx,
        clientY: dy,
      })
      .trigger("pointerup", { force: true });
  }
);

When("I zoom in at the {string}", (position) => {
  cy.get(".mindmap-event-receiver").trigger("wheel", position, {
    clientX: 0,
    clientY: 0,
    deltaY: 50,
  });
});

When("I should see the zoom scale is {string}", (scale) => {
  cy.get(".mindmap-info").findByText(scale);
});

When("I click the zoom indicator", (scale) => {
  cy.get(".mindmap-info").click();
});

When(
  "I should see the notes {string} are around note {string} and apart from each other",
  (noteTitles, parentNoteTitle) => {
    cy.withinMindmap().then((cards) => {
      const titles = noteTitles.commonSenseSplit(",");
      titles.forEach((noteTitle) => {
        cy.wrap(cards).distanceBetweenCardsGreaterThan(
          parentNoteTitle,
          noteTitle,
          100
        );
      });
      cy.wrap(cards).distanceBetweenCardsGreaterThan(titles[0], titles[1], 100);
    });
  }
);

Then("I should see the title {string} of the notebook", (noteTitle) => {
  cy.expectNoteTitle(noteTitle);
});

Then("I should see the child notes {string} in order", (notesStr) => {
  const notes = notesStr.split(",");
  notes.forEach((n) => cy.findByText(n));
  cy.findAllByRole("title").then((elms) => {
    let actual = [];
    elms.map((i, actualNote) => actual.push(actualNote.innerHTML));
    actual = actual.filter((c) => notes.includes(c));
    expect(actual.join(",")).to.equal(notes.join(","));
  });
});

Then("I should see {string} is newer than {string}", (newer, older) => {
  let firstColor;
  cy.jumpToNotePage(newer);
  cy.get(".note-body")
    .invoke("css", "border-color")
    .then((val) => (firstColor = val));
  cy.jumpToNotePage(older);
  cy.get(".note-body")
    .invoke("css", "border-color")
    .then((val) =>
      expect(parseInt(firstColor.match(/\d+/)[0])).to.greaterThan(
        parseInt(val.match(/\d+/)[0])
      )
    );
});

When("I split note {string}", (noteTitle, data) => {
  cy.clickNotePageMoreOptionsButton(noteTitle, "split note");
  cy.findByRole("button", { name: "OK" }).click();
});

When(
  "there is a note {string} with description {string}",
  (notePath, expectedDescription) => {
    cy.navigateToNotePage(notePath);
    cy.expectCurrentNoteDescription(expectedDescription);
  }
);

When("I edit note translation to become", (data) => {
  cy.clickTranslationButton("ID");
  cy.clickNoteToolbarButton("edit note");
  cy.submitNoteTranslationFormsWith(data.hashes());
});


When("I open my note the download button is there", () => {
  cy.get('#note-download-button');
});
