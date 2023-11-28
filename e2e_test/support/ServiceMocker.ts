import { DefaultPredicate, FlexiPredicate, HttpMethod, Predicate } from "@anev/ts-mountebank"
/// <reference types="cypress" />
import MountebankWrapper from "./MountebankWrapper"

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

  public stubGetter(path: string, queryData: unknown, response: unknown) {
    return this.mockWithPredicate(
      new FlexiPredicate().withPath(path).withMethod(HttpMethod.GET).withQuery(queryData),
      response,
    )
  }

  public stubPoster(path: string, response: unknown) {
    return this.mockWithPredicate(new DefaultPredicate(path, HttpMethod.POST), response)
  }

  public mockWithPredicate(predicate: Predicate, response: unknown): Promise<void> {
    return this.mountebank.stubWithPredicates([predicate], response)
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
}

export default ServiceMocker
