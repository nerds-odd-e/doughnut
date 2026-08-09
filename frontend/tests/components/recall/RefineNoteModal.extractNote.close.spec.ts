import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import RefineNoteModal from "@/components/recall/RefineNoteModal.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { defineComponent, ref } from "vue"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  note,
  refinementLayoutItems,
  sampleExtractionPreview,
  threePointLayoutTexts,
} from "./noteRefinementTestSupport"

const routerReplace = vi.fn()

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      replace: routerReplace,
    }),
  }
})

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

function refineNoteModalEl() {
  return document.querySelector('[data-test="refine-note-modal"]')
}

async function selectLayoutItemInModal(itemId: string) {
  const checkbox = document.querySelector(
    `[data-test-id="refinement-layout-checkbox-${itemId}"]`
  ) as HTMLInputElement
  checkbox.checked = true
  checkbox.dispatchEvent(new Event("input", { bubbles: true }))
  checkbox.dispatchEvent(new Event("change", { bubbles: true }))
  await flushPromises()
}

async function clickExtractInModal() {
  ;(
    document.querySelector(
      '[data-test-id="extract-refinement-layout"]'
    ) as HTMLButtonElement
  ).click()
  await flushPromises()
}

async function clickCreateNoteInModal() {
  ;(
    document.querySelector(
      '[data-test-id="extraction-preview-create"]'
    ) as HTMLButtonElement
  ).click()
  await flushPromises()
}

async function mountOpenRefineNoteModal() {
  mockSdkService(AiController, "generateRefinementSuggestions", {
    items: refinementLayoutItems([...threePointLayoutTexts]),
  })
  mockSdkService(AiController, "extractNotePreview", sampleExtractionPreview())
  mockSdkService(
    AiController,
    "createExtractedNote",
    makeMe.aNoteRealm.please()
  )

  const wrapper = helper
    .component(RefineNoteModalHarness)
    .withRouter()
    .withCleanStorage()
    .mount({ attachTo: document.body })
  await flushPromises()
  return wrapper
}

describe("RefineNoteModal extract note close", () => {
  beforeEach(() => {
    routerReplace.mockResolvedValue(undefined)
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.clearAllMocks()
    teardownGlobalClientForTesting()
  })

  it("closes the refine note modal after creating a note from extraction preview", async () => {
    await mountOpenRefineNoteModal()

    expect(refineNoteModalEl()?.classList.contains("daisy-modal-open")).toBe(
      true
    )

    await selectLayoutItemInModal("p2")
    await clickExtractInModal()
    await clickCreateNoteInModal()

    expect(refineNoteModalEl()?.classList.contains("daisy-modal-open")).toBe(
      false
    )
  })
})
