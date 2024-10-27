import AIGenerateImageDialog from "@/components/notes/AIGenerateImageDialog.vue"
import { flushPromises } from "@vue/test-utils"
import { expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

const createWrapper = async () => {
  const note = makeMe.aNoteRealm.please()
  helper.managedApi.restAiController.generateImage = vi.fn().mockResolvedValue({
    b64encoded: "This is an encoded image",
  })
  const wrapper = helper
    .component(AIGenerateImageDialog)
    .withStorageProps({ note: note.note })
    .mount()
  await flushPromises()
  return wrapper
}

describe("AIGeneratedImageDialog", () => {
  it("fetches generated image", async () => {
    const wrapper = await createWrapper()
    expect(wrapper.find("img.ai-art").element).toBeDefined()
    expect(helper.managedApi.restAiController.generateImage).toBeCalled()
  })
})
