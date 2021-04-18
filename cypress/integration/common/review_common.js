import {
    Given,
    And,
    Then,
    When,
    Before
} from "cypress-cucumber-preprocessor/steps";

Given("It's day {int}, {int} hour", (day, hour) => {
    cy.timeTravelTo(day, hour);
});

Then("I do these initial reviews in sequence:", data => {
    cy.initialReviewInSequence(data.hashes());
});

Then("I am repeat-reviewing my old note on day {int}", day => {
    cy.timeTravelTo(day, 8);
    cy.visit("/reviews/repeat");
});

Then('I am learning new note on day {int}', (day) => {
    cy.timeTravelTo(day, 8);
    cy.visit('/reviews/initial');
});

Then("I learned one note {string} on day {int}", (noteTitle, day) => {
    cy.timeTravelTo(day, 8);
    cy.initialReviewNotes(noteTitle);
});

Then(
    "On day {int} I should have {string} note for initial review and {string} for repeat",
    (day, numberOfInitialReviews, numberOfRepeats) => {
        cy.timeTravelTo(day, 8);
        cy.visit("/reviews");
        cy.findByText(numberOfInitialReviews, {
            selector: ".number-of-initial-reviews"
        });
        cy.findByText(numberOfRepeats, { selector: ".number-of-repeats" });
    }
);

Then('I choose answer {string}', (noteTitle) => {
    cy.findByRole('button', { name: noteTitle }).click();
});

Then(
    'On day {int} I repeat old {string} and initial review new {string}',
    (day, repeatNotes, initialNotes) => {
        cy.timeTravelTo(day, 8);

        cy.repeatReviewNotes(repeatNotes);
        cy.initialReviewNotes(initialNotes);
    }
);

Then("I should be asked spelling question {string}", question => {
    cy.findByText(question).should("be.visible");
});

Then('I type my answer {string}', (answer) => {
    cy.getFormControl('Answer').type(answer);
    cy.findByRole('button', { name: 'OK' }).click();
});

Then('I have selected the option {string}', (option) => {
    cy.getFormControl(option).check();
    cy.findByRole('button', { name: 'Keep for repetition' }).click();
});

Then('I should see that my answer is correct', () => {
    cy.findByText('Correct!');
});

Then('I should see that my answer {string} is wrong', (answer) => {
    cy.findByText(`Your answer \`${answer}\` is wrong.`);
});

Then('I should see the satisfied button: {string}', (yesNo) => {
    cy.get('#repeat-satisfied').should(yesNo === 'yes' ? 'exist' : 'not.exist');
});

Then("I am changing note {string}'s review setting", (noteTitle) => {
    cy.get('@seededNoteIdMap').then((seededNoteIdMap) =>
        cy.visit(`/notes/${seededNoteIdMap[noteTitle]}/review_setting`)
    );
});

Then('I have selected the option {string} in review setting', (option) => {
    cy.getFormControl(option).check();
    cy.findByRole('button', { name: 'Update' }).click();
});