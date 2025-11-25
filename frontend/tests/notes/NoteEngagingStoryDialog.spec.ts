import AIGenerateImageDialog from "@/components/notes/AIGenerateImageDialog.vue"
import { flushPromises } from "@vue/test-utils"
import { expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import * as sdk from "@generated/backend/sdk.gen"

const createWrapper = async () => {
  const note = makeMe.aNoteRealm.please()
  mockSdkService("generateImage", { b64encoded: "This is an encoded image" })
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
    expect(sdk.generateImage).toBeCalled()
  })
})
