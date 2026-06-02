import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import type { Router } from "vue-router"
import type { VueWrapper } from "@vue/test-utils"
import { flushPromises } from "@vue/test-utils"

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
  mockSdkService(
    NoteController,
    "getNoteInfo",
    makeMe.aNoteRecallInfo
      .recallSetting({
        level: 0,
        rememberSpelling: false,
        skipMemoryTracking: false,
      })
      .please()
  )
}

type NoteToolbarMountOptions = {
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
