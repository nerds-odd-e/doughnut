import type { Component } from "vue"
import { CircleCheck, Mic, Trash2, Wand2 } from "@lucide/vue"
import {
  noteMoreOptionsTitles,
  type NoteMoreOptionsActionId,
} from "./noteMoreOptionsTitles"

export interface PlainNoteAction {
  id: NoteMoreOptionsActionId
  title: string
  icon: Component
  onClick: () => void
  available: boolean
  toggleable: boolean
  pressed: boolean
}

export interface PlainNoteActionsInput {
  noteHasContent: boolean
  isAudioOpen: boolean
  isAssimilationOpen: boolean
  deleteTitle: string
  onRefineOpen: () => void
  onAudioToggle: () => void
  onAssimilationToggle: () => void
  deleteNote: () => void
}

export const plainNoteActions = (
  input: PlainNoteActionsInput
): PlainNoteAction[] => [
  {
    id: "refine",
    title: noteMoreOptionsTitles.refine,
    icon: Wand2,
    onClick: input.onRefineOpen,
    available: input.noteHasContent,
    toggleable: false,
    pressed: false,
  },
  {
    id: "audio",
    title: noteMoreOptionsTitles.audio,
    icon: Mic,
    onClick: input.onAudioToggle,
    available: true,
    toggleable: true,
    pressed: input.isAudioOpen,
  },
  {
    id: "assimilation",
    title: noteMoreOptionsTitles.assimilation,
    icon: CircleCheck,
    onClick: input.onAssimilationToggle,
    available: true,
    toggleable: true,
    pressed: input.isAssimilationOpen,
  },
  {
    id: "delete",
    title: input.deleteTitle,
    icon: Trash2,
    onClick: input.deleteNote,
    available: true,
    toggleable: false,
    pressed: false,
  },
]
