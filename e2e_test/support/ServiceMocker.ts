/// <reference types="cypress" />
import {
  DefaultPredicate,
  FlexiPredicate,
  HttpMethod,
  Predicate,
  Operator,
} from "@anev/ts-mountebank"
import MountebankWrapper from "./MountebankWrapper"
import { NotPredicate } from "./NotPredicate"

class ServiceMocker {
  private readonly mountebank: MountebankWrapper
  readonly serviceName: string

  constructor(serviceName: string, port: number) {
    this.mountebank = new MountebankWrapper(port)
    this.serviceName = serviceName
  }

  get savedServiceUrlName() {
    return `saved${this.serviceName}Url`
  }

  install() {
    return this.mountebank.createImposter()
  }

  get serviceUrl() {
    return this.mountebank.serviceUrl
  }

  public stubByUrl(url: string, data: unknown) {
    return this.stubGetter(url, {}, data)
  }

  public stubGetter(path: string, queryData: unknown, response: unknown, stubIndex?: number) {
    return this.mockWithPredicates(
      [new FlexiPredicate().withPath(path).withMethod(HttpMethod.GET).withQuery(queryData)],
      response,
      stubIndex,
    )
  }

  public stubPoster(path: string, response: unknown) {
    return this.mockWithPredicates([new DefaultPredicate(path, HttpMethod.POST)], response)
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
    return this.mockWithPredicates([predicate, ...nots], response)
  }

  public stubPosterUnauthorized(pathMatcher: string, response: unknown) {
    return this.mountebank.stubWithErrorResponse(pathMatcher, HttpMethod.POST, 401, response)
  }

  public stubGetterWithError500Response(pathMatcher: string, response: unknown) {
    return this.mountebank.stubWithErrorResponse(pathMatcher, HttpMethod.GET, 500, response)
  }

  public stubPosterWithError500Response(pathMatcher: string, response: unknown) {
    return this.mountebank.stubWithErrorResponse(pathMatcher, HttpMethod.POST, 500, response)
  }

  private mockWithPredicates(
    predicates: Predicate[],
    response: unknown,
    stubIndex?: number,
  ): Promise<void> {
    return this.mountebank.stubWithPredicates(predicates, response, stubIndex)
  }
}

export default ServiceMocker
