import {
  DefaultPredicate,
  HttpMethod,
  FlexiPredicate,
  Operator,
  Predicate,
  Response,
  Stub,
} from "@anev/ts-mountebank"
/// <reference types="cypress" />

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
    return this.stubGetter(url, {}, data)
  }

  public stubGetter(path: string, queryData: unknown, response: unknown) {
    return this.stubWithPredicate(
      new FlexiPredicate().withPath(path).withMethod(HttpMethod.GET).withQuery(queryData),
      response,
    )
  }

  public stubPoster(path: string, response: unknown) {
    return this.stubWithPredicate(new DefaultPredicate(path, HttpMethod.POST), response)
  }

  public stubPosterUnauthorized(pathMatcher: string, response: unknown) {
    return this.stubWithErrorResponse(pathMatcher, HttpMethod.POST, 401, response)
  }

  public stubGetterWithError500Response(pathMatcher: string, response: unknown) {
    return this.stubWithErrorResponse(pathMatcher, HttpMethod.GET, 500, response)
  }

  private stubWithPredicate(predicate: Predicate, response: unknown) {
    return this.stub(
      new Stub()
        .withPredicate(predicate)
        .withResponse(new Response().withStatusCode(200).withJSONBody(response)),
    )
  }

  private stubWithErrorResponse(
    pathMatcher: string,
    method: HttpMethod,
    status: number,
    response: unknown,
  ) {
    return this.stub(
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

  private async stub(stub: Stub) {
    this.mountebank.addStubToImposter(stub)
  }
}

export default ServiceMocker
