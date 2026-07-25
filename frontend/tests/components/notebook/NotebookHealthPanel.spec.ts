import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  createHealthPanelSpies,
  mountPanel,
  notebookId,
  providedCurrentUser,
  removeEmptyFoldersCheckbox,
} from "./notebookHealthPanelTestSupport"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"

describe("NotebookHealthPanel", () => {
  let lintSpy: ReturnType<typeof mockSdkService>
  let updateUserSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    ;({ lintSpy, updateUserSpy } = createHealthPanelSpies())
  })

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

  it("lists dead wiki links by note title with token leaf labels", async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.get('[data-testid="notebook-health-run"]').trigger("click")
    await flushPromises()

    const deadLinks = wrapper.get(
      '[data-testid="notebook-health-group-dead_wiki_links"]'
    )
    expect(deadLinks.text()).toContain("Source")
    expect(deadLinks.text()).toContain("Missing")
    expect(
      deadLinks.find('[data-testid="notebook-health-dead-link-note"]').exists()
    ).toBe(true)
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
