import type { User } from "@generated/doughnut-backend-api"
import {
  NotebookHealthController,
  UserController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NotebookHealthPanel from "@/components/notebook/NotebookHealthPanel.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import type { Ref } from "vue"

const reportFixture = {
  groups: [
    {
      ruleId: "empty_folders",
      title: "Empty folders",
      severity: "warning" as const,
      autoFixable: true,
      items: [{ folderId: 1, label: "Empty Shell" }],
    },
    {
      ruleId: "readme_only_folders",
      title: "Readme-only folders",
      severity: "warning" as const,
      autoFixable: false,
      items: [],
    },
    {
      ruleId: "dead_wiki_links",
      title: "Dead wiki links",
      severity: "warning" as const,
      autoFixable: false,
      items: [],
      children: [
        {
          ruleId: "dead_wiki_links",
          title: "Source",
          severity: "warning" as const,
          autoFixable: false,
          items: [{ noteId: 9, label: "Missing", wikiLinkToken: "Missing" }],
        },
      ],
    },
  ],
}

describe("NotebookHealthPanel", () => {
  const notebookId = 42
  let lintSpy: ReturnType<typeof mockSdkService>
  let updateUserSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    vi.restoreAllMocks()
    lintSpy = mockSdkService(NotebookHealthController, "lint", reportFixture)
    updateUserSpy = mockSdkService(
      UserController,
      "updateUser",
      makeMe.aUser.please()
    )
  })

  function mountPanel(user?: User) {
    const builder = helper
      .component(NotebookHealthPanel)
      .withProps({ notebookId })
    if (user) {
      builder.withCurrentUser(user)
    }
    return builder.mount()
  }

  function providedCurrentUser(wrapper: VueWrapper) {
    return wrapper.vm.$.appContext.provides.currentUser as Ref<User | undefined>
  }

  function removeEmptyFoldersCheckbox(wrapper: VueWrapper) {
    return wrapper.get(
      '[data-testid="notebook-health-remove-empty-folders"] input[type="checkbox"]'
    )
  }

  it("shows idle prompt and action bar without calling lint on mount", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.find('[data-testid="notebook-health-idle"]').exists()).toBe(
      true
    )
    expect(wrapper.find('[data-testid="notebook-health-run"]').exists()).toBe(
      true
    )
    expect(
      wrapper
        .find('[data-testid="notebook-health-remove-empty-folders"]')
        .exists()
    ).toBe(true)
    expect(lintSpy).not.toHaveBeenCalled()
  })

  it("runs bodyless path-only lint and shows report groups", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()

    expect(lintSpy).toHaveBeenCalledOnce()
    expect(lintSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: notebookId },
      })
    )
    const callOptions = lintSpy.mock.calls[0]?.[0] as Record<string, unknown>
    expect(callOptions).not.toHaveProperty("body")

    expect(wrapper.find('[data-testid="notebook-health-idle"]').exists()).toBe(
      false
    )
    const findings = wrapper.find('[data-testid="notebook-health-findings"]')
    expect(findings.exists()).toBe(true)
    expect(
      findings
        .find('[data-testid="notebook-health-group-empty_folders"]')
        .exists()
    ).toBe(true)
    expect(
      findings
        .find('[data-testid="notebook-health-group-readme_only_folders"]')
        .exists()
    ).toBe(true)
    expect(
      findings
        .find('[data-testid="notebook-health-group-dead_wiki_links"]')
        .exists()
    ).toBe(true)
    expect(findings.text()).toContain("Empty Shell")
    expect(findings.text()).toContain("Source")
    expect(findings.text()).toContain("Missing")
  })

  it("keeps lint path-only when Remove empty folders is checked and has no Fix control", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    const checkbox = wrapper.get(
      '[data-testid="notebook-health-remove-empty-folders"] input[type="checkbox"]'
    )
    await checkbox.setValue(true)
    await flushPromises()

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()

    expect(lintSpy).toHaveBeenCalledOnce()
    expect(lintSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: notebookId },
      })
    )
    const callOptions = lintSpy.mock.calls[0]?.[0] as Record<string, unknown>
    expect(callOptions).not.toHaveProperty("body")

    expect(wrapper.text()).not.toMatch(/\bFix\b/)
    expect(wrapper.text()).not.toMatch(/\bApply\b/)
    expect(wrapper.find('[data-testid="notebook-health-fix"]').exists()).toBe(
      false
    )
    expect(wrapper.find('[data-testid="notebook-health-apply"]').exists()).toBe(
      false
    )
  })

  it("expands groups with findings and collapses empty groups by default", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()

    const emptyFolders = wrapper.get(
      '[data-testid="notebook-health-group-empty_folders"]'
    )
    const readmeOnly = wrapper.get(
      '[data-testid="notebook-health-group-readme_only_folders"]'
    )
    const deadLinks = wrapper.get(
      '[data-testid="notebook-health-group-dead_wiki_links"]'
    )

    expect(emptyFolders.text()).toContain("Empty folders (1)")
    expect(readmeOnly.text()).toContain("Readme-only folders (0)")
    expect(deadLinks.text()).toContain("Dead wiki links (1)")

    expect(
      (emptyFolders.get('input[type="checkbox"]').element as HTMLInputElement)
        .checked
    ).toBe(true)
    expect(
      (readmeOnly.get('input[type="checkbox"]').element as HTMLInputElement)
        .checked
    ).toBe(false)
    expect(
      (deadLinks.get('input[type="checkbox"]').element as HTMLInputElement)
        .checked
    ).toBe(true)
  })

  it("shows No findings when an empty group is expanded", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()

    const readmeOnly = wrapper.get(
      '[data-testid="notebook-health-group-readme_only_folders"]'
    )
    const toggle = readmeOnly.get('input[type="checkbox"]')
    await toggle.setValue(true)
    await flushPromises()

    expect(readmeOnly.text()).toContain("No findings")
  })

  it("nests dead wiki links by note title with token leaf labels", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()

    const deadLinks = wrapper.get(
      '[data-testid="notebook-health-group-dead_wiki_links"]'
    )
    const nestedCollapse = deadLinks
      .findAll(".daisy-collapse")
      .find((node) => node.text().includes("Source"))
    expect(nestedCollapse).toBeDefined()
    expect(nestedCollapse!.text()).toContain("Source")
    expect(nestedCollapse!.text()).toContain("Missing")
    expect(nestedCollapse!.find("a").exists()).toBe(false)
    expect(wrapper.html()).not.toMatch(/v-html|innerHTML/)
  })

  it("prefills Remove empty folders from currentUser without calling lint", async () => {
    const wrapper = mountPanel({
      ...makeMe.aUser.please(),
      healthRemoveEmptyFoldersDefault: true,
    })
    await flushPromises()

    expect(
      (removeEmptyFoldersCheckbox(wrapper).element as HTMLInputElement).checked
    ).toBe(true)
    expect(lintSpy).not.toHaveBeenCalled()
    expect(updateUserSpy).not.toHaveBeenCalled()
  })

  it("prefills Remove empty folders unchecked when preference is missing or false", async () => {
    const withoutPreference = makeMe.aUser.please()
    delete withoutPreference.healthRemoveEmptyFoldersDefault

    const missingWrapper = mountPanel(withoutPreference)
    await flushPromises()
    expect(
      (removeEmptyFoldersCheckbox(missingWrapper).element as HTMLInputElement)
        .checked
    ).toBe(false)
    expect(lintSpy).not.toHaveBeenCalled()

    const falseWrapper = mountPanel({
      ...makeMe.aUser.please(),
      healthRemoveEmptyFoldersDefault: false,
    })
    await flushPromises()
    expect(
      (removeEmptyFoldersCheckbox(falseWrapper).element as HTMLInputElement)
        .checked
    ).toBe(false)
    expect(lintSpy).not.toHaveBeenCalled()
  })

  it("shows Save as defaults and does not PATCH when only toggling the checkbox", async () => {
    const user = {
      ...makeMe.aUser.please(),
      healthRemoveEmptyFoldersDefault: false,
    }
    const wrapper = mountPanel(user)
    await flushPromises()

    expect(
      wrapper.find('[data-testid="notebook-health-save-defaults"]').exists()
    ).toBe(true)
    expect(wrapper.text()).toContain("Save as defaults")

    await removeEmptyFoldersCheckbox(wrapper).setValue(true)
    await flushPromises()

    expect(updateUserSpy).not.toHaveBeenCalled()
    expect(lintSpy).not.toHaveBeenCalled()
  })

  it("saves full UserDTO-shaped defaults without calling lint and updates currentUser", async () => {
    const user = {
      ...makeMe.aUser.please(),
      name: "Health Owner",
      dailyAssimilationCount: 12,
      spaceIntervals: "0, 1, 2",
      healthRemoveEmptyFoldersDefault: false,
    }
    const updatedUser = {
      ...user,
      healthRemoveEmptyFoldersDefault: true,
    }
    updateUserSpy = mockSdkService(UserController, "updateUser", updatedUser)

    const wrapper = mountPanel(user)
    await flushPromises()

    await removeEmptyFoldersCheckbox(wrapper).setValue(true)
    await flushPromises()
    await wrapper
      .get('[data-testid="notebook-health-save-defaults"]')
      .trigger("click")
    await flushPromises()

    expect(updateUserSpy).toHaveBeenCalledOnce()
    expect(updateUserSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { user: user.id },
        body: {
          name: "Health Owner",
          dailyAssimilationCount: 12,
          spaceIntervals: "0, 1, 2",
          healthRemoveEmptyFoldersDefault: true,
        },
      })
    )
    expect(lintSpy).not.toHaveBeenCalled()
    expect(providedCurrentUser(wrapper).value).toEqual(updatedUser)
  })
})
