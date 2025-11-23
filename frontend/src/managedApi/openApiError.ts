/**
 * Type for OpenAPI error responses.
 * The error field in API responses is typed as string, but runtime it can be an object (parsed JSON).
 * This type represents the structure of error objects returned by the backend.
 */
export type OpenApiError = {
  errors?: Record<string, string>
  message?: string
}

/**
 * Converts an error value (typed as string but can be an object at runtime) to OpenApiError.
 * The error parameter is the parsed response body from the API, which can be:
 * - A string (if JSON parsing failed or response is plain text)
 * - An object (if JSON parsing succeeded, e.g., { message: "...", errors: {...} } for 400 errors)
 *
 * @param error - The error value from the API response (string | object)
 * @returns OpenApiError object with errors and message fields
 */
export function toOpenApiError(error: unknown): OpenApiError {
  if (typeof error === "string") {
    return { message: error }
  }

  if (typeof error === "object" && error !== null) {
    const errorObj = error as Record<string, unknown>
    return {
      message:
        typeof errorObj.message === "string" ? errorObj.message : undefined,
      errors:
        typeof errorObj.errors === "object" &&
        errorObj.errors !== null &&
        !Array.isArray(errorObj.errors)
          ? (errorObj.errors as Record<string, string>)
          : undefined,
    }
  }

  return {}
}
