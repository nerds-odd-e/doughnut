/// <reference types="cypress" />
// @ts-check
import { Then, When } from "@badeball/cypress-cucumber-preprocessor"

When("I visit the swagger-ui", () => {
  cy.visit("http://localhost:9081/swagger-ui/index.html")
})

Then("I can browse the publicly exposed REST APIs", () => {
  cy.findAllByText("OpenAPI definition")
})
