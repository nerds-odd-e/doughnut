import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { User } from "@generated/donut-backend-api"
import type NoteUndo from "@/store/noteUndo"
import { resetNoteStore } from "@/store/noteStore"
import { screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { beforeEach, vi, describe, it, expect } from "vitest"
import { defineComponent, h, provide, ref } from "vue"

function renderSignedOutGlobalBar() {
  const userRef = ref<User | undefined>()
  const Harness = defineComponent({
    setup() {
      provide("currentUser", userRef)
      return () => h(GlobalBar)
    },
  })
  return helper.component(Harness).withCleanStorage().withRouter().render()
}

describe("global bar", () => {
  let noteEditingHistory: NoteUndo
  let user: User

  beforeEach(() => {
    vi.clearAllMocks()
    user = makeMe.aUser.please()
    const storage = resetNoteStore()
    noteEditingHistory = storage.noteUndo
  })

  it("puts the Donut menu launcher first in the bar when signed in", async () => {
    helper.component(GlobalBar).withCurrentUser(user).withRouter().render()

    const firstControl = document
      .querySelector("nav")!
      .querySelector("button, summary, a, input")
    expect(firstControl).toBe(
      screen.getByRole("button", { name: "Donut menu" })
    )
  })

  it("puts the Donut menu launcher in the bar when signed out", async () => {
    renderSignedOutGlobalBar()

    expect(
      screen.getByRole("button", { name: "Donut menu" })
    ).toBeInTheDocument()
  })

  it("hides undo when there is nothing to undo", async () => {
    helper.component(GlobalBar).withCurrentUser(user).withRouter().render()

    expect(screen.queryByTitle("undo")).toBeNull()
  })

  it("show undo when there is something to undo", async () => {
    const note = makeMe.aNote.please()
    noteEditingHistory.trashNote(note.id, "Note", null)
    helper.component(GlobalBar).withCurrentUser(user).withRouter().render()

    expect(await screen.findByTitle("undo trash note")).not.toBeDisabled()
  })
})
