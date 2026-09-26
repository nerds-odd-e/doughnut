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
  blurTextarea,
  mountNoteEditableContent,
  setTextareaValue,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent save response", () => {
  const noteId = 1

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    setupUpdateNoteContentMock()
    setupPopupsMock(vi.fn().mockResolvedValue(null))
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.useRealTimers()
  })

  it("clears dirty when save returns wrapped ordinary-note content", async () => {
    const wrapped = "---\ntype: Note\n---\nAfter save"
    let wrapper: VueWrapper<ComponentPublicInstance>

    mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      async () => {
        await wrapper.setProps({ noteId, noteContent: wrapped })
        return makeMe.aNoteRealm.id(noteId).content(wrapped).please()
      }
    )

    wrapper = await mountNoteEditableContent({ noteId, noteContent: "Before" })

    await setTextareaValue(wrapper, "After save")
    await blurTextarea(wrapper)

    expect(wrapper.find(".dirty").exists()).toBe(false)
    expect(textareaEl(wrapper).value).toBe(wrapped)

    wrapper.unmount()
  })

  describe("restoring while an older edit is in flight", () => {
    // Each save waits on its own gate; once released, the server echoes the
    // (possibly canonicalized) content back into the store and the props.
    async function mountGatedEditor(options?: {
      toServerContent?: (requested: string) => string
    }) {
      const toServerContent =
        options?.toServerContent ?? ((requested: string) => requested)
      let serverContent = "A"
      const gates: Array<ReturnType<typeof deferred>> = []
      const wrapperRef = {} as { value: VueWrapper<ComponentPublicInstance> }
      const spy = mockSdkServiceWithImplementation(
        TextContentController,
        "updateNoteContent",
        async ({ body }) => {
          const gate = deferred()
          gates.push(gate)
          await gate.promise
          serverContent = toServerContent(body?.content ?? "")
          const realm = makeMe.aNoteRealm
            .id(noteId)
            .content(serverContent)
            .please()
          useStorageAccessor().value.refreshNoteRealm(realm)
          await wrapperRef.value.setProps({
            noteId,
            noteContent: serverContent,
          })
          return realm
        }
      )
      wrapperRef.value = await mountNoteEditableContent({
        noteId,
        noteContent: "A",
      })
      useStorageAccessor().value.refreshNoteRealm(
        makeMe.aNoteRealm.id(noteId).content("A").please()
      )
      return { wrapperRef, spy, gates, getServerContent: () => serverContent }
    }

    it("saves the restored draft after an older in-flight edit is acknowledged", async () => {
      const { wrapperRef, spy, gates, getServerContent } =
        await mountGatedEditor()

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
      const { wrapperRef, spy, gates, getServerContent } =
        await mountGatedEditor()

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
      const { wrapperRef, spy, gates, getServerContent } =
        await mountGatedEditor()

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
      const wrappedAb = "---\ntype: Note\n---\nAB"
      const { wrapperRef, spy, gates, getServerContent } =
        await mountGatedEditor({
          toServerContent: (requested) =>
            requested === "AB" ? wrappedAb : requested,
        })

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
})
