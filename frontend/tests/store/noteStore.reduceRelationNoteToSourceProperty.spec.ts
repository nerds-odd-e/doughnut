import {
  NoteController,
  RelationController,
} from "@generated/donut-backend-api/sdk.gen"
import type { Router } from "vue-router"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { sidebarStructuralRefreshKey } from "@/components/notes/sidebarStructuralRefresh"
import { resetNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { describe, it, expect, vi, beforeEach } from "vitest"

describe("noteStore reduceRelationNoteToSourceProperty", () => {
  const routerReplace = vi.fn()
  const router = { replace: routerReplace } as unknown as Router

  beforeEach(() => vi.clearAllMocks())

  it("calls the reduce endpoint, removes the relationship note from cache, navigates to the source, refreshes the sidebar, and records no undo", async () => {
    const noteStore = resetNoteStore()
    const relationRealm = makeMe.aNoteRealm
      .title("Moon a part of Earth")
      .please()
    noteStore.refreshNoteRealm(relationRealm)
    const relationRef = noteStore.refOfNoteRealm(relationRealm.id)
    const sourceRealm = makeMe.aNoteRealm.title("Moon").please()
    const reduceSpy = mockSdkService(
      RelationController,
      "reduceToSourceProperty",
      sourceRealm
    )
    const refreshKeyBefore = sidebarStructuralRefreshKey.value

    await noteStore.reduceRelationNoteToSourceProperty(router, relationRealm.id)

    expect(reduceSpy).toHaveBeenCalledWith({
      path: { relationNote: relationRealm.id },
    })
    expect(routerReplace).toHaveBeenCalledWith(noteShowLocation(sourceRealm.id))
    expect(relationRef.value).toBeUndefined()
    expect(noteStore.refOfNoteRealm(relationRealm.id).value).toBeFalsy()
    expect(noteStore.peekUndo()).toBeNull()
    expect(sidebarStructuralRefreshKey.value).toBe(refreshKeyBefore + 1)

    const showNoteSpy = mockSdkService(
      NoteController,
      "showNote",
      relationRealm
    )
    noteStore.getNoteRealmRefAndLoadWhenNeeded(relationRealm.id)
    noteStore.getNoteRealmRefAndLoadWhenNeeded(relationRealm.id)
    expect(showNoteSpy).toHaveBeenCalledTimes(0)
  })
})
