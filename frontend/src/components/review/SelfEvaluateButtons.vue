<template>
  <div class="btn-group" role="group" aria-label="First group">
    {{clicked}}
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
  import SvgSad from "../svgs/SvgSad.vue"
  import SvgSatisfying from "../svgs/SvgSatisfying.vue"
  import SvgFailed from "../svgs/SvgFailed.vue"
  import SvgHappy from "../svgs/SvgHappy.vue"

  export default {
    props: { sadOnly: Boolean },
    emits: ['selfEvaluate'],
    components: { SvgSad, SvgSatisfying, SvgFailed, SvgHappy },
    methods: {
      processForm(evaluation) {
        this.$emit('selfEvaluate', evaluation)
      }
    }
  }

</script>
