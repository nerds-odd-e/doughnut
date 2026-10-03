import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import { useNoteStore } from "@/store/noteStore"
import { createRouter, createMemoryHistory } from "vue-router"
import { dummyRouteRecordsFromMetadata } from "@/routes/dummyRouteRecords"
import {
  dictatedTextResponse,
  mountNoteAudioTools,
  processAudio,
  useNoteAudioToolsTestLifecycle,
  type NoteAudioToolsWrapper,
} from "@tests/notes/noteAudioToolsTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return recorderWorkletMockExports()
})

vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return audioRecorderMockExports()
})

vi.mock("@/models/wakeLocker", async () => {
  const { wakeLockerMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return wakeLockerMockExports()
})

useNoteAudioToolsTestLifecycle()

describe("NoteAudioTools content preservation", () => {
  let wrapper: NoteAudioToolsWrapper
  let audioToTextMock: ReturnType<typeof mockSdkService>
  const longBody = [
    "Original paragraph one: The museum opens at nine each morning. Our tickets are booked for Tuesday.",
    "Original paragraph two: The library holds maps of the northern coast. We marked each of the harbours and their walking routes on our paper copy.",
    "Original paragraph three: The train leaves at noon from platform four. We will carry lunch in the blue bag and meet the guide beside the station clock.",
    "Original paragraph four: The garden paths wind through old cedar trees. The gardener has reserved the western gate for our afternoon visit.",
    "Original paragraph five: The evening ferry returns across the bay at six. Our hotel is on the hill above the harbour, near the bakery and the post office.",
  ].join("\n\n")
  const originalRealm = makeMe.aNoteRealm.content(longBody).please()
  const note = originalRealm.note
  const noteStore = useNoteStore()
  let updateContentMock: ReturnType<typeof mockSdkService>
  beforeEach(() => {
    audioToTextMock = mockSdkService(
      AiAudioController,
      "audioToText",
      dictatedTextResponse("text")
    )
    wrapper = mountNoteAudioTools(note)
    noteStore.refreshNoteRealm(originalRealm)
    updateContentMock = mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      (options) =>
        makeMe.aNoteRealm
          .id(options.path.note)
          .content(options.body.content ?? "")
          .please()
    )
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("appends successive results once and sends the current body as context", async () => {
    audioToTextMock
      .mockResolvedValueOnce(
        wrapSdkResponse(
          dictatedTextResponse(" The lighthouse beam sweeps across the bay.")
        )
      )
      .mockResolvedValueOnce(
        wrapSdkResponse(
          dictatedTextResponse(" The ferry arrives after sunset.")
        )
      )

    await processAudio(wrapper)
    await processAudio(wrapper)

    expect(audioToTextMock).toHaveBeenLastCalledWith({
      body: expect.objectContaining({
        previousNoteContentToAppendTo: `...${`${note.content} The lighthouse beam sweeps across the bay.`.slice(-500)}`,
      }),
    })
    expect(updateContentMock).toHaveBeenNthCalledWith(2, {
      path: { note: note.id },
      body: {
        content: `${note.content} The lighthouse beam sweeps across the bay. The ferry arrives after sunset.`,
      },
    })
    expect(updateContentMock).toHaveBeenCalledTimes(2)

    await noteStore.undo(
      createRouter({
        history: createMemoryHistory(),
        routes: dummyRouteRecordsFromMetadata,
      })
    )
    expect(updateContentMock).toHaveBeenLastCalledWith({
      path: { note: note.id },
      body: { content: note.content },
    })
  })

  it.each([
    { when: "longer than the context excerpt", body: longBody },
    { when: "empty", body: "" },
  ])("preserves the exact original body when $when", async ({ body }) => {
    noteStore.refreshNoteRealm(
      makeMe.aNoteRealm.id(note.id).content(body).please()
    )
    await processAudio(wrapper)
    expect(updateContentMock).toHaveBeenCalledExactlyOnceWith({
      path: { note: note.id },
      body: { content: `${body}text` },
    })
  })

  it("targets the originating note's current body after the prop changes", async () => {
    const current = "Current stored body."
    noteStore.refreshNoteRealm(
      makeMe.aNoteRealm.id(note.id).content(current).please()
    )
    const destination = makeMe.aNoteRealm
      .content(
        "DESTINATION ORIGINAL: The violet umbrella stays on shelf seven."
      )
      .please()
    noteStore.refreshNoteRealm(destination)
    await wrapper.setProps({ note: destination.note })
    await processAudio(wrapper)
    expect(audioToTextMock).toHaveBeenCalledWith({
      body: expect.objectContaining({ previousNoteContentToAppendTo: current }),
    })
    expect(updateContentMock).toHaveBeenCalledExactlyOnceWith({
      path: { note: note.id },
      body: { content: `${current}text` },
    })
    expect(noteStore.refOfNoteRealm(destination.id).value?.note.content).toBe(
      destination.note.content
    )
  })

  it("keeps appended content after an API error", async () => {
    audioToTextMock
      .mockResolvedValueOnce(
        wrapSdkResponse(
          dictatedTextResponse(" The lighthouse beam sweeps across the bay.")
        )
      )
      .mockResolvedValueOnce(wrapSdkError("API Error"))
      .mockResolvedValueOnce(
        wrapSdkResponse(
          dictatedTextResponse(" The ferry arrives after sunset.")
        )
      )
    await processAudio(wrapper)
    await processAudio(wrapper)
    await processAudio(wrapper)
    expect(audioToTextMock).toHaveBeenLastCalledWith({
      body: expect.objectContaining({
        previousNoteContentToAppendTo: `...${`${note.content} The lighthouse beam sweeps across the bay.`.slice(-500)}`,
      }),
    })
  })
})
