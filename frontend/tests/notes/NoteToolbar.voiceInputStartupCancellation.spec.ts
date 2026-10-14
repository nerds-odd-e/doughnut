import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import { noteVoiceInputTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  installMockResizeObserver,
  layoutNoteToolbar,
  overflowTogglesNavWidth,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import {
  mountNoteToolbar,
  noteToolbarAction,
  openNoteToolbarOverflowMenu,
  overflowMenuItem,
} from "@tests/notes/noteToolbarTestHelpers"
import {
  audioTextResponse,
  expectIdleVoiceInputInToolbar,
  moveNoteToolbarTo,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
  type MockInstance,
} from "vitest"

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

function microphone() {
  const stop = vi.fn()
  const stream = {
    getTracks: () => [{ stop }],
    getAudioTracks: () => [{ getSettings: () => ({ deviceId: "microphone" }) }],
  } as unknown as MediaStream
  return { stream, stop }
}

function audioGraph(addModule: () => Promise<void>) {
  const worklet = {
    connect: vi.fn(),
    disconnect: vi.fn(),
    port: { onmessage: null as ((event: MessageEvent) => void) | null },
  }
  const source = {
    connect: vi.fn(() => {
      worklet.port.onmessage!(
        new MessageEvent("message", {
          data: { audioBuffer: [new Float32Array(1600).fill(0.25)] },
        })
      )
    }),
    disconnect: vi.fn(),
  }
  const context = {
    audioWorklet: { addModule },
    createMediaStreamSource: () => source,
    destination: {},
  }
  return { worklet, source, context }
}

describe("NoteToolbar cancels content voice startup on departure", () => {
  let wrapper: VueWrapper
  let origin: ReturnType<typeof makeMe.aNoteRealm.please>
  let destination: ReturnType<typeof makeMe.aNoteRealm.please>
  let oldMicrophone: ReturnType<typeof microphone>
  let oldLock: { release: ReturnType<typeof vi.fn> }
  let graphs: ReturnType<typeof audioGraph>[]
  let addModule: ReturnType<typeof vi.fn<() => Promise<void>>>
  let getUserMedia: MockInstance<MediaDevices["getUserMedia"]>
  let requestWakeLock: MockInstance<WakeLock["request"]>
  let audioToText: ReturnType<typeof mockSdkService>
  let saveContent: ReturnType<typeof mockSdkService>

  beforeEach(async () => {
    vi.useFakeTimers()
    installMockResizeObserver()
    graphs = []
    oldMicrophone = microphone()
    oldLock = { release: vi.fn().mockResolvedValue(undefined) }
    getUserMedia = vi
      .spyOn(navigator.mediaDevices, "getUserMedia")
      .mockResolvedValue(oldMicrophone.stream)
    requestWakeLock = vi
      .spyOn(navigator.wakeLock, "request")
      .mockResolvedValue(oldLock as unknown as WakeLockSentinel)
    addModule = vi.fn<() => Promise<void>>().mockResolvedValue(undefined)
    vi.spyOn(globalThis, "AudioContext").mockImplementation(function () {
      const graph = audioGraph(addModule)
      graphs.push(graph)
      return graph.context as unknown as AudioContext
    })
    vi.spyOn(globalThis, "AudioWorkletNode").mockImplementation(
      function (context) {
        return graphs.find((graph) => (graph.context as unknown) === context)!
          .worklet as unknown as AudioWorkletNode
      }
    )
    audioToText = mockSdkService(
      AiAudioController,
      "audioToText",
      audioTextResponse("Destination speech.")
    )
    saveContent = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
    origin = makeMe.aNoteRealm.content("Origin body.").please()
    destination = makeMe.aNoteRealm.content("Destination body.").please()
    wrapper = await mountNoteToolbar(origin)
    useNoteStore().refreshNoteRealm(origin)
  })

  afterEach(async () => {
    wrapper?.unmount()
    await flushPromises()
    restoreNoteToolbarWidthMocks()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  async function clickVoice(title: string = noteVoiceInputTitles.start) {
    await noteToolbarAction(wrapper, title).trigger("click")
    await flushPromises()
  }

  it.each(["microphone", "audio initialization"])(
    "releases resources and discards late audio after pending %s",
    async (boundary) => {
      const media = deferred<MediaStream>()
      const initialization = deferred<void>()
      if (boundary === "microphone")
        getUserMedia.mockReturnValueOnce(media.promise)
      else addModule.mockReturnValueOnce(initialization.promise)
      await clickVoice()
      expect(
        noteToolbarAction(wrapper, noteVoiceInputTitles.start).attributes()
      ).toHaveProperty("disabled")

      await moveNoteToolbarTo(wrapper, destination)
      media.resolve(oldMicrophone.stream)
      initialization.resolve(undefined)
      await flushPromises()

      expect(oldMicrophone.stop).toHaveBeenCalledExactlyOnceWith()
      expect(graphs[0]!.source.disconnect).toHaveBeenCalledExactlyOnceWith()
      expect(graphs[0]!.worklet.disconnect).toHaveBeenCalledExactlyOnceWith()
      expect(oldLock.release).toHaveBeenCalledExactlyOnceWith()
      expect(audioToText).not.toHaveBeenCalled()
      expect(saveContent).not.toHaveBeenCalled()
      expectIdleVoiceInputInToolbar(wrapper)
    }
  )

  it("releases a late wake lock and keeps the destination available", async () => {
    const lock = deferred<WakeLockSentinel>()
    requestWakeLock.mockReturnValueOnce(lock.promise)
    await clickVoice()
    await moveNoteToolbarTo(wrapper, destination)

    lock.resolve(oldLock as unknown as WakeLockSentinel)
    await flushPromises()

    expect(oldLock.release).toHaveBeenCalledExactlyOnceWith()
    expect(getUserMedia).not.toHaveBeenCalled()
    expectIdleVoiceInputInToolbar(wrapper)
    await clickVoice()
    expect(
      noteToolbarAction(wrapper, noteVoiceInputTitles.stop).attributes(
        "aria-pressed"
      )
    ).toBe("true")
    await clickVoice(noteVoiceInputTitles.stop)
    expect(saveContent).toHaveBeenCalledExactlyOnceWith({
      path: { note: destination.note.id },
      body: { content: "Destination body. Destination speech." },
    })
  })

  it("keeps a newer destination recording active while cleaning up the departed attempt", async () => {
    const media = deferred<MediaStream>()
    const destinationMicrophone = microphone()
    const destinationLock = { release: vi.fn().mockResolvedValue(undefined) }
    getUserMedia
      .mockReturnValueOnce(media.promise)
      .mockResolvedValueOnce(destinationMicrophone.stream)
    requestWakeLock
      .mockResolvedValueOnce(oldLock as unknown as WakeLockSentinel)
      .mockResolvedValueOnce(destinationLock as unknown as WakeLockSentinel)
    await clickVoice()
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
    await moveNoteToolbarTo(wrapper, destination)
    await openNoteToolbarOverflowMenu(wrapper)
    overflowMenuItem(noteVoiceInputTitles.start)!.click()
    await flushPromises()

    media.resolve(oldMicrophone.stream)
    await flushPromises()

    expect(oldMicrophone.stop).toHaveBeenCalledExactlyOnceWith()
    expect(oldLock.release).toHaveBeenCalledExactlyOnceWith()
    expect(destinationMicrophone.stop).not.toHaveBeenCalled()
    expect(destinationLock.release).not.toHaveBeenCalled()
    expect(graphs[0]!.worklet.disconnect).not.toHaveBeenCalled()
    expect(
      noteToolbarAction(wrapper, noteVoiceInputTitles.stop).isVisible()
    ).toBe(true)
    expect(audioToText).not.toHaveBeenCalled()
    await clickVoice(noteVoiceInputTitles.stop)
    expect(destinationMicrophone.stop).toHaveBeenCalledExactlyOnceWith()
    expect(destinationLock.release).toHaveBeenCalledExactlyOnceWith()
    expect(saveContent).toHaveBeenCalledExactlyOnceWith({
      path: { note: destination.note.id },
      body: { content: "Destination body. Destination speech." },
    })
  })
})
