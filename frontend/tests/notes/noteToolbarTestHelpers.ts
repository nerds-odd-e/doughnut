import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import type { NoteRealm } from "@generated/donut-backend-api"
import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { notebookSidebarClosedPlugin } from "@tests/helpers/notebookSidebarTestProvide"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useNoteToolbarPanel } from "@/composables/useNoteToolbarPanel"
import type { Router } from "vue-router"
import { flushPromises, type VueWrapper } from "@vue/test-utils"

export function noteToolbarAction(wrapper: VueWrapper, title: string) {
  return wrapper.find(`[data-note-toolbar] [title="${title}"]`)
}

export function overflowMenuItem(title: string) {
  return document.querySelector<HTMLButtonElement>(
    `[data-dropdown-portal-panel] button[title="${title}"]`
  )
}

export async function openNoteToolbarOverflowMenu(wrapper: VueWrapper) {
  await noteToolbarAction(wrapper, noteMoreOptionsTitles.overflowMenu).trigger(
    "click"
  )
  await flushPromises()
}

export function resetNoteToolbarTestState() {
  useAssimilationView().dismiss()
  useNoteToolbarPanel().close()
}

export function noteToolbarProps(
  noteRealm: NoteRealm,
  overrides: Record<string, unknown> = {}
) {
  return {
    note: noteRealm.note,
    notebookId: noteRealm.notebookRealm.notebook.id,
    activeNoteRealm: noteRealm,
    ...overrides,
  }
}

export function mockDefaultNoteRecallInfo() {
  mockSdkService(NoteController, "getNoteInfo", makeMe.aNoteRecallInfo.please())
}

export type NoteToolbarMountOptions = {
  router?: Router
  plugin?: Parameters<
    ReturnType<typeof helper.component<typeof NoteToolbar>>["withPlugin"]
  >[0]
  propsOverrides?: Record<string, unknown>
}

export async function mountNoteToolbar(
  noteRealm: NoteRealm,
  options: NoteToolbarMountOptions = {}
): Promise<VueWrapper> {
  mockDefaultNoteRecallInfo()
  let builder = helper.component(NoteToolbar).withCleanStorage()
  builder = options.router
    ? builder.withRouter(options.router)
    : builder.withRouter()
  builder = builder.withProps(
    noteToolbarProps(noteRealm, options.propsOverrides ?? {})
  )

  if (options.plugin) {
    builder = builder.withPlugin(options.plugin)
  }

  const wrapper = builder.mount({ attachTo: document.body })
  await flushPromises()
  return wrapper
}

export async function mountOverflowToolbar(): Promise<VueWrapper> {
  return mountNoteToolbar(makeMe.aNoteRealm.title("Dummy Title").please(), {
    plugin: notebookSidebarClosedPlugin(),
  })
}
