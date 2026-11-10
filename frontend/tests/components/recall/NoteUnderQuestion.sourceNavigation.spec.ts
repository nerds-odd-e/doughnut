import AnsweredQuestionComponent from "@/components/recall/AnsweredQuestionComponent.vue"
import MemoryTrackerPageView from "@/pages/MemoryTrackerPageView.vue"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import helper, {
  mockNotebookGetForNoteRealm,
  mockSdkService,
  productionRouterAt,
  wrapSdkResponse,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { expect, it } from "vitest"
import {
  notePropertyItemLocation,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  mainNoteContentEl,
  renderNoteShowPageWithoutSidebar,
} from "../../pages/noteShowPageTestSupport"

const selected =
  '[data-test="assimilation-mode-row-UNDERSTANDING"][data-property-value-focused="true"]'

it.each([
  {
    label: "first duplicate",
    value: "[[run]]",
    values: ["[[run]]", "[[run]]", "[[walk]]"],
    index: 0,
  },
  {
    label: "raw alias",
    value: "[[Verbs/run|running]]",
    values: ["[[walk]]", "[[Verbs/run|running]]"],
    index: 1,
  },
  {
    label: "reordered distinct value",
    value: "[[run]]",
    values: ["[[walk]]", "[[jog]]", "[[run]]"],
    index: 2,
  },
])(
  "follows the reviewed source into the note page: $label",
  async ({ value, values, index }) => {
    const realm = makeMe.aNoteRealm
      .title("Sentence")
      .content(
        `---\nexample of: ${JSON.stringify(values)}\n---\nReadable body.`
      )
      .please()
    mockSdkService(NoteController, "showNote", realm)
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
    mockNotebookGetForNoteRealm(realm)
    const router = await productionRouterAt({ name: "root" })
    const wrapper = helper
      .component(AnsweredQuestionComponent)
      .withCleanStorage()
      .withRealRouter(router)
      .withProps({
        answeredQuestion: makeMe.anAnsweredQuestion
          .withNote(realm.note)
          .withPropertyKey("example of", value)
          .please(),
        conversationButton: false,
      })
      .mount()
    await flushPromises()
    await wrapper.get('[data-testid="reviewed-property-link"]').trigger("click")
    await flushPromises()
    expect(router.currentRoute.value).toMatchObject(
      notePropertyItemLocation(realm.id, "example of", { value, occurrence: 1 })
    )
    wrapper.unmount()
    await renderNoteShowPageWithoutSidebar(router, realm.id)
    const content = mainNoteContentEl()!
    const rows = content.querySelectorAll(
      '[data-test="assimilation-mode-row-UNDERSTANDING"]'
    )
    expect(rows[index]!.getAttribute("data-property-value-focused")).toBe(
      "true"
    )
    expect(content.querySelectorAll(selected)).toHaveLength(1)
    const focus = content.querySelector(selected) as HTMLElement
    expect(focus.getAttribute("data-property-value")).toBe(value)
    expect(focus.checkVisibility()).toBe(true)
    expect(
      content
        .querySelector('[data-testid="rich-note-property-panel"]')!
        .checkVisibility()
    ).toBe(true)
    expect(content.textContent).toContain("Readable body.")
  }
)

it("opens a readable current note with stale reviewed-item feedback", async () => {
  const realm = makeMe.aNoteRealm
    .content("---\nexample of: [walk, jog]\n---\nReadable body.")
    .please()
  mockSdkService(NoteController, "showNote", realm)
  mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  mockNotebookGetForNoteRealm(realm)
  const router = await productionRouterAt({ name: "root" })
  const wrapper = helper
    .component(AnsweredQuestionComponent)
    .withCleanStorage()
    .withRealRouter(router)
    .withProps({
      answeredQuestion: makeMe.anAnsweredQuestion
        .withNote(realm.note)
        .withPropertyKey("example of", "run")
        .please(),
      conversationButton: false,
    })
    .mount()
  await flushPromises()
  await wrapper.get('[data-testid="reviewed-property-link"]').trigger("click")
  await flushPromises()
  wrapper.unmount()
  await renderNoteShowPageWithoutSidebar(router, realm.id)
  const content = mainNoteContentEl()!
  expect(
    content.querySelector('[data-testid="rich-note-property-item-not-found"]')!
      .textContent
  ).toContain('Item "run" (occurrence 1) not found in property "example of"')
  expect(content.textContent).toContain("Readable body.")
  expect(content.querySelectorAll(selected)).toHaveLength(0)
  expect(
    content
      .querySelector('[data-testid="rich-note-property-panel"]')!
      .checkVisibility()
  ).toBe(true)
})

it("follows a property-only tracker into the expanded authored key", async () => {
  const realm = makeMe.aNoteRealm
    .content("---\ntopic: movement\n---\nBody.")
    .please()
  mockSdkService(NoteController, "showNote", realm)
  mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  mockNotebookGetForNoteRealm(realm)
  const router = await productionRouterAt({ name: "root" })
  const wrapper = helper
    .component(MemoryTrackerPageView)
    .withCleanStorage()
    .withRealRouter(router)
    .withProps({
      memoryTracker: makeMe.aMemoryTracker
        .ofNote(realm)
        .withPropertyKey("topic")
        .please(),
      memoryTrackerId: 1,
      recallHistory: [],
    })
    .mount()
  await flushPromises()
  await wrapper.get('[data-testid="reviewed-property-link"]').trigger("click")
  await flushPromises()
  expect(router.currentRoute.value).toMatchObject(
    notePropertyLocation(realm.id, "topic")
  )
  wrapper.unmount()
  await renderNoteShowPageWithoutSidebar(router, realm.id)
  expect(
    mainNoteContentEl()!
      .querySelector('[data-testid="rich-note-property-panel"]')!
      .checkVisibility()
  ).toBe(true)
  expect(
    mainNoteContentEl()!.querySelector('[data-property-value-focused="true"]')!
      .textContent
  ).toContain("movement")
})

it("switches result path, title and reviewed item destinations together", async () => {
  const first = makeMe.aNoteRealm
    .title("First note")
    .notebookName("First notebook")
    .please()
  const second = makeMe.aNoteRealm
    .title("Second note")
    .notebookName("Second notebook")
    .please()
  const read = mockSdkService(NoteController, "showNote", first)
  const router = await productionRouterAt({ name: "root" })
  const wrapper = helper
    .component(AnsweredQuestionComponent)
    .withCleanStorage()
    .withRealRouter(router)
    .withProps({
      answeredQuestion: makeMe.anAnsweredQuestion
        .withNote(first.note)
        .withPropertyKey("topic", "old")
        .please(),
      conversationButton: false,
    })
    .mount()
  await flushPromises()
  read.mockResolvedValue(wrapSdkResponse(second))
  await wrapper.setProps({
    answeredQuestion: makeMe.anAnsweredQuestion
      .withNote(second.note)
      .withPropertyKey("example of", "new")
      .please(),
  })
  await flushPromises()
  expect(wrapper.get(".breadcrumb-wrapper").text()).toContain("Second notebook")
  await wrapper.get('[data-testid="reviewed-property-link"]').trigger("click")
  await flushPromises()
  expect(router.currentRoute.value).toMatchObject(
    notePropertyItemLocation(second.id, "example of", {
      value: "new",
      occurrence: 1,
    })
  )
  await wrapper
    .findAll(".breadcrumb-wrapper a")
    .find((a) => a.text() === "Second note")!
    .trigger("click")
  await flushPromises()
  expect(router.currentRoute.value).toMatchObject(noteShowLocation(second.id))
  wrapper.unmount()
})
