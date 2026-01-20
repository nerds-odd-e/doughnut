import AIGenerateImageDialog from "@/components/notes/AIGenerateImageDialog.vue"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { expect, describe, it, afterEach } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

describe("AIGeneratedImageDialog", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const createWrapper = async () => {
    const note = makeMe.aNoteRealm.please()
    const generateImageSpy = mockSdkService("generateImage", {
      b64encoded: "This is an encoded image",
    })
    wrapper = helper
      .component(AIGenerateImageDialog)
      .withCleanStorage()
      .withProps({ note: note.note })
      .mount({ attachTo: document.body })
    await flushPromises()
    return { wrapper, generateImageSpy }
  }

  it("fetches generated image", async () => {
    const { generateImageSpy } = await createWrapper()
    expect(wrapper.find("img.ai-art").element).toBeDefined()
    expect(generateImageSpy).toBeCalled()
  })
})
