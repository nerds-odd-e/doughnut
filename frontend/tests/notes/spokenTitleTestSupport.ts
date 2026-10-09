import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
import NoteEditableTitle from "@/components/notes/core/NoteEditableTitle.vue"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import type { NoteTopology } from "@generated/donut-backend-api"
import type { ComponentPublicInstance } from "vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import {
  clearAudioHardwareMocks,
  installAudioBrowserSpies,
} from "@tests/notes/noteAudioToolsMocks"
import { audioTextResponse } from "@tests/notes/noteAudioToolsTestSupport"
import { titleEditorEl } from "@tests/notes/noteTextContentTestSupport"
import { flushReferencedTitleBlurDiscardCheck } from "@tests/notes/textContentWrapperTestSupport"
import { afterEach, beforeEach, vi } from "vitest"

export function findSpeakTitleButtonByText(
  wrapper: VueWrapper<ComponentPublicInstance>,
  text: string
) {
  return wrapper.findAll("button").find((button) => button.text() === text)
}

export function mountNoteEditableTitle(options: {
  noteTopology: NoteTopology
  noteId: number
  readonly?: boolean
  hasInboundReferences?: boolean
}) {
  return helper
    .component(NoteEditableTitle)
    .withCleanStorage()
    .withProps({
      noteTopology: options.noteTopology,
      noteId: options.noteId,
      readonly: options.readonly ?? false,
      hasInboundReferences: options.hasInboundReferences ?? false,
    })
    .mount({ attachTo: document.body })
}

export function speakTitleStatusNode(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  return wrapper.find(".speak-title-control [role='status']")
}

export function speakTitleStatus(
  wrapper: VueWrapper<ComponentPublicInstance>
): string | undefined {
  const status = speakTitleStatusNode(wrapper)
  return status.exists() ? status.text() : undefined
}

/** Shared timers, audio mocks, and default transcription for spoken-title specs. */
export function useSpokenTitleTestLifecycle(
  defaultTranscript = "Photosynthesis in desert plants."
) {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    installAudioBrowserSpies()
    clearAudioHardwareMocks()
    mockSdkService(
      AiAudioController,
      "audioToText",
      audioTextResponse(defaultTranscript)
    )
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
    document.body.innerHTML = ""
  })
}

export async function speakTheTitle(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await findSpeakTitleButtonByText(wrapper, "Speak the title")!.trigger("click")
  await flushPromises()
}

export async function stopSpeaking(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await findSpeakTitleButtonByText(wrapper, "Stop")!.trigger("click")
  await flushPromises()
}

export async function speakAndStop(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await speakTheTitle(wrapper)
  await stopSpeaking(wrapper)
}

/** Hold Stop in converting until finishStop runs the processor callback. */
export function holdSpeakTitleConvertingUntilFinished() {
  const recorder = vi.mocked(createAudioRecorder).mock.results[0]!.value as {
    stopRecording: ReturnType<typeof vi.fn>
  }
  const processAudio = vi.mocked(createAudioRecorder).mock.calls[0]![0]!
  let finishStop!: () => void
  recorder.stopRecording.mockImplementation(
    () =>
      new Promise<File>((resolve) => {
        finishStop = () => {
          processAudio({
            data: new File([], "test.webm"),
            isMidSpeech: false,
          }).then(() => resolve(new File([], "test.webm")))
        }
      })
  )
  return { finishStop: () => finishStop() }
}

/** Leave the title area so a linked rename can discard (same as typed). */
export async function blurAwayFromSpokenTitle(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  titleEditorEl(wrapper).blur()
  await flushPromises()
  await flushReferencedTitleBlurDiscardCheck()
}
