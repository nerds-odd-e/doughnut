import type { User } from "@generated/doughnut-backend-api"
import {
  NotebookHealthController,
  UserController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NotebookHealthPanel from "@/components/notebook/NotebookHealthPanel.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
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

const emptyReportFixture = {
  groups: [
    {
      ruleId: "empty_folders",
      title: "Empty folders",
      severity: "warning" as const,
      autoFixable: true,
      items: [],
    },
    {
      ruleId: "readme_only_folders",
      title: "Readme-only folders",
      severity: "warning" as const,
      autoFixable: false,
      items: [{ folderId: 2, label: "Readme Only Shell" }],
    },
  ],
}

describe("NotebookHealthPanel", () => {
  const notebookId = 42
  let lintSpy: ReturnType<typeof mockSdkService>
  let fixSpy: ReturnType<typeof mockSdkService>
  let updateUserSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    vi.restoreAllMocks()
    lintSpy = mockSdkService(NotebookHealthController, "lint", reportFixture)
    fixSpy = mockSdkService(NotebookHealthController, "fix", undefined)
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

  function fixButton(wrapper: VueWrapper) {
    return wrapper.get('[data-testid="notebook-health-fix"]')
  }

  async function runLintWithCheckbox(wrapper: VueWrapper, checked: boolean) {
    await removeEmptyFoldersCheckbox(wrapper).setValue(checked)
    await flushPromises()
    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()
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

  it("keeps lint path-only when Remove empty folders is checked; Fix is separate gated control", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await runLintWithCheckbox(wrapper, true)

    expect(lintSpy).toHaveBeenCalledOnce()
    expect(lintSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: notebookId },
      })
    )
    const callOptions = lintSpy.mock.calls[0]?.[0] as Record<string, unknown>
    expect(callOptions).not.toHaveProperty("body")
    expect(fixSpy).not.toHaveBeenCalled()

    expect(wrapper.find('[data-testid="notebook-health-fix"]').exists()).toBe(
      true
    )
    expect(wrapper.find('[data-testid="notebook-health-apply"]').exists()).toBe(
      false
    )
    expect(wrapper.text()).not.toMatch(/\bApply\b/)
  })

  it("shows disabled Fix with fallback label before report and when checkbox is unchecked", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    const idleFix = fixButton(wrapper)
    expect((idleFix.element as HTMLButtonElement).disabled).toBe(true)
    expect(idleFix.text()).toBe("Remove empty folders")

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()

    const uncheckedFix = fixButton(wrapper)
    expect((uncheckedFix.element as HTMLButtonElement).disabled).toBe(true)
    expect(uncheckedFix.text()).toBe("Remove 1 empty folders")
  })

  it("disables Fix when checkbox is checked but empty_folders has no items", async () => {
    lintSpy = mockSdkService(
      NotebookHealthController,
      "lint",
      emptyReportFixture
    )
    const wrapper = mountPanel()
    await flushPromises()

    await runLintWithCheckbox(wrapper, true)

    const fix = fixButton(wrapper)
    expect((fix.element as HTMLButtonElement).disabled).toBe(true)
    expect(fix.text()).toBe("Remove empty folders")
  })

  it("enables Fix with count label when checkbox is checked and empty_folders has items", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await runLintWithCheckbox(wrapper, true)

    const fix = fixButton(wrapper)
    expect((fix.element as HTMLButtonElement).disabled).toBe(false)
    expect(fix.text()).toBe("Remove 1 empty folders")
    expect(fix.classes()).toContain("daisy-btn-secondary")
    expect(fix.classes()).not.toContain("daisy-btn-primary")
  })

  it("calls fix then re-lints and replaces report on success", async () => {
    const postFixReport = emptyReportFixture
    lintSpy = mockSdkService(NotebookHealthController, "lint", reportFixture)
    lintSpy.mockResolvedValueOnce(wrapSdkResponse(reportFixture))
    lintSpy.mockResolvedValueOnce(wrapSdkResponse(postFixReport))

    const wrapper = mountPanel()
    await flushPromises()

    await runLintWithCheckbox(wrapper, true)
    expect(wrapper.text()).toContain("Empty Shell")

    await fixButton(wrapper).trigger("click")
    await flushPromises()

    expect(fixSpy).toHaveBeenCalledOnce()
    expect(fixSpy).toHaveBeenCalledWith({
      path: { notebook: notebookId },
      body: { removeEmptyFolders: true },
    })
    const fixCall = fixSpy.mock.calls[0]?.[0] as
      | { body: Record<string, unknown> }
      | undefined
    expect(fixCall).toBeDefined()
    expect(Object.keys(fixCall!.body)).toEqual(["removeEmptyFolders"])

    expect(lintSpy).toHaveBeenCalledTimes(2)
    expect(fixSpy.mock.invocationCallOrder[0]).toBeLessThan(
      lintSpy.mock.invocationCallOrder[1]!
    )

    expect(wrapper.text()).not.toContain("Empty Shell")
    expect(wrapper.text()).toContain("Readme Only Shell")
  })

  it("keeps prior report and does not re-lint when fix fails", async () => {
    fixSpy.mockResolvedValue(wrapSdkError("fix failed"))

    const wrapper = mountPanel()
    await flushPromises()

    await runLintWithCheckbox(wrapper, true)
    expect(wrapper.text()).toContain("Empty Shell")

    await fixButton(wrapper).trigger("click")
    await flushPromises()

    expect(fixSpy).toHaveBeenCalledOnce()
    expect(lintSpy).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain("Empty Shell")
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
