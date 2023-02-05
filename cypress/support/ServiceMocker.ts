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

  install() {
    this.mountebank.createImposter()
  }

  restore(cy: Cypress.cy & CyEventEmitter) {
    cy.get(`@${this.savedServiceUrlName}`).then((saved) =>
      this.setServiceUrl(cy, saved as unknown as string),
    )
  }

  get savedServiceUrlName() {
    return `saved${this.serviceName}Url`
  }

  get serviceUrl() {
    return this.mountebank.serviceUrl
  }

  private setServiceUrl(cy: Cypress.cy & CyEventEmitter, serviceUrl: string) {
    return new TestabilityHelper()
      .postToTestabilityApi(cy, `replace_service_url`, {
        body: { [this.serviceName]: serviceUrl },
      })
      .then((response) => {
        expect(response.body).to.haveOwnProperty(this.serviceName)
        expect(response.body[this.serviceName]).to.include("http")
        cy.wrap(response.body[this.serviceName])
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
