import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import { useSignedInMenuData } from "@/composables/useSignedInMenuData"
import { focusDirective } from "@/utils/focusTarget"
import { fireEvent } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, onTestFinished, vi } from "vitest"
import { page, server } from "vitest/browser"
import { defineComponent, h, inject, type Ref } from "vue"
import type { User } from "@generated/donut-backend-api"
import {
  panel,
  pressLauncher,
  router,
  searchField,
  setupDonutMenuTests,
  user,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

setupDonutMenuTests()

// The real page bar positions the panel, as on every page.
const PageBarInAppShell = defineComponent({
  setup() {
    useSignedInMenuData(inject<Ref<User | undefined>>("currentUser")!)
    return () => h(GlobalBar)
  },
})

const openDonutMenuInPageBarAt = async (width: number, height: number) => {
  const original = server.config.browser.viewport
  onTestFinished(() => page.viewport(original.width, original.height))
  await page.viewport(width, height)
  helper
    .component(PageBarInAppShell)
    .withCurrentUser(user)
    .withDirective("focus", focusDirective)
    .withRealRouter(router)
    .render()
  await flushPromises()
  await pressLauncher()
}

describe("Donut menu layout", () => {
  it("fits a short window and scrolls, keeping the search field reachable", async () => {
    mockSdkService(
      NoteController,
      "getRecentNotes",
      Array.from({ length: 12 }, (_, i) =>
        makeMe.aNoteSearchResult.title(`Recent ${i}`).please()
      )
    )
    await openDonutMenuInPageBarAt(390, 500)
    await fireEvent.focus(searchField())
    await flushPromises()

    const sheet = panel() as HTMLElement
    expect(sheet.getBoundingClientRect().bottom).toBeLessThanOrEqual(
      window.innerHeight
    )
    expect(sheet.scrollHeight).toBeGreaterThan(sheet.clientHeight)

    searchField().scrollIntoView()
    const field = searchField().getBoundingClientRect()
    const box = sheet.getBoundingClientRect()
    expect(field.top).toBeGreaterThanOrEqual(box.top)
    expect(field.bottom).toBeLessThanOrEqual(box.bottom)
  })
})
