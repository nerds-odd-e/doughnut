import { Operator, Response, Stub } from "@anev/ts-mountebank"
/// <reference types="cypress" />

import { DefaultStub, HttpMethod, FlexiPredicate } from "@anev/ts-mountebank"
import MountebankWrapper from "./MountebankWrapper"

class ServiceMocker {
  private readonly mountebank
  readonly serviceName: string

  constructor(serviceName: string, port: number) {
    this.mountebank = new MountebankWrapper(port)
    this.serviceName = serviceName
  }

  get savedServiceUrlName() {
    return `saved${this.serviceName}Url`
  }

  install() {
    this.mountebank.createImposter()
  }

  get serviceUrl() {
    return this.mountebank.serviceUrl
  }

  public stubByUrl(url: string, data: unknown) {
    return this.stub(new DefaultStub(url, HttpMethod.GET, data, 200))
  }

  public stubGetter(path: string, queryData: unknown, response: unknown) {
    return this.stub(
      new DefaultStub(path, HttpMethod.GET, response, 200).withPredicate(
        new FlexiPredicate().withPath(path).withQuery(queryData).withMethod(HttpMethod.GET),
      ),
    )
  }

  public stubPoster(path: string, response: unknown) {
    return this.stub(
      new DefaultStub(path, HttpMethod.POST, response, 200).withPredicate(
        new FlexiPredicate().withPath(path).withMethod(HttpMethod.POST),
      ),
    )
  }

  public stubPosterUnauthorized(pathMatcher: string, response: unknown) {
    return this.stub(
      new Stub()
        .withPredicate(
          new FlexiPredicate()
            .withOperator(Operator.matches)
            .withPath(pathMatcher)
            .withMethod(HttpMethod.POST),
        )
        .withResponse(new Response().withStatusCode(401).withJSONBody(response)),
    )
  }

  public stubGetterWithError500Response(path: string, response: unknown) {
    return this.stub(
      new DefaultStub(path, HttpMethod.GET, response, 500).withPredicate(
        new FlexiPredicate().withPath(path).withMethod(HttpMethod.GET),
      ),
    )
  }

  private async stub(stub: Stub) {
    this.mountebank.addStubToImposter(stub)
  }
}

export default ServiceMocker
