import RenderingHelper from "./RenderingHelper"
import matchByText from "./matchByText"
import { vi } from "vitest"
import * as sdk from "@generated/backend/sdk.gen"
import type { NoteRealm } from "@generated/backend"

class StoredComponentTestHelper {
  component<T>(comp: T) {
    return new RenderingHelper(comp)
  }
}

/**
 * Creates a mock SDK response object with the standard structure.
 * This helper ensures type safety and consistency across all test mocks.
 */
export function createMockResponse<T>(data: T) {
  return {
    data,
    error: undefined,
    request: {} as Request,
    response: {} as Response,
  } as const
}

/**
 * Mocks an SDK service method with a resolved value.
 * This is a type-safe helper that simplifies mocking SDK calls in tests.
 *
 * @param methodName - The name of the SDK method to mock (e.g., "showNote", "recalling")
 * @param data - The data to return in the mock response
 * @returns A vitest spy on the mocked method
 *
 * @example
 * ```ts
 * mockSdkMethod("showNote", makeMe.aNoteRealm.please())
 * mockSdkMethod("getRecentNotes", [])
 * ```
 */
export function mockSdkMethod<K extends keyof typeof sdk>(
  methodName: K,
  data: Awaited<ReturnType<(typeof sdk)[K]>>["data"]
) {
  // biome-ignore lint/suspicious/noExplicitAny: vi.spyOn has complex types that require type assertions
  return (vi.spyOn(sdk, methodName as any) as any).mockResolvedValue(
    createMockResponse(data) as Awaited<ReturnType<(typeof sdk)[K]>>
  )
}

/**
 * Mocks an SDK service method with a custom implementation.
 * Use this when you need to control the behavior based on input parameters.
 *
 * @param methodName - The name of the SDK method to mock
 * @param implementation - A function that receives the options and returns the mock response data
 * @returns A vitest spy on the mocked method
 *
 * @example
 * ```ts
 * const mockedCall = vi.fn()
 * mockSdkMethodWithImplementation("recalling", async (options) => {
 *   const result = await mockedCall(options)
 *   return result
 * })
 * ```
 */
export function mockSdkMethodWithImplementation<
  K extends keyof typeof sdk,
  TData = Awaited<ReturnType<(typeof sdk)[K]>>["data"],
>(
  methodName: K,
  implementation: (
    options: Parameters<(typeof sdk)[K]>[0]
  ) => Promise<TData> | TData
) {
  // biome-ignore lint/suspicious/noExplicitAny: vi.spyOn has complex types that require type assertions
  return (vi.spyOn(sdk, methodName as any) as any).mockImplementation(
    async (options: Parameters<(typeof sdk)[K]>[0]) => {
      const result = await implementation(options)
      return createMockResponse(result) as Awaited<ReturnType<(typeof sdk)[K]>>
    }
  )
}

/**
 * Mocks showNoteAccessory service to prevent unhandled promise rejections
 * in tests that use NoteAccessoryAsync component.
 */
export function mockShowNoteAccessory() {
  return mockSdkMethod("showNoteAccessory", undefined as never)
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
  return mockSdkMethod("showNote", defaultNote)
}

export default new StoredComponentTestHelper()
export { matchByText }
