import { ApiError } from "@generated/backend"
import type { ApiRequestOptions } from "@generated/backend/core/ApiRequestOptions"
import type { ApiResult } from "@generated/backend/core/ApiResult"
import Builder from "./Builder"

class ApiErrorBuilder extends Builder<ApiError> {
  request: ApiRequestOptions = {
    url: "",
    method: "GET",
  }

  response: ApiResult = {
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
      ...new ApiError(this.request, this.response, this.message),
      ...this.errors,
    }
  }
}

export default ApiErrorBuilder
