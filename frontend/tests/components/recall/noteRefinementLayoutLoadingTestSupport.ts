import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import type { NoteRefinementLayoutItem } from "@generated/doughnut-backend-api"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import { screen } from "@testing-library/vue"
import { nextTick } from "vue"
import {
  mountNoteRefinement,
  note,
  refinementLayoutItems,
  renderer,
} from "./noteRefinementTestSupport"

export function createDeferredGate() {
  let resolveGate!: () => void
  const gate = new Promise<void>((resolve) => {
    resolveGate = resolve
  })
  return { gate, resolve: () => resolveGate() }
}

export const loadingModalMask = () =>
  document.querySelector(".loading-modal-mask")

/**
 * Mounts NoteRefinement with generateRefinementSuggestions held pending.
 * Does not flushPromises — caller asserts Cancel while the layout request is open.
 */
export async function mountNoteRefinementPendingLayout(
  itemsWhenResolved: NoteRefinementLayoutItem[] = refinementLayoutItems([
    "Should not appear",
  ]),
  overrides?: { note?: typeof note }
) {
  const { gate, resolve } = createDeferredGate()
  const generateSpy = mockSdkServiceWithImplementation(
    AiController,
    "generateRefinementSuggestions",
    async () => {
      await gate
      return { items: itemsWhenResolved }
    }
  )
  const wrapper = renderer
    .withCleanStorage()
    .withProps({
      note: overrides?.note ?? note,
    })
    .mount()
  await nextTick()
  return { wrapper, resolve, gate, generateSpy }
}

export function clickLoadingModalCancel() {
  screen.getByText("Cancel").click()
}

export async function clickRetryRefinementLayout(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper
    .find('[data-test-id="retry-refinement-layout"]')
    .trigger("click")
}
