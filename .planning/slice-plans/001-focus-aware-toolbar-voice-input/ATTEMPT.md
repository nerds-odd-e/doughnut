# Toolbar targeting attempt

Original slice 2 stopped at approximately eight active minutes before its hard
ten-minute limit, after bounded reads and a red spec; Nix startup/test wait was
separate. No production/E2E edits. Three red cases proved current toolbar routing
still targets content. The failed sizing assumption combined page ownership,
pre-start capture, linked-draft focus lifetime, recovery/locking proof, E2E
action adaptation and persisted reload into one leaf. The attempt-owned spec
is parked here; only that incomplete file was removed from executable tests.

Command: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteShow.toolbarTitleVoiceInput.spec.ts`
Result: 3 failed, exit 1; 16.47 seconds test-run duration. Caret case failed on
title read-only; partial/whole selections failed on unchanged title.

```typescript
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import NoteShow from "@/components/notes/NoteShow.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { noteTitleText } from "@tests/notes/noteNewFormTestSupport"
import { placeCaretInTitle, selectWholeTitle, hearing, titleEditable, useSpokenTitleTestLifecycle } from "@tests/notes/spokenTitleTestSupport"
import { mockUpdateNoteTitle } from "@tests/notes/noteTextContentTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import("@tests/notes/noteVoiceInputButtonMocks")
  return recorderWorkletMockExports()
})
vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderInvokingCallbackMockExports } = await import("@tests/notes/noteVoiceInputButtonMocks")
  return audioRecorderInvokingCallbackMockExports()
})
vi.mock("@/models/wakeLocker", async () => {
  const { wakeLockerMockExports } = await import("@tests/notes/noteVoiceInputButtonMocks")
  return wakeLockerMockExports()
})
useSpokenTitleTestLifecycle("harvest")

describe("Toolbar voice input with a focused title", () => {
  let wrapper: VueWrapper
  beforeEach(async () => {
    const realm = makeMe.aNoteRealm.title("Orchard notes").please()
    mockSdkService(NoteController, "showNote", realm)
    mockUpdateNoteTitle()
    wrapper = helper.component(NoteShow).withRouter().withCleanStorage().withCurrentUser(makeMe.aUser.please()).withProps({ noteId: realm.id }).mount({ attachTo: document.body })
    await flushPromises()
  })
  afterEach(() => wrapper.unmount())

  async function start() {
    const button = wrapper.find('button[aria-label="Voice input"]')
    await button.trigger("pointerdown")
    await button.trigger("click")
    await flushPromises()
  }
  async function stop() {
    await wrapper.find('button[aria-label="Stop voice input"]').trigger("click")
    await flushPromises()
  }

  it("inserts at the title caret and restores editing after Stop", async () => {
    placeCaretInTitle(wrapper, 7)
    await start()
    expect(titleEditable(wrapper)).toBe("false")
    await stop()
    expect(noteTitleText(wrapper)).toBe("Orchard harvest notes")
    expect(titleEditable(wrapper)).toBe("true")
  })

  it("replaces only the selected title text", async () => {
    placeCaretInTitle(wrapper, 0, 7)
    hearing("Garden")
    await start()
    await stop()
    expect(noteTitleText(wrapper)).toBe("Garden notes")
  })

  it("replaces the whole selected title", async () => {
    selectWholeTitle(wrapper)
    hearing("Pear orchard")
    await start()
    await stop()
    expect(noteTitleText(wrapper)).toBe("Pear orchard")
  })
})
```
