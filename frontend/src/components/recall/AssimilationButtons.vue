<template>
  <div :class="{ 'daisy-join': showCommissionedOption }">
    <input
      type="submit"
      name="submit"
      value="Assimilate"
      :class="[
        'daisy-btn daisy-btn-primary',
        sizeClass,
        { 'daisy-join-item': showCommissionedOption },
      ]"
      data-test="assimilate"
      :disabled="disabled || assimilateDisabled"
      @click="$emit('assimilate', false)"
    />
    <AutoCollapseDropdown
      v-if="showCommissionedOption"
      v-slot="{ closeDropdown }"
      class="daisy-dropdown daisy-dropdown-end daisy-dropdown-top daisy-join-item shrink-0"
    >
      <summary
        data-test="assimilate-as-commissioned-caret"
        :class="[
          'daisy-btn daisy-btn-primary list-none cursor-pointer px-2',
          sizeClass,
          { 'pointer-events-none opacity-50': disabled || assimilateDisabled },
        ]"
        aria-label="Assimilate options"
      >
        <ChevronDown class="h-4 w-4" aria-hidden="true" />
      </summary>
      <DropdownMenu>
        <DropdownMenuItem>
          <button
            type="button"
            data-test="assimilate-as-commissioned"
            :class="dropdownMenuButtonClass"
            :disabled="disabled || assimilateDisabled"
            @click="
              $emit('assimilateAsCommissioned');
              closeDropdown()
            "
          >
            Assimilate as commissioned
          </button>
        </DropdownMenuItem>
      </DropdownMenu>
    </AutoCollapseDropdown>
  </div>
  <input
    v-if="showSkip && skippedForRecall"
    type="submit"
    name="revive"
    value="Revive"
    :class="['daisy-btn daisy-btn-secondary', sizeClass]"
    data-test="revive"
    :disabled="disabled"
    @click="$emit('revive')"
  />
  <input
    v-else-if="showSkip"
    type="submit"
    name="skip"
    value="Skip recall"
    :class="['daisy-btn daisy-btn-secondary', sizeClass]"
    :disabled="disabled"
    @click="$emit('assimilate', true)"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue"
import { ChevronDown } from "@lucide/vue"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import DropdownMenu from "@/components/commons/DropdownMenu.vue"
import DropdownMenuItem from "@/components/commons/DropdownMenuItem.vue"
import { dropdownMenuButtonClass } from "@/components/commons/dropdownMenuClasses"

export default defineComponent({
  components: {
    AutoCollapseDropdown,
    ChevronDown,
    DropdownMenu,
    DropdownMenuItem,
  },
  props: {
    disabled: {
      type: Boolean,
      default: false,
    },
    assimilateDisabled: {
      type: Boolean,
      default: false,
    },
    size: {
      type: String as () => "default" | "sm",
      default: "default",
      validator: (value: string) => ["default", "sm"].includes(value),
    },
    showSkip: {
      type: Boolean,
      default: true,
    },
    skippedForRecall: {
      type: Boolean,
      default: false,
    },
    showCommissionedOption: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["assimilate", "revive", "assimilateAsCommissioned"],
  setup() {
    return { dropdownMenuButtonClass }
  },
  computed: {
    sizeClass(): string {
      return this.size === "sm" ? "daisy-btn-sm" : ""
    },
  },
})
</script>
