import ManagedApi from "@/managedApi/ManagedApi"
import RenderingHelper from "./RenderingHelper"
import matchByText from "./matchByText"
import { vi } from "vitest"
import * as sdk from "@generated/backend/sdk.gen"

class StoredComponentTestHelper {
  public managedApi = new ManagedApi({ states: [], errors: [] })

  component<T>(comp: T) {
    return new RenderingHelper(comp, this.managedApi)
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

export default new StoredComponentTestHelper()
export { matchByText }
