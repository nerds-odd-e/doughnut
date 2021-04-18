import {
    Given,
    And,
    Then,
    When,
    Before
} from "cypress-cucumber-preprocessor/steps";

Then(
    'I should be asked cloze deletion question {string} with options {string}',
    (question, options) => {
        cy.shouldSeeQuizWithOptions([question], options);
    }
);

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

Then('I have unselected the option {string}', (option) => {
    cy.getFormControl(option).uncheck();
    cy.findByRole('button', { name: 'Keep for repetition' }).click();
});

Then('I should see the option {string} is {string}', (option, status) => {
    const elm = cy.getFormControl(option);
    if (status === 'on') {
        elm.should('be.checked');
    } else {
        elm.should('not.be.checked');
    }
});