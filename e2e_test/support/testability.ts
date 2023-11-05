/// <reference types="cypress" />
// @ts-check
import ServiceMocker from "./ServiceMocker"
import start from "start"

Cypress.Commands.add("mock", { prevSubject: true }, (serviceMocker: ServiceMocker) => {
  start
    .testability()
    .setServiceUrl(serviceMocker.serviceName, serviceMocker.serviceUrl)
    .as(serviceMocker.savedServiceUrlName)
  serviceMocker.install()
})

Cypress.Commands.add("restore", { prevSubject: true }, (serviceMocker: ServiceMocker): void => {
  cy.get(`@${serviceMocker.savedServiceUrlName}`).then((saved) =>
    start.testability().setServiceUrl(serviceMocker.serviceName, saved as unknown as string),
  )
})
