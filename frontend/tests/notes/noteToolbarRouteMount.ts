import makeMe from "donut-test-fixtures/makeMe"
import {
  mountNoteToolbar,
  type NoteToolbarMountOptions,
} from "@tests/notes/noteToolbarTestHelpers"
import {
  createRouter,
  createWebHistory,
  type RouteLocationNamedRaw,
} from "vue-router"
import routes from "@/routes/routes"

export async function mountNoteToolbarAt(
  locationForNoteId: (noteId: number) => RouteLocationNamedRaw,
  options: Omit<NoteToolbarMountOptions, "router"> = {}
) {
  const router = createRouter({
    history: createWebHistory(),
    routes,
  })
  const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
  await router.push(locationForNoteId(noteRealm.note.id))
  const wrapper = await mountNoteToolbar(noteRealm, { ...options, router })
  return { wrapper, router, noteRealm }
}
