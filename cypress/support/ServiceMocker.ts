import { Stub } from "@anev/ts-mountebank"
/// <reference types="cypress" />

import { DefaultStub, HttpMethod, FlexiPredicate } from "@anev/ts-mountebank"
import TestabilityHelper from "./TestabilityHelper"
import MountebankWrapper from "./MountebankWrapper"

class ServiceMocker {
  mountebank
  serviceName

  constructor(port: number, serviceName: string) {
    this.mountebank = new MountebankWrapper(port)
    this.serviceName = serviceName
  }

  mock(cy: Cypress.cy & CyEventEmitter) {
    this.mountebank.createImposter()
    this.setWikidataServiceUrl(cy, this.mountebank.serviceUrl).as(this.savedServiceUrlName)
  }

  restore(cy: Cypress.cy & CyEventEmitter) {
    cy.get(`@${this.savedServiceUrlName}`).then((saved) =>
      this.setWikidataServiceUrl(cy, saved as unknown as string),
    )
  }

  get savedServiceUrlName() {
    return `saved${this.serviceName}Url`
  }

  private setWikidataServiceUrl(cy: Cypress.cy & CyEventEmitter, wikidataServiceUrl: string) {
    return new TestabilityHelper()
      .postToTestabilityApi(cy, `replace_service_url`, {
        body: { [this.serviceName]: wikidataServiceUrl },
      })
      .then((response) => {
        expect(response.body).to.include("http")
        cy.wrap(response.body)
      })
  }

  public stubByUrl(url: string, data: unknown) {
    return this.stub(new DefaultStub(url, HttpMethod.GET, data, 200))
  }

  public stubGetter(path: string, queryData: unknown, data: unknown) {
    return this.stub(
      new DefaultStub(path, HttpMethod.GET, data, 200).withPredicate(
        new FlexiPredicate().withPath(path).withQuery(queryData).withMethod(HttpMethod.GET),
      ),
    )
  }

  private async stub(stub: Stub) {
    this.mountebank.addStubToImposter(stub)
  }
}

export default ServiceMocker
