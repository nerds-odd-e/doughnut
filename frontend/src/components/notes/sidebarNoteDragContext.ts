import type { Note } from "@generated/doughnut-backend-api"
import type { InjectionKey, Ref } from "vue"

export type SidebarNoteDragState = {
  draggedNote: Ref<Note | null>
  isDraggedOver: Ref<number | null>
  dropMode: Ref<"after" | "asFirstChild">
  dropIndicatorStyle: Ref<Record<string, string>>
}

export const sidebarNoteDragStateKey: InjectionKey<SidebarNoteDragState> =
  Symbol("sidebarNoteDragState")
