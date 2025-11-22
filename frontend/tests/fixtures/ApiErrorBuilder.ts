// Mock ApiError class for tests - the new client doesn't use ApiError
class MockApiError extends Error {
  url: string
  status: number
  statusText: string
  body: unknown
  request: { url: string; method: string }

  constructor(
    request: { url: string; method: string },
    response: {
      url: string
      status: number
      statusText: string
      body: unknown
    },
    message: string
  ) {
    super(message)
    this.name = "ApiError"
    this.url = response.url
    this.status = response.status
    this.statusText = response.statusText
    this.body = response.body
    this.request = request
  }
}

import Builder from "./Builder"

class ApiErrorBuilder extends Builder<MockApiError> {
  request: { url: string; method: string } = {
    url: "",
    method: "GET",
  }

  response: {
    url: string
    ok: boolean
    status: number
    statusText: string
    body: unknown
  } = {
    url: "",
    ok: false,
    status: 404,
    statusText: "not found",
    body: "not found",
  }

  message = "not found"

  errors: Record<string, unknown> = {}

  of401() {
    this.response = {
      ...this.response,
      status: 401,
    }
    return this
  }

  ofBindingError(errors: Record<string, unknown>) {
    this.errors = errors
    this.response = {
      ...this.response,
      status: 400,
    }
    this.message = "bad request"
    return this
  }

  error404(): ApiErrorBuilder {
    this.response = {
      ...this.response,
      status: 404,
    }
    return this
  }

  do() {
    return {
      ...new MockApiError(this.request, this.response, this.message),
      ...this.errors,
    }
  }
}

export default ApiErrorBuilder
