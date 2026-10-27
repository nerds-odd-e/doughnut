import { dispatchArrowKey } from "@tests/helpers/searchDialogKeyboardTestSupport"
import { fireEvent } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"
import { setupCatalogKeyboardTest } from "./notebookCatalogKeyboardTestSupport"

describe.each(["list", "grid"])(
  "catalog group keyboard in %s layout",
  (layout) => {
    const { mountCatalog, expectFocusedEntry } =
      setupCatalogKeyboardTest(layout)

    it("steps into a group and back from its members while preserving the displayed journey", async () => {
      const group = makeMe.notebookCatalogGroup
        .name("A Group")
        .names("Alpha", "Beta")
        .please()
      await mountCatalog(
        makeMe.notebookCatalog.entry(group).notebook("C Outside").please()
      )
      dispatchArrowKey("ArrowDown")
      expectFocusedEntry("A Group")
      await fireEvent.keyDown(document.activeElement!, { key: "ArrowLeft" })
      expectFocusedEntry("A Group")
      await fireEvent.keyDown(document.activeElement!, { key: "ArrowRight" })
      expectFocusedEntry("Alpha")
      await fireEvent.keyDown(document.activeElement!, { key: "ArrowRight" })
      expectFocusedEntry("Alpha")
      dispatchArrowKey("ArrowDown")
      expectFocusedEntry("Beta")
      await fireEvent.keyDown(document.activeElement!, { key: "ArrowLeft" })
      expectFocusedEntry("A Group")
      dispatchArrowKey("ArrowDown")
      expectFocusedEntry("Alpha")
      dispatchArrowKey("ArrowDown")
      expectFocusedEntry("Beta")
      dispatchArrowKey("ArrowDown")
      expectFocusedEntry("C Outside")
      await fireEvent.keyDown(document.activeElement!, { key: "ArrowLeft" })
      expectFocusedEntry("C Outside")
      await fireEvent.keyDown(document.activeElement!, { key: "ArrowRight" })
      expectFocusedEntry("C Outside")
    })

    it("enters the first currently displayed member after filtering", async () => {
      const group = makeMe.notebookCatalogGroup
        .name("Group")
        .names("Alpha", "Beta")
        .please()
      const input = await mountCatalog([group])
      await input.setValue("beta")
      dispatchArrowKey("ArrowDown")
      expectFocusedEntry("Group")
      await fireEvent.keyDown(document.activeElement!, { key: "ArrowRight" })
      expectFocusedEntry("Beta")
    })

    it("keeps empty and preview-limited groups available through their group links", async () => {
      const empty = makeMe.notebookCatalogGroup.name("A Empty").please()
      const preview = makeMe.notebookCatalogGroup
        .name("B Preview")
        .names("One", "Two", "Three", "Four", "Five")
        .please()
      await mountCatalog([empty, preview])
      dispatchArrowKey("ArrowDown")
      expect(expectFocusedEntry("A Empty").props("to")).toEqual({
        name: "notebookGroup",
        params: { groupId: empty.id },
      })
      await fireEvent.keyDown(document.activeElement!, { key: "ArrowRight" })
      expectFocusedEntry("A Empty")
      dispatchArrowKey("ArrowDown")
      expect(expectFocusedEntry("B Preview").props("to")).toEqual({
        name: "notebookGroup",
        params: { groupId: preview.id },
      })
      await fireEvent.keyDown(document.activeElement!, { key: "ArrowRight" })
      expectFocusedEntry("Five")
    })
  }
)
