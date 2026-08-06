import AiResponse from "@/components/conversations/AiResponse.vue"
import createNoteStorage from "@/store/createNoteStorage"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockShowNote } from "@tests/helpers"
import {
  getLastInstance,
  resetInstance,
} from "@tests/helpers/aiReplyEventSourceTracker"
import { flushPromises } from "@vue/test-utils"
import type {
  Conversation,
  Note,
  NoteRealm,
} from "@generated/doughnut-backend-api"

export const simulateAiResponse = (content = "## I'm ChatGPT") => {
  const instance = getLastInstance()
  if (!instance) {
    throw new Error("No AiReplyEventSource instance available")
  }
  const chunk = {
    choices: [
      {
        index: 0,
        message: {
          role: "assistant",
          content,
        },
        finish_reason: null,
      },
    ],
  }

  instance.onMessageCallback("chat.completion.chunk", JSON.stringify(chunk))
}

export const createToolCallChunk = (functionName: string, args: object) => {
  const argumentsString = JSON.stringify(args)
  return [
    {
      choices: [
        {
          index: 0,
          delta: {
            role: "assistant",
            tool_calls: [
              {
                index: 0,
                id: "call-456",
                type: "function",
                function: {
                  name: functionName,
                  arguments: argumentsString.substring(
                    0,
                    Math.min(20, argumentsString.length)
                  ),
                },
              },
            ],
          },
          finish_reason: null,
        },
      ],
    },
    ...(argumentsString.length > 20
      ? [
          {
            choices: [
              {
                index: 0,
                delta: {
                  tool_calls: [
                    {
                      index: 0,
                      function: {
                        arguments: argumentsString.substring(20),
                      },
                    },
                  ],
                },
                finish_reason: null,
              },
            ],
          },
        ]
      : []),
    {
      choices: [
        {
          index: 0,
          delta: {},
          finish_reason: "tool_calls",
        },
      ],
    },
  ]
}

export type AiResponseFixture = {
  note: Note
  noteRealm: NoteRealm
  conversation: Conversation
}

export const setupAiResponseFixture = (content?: string): AiResponseFixture => {
  const noteRealm =
    content === undefined
      ? makeMe.aNoteRealm.please()
      : makeMe.aNoteRealm.content(content).please()
  const note = noteRealm.note
  const conversation = makeMe.aConversation.forANote(note).please()
  return { note, noteRealm, conversation }
}

export const mountAiResponse = (conversation: Conversation) =>
  helper
    .component(AiResponse)
    .withProps({ conversation, aiReplyTrigger: 0 })
    .mount()

export const submitAiReply = async (wrapper: {
  vm: { getAiReply: () => Promise<void> }
}) => {
  await wrapper.vm.getAiReply()
  await flushPromises()
}

export const submitMessageAndSimulateToolCall = async (
  wrapper: { vm: { getAiReply: () => Promise<void> } },
  toolCallChunks: ReturnType<typeof createToolCallChunk> | object
) => {
  await submitAiReply(wrapper)
  const instance = getLastInstance()
  if (!instance) {
    throw new Error("No AiReplyEventSource instance available")
  }
  const chunks = Array.isArray(toolCallChunks)
    ? toolCallChunks
    : [toolCallChunks]
  for (const chunk of chunks) {
    instance.onMessageCallback("chat.completion.chunk", JSON.stringify(chunk))
    await flushPromises()
  }
}

export function useAiResponseMount() {
  const storageAccessor = useStorageAccessor()
  let note: Note
  let noteRealm: NoteRealm
  let conversation: Conversation
  let wrapper: ReturnType<typeof mountAiResponse>

  const beforeEachMount = (content?: string) => {
    storageAccessor.value = createNoteStorage()
    resetInstance()
    mockShowNote()

    const fixture = setupAiResponseFixture(content)
    note = fixture.note
    noteRealm = fixture.noteRealm
    conversation = fixture.conversation
    wrapper = mountAiResponse(conversation)
  }

  const afterEachReset = () => {
    resetInstance()
  }

  const refreshNoteContent = (content: string) => {
    noteRealm = makeMe.aNoteRealm.id(note.id).content(content).please()
    note = noteRealm.note
    storageAccessor.value.refreshNoteRealm(noteRealm)
  }

  return {
    get wrapper() {
      return wrapper
    },
    get note() {
      return note
    },
    get noteRealm() {
      return noteRealm
    },
    get conversation() {
      return conversation
    },
    storageAccessor,
    beforeEachMount,
    afterEachReset,
    refreshNoteContent,
  }
}
