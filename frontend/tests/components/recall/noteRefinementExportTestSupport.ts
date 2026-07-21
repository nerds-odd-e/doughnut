import { mountNoteRefinement } from "./noteRefinementTestSupport"

export const exportExtractRequestButtonTitle =
  "Export extract request for ChatGPT"

export const exportBreakdownRequestButtonTitle =
  "Export breakdown request for ChatGPT"

export const sampleExtractExportData = {
  model: "gpt-4",
  instructions: "Extract selected layout points",
  input: "Note content",
  text: {
    format: {
      type: "json_schema",
      schema: { type: "object" },
    },
  },
}

export function exportLoadingEl() {
  return document.body.querySelector('[data-testid="export-loading"]')
}

export function exportTextarea() {
  return document.body.querySelector(
    '[data-testid="export-textarea"]'
  ) as HTMLTextAreaElement | null
}

export async function clickExportExtractRequest(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper.find('[data-test-id="export-extract-request"]').trigger("click")
}
