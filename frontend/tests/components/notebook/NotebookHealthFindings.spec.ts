import type { HealthFindingGroup } from "@generated/doughnut-backend-api"
import NotebookHealthFindings from "@/components/notebook/NotebookHealthFindings.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import helper from "@tests/helpers"
import { describe, expect, it } from "vitest"
import { reportFixture } from "./notebookHealthPanelTestSupport"

function mountFindings(groups: HealthFindingGroup[] = reportFixture.groups) {
  return helper.component(NotebookHealthFindings).withProps({ groups }).mount()
}

function linkTo(el: { attributes: (name: string) => string | undefined }) {
  return JSON.parse(el.attributes("to") ?? "{}") as {
    name?: string
    params?: { noteId?: string | number }
  }
}

describe("NotebookHealthFindings dead wiki links", () => {
  it("shows note and token findings without nested collapse when section is open", () => {
    const wrapper = mountFindings()
    const deadLinks = wrapper.get(
      '[data-testid="notebook-health-group-dead_wiki_links"]'
    )

    expect(deadLinks.findAll('input[type="checkbox"]').length).toBe(1)
    expect(
      deadLinks.find('[data-testid="notebook-health-dead-link-note"]').exists()
    ).toBe(true)
    expect(deadLinks.text()).toContain("Source")
    expect(deadLinks.text()).toContain("Missing")
  })

  it("links note title and token items to note show by noteId", () => {
    const wrapper = mountFindings()
    const noteTitle = wrapper.get(
      '[data-testid="notebook-health-dead-link-note-title"]'
    )
    const token = wrapper.get('[data-testid="notebook-health-dead-link-token"]')

    expect(noteTitle.text()).toContain("Source")
    expect(linkTo(noteTitle)).toEqual(noteShowLocation(9))
    expect(token.text()).toContain("Missing")
    expect(linkTo(token)).toEqual(noteShowLocation(9))
  })

  it("keeps nested collapse for non-dead-link child groups", () => {
    const groups = [
      {
        ruleId: "empty_folders",
        title: "Empty folders",
        severity: "warning" as const,
        autoFixable: true,
        items: [],
        children: [
          {
            ruleId: "empty_folders",
            title: "Nested empty",
            severity: "warning" as const,
            autoFixable: true,
            items: [{ folderId: 3, label: "Shell" }],
          },
        ],
      },
    ]
    const wrapper = mountFindings(groups)
    const group = wrapper.get(
      '[data-testid="notebook-health-group-empty_folders"]'
    )
    const nested = group
      .findAll(".daisy-collapse")
      .find((node) => node.text().includes("Nested empty"))
    expect(nested).toBeDefined()
    expect(nested!.find('input[type="checkbox"]').exists()).toBe(true)
    expect(
      group.find('[data-testid="notebook-health-dead-link-note"]').exists()
    ).toBe(false)
  })
})
