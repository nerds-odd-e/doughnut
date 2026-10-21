import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import NoteShow from "@/components/notes/NoteShow.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { installMockResizeObserver } from "@tests/helpers/mockNoteToolbarNavWidth"
import type { NoteRealm } from "@generated/donut-backend-api"
import { afterEach, expect, it, onTestFinished, vi } from "vitest"
import { page } from "vitest/browser"

let wrapper: VueWrapper

afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ""
  document.documentElement.removeAttribute("data-theme")
  vi.restoreAllMocks()
})

const realmWith = (createdAt: string, updatedAt: string): NoteRealm => {
  const realm = makeMe.aNoteRealm
    .content("---\nimage: images/example.png\n---\n\nBody.")
    .createdAt(createdAt)
    .updatedAt(updatedAt)
    .please()
  return {
    ...realm,
    references: [
      makeMe.aNoteRealm.title("Referrer").please().note.noteTopology,
    ],
  }
}

async function mountNoteShow(realm: NoteRealm, signedIn: boolean) {
  installMockResizeObserver()
  mockSdkService(NoteController, "showNote", realm)
  const builder = helper.component(NoteShow).withRouter().withCleanStorage()
  if (signedIn) builder.withCurrentUser(makeMe.aUser.please())
  wrapper = builder
    .withProps({ noteId: realm.id })
    .mount({ attachTo: document.body })
  await flushPromises()
}

function expectFooterBetweenContentAndReferences() {
  const footer = wrapper.get('[data-testid="note-timestamps"]').element
  expect(footer.querySelectorAll("dt")).toHaveLength(2)
  expect(
    [...footer.querySelectorAll("dt")].map((dt) => dt.textContent)
  ).toEqual(["Created", "Last updated"])
  expect(
    [...footer.querySelectorAll("time")].map((time) => [
      time.getAttribute("datetime"),
      time.textContent,
    ])
  ).toEqual([
    ["2026-02-10T12:00:00Z", "February 10, 2026"],
    ["2026-05-15T12:00:00Z", "May 15, 2026"],
  ])
  const bounds = footer.getBoundingClientRect()
  const content = wrapper.get('[aria-label="Note content"]').element
  const image = wrapper.get("#note-image").element
  const references = wrapper
    .findAll("h3")
    .find((heading) => heading.text() === "References")!.element
  expect(bounds.top).toBeGreaterThanOrEqual(
    content.getBoundingClientRect().bottom
  )
  expect(bounds.top).toBeGreaterThanOrEqual(
    image.getBoundingClientRect().bottom
  )
  expect(bounds.bottom).toBeLessThanOrEqual(
    references.getBoundingClientRect().top
  )
  expect(
    image.compareDocumentPosition(footer) & Node.DOCUMENT_POSITION_FOLLOWING
  ).toBeTruthy()
  expect(
    footer.compareDocumentPosition(references) &
      Node.DOCUMENT_POSITION_FOLLOWING
  ).toBeTruthy()
}

it("shows Created and Last updated after the body and image, before references, in rich and raw modes", async () => {
  await mountNoteShow(
    realmWith("2026-02-10T12:00:00Z", "2026-05-15T12:00:00Z"),
    true
  )
  expectFooterBetweenContentAndReferences()

  ;(
    document.querySelector(
      'button[title="Edit as markdown (m)"]'
    ) as HTMLElement
  ).click()
  await flushPromises()
  expect(wrapper.find('[aria-label="Note content"] textarea').exists()).toBe(
    true
  )
  expectFooterBetweenContentAndReferences()
})

it("shows the footer on a read-only note", async () => {
  await mountNoteShow(
    realmWith("2026-02-10T12:00:00Z", "2026-05-15T12:00:00Z"),
    false
  )
  expectFooterBetweenContentAndReferences()
})

async function viewport(width: number) {
  const original = { width: window.innerWidth, height: window.innerHeight }
  onTestFinished(() => page.viewport(original.width, original.height))
  await page.viewport(width, 900)
}

it.each([
  [375, "light"],
  [375, "dark"],
  [1440, "light"],
  [1440, "dark"],
])(
  "keeps dates readable at %spx in the %s theme and emphasizes recent updates",
  async (width, theme) => {
    await viewport(width)
    document.documentElement.setAttribute("data-theme", theme)
    const now = new Date("2026-05-15T12:00:00Z").getTime()
    vi.spyOn(Date, "now").mockReturnValue(now)
    await mountNoteShow(
      realmWith(
        "2026-02-10T12:00:00Z",
        new Date(now - 60 * 60 * 1000).toISOString()
      ),
      false
    )
    const footer = wrapper.get('[data-testid="note-timestamps"]')
      .element as HTMLElement
    const container = footer.getBoundingClientRect()
    expect(footer.scrollWidth).toBeLessThanOrEqual(footer.clientWidth)
    for (const time of footer.querySelectorAll("time")) {
      const bounds = time.getBoundingClientRect()
      expect(bounds.left).toBeGreaterThanOrEqual(container.left)
      expect(bounds.right).toBeLessThanOrEqual(container.right)
      expect(bounds.right).toBeLessThanOrEqual(width)
    }
    const emphasis = () =>
      Number(
        getComputedStyle(wrapper.get('[data-testid="note-updated-at"]').element)
          .opacity
      )
    const recent = emphasis()
    wrapper.unmount()
    await mountNoteShow(
      realmWith("2026-02-10T12:00:00Z", "2025-05-15T12:00:00Z"),
      false
    )
    expect(recent).toBeGreaterThan(emphasis())
  }
)
