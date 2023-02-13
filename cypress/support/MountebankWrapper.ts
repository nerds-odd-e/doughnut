import {
  Predicate,
  Response,
  Stub,
  Mountebank,
  Imposter,
  HttpMethod,
  FlexiPredicate,
  Operator,
} from "@anev/ts-mountebank"
/// <reference types="cypress" />

import request from "superagent"

// @ts-check

class MountebankWrapper {
  mountebank = new Mountebank()
  port: number

  constructor(port: number) {
    this.port = port
  }

  public get serviceUrl(): string {
    return `http://localhost:${this.port}`
  }
  public async tryDeleteImposter(): Promise<void> {
    try {
      // just try to delete in case an imposter is there
      await this.mountebank.deleteImposter(this.port)
    } catch (error) {} // eslint-disable-line
  }

  public async createImposter(): Promise<void> {
    const imposter = new Imposter().withPort(this.port)
    this.tryDeleteImposter()
    const response = await request
      .post(`${this.mountebank.mountebankUrl}/imposters`)
      .send(JSON.stringify(imposter))

    if (response.statusCode != 201)
      throw new Error(`Problem creating imposter: ${JSON.stringify(response?.error)}`)
  }

  public stubWithPredicate(predicate: Predicate, response: unknown) {
    return this.addStubToImposter(
      new Stub()
        .withPredicate(predicate)
        .withResponse(new Response().withStatusCode(200).withJSONBody(response)),
    )
  }

  public stubWithErrorResponse(
    pathMatcher: string,
    method: HttpMethod,
    status: number,
    response: unknown,
  ) {
    return this.addStubToImposter(
      new Stub()
        .withPredicate(
          new FlexiPredicate()
            .withOperator(Operator.matches)
            .withPath(pathMatcher)
            .withMethod(method),
        )
        .withResponse(new Response().withStatusCode(status).withJSONBody(response)),
    )
  }

  private async addStubToImposter(stub: Stub): Promise<void> {
    const response = await request
      .post(`${this.mountebank.mountebankUrl}/imposters/${this.port}/stubs`)
      .send(JSON.stringify({ stub }))

    if (response.statusCode != 200)
      throw new Error(`Problem adding stub to imposter: ${JSON.stringify(response?.error)}`)
  }
}

export default MountebankWrapper
