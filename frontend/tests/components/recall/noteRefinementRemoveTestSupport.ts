import usePopups from "@/components/commons/Popups/usePopups"
import type { NoteRefinementLayoutItem } from "@generated/doughnut-backend-api"
import { flushPromises } from "@vue/test-utils"
import { expect } from "vitest"
import {
  mountNoteRefinement,
  mountNoteRefinementWithLayoutReady,
  selectRefinementLayoutItem,
} from "./noteRefinementTestSupport"

export const sampleNestedLayout = (): NoteRefinementLayoutItem[] => [
  {
    id: "p1",
    text: "Parent point",
    alreadyExtracted: false,
    children: [
      {
        id: "p1-1",
        text: "Child point A",
        alreadyExtracted: false,
        children: [],
      },
      {
        id: "p1-2",
        text: "Child point B",
        alreadyExtracted: true,
        children: [],
      },
    ],
  },
  {
    id: "p2",
    text: "Separate point",
    alreadyExtracted: false,
    children: [],
  },
]

export async function mountNestedLayoutWithIndeterminateParentSelection() {
  const layout = sampleNestedLayout()
  const wrapper = await mountNoteRefinementWithLayoutReady(layout)
  await selectRefinementLayoutItem(wrapper, "p1")
  await selectRefinementLayoutItem(wrapper, "p1-2", false)
  return { layout, wrapper }
}

export async function openRemoveRefinementConfirmDialog(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper
    .find('[data-test-id="remove-refinement-layout"]')
    .trigger("click")
  await flushPromises()
}

export function expectRemoveConfirmPopup() {
  const popups = usePopups().popups.peek()
  expect(popups).toHaveLength(1)
  expect(popups[0]!.type).toBe("confirm")
  expect(popups[0]!.message).toContain("remove")
}

export async function clickRemoveRefinementLayout(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await openRemoveRefinementConfirmDialog(wrapper)
  usePopups().popups.done(true)
  await flushPromises()
}
