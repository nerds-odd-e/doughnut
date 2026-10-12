import {
  AiController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import RefineNoteModal from "@/components/recall/RefineNoteModal.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { flushPromises, enableAutoUnmount } from "@vue/test-utils"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  productionRouterAt,
} from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import makeMe from "donut-test-fixtures/makeMe"
import { screen } from "@testing-library/vue"
import { defineComponent, ref } from "vue"
import { afterEach, describe, expect, it, vi } from "vitest"
import { createDeferredGate } from "./noteRefinementLayoutLoadingTestSupport"
import {
  refinementLayoutItems,
  threePointLayoutTexts,
  sampleExtractionPreview,
} from "./noteRefinementLayoutFixtures"

const note = makeMe.aNote.content("Original knowledge").please()
const layout = { items: refinementLayoutItems([...threePointLayoutTexts]) }
const RefineNoteModalHarness = defineComponent({
  components: { RefineNoteModal, GlobalApiLoadingModal },
  setup() {
    const open = ref(true)
    return { open, note }
  },
  template: `
    <RefineNoteModal v-model:open="open" :note="note" />
    <GlobalApiLoadingModal />
  `,
})

function refinementDialog() {
  return document
    .querySelector('[data-test="refine-note-modal"]')!
    .closest("dialog")!
}

async function mountRefinement() {
  const router = await productionRouterAt(noteShowLocation(note.id))
  const wrapper = helper
    .component(RefineNoteModalHarness)
    .withRouter(router)
    .withCleanStorage()
    .mount({ attachTo: document.body })
  await flushPromises()
  return { wrapper, router }
}

function control(testId: string) {
  return document.querySelector<HTMLElement>(`[data-test-id="${testId}"]`)!
}

describe("RefineNoteModal extraction and loading", () => {
  afterEach(() => {
    document.body.innerHTML = ""
    vi.clearAllMocks()
    teardownGlobalClientForTesting()
  })
  enableAutoUnmount(afterEach)

  it("creates the extracted note, opens it, and closes the refinement dialog", async () => {
    const created = makeMe.aNoteRealm.id(975).please()
    const preview = sampleExtractionPreview()
    mockSdkService(AiController, "generateRefinementSuggestions", layout)
    mockSdkService(AiController, "extractNotePreview", preview)
    const create = mockSdkService(AiController, "createExtractedNote", created)
    mockSdkService(NoteController, "showNote", created)
    const { router } = await mountRefinement()
    const dialog = refinementDialog()
    expect(dialog.open).toBe(true)

    control("refinement-layout-checkbox-p2").click()
    await flushPromises()
    control("extract-refinement-layout").click()
    await flushPromises()
    control("extraction-preview-create").click()
    await flushPromises()

    expect(create).toHaveBeenCalledWith({
      path: { note: note.id },
      body: preview,
    })
    expect(router.currentRoute.value).toMatchObject(
      noteShowLocation(created.note.id)
    )
    expect(dialog.isConnected).toBe(false)
  })

  it("keeps refinement available for retry after cancelling layout generation", async () => {
    const { gate, resolve } = createDeferredGate()
    mockSdkServiceWithImplementation(
      AiController,
      "generateRefinementSuggestions",
      async () => {
        await gate
        return layout
      }
    )
    await mountRefinement()
    const dialog = refinementDialog()
    expect(dialog.open).toBe(true)
    expect(
      document.querySelector<HTMLDialogElement>(".loading-modal-mask")?.open
    ).toBe(true)

    screen.getByText("Cancel").click()
    await flushPromises()

    expect(dialog.open).toBe(true)
    expect(control("retry-refinement-layout").textContent).toContain(
      "Ask AI to retry"
    )
    resolve()
    await flushPromises()
  })
})
