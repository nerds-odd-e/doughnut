import {
  ConversationMessageController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  noteMoreOptionsTitles,
  noteToolbarEditTitles,
} from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  clickDialogOk,
  clickUndoButton,
} from "@tests/toolbars/noteUndoButtonTestSupport"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { page } from "vitest/browser"
import {
  atViewportWidth,
  chooseFromMore,
  clickPageBarCommand,
  commandsSitInPageBar,
  mountNotebookWorkspace,
  noteCommandTitlesInOrder,
  pageBarCommand,
  pageBarEl,
  visibleCommandTitles,
} from "./notebookWorkspaceHeaderTestSupport"

const titles = noteMoreOptionsTitles

describe("Notebook workspace header", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    useAssimilationView().dismiss()
  })

  afterEach(() => {
    wrapper.unmount()
    document.body.innerHTML = ""
  })

  it.each([320, 390, 768, 1280])(
    "puts the note's commands in the shared bar row at %ipx",
    async (width) => {
      await atViewportWidth(width)
      ;({ wrapper } = await mountNotebookWorkspace([
        makeMe.aNoteRealm.title("Orchard").please(),
      ]))

      await vi.waitFor(() => {
        expect(visibleCommandTitles().length).toBeGreaterThan(0)
        expect(commandsSitInPageBar(width)).toBe(true)
      })
      const visible = visibleCommandTitles()
      expect(visible).toEqual(
        noteCommandTitlesInOrder.filter((t) => visible.includes(t))
      )
    }
  )

  it("selects an overflow action at phone width and keeps its editor mode while widening", async () => {
    await atViewportWidth(320)
    ;({ wrapper } = await mountNotebookWorkspace([
      makeMe.aNoteRealm.title("Orchard").please(),
    ]))
    await vi.waitFor(() =>
      expect(pageBarCommand(titles.overflowMenu)).not.toBeNull()
    )

    await chooseFromMore(noteToolbarEditTitles.markdown)
    expect(document.querySelector("#main-note-content textarea")).not.toBeNull()

    await page.viewport(1280, 700)
    await vi.waitFor(() => {
      expect(pageBarCommand(noteToolbarEditTitles.rich)).not.toBeNull()
      expect(commandsSitInPageBar(1280)).toBe(true)
    })
    expect(document.querySelector("#main-note-content textarea")).not.toBeNull()
  })

  it("keeps Assimilate pressed in the bar while its settings stay with the note", async () => {
    await atViewportWidth(320)
    ;({ wrapper } = await mountNotebookWorkspace([
      makeMe.aNoteRealm.title("Orchard").please(),
    ]))
    await vi.waitFor(() =>
      expect(pageBarCommand(titles.overflowMenu)).not.toBeNull()
    )

    await chooseFromMore(titles.assimilation)

    const settings = () =>
      document.querySelector('[data-testid="note-assimilation-modes"]')
    await vi.waitFor(() => {
      expect(
        pageBarCommand(titles.assimilation)?.getAttribute("aria-pressed")
      ).toBe("true")
      expect(commandsSitInPageBar(320)).toBe(true)
    })
    expect(settings()?.closest(".note-show-container")).not.toBeNull()
    expect(pageBarEl().contains(settings())).toBe(false)

    await page.viewport(1280, 700)
    await vi.waitFor(() => expect(commandsSitInPageBar(1280)).toBe(true))
    expect(
      pageBarCommand(titles.assimilation)?.getAttribute("aria-pressed")
    ).toBe("true")
    expect(settings()).not.toBeNull()
  })

  it("keeps Conversation but no editing commands for a read-only note", async () => {
    await atViewportWidth(1280)
    const realm = makeMe.aNoteRealm.title("Shared").please()
    realm.notebookRealm.readonly = true
    ;({ wrapper } = await mountNotebookWorkspace([realm]))

    await vi.waitFor(() =>
      expect(pageBarCommand(titles.conversation)).not.toBeNull()
    )
    expect(pageBarCommand(noteToolbarEditTitles.markdown)).toBeNull()
    expect(pageBarCommand(titles.overflowMenu)).toBeNull()
    expect(commandsSitInPageBar(1280)).toBe(true)
  })

  it("starts a conversation on the property route and leaves the bar when the conversation is maximized", async () => {
    await atViewportWidth(1280)
    const realm = makeMe.aNoteRealm.title("Orchard").please()
    let router: Awaited<ReturnType<typeof mountNotebookWorkspace>>["router"]
    ;({ wrapper, router } = await mountNotebookWorkspace(
      [realm],
      notePropertyLocation(realm.id, "Due")
    ))
    mockSdkService(
      ConversationMessageController,
      "getConversationsAboutNote",
      []
    )
    await vi.waitFor(() =>
      expect(pageBarCommand(titles.conversation)).not.toBeNull()
    )

    await clickPageBarCommand(titles.conversation)
    expect(router.currentRoute.value).toMatchObject({
      ...notePropertyLocation(realm.id, "Due"),
      query: { conversation: "true" },
    })

    await vi.waitFor(() =>
      expect(document.querySelector("button.maximize-button")).not.toBeNull()
    )
    ;(document.querySelector("button.maximize-button") as HTMLElement).click()
    await flushPromises()
    expect(document.querySelector("[data-note-toolbar]")).toBeNull()
  })

  it("gives the next note its own working commands after switching", async () => {
    await atViewportWidth(1280)
    const first = makeMe.aNoteRealm.title("First").please()
    const second = makeMe.aNoteRealm.title("Second").please()
    let router: Awaited<ReturnType<typeof mountNotebookWorkspace>>["router"]
    ;({ wrapper, router } = await mountNotebookWorkspace([first, second]))
    await vi.waitFor(() =>
      expect(pageBarCommand(noteToolbarEditTitles.markdown)).not.toBeNull()
    )

    await router.push(noteShowLocation(second.id))
    await flushPromises()
    await vi.waitFor(() =>
      expect(document.body.textContent).toContain("Second")
    )

    expect(document.querySelectorAll("[data-note-toolbar]")).toHaveLength(1)
    await clickPageBarCommand(noteToolbarEditTitles.markdown)
    expect(document.querySelector("#main-note-content textarea")).not.toBeNull()
    expect(commandsSitInPageBar(1280)).toBe(true)
  })

  it("undoes the latest change of another note from the bar and goes there", async () => {
    await atViewportWidth(390)
    const open = makeMe.aNoteRealm.title("Open note").please()
    const edited = makeMe.aNoteRealm.title("Edited note").please()
    let router: Awaited<ReturnType<typeof mountNotebookWorkspace>>["router"]
    ;({ wrapper, router } = await mountNotebookWorkspace([open, edited]))
    useNoteStore().noteUndo.addEditingToUndoHistory(
      edited.id,
      "edit title",
      "Old Title"
    )
    const undone = mockSdkService(
      TextContentController,
      "updateNoteTitle",
      edited
    )
    await flushPromises()
    expect(
      pageBarEl().contains(document.querySelector('[title="undo edit title"]'))
    ).toBe(true)

    await clickUndoButton("undo edit title")
    await clickDialogOk()

    expect(undone).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { note: edited.id },
        body: { newTitle: "Old Title" },
      })
    )
    await vi.waitFor(() =>
      expect(router.currentRoute.value).toMatchObject(
        noteShowLocation(edited.id)
      )
    )
  })
})
