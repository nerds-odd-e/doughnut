<template>
  <div class="btn-toolbar" :key="note.id">

    <RadioButtons
      v-model="viewType"
      @update:modelValue="viewTypeChange"
      v-bind="{ options: [
        {value: 'cards', title: 'cards view'},
        {value: 'mindmap', title: 'mindmap view'},
        {value: 'article', title: 'article view'}] }"
    >
      <template #labelAddition="{ value }">
        <SvgMindmap v-if="value==='mindmap'"/>
        <SvgArticle v-if="value==='article'"/>
        <SvgCards v-if="value==='cards'"/>
      </template>
    </RadioButtons>

    <div class="btn-group btn-group-sm">
      <NoteNewButton :parentId="note.id">
        <template #default="{ open }">
          <button class="btn btn-small" @click="open()" :title="`Add Child Note`">
            <SvgAddChild />
          </button>
        </template>
      </NoteNewButton>

      <NoteNewButton
        :parentId="note.parentId"
        v-if="!!note.parentId"
      >
        <template #default="{ open }">
          <button class="btn btn-small" @click="open()" title="Add Sibling Note">
            <SvgAddSibling />
          </button>
        </template>
      </NoteNewButton>

      <NoteEditButton
        :noteId="note.id"
        :oldTitle="note.title"
      />

      <LinkNoteButton :note="note"/>


      <a
        class="btn btn-light dropdown-toggle"
        data-bs-toggle="dropdown"
        aria-haspopup="true"
        aria-expanded="false"
        role="button"
        title="more options"
      >
        <SvgCog />
      </a>
      <div class="dropdown-menu dropdown-menu-right">
        <ReviewSettingEditButton
          :noteId="note.id"
          :oldTitle="note.title"
        >
          Edit review settings
        </ReviewSettingEditButton>
        <NoteSplitButton
            :noteId="note.id"
            :oldTitle="note.title"
        >
        Split this note
        </NoteSplitButton>
        <button
          class="dropdown-item"
          title="delete note"
          v-on:click="deleteNote"
        >
          <SvgRemove />
          Delete
        </button>
      </div>
    </div>

  </div>
</template>

<script>
import RadioButtons from "../form/RadioButtons.vue";
import SvgAddChild from "../svgs/SvgAddChild.vue";
import SvgAddSibling from "../svgs/SvgAddSibling.vue";
import SvgCog from "../svgs/SvgCog.vue";
import SvgArticle from "../svgs/SvgArticle.vue";
import SvgMindmap from "../svgs/SvgMindmap.vue";
import SvgCards from "../svgs/SvgCards.vue";
import SvgRemove from "../svgs/SvgRemove.vue";
import LinkNoteButton from "../links/LinkNoteButton.vue";
import ReviewSettingEditButton from "../review/ReviewSettingEditButton.vue";
import NoteEditButton from "./NoteEditButton.vue";
import NoteSplitButton from "./NoteSplitButton.vue";
import NoteNewButton from "./NoteNewButton.vue";
import { storedApiDeleteNote } from "../../storedApi";
export default {
  name: "NoteButtons",
  props: {
    note: Object,
    deleteRedirect: { type: Boolean, required: true},
    viewType: String,
  },
  components: {
    RadioButtons,
    SvgCog,
    SvgCards,
    SvgMindmap,
    SvgArticle,
    SvgAddChild,
    SvgAddSibling,
    ReviewSettingEditButton,
    SvgRemove,
    LinkNoteButton,
    NoteEditButton,
    NoteSplitButton,
    NoteNewButton,
  },

  methods: {

    viewTypeChange(newType) {
      this.$router.push({name: this.viewTypeToRouteName(newType), params:{ noteId: this.note.id}})
    },

    viewTypeToRouteName(newType) {
      if (newType === 'cards') return 'noteCards'
      if (newType === 'mindmap') return 'noteMindmap'
      return 'noteArticle'
    },

    async deleteNote() {
      if (await this.$popups.confirm(`Are you sure to delete this note?`)) {
        const parentId = this.note.parentId
        await storedApiDeleteNote(this.$store, this.note.id)
        this.$emit('ensureVisible', parentId)
        if(parentId) {
          if(this.deleteRedirect) {
            this.$router.push({
              name: "noteCards",
              params: { noteId: parentId },
            });
          }
        } else {
          this.$router.push({ name: "notebooks" });
        }
      }
    },
  },
};
</script>
