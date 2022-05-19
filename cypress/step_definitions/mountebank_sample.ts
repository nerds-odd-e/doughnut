/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"
import {
  HttpMethod,
  Imposter,
  Mountebank,
  DefaultStub,
} from '@anev/ts-mountebank';

Given("I have a record on {string} on the external service", async (record) => {
  const mb = new Mountebank();
  let imposter = new Imposter().withPort(5000).withStub(
      new DefaultStub(`/external/${record}`, HttpMethod.GET, "foo", 200));
  await mb.createImposter(imposter);
})

When("I ask for the {string} record", (record) => {
  cy.request(`http://localhost:5000/external/${record}`).as("resp")
})

Then("I should get the expected payload", (record) => {
  cy.get("@resp").its("body").should("equals", "foo")
})
