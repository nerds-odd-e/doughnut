import { Stub } from "@anev/ts-mountebank"
/// <reference types="cypress" />

import { Mountebank, Imposter, DefaultStub, HttpMethod, FlexiPredicate } from "@anev/ts-mountebank"
import TestabilityHelper from "./TestabilityHelper"

// @ts-check

class OpenAiServiceTester {
  imposter = new Imposter().withPort(5002)
  onGoingStubbing?: Promise<void>

  restore(cy: Cypress.cy & CyEventEmitter) {
    cy.get(`@${this.savedServiceUrlName}`).then((saved) =>
      this.setOpenAiServiceUrl(cy, saved as unknown as string),
    )
  }
  mock(cy: Cypress.cy & CyEventEmitter) {
    this.setOpenAiServiceUrl(cy, `http://localhost:${this.imposter.port}`).as(
      this.savedServiceUrlName,
    )
  }

  get savedServiceUrlName() {
    return "savedOpenAiServiceUrl"
  }
  private setOpenAiServiceUrl(cy: Cypress.cy & CyEventEmitter, openAiServiceUrl: string) {
    return new TestabilityHelper()
      .postToTestabilityApi(cy, `use_openai_service`, {
        body: { openAiServiceUrl: openAiServiceUrl },
      })
      .then((response) => {
        expect(response.body).to.include("http")
        cy.wrap(response.body)
      })
  }

  private stubByUrl(url: string, data: unknown) {
    return this.stub(new DefaultStub(url, HttpMethod.GET, data, 200))
  }

  private stubWikidataApi(action: string, query: Record<string, string>, data: unknown) {
    const path = `/w/api.php`
    return this.stub(
      new DefaultStub(path, HttpMethod.GET, data, 200).withPredicate(
        new FlexiPredicate()
          .withPath(path)
          .withQuery({ action, ...query })
          .withMethod(HttpMethod.GET),
      ),
    )
  }

  // each stub step in Gherkin will call mountebank API again,
  // however only the last step will take affect.
  // But we have to wait until the previous stubs are done.
  // Alternatively, we can combine all stubs in one step, or have an additional step.
  private async stub(stub: Stub) {
    this.imposter.withStub(stub)

    if (this.onGoingStubbing) {
      this.onGoingStubbing = this.onGoingStubbing.then(() =>
        new Mountebank().createImposter(this.imposter),
      )
      return
    }
    this.onGoingStubbing = new Mountebank().createImposter(this.imposter)
  }
}

export default OpenAiServiceTester
