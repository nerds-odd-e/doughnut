import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { dummyRouteRecordsFromMetadata } from "@/routes/dummyRouteRecords"
import { notePropertyLocation } from "@/routes/noteShowLocation"
import helper from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { createRouter, createWebHistory } from "vue-router"
import { describe, it, expect } from "vitest"
import { flushPromises } from "@vue/test-utils"

describe("NoteToolbar conversation overflow", () => {
  it("keeps the current property location when starting a conversation from overflow", async () => {
    const note = makeMe.aNote.please()
    const router = createRouter({
      history: createWebHistory(),
      routes: dummyRouteRecordsFromMetadata,
    })
    await router.push(notePropertyLocation(note.id, "topic"))
    const wrapper = helper
      .component(NoteMoreOptionsForm)
      .withRouter(router)
      .withProps({ note, only: ["conversation"] })
      .mount()

    await wrapper
      .find(`button[title="${noteMoreOptionsTitles.conversation}"]`)
      .trigger("click")
    await flushPromises()

    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(note.id, "topic")
    )
    expect(router.currentRoute.value.query).toEqual({ conversation: "true" })
  })
})
