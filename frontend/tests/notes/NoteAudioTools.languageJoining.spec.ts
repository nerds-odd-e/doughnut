import { wrapSdkResponse } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
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

describe("NoteAudioTools language joining", () => {
  const dictation = useSavedBodyDictation()
  const { note, noteStore } = dictation

  it.each([
    [
      "Japanese",
      "鐘は毎時間鳴ります。",
      "果樹園は古いです。",
      "鐘は毎時間鳴ります。果樹園は古いです。",
    ],
    [
      "Chinese",
      "钟每小时响一次。",
      "果园很古老。",
      "钟每小时响一次。果园很古老。",
    ],
    [
      "Latin to hiragana",
      "私はPython",
      "が好きです。",
      "私はPythonが好きです。",
    ],
    [
      "Japanese to English",
      "鐘は毎時間鳴ります。",
      "The orchard is old.",
      "鐘は毎時間鳴ります。The orchard is old.",
    ],
    [
      "English to Japanese",
      "The bell rings every hour.",
      "果樹園は古いです。",
      "The bell rings every hour.果樹園は古いです。",
    ],
    [
      "Latin on both sides",
      "私はPython",
      "is useful.",
      "私はPython is useful.",
    ],
    [
      "Korean",
      "종은 매시간 울립니다.",
      "과수원은 오래되었습니다.",
      "종은 매시간 울립니다. 과수원은 오래되었습니다.",
    ],
    ["kanji to Latin", "鐘", "bell", "鐘bell"],
    ["hiragana to Latin", "あ", "a", "あa"],
    ["katakana to Latin", "カ", "ka", "カka"],
    ["prolonged sound mark", "コー", "coffee", "コーcoffee"],
    ["full-width punctuation", "Hello！", "World", "Hello！World"],
    ["opening punctuation", "Hello", "「世界」", "Hello「世界」"],
    ["supplementary kanji before", "𠮷", "Yoshi", "𠮷Yoshi"],
    ["supplementary kanji after", "Yoshi", "𠮷", "Yoshi𠮷"],
    ["empty Japanese body", "", "果樹園は古いです。", "果樹園は古いです。"],
    [
      "Japanese trailing whitespace",
      "鐘は毎時間鳴ります。\n",
      "果樹園は古いです。",
      "鐘は毎時間鳴ります。\n果樹園は古いです。",
    ],
  ])(
    "joins %s by the two adjacent characters",
    async (_, body, passage, saved) => {
      noteStore.refreshNoteRealm(
        makeMe.aNoteRealm.id(note.id).content(body).please()
      )
      dictation.audioToTextMock.mockResolvedValueOnce(
        wrapSdkResponse(audioTextResponse(passage))
      )

      await processAudio(dictation.wrapper)

      expect(dictation.updateContentMock).toHaveBeenCalledExactlyOnceWith({
        path: { note: note.id },
        body: { content: saved },
      })
    }
  )

  it("joins Japanese segments and a later passage of the same recording without spaces", async () => {
    noteStore.refreshNoteRealm(
      makeMe.aNoteRealm.id(note.id).content("鐘は毎時間鳴ります。").please()
    )
    dictation.audioToTextMock
      .mockResolvedValueOnce(
        wrapSdkResponse(
          audioTextResponse(["果樹園は古いです。", "ベンチがあります。"])
        )
      )
      .mockResolvedValueOnce(
        wrapSdkResponse(audioTextResponse("川が流れています。"))
      )

    await processAudio(dictation.wrapper, midSpeechChunk())
    await processAudio(dictation.wrapper, audioChunk())

    expect(dictation.updateContentMock).toHaveBeenLastCalledWith({
      path: { note: note.id },
      body: {
        content:
          "鐘は毎時間鳴ります。果樹園は古いです。ベンチがあります。川が流れています。",
      },
    })
  })
})
