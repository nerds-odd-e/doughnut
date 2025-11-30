import AIGenerateImageDialog from "@/components/notes/AIGenerateImageDialog.vue"
import { flushPromises } from "@vue/test-utils"
import { expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

const createWrapper = async () => {
  const note = makeMe.aNoteRealm.please()
  const generateImageSpy = mockSdkService("generateImage", {
    b64encoded: "This is an encoded image",
  })
  const wrapper = helper
    .component(AIGenerateImageDialog)
    .withCleanStorage()
    .withProps({ note: note.note })
    .mount()
  await flushPromises()
  return { wrapper, generateImageSpy }
}

describe("AIGeneratedImageDialog", () => {
  it("fetches generated image", async () => {
    const { wrapper, generateImageSpy } = await createWrapper()
    expect(wrapper.find("img.ai-art").element).toBeDefined()
    expect(generateImageSpy).toBeCalled()
  })
})
