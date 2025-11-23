import assignBadRequestProperties from "./window/assignBadRequestProperties"

/**
 * Type for OpenAPI error responses.
 * The error field in API responses is typed as string, but runtime it can be an object (parsed JSON).
 * This type represents the structure of error objects returned by the backend.
 */
export type OpenApiError = {
  errors?: Record<string, string>
  message?: string
  status?: number
}

// Module-level variable to store the error object for the current call
// This allows toOpenApiError to attach field errors without changing its signature
let currentErrorObject: (Error & { [key: string]: unknown }) | undefined

/**
 * Converts an error value (typed as string but can be an object at runtime) to OpenApiError.
 * The error parameter is the parsed response body from the API, which can be:
 * - A string (if JSON parsing failed or response is plain text)
 * - An object (if JSON parsing succeeded, e.g., { message: "...", errors: {...} } for 400 errors)
 *
 * If field-level errors exist and an error object has been set via setErrorObjectForFieldErrors,
 * this function will call assignBadRequestProperties to attach those errors as properties.
 *
 * @param error - The error value from the API response (string | object)
 * @returns OpenApiError object with errors and message fields
 */
export function toOpenApiError(error: unknown): OpenApiError {
  let errorObj: OpenApiError

  if (typeof error === "string") {
    errorObj = { message: error }
  } else if (typeof error === "object" && error !== null) {
    const parsedError = error as Record<string, unknown>
    errorObj = {
      message:
        typeof parsedError.message === "string"
          ? parsedError.message
          : undefined,
      status:
        typeof parsedError.status === "number" ? parsedError.status : undefined,
      errors:
        typeof parsedError.errors === "object" &&
        parsedError.errors !== null &&
        !Array.isArray(parsedError.errors)
          ? (parsedError.errors as Record<string, string>)
          : undefined,
    }
  } else {
    errorObj = {}
  }

  // If field-level errors exist and an error object has been set, attach them
  if (errorObj.errors && currentErrorObject) {
    assignBadRequestProperties(currentErrorObject, { errors: errorObj.errors })
    currentErrorObject = undefined // Clear after use
  }

  return errorObj
}

/**
 * Sets the error object that should receive field-level error properties.
 * This should be called before toOpenApiError if you want field errors attached.
 *
 * @param errorObject - The Error object to attach field-level properties to
 */
export function setErrorObjectForFieldErrors(
  errorObject: Error & { [key: string]: unknown }
): void {
  currentErrorObject = errorObject
}
