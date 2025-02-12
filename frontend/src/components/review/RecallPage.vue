<Quiz
  v-if="toRepeatCount !== 0"
  v-show="!currentAnsweredQuestion && !currentAnsweredSpelling"
  :memory-trackers="toRepeat"
  :current-index="currentIndex"
  :eager-fetch-count="eagerFetchCount ?? 5"
  :storage-accessor="storageAccessor"
  @answered-question="onAnsweredQuestion"
  @answered-spelling="onAnsweredSpelling"
  @just-reviewed="onJustReviewed"
  @move-to-end="moveMemoryTrackerToEnd"
/>

const onAnsweredQuestion = (answerResult: AnsweredQuestion) => {
  currentIndex.value += 1
  previousResults.value.push({
    type: "question",
    answeredQuestion: answerResult,
  })
  if (!answerResult.answer.correct) {
    viewLastResult(previousResults.value.length - 1)
  }
  decrementToRepeatCount()
}

const onAnsweredSpelling = (answerResult: AnsweredQuestion) => {
  currentIndex.value += 1
  if (answerResult.predefinedQuestion?.bareQuestion.checkSpell && answerResult.note) {
    previousResults.value.push({
      type: "spelling",
      note: answerResult.note,
      answer: answerResult.answerDisplay || "",
      isCorrect: answerResult.answer.correct,
    })
    if (!answerResult.answer.correct) {
      viewLastResult(previousResults.value.length - 1)
    }
  }
  decrementToRepeatCount()
}

const onJustReviewed = (answerResult: AnsweredQuestion | undefined) => {
  currentIndex.value += 1
  previousResults.value.push(undefined)
  decrementToRepeatCount()
}

const viewLastResult = (cursor: number | undefined) => {
  previousResultCursor.value = cursor
}

const loadMore = async (dueInDays?: number) => {
  const response = await managedApi.restRecallsController.recalling(
    timezoneParam(),
    dueInDays
  )
  toRepeat.value = response.toRepeat
  currentIndex.value = 0
  if (toRepeat.value?.length === 0) {
    return response
  }
  if (getEnvironment() !== "testing" && toRepeat.value) {
    toRepeat.value = shuffle(toRepeat.value)
  }
  return response
}

const moveMemoryTrackerToEnd = (index: number) => {
  // ... existing code ...
}
