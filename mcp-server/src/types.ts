export type NoteUpdateResult = {
  note?: {
    topicConstructor?: string
    details?: string
  }
}

export type ToolResponseContent = { type: 'text'; text: string }

export type ToolResponse = {
  content: ToolResponseContent[]
}

// Minimal API surface used by tools
export type ServerApi = {
  restTextContentController: {
    updateNoteTitle: (
      noteId: number,
      body: { newTitle: string }
    ) => Promise<unknown>
    updateNoteDetails: (
      noteId: number,
      body: { details: string }
    ) => Promise<unknown>
  }
  restUserController: {
    getUserProfile: () => Promise<unknown>
  }
  restNotebookController: {
    myNotebooks: () => Promise<unknown>
  }
  restNoteController: {
    getGraph: (noteId: number) => Promise<unknown>
  }
  restSearchController: {
    searchForLinkTarget: (searchTerm: { searchKey: string }) => Promise<unknown>
  }
}

export type ServerContext = {
  api: ServerApi
  authToken?: string
}

export type ToolDescriptor = {
  name: string
  description: string
  inputSchema: Record<string, unknown>
  handle: (
    ctx: ServerContext,
    args: Record<string, unknown>,
    request?: unknown
  ) => Promise<ToolResponse> | ToolResponse
}
