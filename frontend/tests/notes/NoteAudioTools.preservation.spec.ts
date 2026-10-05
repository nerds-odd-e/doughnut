import { wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { createRouter, createMemoryHistory } from "vue-router"
import { dummyRouteRecordsFromMetadata } from "@/routes/dummyRouteRecords"
import {
  audioChunk,
  audioTextResponse,
  midSpeechChunk,
  processAudio,
  useNoteAudioToolsTestLifecycle,
} from "@tests/notes/noteAudioToolsTestSupport"
import { useSavedBodyDictation } from "@tests/notes/noteAudioToolsSavedContentTestSupport"
import { describe, expect, it, vi } from "vitest"

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
  const longBody = [
    "Original paragraph one: The museum opens at nine each morning. Our tickets are booked for Tuesday.",
    "Original paragraph two: The library holds maps of the northern coast. We marked each of the harbours and their walking routes on our paper copy.",
    "Original paragraph three: The train leaves at noon from platform four. We will carry lunch in the blue bag and meet the guide beside the station clock.",
    "Original paragraph four: The garden paths wind through old cedar trees. The gardener has reserved the western gate for our afternoon visit.",
    "Original paragraph five: The evening ferry returns across the bay at six. Our hotel is on the hill above the harbour, near the bakery and the post office.",
  ].join("\n\n")
  const dictation = useSavedBodyDictation(longBody)
  const { note, noteStore } = dictation

  it("appends successive results once", async () => {
    dictation.audioToTextMock
      .mockResolvedValueOnce(
        wrapSdkResponse(
          audioTextResponse("The lighthouse beam sweeps across the bay.")
        )
      )
      .mockResolvedValueOnce(
        wrapSdkResponse(audioTextResponse("The ferry arrives after sunset."))
      )

    await processAudio(dictation.wrapper)
    await processAudio(dictation.wrapper)

    expect(dictation.updateContentMock).toHaveBeenNthCalledWith(2, {
      path: { note: note.id },
      body: {
        content: `${note.content} The lighthouse beam sweeps across the bay. The ferry arrives after sunset.`,
      },
    })
    expect(dictation.updateContentMock).toHaveBeenCalledTimes(2)

    await noteStore.undo(
      createRouter({
        history: createMemoryHistory(),
        routes: dummyRouteRecordsFromMetadata,
      })
    )
    expect(dictation.updateContentMock).toHaveBeenLastCalledWith({
      path: { note: note.id },
      body: { content: note.content },
    })
  })

  it("joins every segment of a response to the body and saves once", async () => {
    dictation.audioToTextMock.mockResolvedValueOnce(
      wrapSdkResponse(
        audioTextResponse([
          "The lighthouse beam sweeps across the bay.",
          "The ferry arrives after sunset.",
          "The harbour lights stay on.",
        ])
      )
    )

    await processAudio(dictation.wrapper)

    expect(dictation.updateContentMock).toHaveBeenCalledExactlyOnceWith({
      path: { note: note.id },
      body: {
        content: `${note.content} The lighthouse beam sweeps across the bay. The ferry arrives after sunset. The harbour lights stay on.`,
      },
    })
  })

  it("keeps the body when a response has no written segments", async () => {
    dictation.audioToTextMock.mockResolvedValueOnce(
      wrapSdkResponse(audioTextResponse([]))
    )

    await processAudio(dictation.wrapper)

    expect(dictation.updateContentMock).not.toHaveBeenCalled()
  })

  it("writes the timed chunk, Flush, and Stop passages once each, in order, one space apart", async () => {
    dictation.audioToTextMock
      .mockResolvedValueOnce(
        wrapSdkResponse(audioTextResponse("The museum opens at nine."))
      )
      .mockResolvedValueOnce(
        wrapSdkResponse(audioTextResponse("The train leaves at noon."))
      )
      .mockResolvedValueOnce(
        wrapSdkResponse(audioTextResponse("The ferry returns at six."))
      )

    await processAudio(dictation.wrapper, midSpeechChunk())
    await processAudio(dictation.wrapper, midSpeechChunk())
    await processAudio(dictation.wrapper, audioChunk())

    expect(dictation.updateContentMock).toHaveBeenCalledTimes(3)
    ;[
      `${note.content} The museum opens at nine.`,
      `${note.content} The museum opens at nine. The train leaves at noon.`,
      `${note.content} The museum opens at nine. The train leaves at noon. The ferry returns at six.`,
    ].forEach((content, index) =>
      expect(dictation.updateContentMock).toHaveBeenNthCalledWith(index + 1, {
        path: { note: note.id },
        body: { content },
      })
    )
  })

  it.each([
    {
      when: "several paragraphs long",
      body: longBody,
      saved: `${longBody} text`,
    },
    { when: "empty", body: "", saved: "text" },
    {
      when: "ending in whitespace",
      body: "Ends with a newline.\n",
      saved: "Ends with a newline.\ntext",
    },
  ])(
    "preserves the exact original body when $when",
    async ({ body, saved }) => {
      noteStore.refreshNoteRealm(
        makeMe.aNoteRealm.id(note.id).content(body).please()
      )
      await processAudio(dictation.wrapper)
      expect(dictation.updateContentMock).toHaveBeenCalledExactlyOnceWith({
        path: { note: note.id },
        body: { content: saved },
      })
    }
  )

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
    await dictation.wrapper.setProps({ note: destination.note })
    await processAudio(dictation.wrapper)
    expect(dictation.updateContentMock).toHaveBeenCalledExactlyOnceWith({
      path: { note: note.id },
      body: { content: `${current} text` },
    })
    expect(noteStore.refOfNoteRealm(destination.id).value?.note.content).toBe(
      destination.note.content
    )
  })

  it("keeps appended content after an API error", async () => {
    dictation.audioToTextMock
      .mockResolvedValueOnce(
        wrapSdkResponse(
          audioTextResponse("The lighthouse beam sweeps across the bay.")
        )
      )
      .mockResolvedValueOnce(wrapSdkError("API Error"))
      .mockResolvedValueOnce(
        wrapSdkResponse(audioTextResponse("The ferry arrives after sunset."))
      )
    await processAudio(dictation.wrapper)
    await expect(processAudio(dictation.wrapper)).rejects.toThrow()
    await processAudio(dictation.wrapper)
    expect(dictation.updateContentMock).toHaveBeenLastCalledWith({
      path: { note: note.id },
      body: {
        content: `${note.content} The lighthouse beam sweeps across the bay. The ferry arrives after sunset.`,
      },
    })
  })
})
