import { AiController } from "@generated/donut-backend-api/sdk.gen"
import RefineNoteModal from "@/components/recall/RefineNoteModal.vue"
import NoteRefinement from "@/components/recall/NoteRefinement.vue"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { defineComponent, ref } from "vue"
import { afterEach, describe, expect, it, vi } from "vitest"
import {
  note,
  refinementLayoutItems,
  threePointLayoutTexts,
} from "./noteRefinementTestSupport"

const RefineNoteModalHarness = defineComponent({
  components: { RefineNoteModal },
  setup() {
    const open = ref(true)
    return { open, note }
  },
  template: `
    <RefineNoteModal v-model:open="open" :note="note" />
  `,
})

function refineNoteModalEl() {
  return document.querySelector('[data-test="refine-note-modal"]')
}

async function mountOpenRefineNoteModal() {
  mockSdkService(AiController, "generateRefinementSuggestions", {
    items: refinementLayoutItems([...threePointLayoutTexts]),
  })

  const wrapper = helper
    .component(RefineNoteModalHarness)
    .withRouter()
    .withCleanStorage()
    .mount({ attachTo: document.body })
  await flushPromises()
  return wrapper
}

describe("RefineNoteModal extract note close", () => {
  afterEach(() => {
    document.body.innerHTML = ""
    vi.clearAllMocks()
    teardownGlobalClientForTesting()
  })

  it("closes the refine note modal when note refinement completes extraction", async () => {
    const wrapper = await mountOpenRefineNoteModal()

    expect(refineNoteModalEl()?.classList.contains("daisy-modal-open")).toBe(
      true
    )

    wrapper.findComponent(NoteRefinement).vm.$emit("extracted")
    await flushPromises()

    expect(refineNoteModalEl()?.classList.contains("daisy-modal-open")).toBe(
      false
    )
  })
})
