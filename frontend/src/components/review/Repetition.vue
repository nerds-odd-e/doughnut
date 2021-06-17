<template>
        <div class="btn-toolbar justify-content-between">
            <form :action="`/reviews/${reviewPoint.id}`" method="post">
                <div class="btn-group" role="group" aria-label="First group">
                    <button type="submit" class="btn btn-light" id="repeat-again" name="again"
                            title="repeat immediately">
                        <SvgFailed/>
                    </button>
                    <template v-if="!sadOnly">
                      <button type="submit" class="btn btn-light" id="repeat-sad" name="sad"
                              title="reduce next repeat interval (days) by half">
                          <SvgSad/>
                      </button>
                      <button type="submit" class="btn btn-light" id="repeat-satisfied" name="satisfying"
                              title="use normal repeat interval (days)">
                          <SvgSatisfying/>
                      </button>
                      <button type="submit" class="btn btn-light" id="repeat-happy" name="happy"
                              title="add to next repeat interval (days) by half">
                          <SvgHappy/>
                      </button>
                    </template>

                </div>
            </form>
            <div class="btn-group dropup">
                <button type="button" id="more-action-for-repeat" class="btn btn-light dropdown-toggle"
                        data-toggle="dropdown" aria-haspopup="true"
                        aria-expanded="false">
                    <SvgCog/>
                </button>
                <div class="dropdown-menu dropdown-menu-right">
                    <a class="dropdown-item"
                       :href="`/notes/${reviewPoint.sourceNote.id}/review_setting`">
                        <SvgReviewSetting/>
                        Edit Review Settings
                    </a>
                    <a class="dropdown-item" :href="`/links/${reviewPoint.sourceNote.id}/link`">
                        <SvgLinkNote/>
                        Make Link
                      </a>
                      <a class="dropdown-item"
                          href="`/notes/${reviewPoint.sourceNote.id}/edit`">
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
  import SvgReviewSetting from "../svgs/SvgReviewSetting.vue"
  import SvgLinkNote from "../svgs/SvgLinkNote.vue"
  import SvgNoReview from "../svgs/SvgNoReview.vue"
  const props = defineProps({reviewPoint: Object, sadOnly: Boolean})
</script>
