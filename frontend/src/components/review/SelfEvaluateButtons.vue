<template>
  <div class="btn-group" role="group" aria-label="First group">
      <button class="btn btn-light" id="repeat-again" name="again"
              @click.once="processForm('again')"
              title="repeat immediately">
          <SvgFailed/>
      </button>
      <template v-if="!sadOnly">
        <button class="btn btn-light" id="repeat-sad" name="sad"
              @click.once="processForm('sad')"
                title="reduce next repeat interval (days) by half">
            <SvgSad/>
        </button>
        <button class="btn btn-light" id="repeat-satisfied" name="satisfying"
              @click.once="processForm('satisfying')"
                title="use normal repeat interval (days)">
            <SvgSatisfying/>
        </button>
        <button class="btn btn-light" id="repeat-happy" name="happy"
              @click.once="processForm('happy')"
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
