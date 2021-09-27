<template>
  <ModalWithButton v-model="show">
    <template #button>
      <button class="btn btn-small" title="link note" @click="show = true">
        <SvgLinkNote />
      </button>
    </template>
    <template #header>
      <h3>
        Link <strong>{{ note.title }}</strong> to
      </h3>
    </template>
    <template #body>
      <LinkNote v-bind="{ note }" @done="done()" />
    </template>
  </ModalWithButton>
</template>

<script>
import ModalWithButton from "../commons/ModalWithButton.vue";
import LinkNote from "./LinkNote.vue";
import SvgLinkNote from "../svgs/SvgLinkNote.vue";

export default {
  name: "LinkNoteButton",
  props: { note: Object },
  components: { ModalWithButton, LinkNote, SvgLinkNote },
  data() {
    return { show: false };
  },
  methods: {
    done() {
      this.show = false;
    },
  },
};
</script>
