/// <reference types="cypress" />
import {
  DefaultPredicate,
  FlexiPredicate,
  HttpMethod,
  Predicate,
  Operator,
  Stub,
} from "@anev/ts-mountebank"
import MountebankWrapper from "./MountebankWrapper"
import { NotPredicate } from "./NotPredicate"
import MountebankStubBuilder from "./MountebankStubBuilder"

class ServiceMocker {
  private readonly mountebank: MountebankWrapper
  private readonly mountebankStubBuilder: MountebankStubBuilder
  readonly serviceName: string

  constructor(serviceName: string, port: number) {
    this.mountebank = new MountebankWrapper(port)
    this.mountebankStubBuilder = new MountebankStubBuilder()
    this.serviceName = serviceName
  }

  get savedServiceUrlName() {
    return `saved${this.serviceName}Url`
  }

  private get stubsName() {
    return `${this.serviceName}Stubs`
  }

  install() {
    cy.wrap([]).as(this.stubsName)
    return this.mountebank.createImposter()
  }

  get serviceUrl() {
    return this.mountebank.serviceUrl
  }

  public stubByUrl(url: string, data: unknown) {
    return this.stubGetter(url, {}, data)
  }

  public stubGetter(path: string, queryData: unknown, response: unknown) {
    return this.mockWithPredicates(
      [new FlexiPredicate().withPath(path).withMethod(HttpMethod.GET).withQuery(queryData)],
      [response],
    )
  }

  public stubGetterWithMutipleResponses(path: string, queryData: unknown, responses: unknown[]) {
    return this.mockWithPredicates(
      [new FlexiPredicate().withPath(path).withMethod(HttpMethod.GET).withQuery(queryData)],
      responses,
    )
  }

  public stubPoster(path: string, response: unknown) {
    return this.mockWithPredicates([new DefaultPredicate(path, HttpMethod.POST)], [response])
  }

  public mockMatchsAndNotMatches(
    path: string,
    bodyToMatch: unknown,
    bodyNotToMatch: unknown,
    response: unknown,
  ): Promise<void> {
    const nots = bodyNotToMatch
      ? [new NotPredicate(new FlexiPredicate().withBody(bodyNotToMatch))]
      : []

    const predicate = new FlexiPredicate()
      .withOperator(Operator.matches)
      .withPath(path)
      .withMethod(HttpMethod.POST)
      .withBody(bodyToMatch)
    return this.mockWithPredicates([predicate, ...nots], [response])
  }

  public stubPosterUnauthorized(pathMatcher: string, response: unknown) {
    const stub = this.mountebankStubBuilder.stubWithErrorResponse(
      pathMatcher,
      HttpMethod.POST,
      401,
      response,
    )
    return this.addStubToMountebank(stub)
  }

  public stubGetterWithError500Response(pathMatcher: string, response: unknown) {
    const stub = this.mountebankStubBuilder.stubWithErrorResponse(
      pathMatcher,
      HttpMethod.GET,
      500,
      response,
    )
    return this.addStubToMountebank(stub)
  }

  public stubPosterWithError500Response(pathMatcher: string, response: unknown) {
    const stub = this.mountebankStubBuilder.stubWithErrorResponse(
      pathMatcher,
      HttpMethod.POST,
      500,
      response,
    )
    return this.addStubToMountebank(stub)
  }

  private mockWithPredicates(predicates: Predicate[], responses: unknown[]): Promise<void> {
    const stub = this.mountebankStubBuilder.stubWithPredicates(predicates, responses)
    return this.addStubToMountebank(stub)
  }

  private async addStubToMountebank(stub: Stub): Promise<void> {
    cy.get(`@${this.stubsName}`).then((stubs) => {
      const newStubs = [...stubs, stub]
      cy.wrap(newStubs).as(this.stubsName)
      return this.mountebank.addStubsToImposter(newStubs)
    })
  }
}

export default ServiceMocker
