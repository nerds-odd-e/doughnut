import RenderingHelper from "./RenderingHelper"
import matchByText from "./matchByText"
import { vi } from "vitest"
import * as sdk from "@generated/backend/sdk.gen"
import type { NoteRealm } from "@generated/backend"

type SdkServiceName = keyof typeof sdk
type SdkService<K extends SdkServiceName> = (typeof sdk)[K]
type SdkServiceReturnType<K extends SdkServiceName> = ReturnType<SdkService<K>>
type SdkServiceData<K extends SdkServiceName> = Awaited<
  SdkServiceReturnType<K>
> extends {
  data: infer D
}
  ? D
  : never
type SdkServiceOptions<K extends SdkServiceName> = Parameters<SdkService<K>>[0]

class StoredComponentTestHelper {
  component<T>(comp: T) {
    return new RenderingHelper(comp)
  }
}

/**
 * Mocks showNoteAccessory service to prevent unhandled promise rejections
 * in tests that use NoteAccessoryAsync component.
 */
export function mockShowNoteAccessory() {
  return vi.spyOn(sdk, "showNoteAccessory").mockResolvedValue({
    data: undefined as never,
    error: undefined as never,
    request: {} as Request,
    response: {} as Response,
  })
}

/**
 * Mocks showNote service to prevent unhandled promise rejections
 * in tests that use StoredApiCollection.loadNote (via storageAccessor).
 */
export function mockShowNote(noteRealm?: NoteRealm) {
  const defaultNote =
    noteRealm ||
    ({
      id: 1,
      note: { id: 1 },
      children: [],
    } as unknown as NoteRealm)
  return vi.spyOn(sdk, "showNote").mockResolvedValue({
    data: defaultNote,
    error: undefined,
    request: {} as Request,
    response: {} as Response,
  })
}

/**
 * Wraps data in the standard SDK response format.
 * Useful for updating mocks that need to return different values.
 */
function wrapSdkResponse<T>(data: T) {
  return {
    data,
    error: undefined,
    request: {} as Request,
    response: {} as Response,
  } as {
    data: T
    error: undefined
    request: Request
    response: Response
  }
}

/**
 * Type-safe helper to mock SDK service calls with resolved data.
 * Automatically wraps the response in the standard format.
 * Returns a spy that can be reconfigured with `.mockResolvedValue(wrapSdkResponse(newData))`.
 *
 * @param serviceName - The name of the SDK service to mock (type-safe)
 * @param data - The data value to return (type-safe based on service)
 * @returns A Vitest Mock that can be further configured
 *
 * @example
 * ```ts
 * // Simple usage
 * mockSdkService("getRecentNotes", [])
 *
 * // Reconfiguring the mock in tests
 * const spy = mockSdkService("showNote", makeMe.aNoteRealm.please())
 * spy.mockResolvedValue(wrapSdkResponse(differentNote))
 * ```
 */
export function mockSdkService<K extends SdkServiceName>(
  serviceName: K,
  data: SdkServiceData<K>
) {
  return (
    vi
      .spyOn(sdk, serviceName)
      // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
      .mockResolvedValue(wrapSdkResponse(data) as any)
  )
}

/**
 * Type-safe helper to mock SDK service calls with a custom async implementation.
 * Automatically wraps the result in the standard format.
 *
 * @param serviceName - The name of the SDK service to mock (type-safe)
 * @param implementation - An async function that receives options and returns data
 * @returns A Vitest Mock that can be further configured
 *
 * @example
 * ```ts
 * const mockedCall = vi.fn()
 * mockSdkServiceWithImplementation("updateNoteDetails", async (options) => {
 *   const result = await mockedCall(options)
 *   return result
 * })
 * ```
 */
export function mockSdkServiceWithImplementation<K extends SdkServiceName>(
  serviceName: K,
  implementation: (
    options: SdkServiceOptions<K>
  ) => Promise<SdkServiceData<K>> | SdkServiceData<K>
) {
  // biome-ignore lint/suspicious/noExplicitAny: Vitest spy types are complex and require any for proper typing
  const spy = vi.spyOn(sdk, serviceName) as any
  spy.mockImplementation(async (options: SdkServiceOptions<K>) => {
    const result = await implementation(options)
    return {
      data: result,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
      // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
    } as any
  })
  return spy
}

export default new StoredComponentTestHelper()
export { matchByText, wrapSdkResponse }
