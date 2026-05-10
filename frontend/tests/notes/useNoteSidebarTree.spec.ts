import { resolvedCreateParentFolderIdFrom } from "@/components/notes/useNoteSidebarTree"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"

describe("useNoteSidebarTree create context", () => {
  it("resolvedCreateParentFolderIdFrom prefers the sidebar active folder over the active note folder", () => {
    const realm = makeMe.aNoteRealm.inFolder(10, "From note").please()
    expect(
      resolvedCreateParentFolderIdFrom(
        makeMe.aFolderRealm.folder(99, "Pinned").please(),
        realm,
        true
      )
    ).toBe(99)
  })

  it("resolvedCreateParentFolderIdFrom uses the active note folder when sidebar active folder is null", () => {
    const realm = makeMe.aNoteRealm.inFolder(42, "Science").please()
    expect(resolvedCreateParentFolderIdFrom(null, realm, true)).toBe(42)
  })

  it("resolvedCreateParentFolderIdFrom is null when note context is not resolved", () => {
    const realm = makeMe.aNoteRealm.inFolder(7, "X").please()
    expect(resolvedCreateParentFolderIdFrom(null, realm, false)).toBe(null)
  })

  it("resolvedCreateParentFolderIdFrom is null for a note at the notebook root", () => {
    const realm = makeMe.aNoteRealm.please()
    expect(resolvedCreateParentFolderIdFrom(null, realm, true)).toBe(null)
  })
})
