import AIGenerateImageDialog from "@/components/notes/AIGenerateImageDialog.vue"
import { flushPromises } from "@vue/test-utils"
import { expect, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { AiController } from "@generated/backend/sdk.gen"

const createWrapper = async () => {
  const note = makeMe.aNoteRealm.please()
    vi.spyOn(AiController, "generateImage").mockResolvedValue({
    data: { b64encoded: "This is an encoded image" },
    error: undefined,
    request: {} as Request,
    response: {} as Response,
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
    expect(AiController.generateImage).toBeCalled()
  })
})
