import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Then("I do these initial reviews in sequence:", (data) => {
  cy.initialReviewInSequence(data.hashes());
})

Given("It's day {int}, {int} hour", (day, hour) => {
    cy.timeTravelTo(day, hour);
});

Then("On day {int} I repeat old {string} and initial review new {string}", (day, repeatNotes, initialNotes) => {
    cy.timeTravelTo(day, 8);

    cy.repeatReviewNotes(repeatNotes);
    cy.initialReviewNotes(initialNotes);
});

Given("I go to the reviews page", () => {
    cy.visit("/reviews");
});

Then("I should see that I have old notes to repeat", () => {
  cy.findByRole('button', {name: "Start reviewing old notes"});
});

Then("I should see that I have new notes to learn", () => {
  cy.findByRole('button', {name: "Start reviewing new notes"});
});

Then("On day {int} I should have {string} note for initial review and {string} for repeat", (day, numberOfInitialReviews, numberOfRepeats) => {
    cy.timeTravelTo(day, 8);
    cy.visit('/reviews');
    cy.findByText(numberOfInitialReviews, {selector: '.number-of-initial-reviews'});
    cy.findByText(numberOfRepeats, {selector: '.number-of-repeats'});
});

Then("I should have {string} for repeat now", (numberOfRepeats) => {
    cy.visit('/reviews');
    cy.findByText(numberOfRepeats, {selector: '.number-of-repeats'});
});

Then("choose to remove it from reviews", () => {
  cy.get("#more-action-for-repeat").click();
  cy.findByRole("button", { name: "Remove This Note from Review" }).click();
});

Then("I initial review {string}", (noteTitle) => {
    cy.initialReviewNotes(noteTitle);
});

Then("I added and learned one note {string} on day {int}", (noteTitle, day) => {
    cy.seedNotes([{'title': noteTitle}]);
    cy.timeTravelTo(day, 8);
    cy.initialReviewNotes(noteTitle);
});

Then("I learned one note {string} on day {int}", (noteTitle, day) => {
    cy.timeTravelTo(day, 8);
    cy.initialReviewNotes(noteTitle);
});

Then("I am repeat-reviewing my old note on day {int}", (day) => {
    cy.timeTravelTo(day, 8);
    cy.visit('/reviews/repeat');
});

Then("I am learning new note on day {int}", (day) => {
    cy.timeTravelTo(day, 8);
    cy.visit('/reviews/initial');
});

Then("I have selected the option {string} in review setting", (option) => {
    cy.getFormControl(option).check();
    cy.findByRole('button', {name: 'Update'}).click();
});

Then("I have selected the option {string}", (option) => {
    cy.getFormControl(option).check();
    cy.findByRole('button', {name: 'Keep for repetition'}).click();
});

Then("I have unselected the option {string}", (option) => {
    cy.getFormControl(option).uncheck();
    cy.findByRole('button', {name: 'Keep for repetition'}).click();
});

Then("I should see the option {string} is {string}", (option, status) => {
  const elm = cy.getFormControl(option);
  if ((status === "on")) {
      elm.should("be.checked")
  }
  else {
      elm.should("not.be.checked");
  }
});

Then("choose to remove it fromm reviews", () => {
    cy.get("#more-action-for-repeat").click();
    cy.findByRole('button', {name: 'Remove This Note from Review'}).click();
});

Then("I choose to do it again", () => {
    cy.get("#repeat-again").click();
});


Then("I should be asked cloze deletion question {string} with options {string}", (question, options) => {
    cy.shouldSeeQuizWithOptions([question], options);
});

Then("I should be asked picture question {string} with options {string}", (pictureInQuestion, options) => {
    cy.shouldSeeQuizWithOptions([], options);
});

Then("I should be asked picture selection question {string} with {string}", (question, options) => {
    cy.shouldSeeQuizWithOptions([question], "");
});



Then("I should be asked spelling question {string}", (question) => {
    cy.findByText(question).should('be.visible');
});

Then("I should be asked link question {string} {string} with options {string}", (noteTitle, linkType, options) => {
    cy.shouldSeeQuizWithOptions([noteTitle, linkType], options);
});

Then("I type my answer {string}", (answer) => {
    cy.getFormControl('Answer').type(answer);
    cy.findByRole('button', {name: 'OK'}).click();
});



Then("I choose answer {string}", (noteTitle) => {
    cy.findByRole('button', {name: noteTitle}).click();
});

Then("I should see that my answer is correct", () => {
    cy.findByText('Correct!');
});

Then("I should see the information of note {string}", (noteTitle) => {
    cy.findByText(noteTitle);
});

Then("I should see that my answer {string} is wrong", (answer) => {
    cy.findByText(`Your answer \`${answer}\` is wrong.`);
});

Then("I should see the satisfied button: {string}", (yesNo) => {
    cy.get('#repeat-satisfied').should(yesNo === 'yes' ? 'exist' : 'not.exist');
});

Then("I am changing note {string}'s review setting", (noteTitle) => {
  cy.get('@seededNoteIdMap').then(seededNoteIdMap=>
    cy.visit(`/notes/${seededNoteIdMap[noteTitle]}/review_setting`)
  );
});

Then("The randomizer always choose the last", (yesNo) => {
    cy.randomizerAlwaysChooseLast();
});

