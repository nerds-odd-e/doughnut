/// <reference types="cypress" />
// @ts-check
import {
  FlexiPredicate,
  HttpMethod,
  Operator,
  Predicate,
  Response,
  Stub,
} from '@anev/ts-mountebank'

class MountebankStubBuilder {
  public stubWithPredicates(
    predicates: Predicate[],
    responses: unknown[],
    headers?: Record<string, string>
  ): Stub {
    const stub = new Stub()
    predicates.forEach((predicate) => stub.withPredicate(predicate))
    responses.forEach((response) => {
      const resp = new Response().withStatusCode(200)

      if (typeof response === 'string') resp.withBody(response)
      else resp.withJSONBody(response)

      if (headers !== undefined) {
        Object.entries(headers).forEach(([key, value]) => {
          resp.withHeader(key, value)
        })
      }
      return stub.withResponse(resp)
    })
    return stub
  }

  public stubWithErrorResponse(
    pathMatcher: string,
    method: HttpMethod,
    status: number,
    response: unknown
  ) {
    return new Stub()
      .withPredicate(
        new FlexiPredicate()
          .withOperator(Operator.matches)
          .withPath(pathMatcher)
          .withMethod(method)
      )
      .withResponse(
        new Response().withStatusCode(status).withJSONBody(response)
      )
  }
}

export default MountebankStubBuilder
