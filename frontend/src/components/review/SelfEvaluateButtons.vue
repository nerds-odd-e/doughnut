<template>
  <div class="btn-group" role="group" aria-label="First group">
      <button type="submit" class="btn btn-light" id="repeat-again" name="again"
              v-on:click="processForm('again')"
              title="repeat immediately">
          <SvgFailed/>
      </button>
      <template v-if="!sadOnly">
        <button type="submit" class="btn btn-light" id="repeat-sad" name="sad"
              v-on:click="processForm('sad')"
                title="reduce next repeat interval (days) by half">
            <SvgSad/>
        </button>
        <button type="submit" class="btn btn-light" id="repeat-satisfied" name="satisfying"
              v-on:click="processForm('satisfying')"
                title="use normal repeat interval (days)">
            <SvgSatisfying/>
        </button>
        <button type="submit" class="btn btn-light" id="repeat-happy" name="happy"
              v-on:click="processForm('happy')"
                title="add to next repeat interval (days) by half">
            <SvgHappy/>
        </button>
      </template>
  </div>
</template>

<script>
export default { name: "SelfEvaluateButtons" };
</script>

<script setup>
  import SvgSad from "../svgs/SvgSad.vue"
  import SvgSatisfying from "../svgs/SvgSatisfying.vue"
  import SvgFailed from "../svgs/SvgFailed.vue"
  import SvgHappy from "../svgs/SvgHappy.vue"
  const props = defineProps({ sadOnly: Boolean })
  const emit = defineEmit(['selfEvaluate'])

  const processForm = function(evaluation) {
      emit('selfEvaluate', evaluation)
    }

</script>
