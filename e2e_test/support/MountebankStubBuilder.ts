import {
  Predicate,
  Response,
  Stub,
  HttpMethod,
  FlexiPredicate,
  Operator,
} from "@anev/ts-mountebank"
/// <reference types="cypress" />

// @ts-check

class MountebankStubBuilder {
  public stubWithPredicates(predicates: Predicate[], responses: unknown[]): Stub {
    const stub = new Stub()
    predicates.forEach((predicate) => stub.withPredicate(predicate))
    responses.forEach((response) =>
      stub.withResponse(new Response().withStatusCode(200).withJSONBody(response)),
    )
    return stub
  }

  public stubWithErrorResponse(
    pathMatcher: string,
    method: HttpMethod,
    status: number,
    response: unknown,
  ) {
    return new Stub()
      .withPredicate(
        new FlexiPredicate()
          .withOperator(Operator.matches)
          .withPath(pathMatcher)
          .withMethod(method),
      )
      .withResponse(new Response().withStatusCode(status).withJSONBody(response))
  }
}

export default MountebankStubBuilder
