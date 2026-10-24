import { focusDirective } from "@/utils/focusTarget"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { mountSoftKeyboardPrimer } from "@tests/helpers/softKeyboardPrimerTestSupport"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, expect, it, vi } from "vitest"
import { fireEvent } from "@testing-library/vue"
import { ref } from "vue"
import {
  createMemoryTrackerLite,
  givenRecallQueue,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

const ctx = useRecallPageSpecContext()

describe("Recall spelling resume focus handoff", () => {
  it("hands focus to the answer input in the resume update", async () => {
    mountSoftKeyboardPrimer()
    ctx.renderer
      .withCurrentUserRef(ref(makeMe.aUser.please()))
      .withDirective("focus", focusDirective)
    const realm = makeMe.aNoteRealm.please()
    mockSdkService(NoteController, "showNote", realm)
    ctx.previouslyAnsweredSpy.mockResolvedValueOnce(
      wrapSdkResponse([
        makeMe.anAnsweredQuestion
          .withId(1)
          .withNote(realm.note)
          .spelling()
          .withAnswer({ id: 1, correct: true, spellingAnswer: "done" })
          .please(),
      ])
    )
    givenRecallQueue(createMemoryTrackerLite(123, true))
    const wrapper = await ctx.mountPage({ attachTo: document.body })
    await settleScheduledAutofocus()
    await fireEvent.click(
      wrapper.find('button[title="view last answered question"]').element
    )
    await flushPromises()
    const input = wrapper.find("input#memory_tracker-answer").element
    vi.spyOn(window, "requestAnimationFrame").mockReturnValue(1)

    await fireEvent.click(wrapper.find('[aria-label="Resume"]').element)
    await flushPromises()

    expect(document.activeElement).toBe(input)
  })
})
