<template>
  <nav :class="noteChromeToolbarNavClass">
    <div class="daisy-btn-group daisy-btn-group-sm">
      <PopButton
        v-if="!readonly"
        title="Link"
        align-modal-top
        :show-close-button="false"
      >
        <template #button_face>
          <SvgSearchForLink />
        </template>
        <template #default="{ closer }">
          <SearchForm
            v-bind="{ note }"
            :modal-closer="closer"
            @close-dialog="closer"
          />
        </template>
      </PopButton>

      <a
        v-if="!conversationButton"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        role="button"
        aria-label="Star a conversation about this note"
        @click="() => router.push({
          ...noteShowLocation(note.noteTopology.id),
          query: { conversation: 'true' },
        })"
        title="Star a conversation about this note"
      >
        <MessageCircle class="daisy-w-6 daisy-h-6" />
      </a>

      <button v-if="!readonly && !asMarkdown" type="button" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Edit as markdown" aria-label="Edit as markdown" @click="$emit('edit-as-markdown', true)">
        <FileCode class="daisy-w-6 daisy-h-6" />
      </button>
      <button v-else-if="!readonly" type="button" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Edit as rich content" aria-label="Edit as rich content" @click="$emit('edit-as-markdown', false)">
        <LayoutTemplate class="daisy-w-6 daisy-h-6" />
      </button>

      <button v-if="!readonly && !audioTools" class="daisy-btn daisy-btn-ghost daisy-btn-sm" title="Audio tools" @click="audioTools = true">
        <Mic class="daisy-w-6 daisy-h-6" />
      </button>

      <button
        v-if="!readonly"
        :class="['daisy-btn daisy-btn-ghost daisy-btn-sm', { 'daisy-btn-active': moreOptions }]"
        title="more options"
        @click="moreOptions = !moreOptions"
      >
        <Settings class="daisy-w-6 daisy-h-6" />
      </button>
    </div>
  </nav>
  <NoteAudioTools
    v-if="!readonly && audioTools"
    v-bind="{ note }"
    @close-dialog="audioTools = false"
  />
  <NoteMoreOptionsForm
    v-if="!readonly && moreOptions"
    v-bind="{ note }"
    @close-dialog="moreOptions = false"
  />

</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import type { Note } from "@generated/doughnut-backend-api"
import SvgSearchForLink from "../../svgs/SvgSearchForLink.vue"
import SearchForm from "../../links/SearchForm.vue"
import {
  FileCode,
  LayoutTemplate,
  MessageCircle,
  Mic,
  Settings,
} from "lucide-vue-next"
import NoteAudioTools from "../accessory/NoteAudioTools.vue"
import { useRouter } from "vue-router"
import NoteMoreOptionsForm from "../accessory/NoteMoreOptionsForm.vue"
import { noteChromeToolbarNavClass } from "../noteChromeToolbarNavClass"
import { noteShowLocation } from "@/routes/noteShowLocation"

const { note } = defineProps<{
  note: Note
  asMarkdown?: boolean
  conversationButton?: boolean
  readonly?: boolean
}>()

const audioTools = ref(false)
const moreOptions = ref(false)

const router = useRouter()

defineEmits<{
  (e: "edit-as-markdown", value: boolean): void
}>()

watch(
  () => note.id,
  () => {
    moreOptions.value = false
  }
)
</script>

