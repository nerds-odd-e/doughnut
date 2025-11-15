import AIGenerateImageDialog from "@/components/notes/AIGenerateImageDialog.vue"
import { flushPromises } from "@vue/test-utils"
import { expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

const createWrapper = async () => {
  const note = makeMe.aNoteRealm.please()
  vi.spyOn(
    helper.managedApi.services,
    "generateImage"
  ).mockResolvedValue({
    b64encoded: "This is an encoded image",
  } as never)
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
    expect(helper.managedApi.services.generateImage).toBeCalled()
  })
})
