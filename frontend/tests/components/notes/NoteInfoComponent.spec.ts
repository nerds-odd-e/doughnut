import NoteInfoComponent from "@/components/notes/NoteInfoComponent.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import type { NoteInfo } from "@generated/backend"

describe("NoteInfoComponent", () => {
  it("should display all memory trackers including skipped ones", () => {
    const noteInfo: NoteInfo = {
      memoryTrackers: [
        makeMe.aMemoryTracker
          .removedFromTracking(false)
          .repetitionCount(5)
          .please(),
        makeMe.aMemoryTracker
          .removedFromTracking(true)
          .repetitionCount(3)
          .please(),
      ],
      note: makeMe.aNoteRealm.please(),
      createdAt: "",
    }

    const wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        noteInfo,
      })
      .withRouter()
      .mount()

    const rows = wrapper.findAll("tbody tr")
    expect(rows).toHaveLength(2)
  })

  it("should make skipped memory trackers clickable", async () => {
    const skippedMemoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(true)
      .please()
    skippedMemoryTracker.id = 123

    const noteInfo: NoteInfo = {
      memoryTrackers: [skippedMemoryTracker],
      note: makeMe.aNoteRealm.please(),
      createdAt: "",
    }

    const wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        noteInfo,
      })
      .withRouter()
      .mount()

    await flushPromises()

    const row = wrapper.find("tbody tr")
    expect(row?.classes()).toContain("clickable-row")

    await row?.trigger("click")
    await flushPromises()

    expect(wrapper.vm.$router.currentRoute.value.name).toBe("memoryTrackerShow")
    expect(wrapper.vm.$router.currentRoute.value.params.memoryTrackerId).toBe(
      "123"
    )
  })

  it("should display memory trackers table when there are memory trackers", () => {
    const noteInfo: NoteInfo = {
      memoryTrackers: [makeMe.aMemoryTracker.please()],
      note: makeMe.aNoteRealm.please(),
      createdAt: "",
    }

    const wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        noteInfo,
      })
      .withRouter()
      .mount()

    expect(wrapper.find("table").exists()).toBe(true)
    const h6Elements = wrapper.findAll("h6")
    expect(h6Elements.some((h6) => h6.text().includes("Memory Trackers"))).toBe(
      true
    )
  })

  it("should not display memory trackers table when there are no memory trackers", () => {
    const noteInfo: NoteInfo = {
      memoryTrackers: [],
      note: makeMe.aNoteRealm.please(),
      createdAt: "",
    }

    const wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        noteInfo,
      })
      .withRouter()
      .mount()

    expect(wrapper.find("table").exists()).toBe(false)
  })
})
