import type {
  Circle,
  NoteRealm,
  Notebook,
} from "@generated/doughnut-backend-api"
import {
  AiController,
  AssimilationController,
  NoteController,
  NotebookController,
} from "@generated/doughnut-backend-api/sdk.gen"

type MockedCall = {
  // biome-ignore lint/suspicious/noExplicitAny: restored SDK method reference
  target: any
  key: string
  original: unknown
}

const activeMocks: MockedCall[] = []

function wrapSdkResponse<T>(data: T) {
  return {
    data,
    error: undefined,
    request: {} as Request,
    response: {} as Response,
  }
}

function spyMethod<T extends object, K extends keyof T>(
  target: T,
  key: K,
  replacement: T[K]
) {
  activeMocks.push({
    target,
    key: key as string,
    original: target[key],
  })
  target[key] = replacement
}

export function restoreStorySdkMocks() {
  for (const { target, key, original } of activeMocks) {
    target[key] = original
  }
  activeMocks.length = 0
}

function mockNotebookGetForNoteRealm(realm: NoteRealm, circle?: Circle) {
  const ts =
    realm.note.noteTopology.updatedAt ??
    realm.note.noteTopology.createdAt ??
    new Date().toISOString()
  const notebook: Notebook = {
    id: realm.notebookRealm.notebook.id,
    name: "Notebook",
    notebookSettings: { skipMemoryTrackingEntirely: false },
    createdAt: realm.note.noteTopology.createdAt ?? ts,
    updatedAt: ts,
    ...(circle ? { circle } : {}),
  }
  spyMethod(NotebookController, "get", async () =>
    wrapSdkResponse({
      notebook,
      hasAttachedBook: false,
      readonly: realm.notebookRealm.readonly ?? false,
    })
  )
}

/** Mocks SDK calls used when AssimilationPageView renders Assimilation → NoteShow. */
export function mockAssimilationStoryApis(noteRealms: NoteRealm[]) {
  const byNoteId = new Map(noteRealms.map((r) => [r.note.id, r]))
  const notebookIds = new Set(
    noteRealms.map((r) => r.notebookRealm.notebook.id)
  )

  spyMethod(NoteController, "showNote", (async (options) => {
    const realm = byNoteId.get(options.path.note)
    if (realm) return wrapSdkResponse(realm)
    return {
      data: undefined,
      error: { message: "Not Found" },
      request: {} as Request,
      response: { status: 404 } as Response,
    }
  }) as typeof NoteController.showNote)

  spyMethod(NoteController, "getNoteInfo", async () =>
    wrapSdkResponse({ memoryTrackers: [] })
  )

  spyMethod(AiController, "generateUnderstandingChecklist", async () =>
    wrapSdkResponse({ points: [] })
  )

  spyMethod(AssimilationController, "assimilate", async () =>
    wrapSdkResponse([])
  )

  for (const notebookId of notebookIds) {
    const realm = noteRealms.find(
      (r) => r.notebookRealm.notebook.id === notebookId
    )
    if (realm) mockNotebookGetForNoteRealm(realm)
  }
}
