import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import { TextContentController } from "@generated/donut-backend-api/sdk.gen"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  advanceNoteContentSaveDebounce,
  deferred,
} from "@tests/helpers/noteContentDebounceTestSupport"
import {
  mountMarkdownTextarea,
  setTextareaValue,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent debounced save — restoring while an edit is in flight", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setupUpdateNoteContentMock()
    setupPopupsMock(vi.fn().mockResolvedValue(null))
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.useRealTimers()
  })

  async function mountWithSeededContent(noteId: number, noteContent: string) {
    const wrapper = await mountMarkdownTextarea({ noteId, noteContent })
    useStorageAccessor().value.refreshNoteRealm(
      makeMe.aNoteRealm.id(noteId).content(noteContent).please()
    )
    return wrapper
  }

  function mockGatedContentSaves(
    noteId: number,
    wrapper: { value: VueWrapper<ComponentPublicInstance> },
    options?: {
      toServerContent?: (requested: string) => string
    }
  ) {
    const toServerContent =
      options?.toServerContent ?? ((requested: string) => requested)
    let serverContent = "A"
    const gates: Array<ReturnType<typeof deferred>> = []
    const spy = mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      async ({ body }) => {
        const requested = body?.content ?? ""
        const gate = deferred()
        gates.push(gate)
        await gate.promise
        serverContent = toServerContent(requested)
        const realm = makeMe.aNoteRealm
          .id(noteId)
          .content(serverContent)
          .please()
        useStorageAccessor().value.refreshNoteRealm(realm)
        await wrapper.value.setProps({ noteId, noteContent: serverContent })
        return realm
      }
    )
    return {
      spy,
      gates,
      getServerContent: () => serverContent,
    }
  }

  async function mountGatedEditor(
    noteId: number,
    options?: {
      toServerContent?: (requested: string) => string
    }
  ) {
    const wrapperRef: {
      value: VueWrapper<ComponentPublicInstance>
    } = { value: undefined! }
    const gated = mockGatedContentSaves(noteId, wrapperRef, options)
    wrapperRef.value = await mountWithSeededContent(noteId, "A")
    return { wrapperRef, ...gated }
  }

  it("saves the restored draft after an older in-flight edit is acknowledged", async () => {
    vi.useFakeTimers()
    const noteId = 1
    const { wrapperRef, spy, gates, getServerContent } =
      await mountGatedEditor(noteId)

    await setTextareaValue(wrapperRef.value, "AB")
    await advanceNoteContentSaveDebounce()
    expect(gates).toHaveLength(1)
    expect(spy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "AB" },
    })

    await setTextareaValue(wrapperRef.value, "A")
    expect(textareaEl(wrapperRef.value).value).toBe("A")

    gates[0]!.resolve()
    await flushPromises()
    await advanceNoteContentSaveDebounce()
    await flushPromises()

    expect(spy).toHaveBeenCalledTimes(2)
    expect(spy).toHaveBeenLastCalledWith({
      path: { note: noteId },
      body: { content: "A" },
    })
    gates[1]!.resolve()
    await flushPromises()

    expect(getServerContent()).toBe("A")
    expect(textareaEl(wrapperRef.value).value).toBe("A")
    expect(wrapperRef.value.find(".dirty").exists()).toBe(false)

    wrapperRef.value.unmount()
  })

  it("keeps an empty restored draft and persists it after the older save", async () => {
    vi.useFakeTimers()
    const noteId = 1
    const { wrapperRef, spy, gates, getServerContent } =
      await mountGatedEditor(noteId)

    await setTextareaValue(wrapperRef.value, "AB")
    await advanceNoteContentSaveDebounce()
    await setTextareaValue(wrapperRef.value, "")

    gates[0]!.resolve()
    await flushPromises()
    expect(textareaEl(wrapperRef.value).value).toBe("")

    await advanceNoteContentSaveDebounce()
    await flushPromises()
    expect(spy).toHaveBeenLastCalledWith({
      path: { note: noteId },
      body: { content: "" },
    })
    gates[1]!.resolve()
    await flushPromises()

    expect(getServerContent()).toBe("")
    expect(textareaEl(wrapperRef.value).value).toBe("")

    wrapperRef.value.unmount()
  })

  it("persists a newer ordinary edit made before the older save returns", async () => {
    vi.useFakeTimers()
    const noteId = 1
    const { wrapperRef, spy, gates, getServerContent } =
      await mountGatedEditor(noteId)

    await setTextareaValue(wrapperRef.value, "AB")
    await advanceNoteContentSaveDebounce()
    await setTextareaValue(wrapperRef.value, "AC")

    gates[0]!.resolve()
    await flushPromises()
    expect(textareaEl(wrapperRef.value).value).toBe("AC")

    await advanceNoteContentSaveDebounce()
    await flushPromises()
    expect(spy).toHaveBeenLastCalledWith({
      path: { note: noteId },
      body: { content: "AC" },
    })
    gates[1]!.resolve()
    await flushPromises()

    expect(getServerContent()).toBe("AC")
    expect(textareaEl(wrapperRef.value).value).toBe("AC")

    wrapperRef.value.unmount()
  })

  it("accepts a canonical older response without replacing a newer draft", async () => {
    vi.useFakeTimers()
    const noteId = 1
    const wrappedAb = "---\ntype: Note\n---\nAB"
    const { wrapperRef, spy, gates, getServerContent } = await mountGatedEditor(
      noteId,
      {
        toServerContent: (requested) =>
          requested === "AB" ? wrappedAb : requested,
      }
    )

    await setTextareaValue(wrapperRef.value, "AB")
    await advanceNoteContentSaveDebounce()
    await setTextareaValue(wrapperRef.value, "A")

    gates[0]!.resolve()
    await flushPromises()
    expect(textareaEl(wrapperRef.value).value).toBe("A")
    expect(getServerContent()).toBe(wrappedAb)

    await advanceNoteContentSaveDebounce()
    await flushPromises()
    expect(spy).toHaveBeenLastCalledWith({
      path: { note: noteId },
      body: { content: "A" },
    })
    gates[1]!.resolve()
    await flushPromises()

    expect(getServerContent()).toBe("A")
    expect(textareaEl(wrapperRef.value).value).toBe("A")
    expect(wrapperRef.value.find(".dirty").exists()).toBe(false)

    wrapperRef.value.unmount()
  })
})
