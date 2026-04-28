import NoteInfoComponent from "@/components/notes/NoteInfoComponent.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, describe, expect, it, vi } from "vitest"
import helper from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
describe("NoteInfoComponent", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    vi.clearAllMocks()
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("should display all memory trackers including skipped ones", () => {
    const noteRealm = makeMe.aNoteRealm.please()
    const noteRecallInfo = makeMe.aNoteRecallInfo
      .memoryTrackers([
        makeMe.aMemoryTracker
          .removedFromTracking(false)
          .recallCount(5)
          .please(),
        makeMe.aMemoryTracker.removedFromTracking(true).recallCount(3).please(),
      ])
      .please()

    wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        note: noteRealm.note,
        noteRecallInfo,
      })
      .withRouter()
      .mount({ attachTo: document.body })

    const rows = wrapper.findAll("tbody tr")
    expect(rows).toHaveLength(2)
  })

  it("should make skipped memory trackers clickable", async () => {
    const skippedMemoryTracker = makeMe.aMemoryTracker
      .removedFromTracking(true)
      .please()
    skippedMemoryTracker.id = 123
    const noteRealm = makeMe.aNoteRealm.please()
    const noteRecallInfo = makeMe.aNoteRecallInfo
      .memoryTrackers([skippedMemoryTracker])
      .please()

    wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        note: noteRealm.note,
        noteRecallInfo,
      })
      .withRouter()
      .mount({ attachTo: document.body })

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
    const noteRealm = makeMe.aNoteRealm.please()
    const noteRecallInfo = makeMe.aNoteRecallInfo
      .memoryTrackers([makeMe.aMemoryTracker.please()])
      .please()

    wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        note: noteRealm.note,
        noteRecallInfo,
      })
      .withRouter()
      .mount({ attachTo: document.body })

    expect(wrapper.find("table").exists()).toBe(true)
    const h6Elements = wrapper.findAll("h6")
    expect(h6Elements.some((h6) => h6.text().includes("Memory Trackers"))).toBe(
      true
    )
  })

  it("should not display memory trackers table when there are no memory trackers", () => {
    const noteRealm = makeMe.aNoteRealm.please()
    const noteRecallInfo = makeMe.aNoteRecallInfo.please()

    wrapper = helper
      .component(NoteInfoComponent)
      .withProps({
        note: noteRealm.note,
        noteRecallInfo,
      })
      .withRouter()
      .mount({ attachTo: document.body })

    expect(wrapper.find("table").exists()).toBe(false)
  })
})
