<template>
  <div>
    <LinkTypeSelect
      field="linkType"
      scope-name="link"
      v-model="formData.linkType"
      :errors="linkFormErrors.linkType"
      :inverse-icon="true"
    />
    <div>
      Target: <strong>{{ targetNote.title }}</strong>
    </div>
    <CheckInput
      scope-name="link"
      v-model="formData.moveUnder"
      :errors="linkFormErrors.moveUnder"
      field="alsoMoveToUnderTargetNote"
    />

    <RadioButtons
      v-if="!!formData.moveUnder"
      scope-name="link"
      v-model="formData.asFirstChild"
      :errors="linkFormErrors.asFirstChild"
      :options="[
        { value: true, label: 'as its first child' },
        { value: false, label: 'as its last child' },
      ]"
    />

    <button class="btn btn-secondary go-back-button" @click="$emit('goBack')">
      <SvgGoBack />
    </button>
    <button class="btn btn-primary" @click.once="createLink()">
      Create Link
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkTypeSelect from "./LinkTypeSelect.vue";
import CheckInput from "../form/CheckInput.vue";
import RadioButtons from "../form/RadioButtons.vue";
import SvgGoBack from "../svgs/SvgGoBack.vue";
import usePopups from "../commons/Popups/usePopups";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return {
      ...usePopups(),
    };
  },
  name: "LinkNoteFinalize",
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    targetNote: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { LinkTypeSelect, SvgGoBack, CheckInput, RadioButtons },
  emits: ["success", "goBack"],
  data() {
    return {
      formData: {
        asFirstChild: false,
        linkType: "no link",
        moveUnder: false,
      } as Generated.LinkCreation,
      linkFormErrors: {
        asFirstChild: undefined as string | undefined,
        linkType: undefined as string | undefined,
        moveUnder: undefined as string | undefined,
      },
    };
  },
  methods: {
    async createLink() {
      if (this.formData.moveUnder && this.note.parentId === null) {
        if (
          !(await this.popups.confirm(
            `"${this.note.title}" is a top level notebook. Do you want to move it under other notebook?`
          ))
        ) {
          return;
        }
      }
      this.storageAccessor
        .api()
        .createLink(this.note.id, this.targetNote.id, this.formData)
        .then((r) => this.$emit("success", r))
        .catch((res) => {
          this.linkFormErrors = res;
        });
    },
  },
});
</script>
