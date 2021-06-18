<template>
        <div v-if="answerResult">
            <div class="alert alert-success" v-if="answerResult.correct">Correct!</div>
            <div class="alert alert-danger" v-else>
                 {{'Your answer `' + answerResult.answerDisplay + '` is wrong.'}}
            </div>
        </div>

        <ShowReviewPoint v-bind="{reviewPoint, noteViewedByUser, linkViewedByUser}" />
        <div class="btn-toolbar justify-content-between">
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
            <div class="btn-group dropup">
                <button type="button" id="more-action-for-repeat" class="btn btn-light dropdown-toggle"
                        data-toggle="dropdown" aria-haspopup="true"
                        aria-expanded="false">
                    <SvgCog/>
                </button>
                <div class="dropdown-menu dropdown-menu-right">
                    <a class="dropdown-item"
                       :href="`/notes/${sourceNoteViewedByUser.note.id}/review_setting`">
                        <SvgReviewSetting/>
                        Edit Review Settings
                    </a>
                    <a class="dropdown-item" :href="`/links/${sourceNoteViewedByUser.note.id}/link`">
                        <SvgLinkNote/>
                        Make Link
                      </a>
                      <a class="dropdown-item"
                          :href="`/notes/${sourceNoteViewedByUser.note.id}/edit`">
                          <SvgEdit/>
                          Edit Note
                      </a>
                      <div class="dropdown-divider"></div>
                      <form :action="`/reviews/${reviewPoint.id}`" method="post"
                            onsubmit="return confirm('Are you sure to hide this note from reviewing in the future?')">
                          <button type="submit" class="dropdown-item" name="remove">
                              <SvgNoReview/>
                              Remove This Note from Review
                          </button>
                      </form>
                </div>
            </div>
        </div>
</template>

<script setup>
  import SvgSad from "../svgs/SvgSad.vue"
  import SvgSatisfying from "../svgs/SvgSatisfying.vue"
  import SvgFailed from "../svgs/SvgFailed.vue"
  import SvgHappy from "../svgs/SvgHappy.vue"
  import SvgCog from "../svgs/SvgCog.vue"
  import SvgEdit from "../svgs/SvgEdit.vue"
  import SvgReviewSetting from "../svgs/SvgReviewSetting.vue"
  import SvgLinkNote from "../svgs/SvgLinkNote.vue"
  import SvgNoReview from "../svgs/SvgNoReview.vue"
  import ShowReviewPoint from "./ShowReviewPoint.vue"
  import { computed } from 'vue'
  const props = defineProps({
    reviewPoint: Object,
    answerResult: Object,
    noteViewedByUser: Object,
    linkViewedByUser: Object})

  const emit = defineEmit(['evaluated'])

  const sourceNoteViewedByUser = computed(()=> {
    if(!!props.noteViewedByUser) {
      return props.noteViewedByUser
    }
    else {
      return props.linkViewedByUser.sourceNoteViewedByUser
    }
  })

  const sadOnly = computed(()=> {
    if(!!props.answerResult) {
      return !props.answerResult.correct
    }
    return false
  })

  const processForm = function(selfEvaluate) {
      fetch(`/api/reviews/${props.reviewPoint.id}/self-evaluate`, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: selfEvaluate
      })
        .then(res => {
          return res.json();
        })
        .then(resp => {
          console.log(resp)
          emit('evaluated', resp)
        })
        .catch(error => {
          window.alert(error);
        });
    }

</script>
