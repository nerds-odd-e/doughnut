import RenderingHelper from "./RenderingHelper"
import matchByText from "./matchByText"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  NoteController,
  NotebookController,
} from "@generated/doughnut-backend-api/sdk.gen"
import type {
  Circle,
  Folder,
  NoteRealm,
  Notebook,
} from "@generated/doughnut-backend-api"
import { vi } from "vitest"

/** API-shaped `Folder` rows for tests and stories (timestamps required by the type). */
export function testFolderStub(id: number, name: string): Folder {
  return makeMe.aFolder.folder(id, name).please()
}

// biome-ignore lint/suspicious/noExplicitAny: controller statics are heterogeneous; any is required for Parameters/ReturnType plumbing
type ControllerMethod = (...args: any[]) => any

type StaticSdkKeys<C> = {
  [K in keyof C]: C[K] extends ControllerMethod ? K : never
}[keyof C] &
  string

type SdkMethod<C, K extends StaticSdkKeys<C>> = Extract<C[K], ControllerMethod>

type SdkUnpackedData<R> = Awaited<R> extends { data: infer D } ? D : never

type SdkMethodReturn<C, K extends StaticSdkKeys<C>> = ReturnType<
  SdkMethod<C, K>
>

class StoredComponentTestHelper {
  component<T>(comp: T) {
    return new RenderingHelper(comp)
  }
}

/**
 * Mocks showNote service to prevent unhandled promise rejections
 * in tests that use StoredApiCollection.loadNote (via storageAccessor).
 */
export function mockShowNote(noteRealm?: NoteRealm) {
  return mockSdkService(
    NoteController,
    "showNote",
    noteRealm ?? makeMe.aNoteRealm.please()
  )
}

/**
 * Wraps data in the standard SDK response format.
 * Useful for updating mocks that need to return different values.
 */
export function wrapSdkResponse<T>(data: T) {
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
 * Wraps an error in the standard SDK response format.
 * Useful for mocking error responses in tests.
 */
export function wrapSdkError(error: string | Record<string, unknown>) {
  return {
    data: undefined,
    error,
    request: {} as Request,
    response: {} as Response,
    // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
  } as any
}

/**
 * Type-safe helper to mock SDK service calls with resolved data.
 * Automatically wraps the response in the standard format.
 * Returns a spy that can be reconfigured with `.mockResolvedValue(wrapSdkResponse(newData))`.
 *
 * @example
 * ```ts
 * mockSdkService(NoteController, "getRecentNotes", [])
 * const spy = mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
 * spy.mockResolvedValue(wrapSdkResponse(differentNote))
 * ```
 */
export function mockSdkService<
  // biome-ignore lint/suspicious/noExplicitAny: generated controller classes are not `Record<string, unknown>`; `any` bounds the static object for Vitest
  C extends Record<string, any>,
  K extends StaticSdkKeys<C>,
>(controller: C, methodName: K, data: SdkUnpackedData<SdkMethodReturn<C, K>>) {
  return (
    vi
      // biome-ignore lint/suspicious/noExplicitAny: Vitest spyOn key type does not align with StaticSdkKeys<C>
      .spyOn(controller, methodName as any)
      // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
      .mockResolvedValue(wrapSdkResponse(data) as any)
  )
}

/** Mocks `NotebookController.get` for the notebook that owns `realm` (e.g. note show breadcrumb circle). */
export function mockNotebookGetForNoteRealm(realm: NoteRealm, circle?: Circle) {
  const ts =
    realm.note.noteTopology.updatedAt ??
    realm.note.noteTopology.createdAt ??
    new Date().toISOString()
  const notebook: Notebook = {
    id: realm.notebookView.notebook.id,
    name: "Notebook",
    notebookSettings: { skipMemoryTrackingEntirely: false },
    createdAt: realm.note.noteTopology.createdAt ?? ts,
    updatedAt: ts,
    ...(circle ? { circle } : {}),
  }
  return mockSdkService(NotebookController, "get", {
    notebook,
    hasAttachedBook: false,
    readonly: realm.notebookView.readonly ?? false,
  })
}

/**
 * Type-safe helper to mock SDK service calls with a custom implementation.
 * Automatically wraps the result in the standard format (sync or Promise).
 *
 * @example
 * ```ts
 * const mockedCall = vi.fn()
 * mockSdkServiceWithImplementation(TextContentController, "updateNoteContent", async (options) => {
 *   return await mockedCall(options)
 * })
 * ```
 */
export function mockSdkServiceWithImplementation<
  // biome-ignore lint/suspicious/noExplicitAny: generated controller classes are not `Record<string, unknown>`; `any` bounds the static object for Vitest
  C extends Record<string, any>,
  K extends StaticSdkKeys<C>,
>(
  controller: C,
  methodName: K,
  implementation: (
    options: Parameters<SdkMethod<C, K>>[0]
  ) =>
    | Promise<SdkUnpackedData<SdkMethodReturn<C, K>>>
    | SdkUnpackedData<SdkMethodReturn<C, K>>
) {
  // biome-ignore lint/suspicious/noExplicitAny: Vitest spy types and spyOn key do not align with StaticSdkKeys<C>
  const spy = vi.spyOn(controller, methodName as any) as any
  spy.mockImplementation((options: Parameters<SdkMethod<C, K>>[0]) => {
    const out = implementation(options)
    if (out instanceof Promise) {
      return out.then((result) => wrapSdkResponse(result)) as SdkMethodReturn<
        C,
        K
      >
    }
    return wrapSdkResponse(out) as SdkMethodReturn<C, K>
  })
  return spy
}

export default new StoredComponentTestHelper()
export { matchByText }
