import usePopups from "@/components/commons/Popups/usePopups"
import type { NoteRefinementLayoutItem } from "@generated/donut-backend-api"
import { flushPromises } from "@vue/test-utils"
import { expect } from "vitest"
import { selectRefinementLayoutItems } from "./noteRefinementLayoutFixtures"
import {
  mountNoteRefinement,
  mountNoteRefinementWithLayoutReady,
} from "./noteRefinementTestSupport"

export const sampleNestedLayout = (): NoteRefinementLayoutItem[] => [
  {
    id: "p1",
    text: "Parent point",
    alreadyExtracted: false,
    ledToQuestion: false,
    children: [
      {
        id: "p1-1",
        text: "Child point A",
        alreadyExtracted: false,
        ledToQuestion: false,
        children: [],
      },
      {
        id: "p1-2",
        text: "Child point B",
        alreadyExtracted: true,
        ledToQuestion: false,
        children: [],
      },
    ],
  },
  {
    id: "p2",
    text: "Separate point",
    alreadyExtracted: false,
    ledToQuestion: false,
    children: [],
  },
]

export async function mountNestedLayoutWithIndeterminateParentSelection() {
  const layout = sampleNestedLayout()
  const wrapper = await mountNoteRefinementWithLayoutReady(layout)
  await selectRefinementLayoutItems(wrapper, "p1", {
    itemId: "p1-2",
    checked: false,
  })
  return { layout, wrapper }
}

export async function openRemoveRefinementConfirmDialog(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper
    .find('[data-test-id="remove-refinement-layout"]')
    .trigger("click")
}

export function expectRemoveConfirmPopup() {
  const popups = usePopups().popups.peek()
  expect(popups).toHaveLength(1)
  expect(popups[0]!.type).toBe("confirm")
  expect(popups[0]!.message).toContain("selected refinement layout item(s)")
}

export async function clickRemoveRefinementLayout(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await openRemoveRefinementConfirmDialog(wrapper)
  usePopups().popups.done(true)
  await flushPromises()
}
