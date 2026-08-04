import { saveAs } from "file-saver"
import { useToast } from "vue-toastification"
import { contentDispositionFileName } from "@/utils/contentDispositionFileName"
import { isPrintableAscii } from "@/utils/isPrintableAscii"

export const NOTEBOOK_EXPORT_BUTTON_LABEL = "Export as markdowns in a zip file"

export async function downloadNotebookExport(
  notebookId: number,
  notebookName: string
): Promise<void> {
  const response = await fetch(`/api/notebooks/${notebookId}/export`, {
    credentials: "same-origin",
  })
  if (!response.ok) {
    useToast().error("Failed to export notebook.")
    return
  }
  const blob = await response.blob()
  const parsed = contentDispositionFileName(
    response.headers.get("content-disposition")
  )
  const fileName =
    parsed !== undefined && isPrintableAscii(parsed)
      ? parsed
      : `${notebookName}.zip`
  saveAs(blob, fileName)
}
