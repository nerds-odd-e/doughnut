import CurrentBlockNavigationBar from "@/components/book-reading/CurrentBlockNavigationBar.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import {
  currentBlockNavBar,
  mountNavBarScenario,
} from "./bookReadingPageNavigationBarTestSupport"
import { expectCurrentSelection } from "./bookReadingPageInteractionTestSupport"
import {
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
} from "./bookReadingPageTestSupport"

describe("BookReadingPage navigation bar", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
  })

  it("shows navigation bar when current block differs from selected block", async () => {
    const wrapper = await mountNavBarScenario(500)

    expect(currentBlockNavBar(wrapper).exists()).toBe(true)
    expect(currentBlockNavBar(wrapper).text()).toContain("Section 2")
  })

  it("hides navigation bar when current block equals selected block", async () => {
    const wrapper = await mountNavBarScenario(10)

    expect(currentBlockNavBar(wrapper).exists()).toBe(false)
  })

  it("Read from here makes current block the selected block and hides nav bar", async () => {
    const wrapper = await mountNavBarScenario(500)

    await wrapper
      .findComponent(CurrentBlockNavigationBar)
      .vm.$emit("readFromHere")
    await flushPromises()

    expectCurrentSelection(wrapper, "Section 2")
    expect(currentBlockNavBar(wrapper).exists()).toBe(false)
  })

  it("Back to selected scrolls to selected block and hides nav bar", async () => {
    const wrapper = await mountNavBarScenario(500)

    await wrapper
      .findComponent(CurrentBlockNavigationBar)
      .vm.$emit("backToSelected")
    await flushPromises()

    expect(currentBlockNavBar(wrapper).exists()).toBe(false)
    expectCurrentSelection(wrapper, "Section 1")
  })
})
