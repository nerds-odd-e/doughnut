import {
  NoteController,
  SearchController,
} from "@generated/donut-backend-api/sdk.gen"
import type { Note } from "@generated/donut-backend-api"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import SearchForm from "@/components/wiki-link-or-relationship/SearchForm.vue"
import { dummyRouteRecordsFromMetadata } from "@/routes/dummyRouteRecords"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { fireEvent, render, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { userEvent } from "vitest/browser"
import { defineComponent } from "vue"
import { createMemoryHistory, createRouter } from "vue-router"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { describe, expect, it } from "vitest"
import {
  makeFolderHit,
  setupSearchDialogTests,
} from "./searchDialogTestSupport"

const SearchPopup = defineComponent({
  components: { PopButton, SearchForm },
  props: ["note"],
  template: `<PopButton title="Search" :show-close-button="false">
    <template #default="{ closer }">
      <SearchForm :note="note" :modal-closer="closer" @close-dialog="closer" />
    </template>
  </PopButton>`,
})

describe("Search popup result navigation", () => {
  setupSearchDialogTests()

  async function openSearch(note?: Note) {
    const current = makeMe.aNoteSearchResult.title("当前笔记").please()
    const other = makeMe.aNoteSearchResult.title("Another note").please()
    mockSdkService(NoteController, "getRecentNotes", [current, other])
    const router = createRouter({
      history: createMemoryHistory(),
      routes: dummyRouteRecordsFromMetadata,
    })
    await router.push(noteShowLocation(current.noteTopology.id))
    render(SearchPopup, {
      props: { note },
      global: { plugins: [router], directives: { focus: () => undefined } },
    })
    await fireEvent.click(screen.getByText("Search"))
    await flushPromises()
    return { router, current, other }
  }

  it("dismisses Recent when its current-route anchor is activated with Enter", async () => {
    const { router, current } = await openSearch()
    const currentRoute = router.currentRoute.value
    const link = screen.getByText("当前笔记").closest("a")!
    expect(link.getAttribute("href")).toBe(
      router.resolve(noteShowLocation(current.noteTopology.id)).href
    )
    link.focus()
    await userEvent.keyboard("{Enter}")
    await flushPromises()
    expect(document.querySelector("dialog[open]")).toBeNull()
    expect(router.currentRoute.value).toBe(currentRoute)
  })

  it("navigates to a different result and dismisses the popup", async () => {
    const { router, other } = await openSearch()
    await fireEvent.click(screen.getByText("Another note"))
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe(
      router.resolve(noteShowLocation(other.noteTopology.id)).fullPath
    )
    expect(document.querySelector("dialog[open]")).toBeNull()
  })

  it("keeps the popup and current route for a modified result click", async () => {
    const { router } = await openSearch()
    const currentRoute = router.currentRoute.value
    const link = screen.getByText("Another note").closest("a")!
    link.addEventListener("click", (event) => event.preventDefault(), {
      once: true,
    })
    await fireEvent.click(link, { ctrlKey: true })
    await flushPromises()
    expect(document.querySelector("dialog[open]")).not.toBeNull()
    expect(router.currentRoute.value).toBe(currentRoute)
  })

  it("keeps search open when clicking a non-link row area or changing list mode", async () => {
    await openSearch()
    await fireEvent.click(screen.getByText("当前笔记").closest("li")!)
    await fireEvent.click(screen.getByTestId("search-list-mode-matches"))
    expect(document.querySelector("dialog[open]")).not.toBeNull()
    expect(screen.getByPlaceholderText("Search")).toBeInTheDocument()
  })

  it("keeps the choice panel open after Use this note", async () => {
    await openSearch(makeMe.aNote.please())
    const row = screen.getByText("Another note").closest("li")!
    await fireEvent.click(row.querySelector("button")!)
    await flushPromises()
    expect(document.querySelector("dialog[open]")).not.toBeNull()
    expect(screen.getByText("Add a new relationship note")).toBeInTheDocument()
  })

  it("keeps search open after requesting a move", async () => {
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
      makeFolderHit(42, "Archive"),
    ])
    await openSearch(makeMe.aNote.please())
    await fireEvent.update(screen.getByPlaceholderText("Search"), "Archive")
    await fireEvent.click(await screen.findByText("Move Under"))
    await flushPromises()
    expect(document.querySelector("dialog[open]")).not.toBeNull()
    expect(screen.getByPlaceholderText("Search")).toBeInTheDocument()
    usePopups().popups.done(false)
    await flushPromises()
  })
})
