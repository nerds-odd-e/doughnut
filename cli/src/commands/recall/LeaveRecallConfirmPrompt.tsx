import type { MutableRefObject, ReactNode } from 'react'
import { YesNoStagePrompt } from '../../YesNoStagePrompt.js'
import { LEAVE_RECALL_PROMPT } from './leaveRecallSessionCopy.js'

export function LeaveRecallConfirmPrompt({
  onConfirmLeave,
  onDismiss,
  inputBlockedRef,
  header,
}: {
  readonly onConfirmLeave: () => void
  readonly onDismiss: () => void
  readonly inputBlockedRef: MutableRefObject<boolean>
  readonly header?: ReactNode
}) {
  return (
    <YesNoStagePrompt
      prompt={LEAVE_RECALL_PROMPT}
      onAnswer={(yes) => {
        if (yes) {
          onConfirmLeave()
          return
        }
        onDismiss()
      }}
      onCancel={onDismiss}
      inputBlockedRef={inputBlockedRef}
      header={header}
    />
  )
}
