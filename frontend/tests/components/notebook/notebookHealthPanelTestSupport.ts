import type { User } from "@generated/doughnut-backend-api"
import {
  NotebookHealthController,
  UserController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NotebookHealthPanel from "@/components/notebook/NotebookHealthPanel.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import type { VueWrapper } from "@vue/test-utils"
import { vi } from "vitest"
import type { Ref } from "vue"

export const notebookId = 42

export const reportFixture = {
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

export const emptyReportFixture = {
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

export function createHealthPanelSpies(
  lintReport: typeof reportFixture = reportFixture
) {
  vi.restoreAllMocks()
  return {
    lintSpy: mockSdkService(NotebookHealthController, "lint", lintReport),
    fixSpy: mockSdkService(NotebookHealthController, "fix", undefined),
    updateUserSpy: mockSdkService(
      UserController,
      "updateUser",
      makeMe.aUser.please()
    ),
  }
}

export function mountPanel(user?: User) {
  const builder = helper
    .component(NotebookHealthPanel)
    .withProps({ notebookId })
  if (user) {
    builder.withCurrentUser(user)
  }
  return builder.mount()
}

export function providedCurrentUser(wrapper: VueWrapper) {
  return wrapper.vm.$.appContext.provides.currentUser as Ref<User | undefined>
}

export function removeEmptyFoldersCheckbox(wrapper: VueWrapper) {
  return wrapper.get(
    '[data-testid="notebook-health-remove-empty-folders"] input[type="checkbox"]'
  )
}

export function fixButton(wrapper: VueWrapper) {
  return wrapper.get('[data-testid="notebook-health-fix"]')
}
