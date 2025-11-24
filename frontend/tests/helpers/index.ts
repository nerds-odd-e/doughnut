import RenderingHelper from "./RenderingHelper"
import matchByText from "./matchByText"
import { vi } from "vitest"
import { NoteController } from "@generated/backend/sdk.gen"
import type { NoteRealm } from "@generated/backend"

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
  return vi.spyOn(NoteController, "showNoteAccessory").mockResolvedValue({
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
  return vi.spyOn(NoteController, "showNote").mockResolvedValue({
    data: defaultNote,
    error: undefined,
    request: {} as Request,
    response: {} as Response,
  })
}

export default new StoredComponentTestHelper()
export { matchByText }
