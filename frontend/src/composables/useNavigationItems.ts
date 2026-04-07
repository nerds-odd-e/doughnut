import { computed } from "vue"
import { useRoute } from "vue-router"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useRecallData } from "@/composables/useRecallData"
import {
  BookText,
  CalendarCheck,
  CircleCheck,
  MessageCircle,
  Play,
  Store,
  Users,
} from "lucide-vue-next"
import { messageCenterConversations } from "@/store/messageStore"

export function useNavigationItems() {
  const route = useRoute()
  const { dueCount } = useAssimilationCount()
  const {
    toRepeatCount,
    toRepeat,
    isRecallPaused,
    currentIndex,
    diligentMode,
  } = useRecallData()

  const upperNavItems = computed(() => {
    const baseItems = [
      {
        name: "notebooks",
        label: "Note",
        icon: BookText,
        isActive: ["notebooks", "noteShow", "notebookEdit"].includes(
          route.name as string
        ),
      },
      {
        name: "assimilate",
        label: "Assimilate",
        icon: CircleCheck,
        badge: dueCount.value,
        badgeClass: "due-count",
        isActive: ["assimilate"].includes(route.name as string),
      },
      {
        name: "recall",
        label: "Recall",
        icon: CalendarCheck,
        badge: toRepeatCount.value,
        badgeClass: diligentMode.value
          ? "recall-count diligent-mode"
          : "recall-count",
        isActive: ["recall"].includes(route.name as string),
      },
    ]

    // Recall is paused if:
    // 1. previousAnsweredQuestionCursor is set (viewing answered question), OR
    // 2. currentIndex > 0 (not at first memory tracker) AND not on recall page
    // Resume button shows when:
    // - isPausedByCursor is true AND memory trackers are loaded (viewing previously answered question)
    // - isPausedByIndex is true AND there are memory trackers remaining to recall
    const hasLoadedTrackers = (toRepeat.value?.length ?? 0) > 0
    const isPausedByCursor = isRecallPaused.value
    const isPausedByIndex = currentIndex.value > 0 && route.name !== "recall"
    const shouldShowResume =
      (isPausedByCursor && hasLoadedTrackers) ||
      (isPausedByIndex && toRepeatCount.value > 0)

    if (shouldShowResume) {
      return [
        {
          name: "resumeRecall",
          label: "Resume",
          icon: Play,
          badge: toRepeatCount.value,
          badgeClass: diligentMode.value
            ? "recall-count diligent-mode"
            : "recall-count",
          isActive: true,
        },
        ...baseItems,
      ]
    }

    return baseItems
  })

  const lowerNavItems = computed(() => [
    {
      name: "circles",
      label: "Circles",
      icon: Users,
      isActive: ["circles", "circleShow", "circleJoin"].includes(
        route.name as string
      ),
    },
    {
      name: "bazaar",
      label: "Bazaar",
      icon: Store,
      isActive: ["bazaar"].includes(route.name as string),
    },
    {
      name: "messageCenter",
      label: "Messages",
      icon: MessageCircle,
      badge: messageCenterConversations.unreadConversations.length,
      badgeClass: "unread-count",
      isActive: ["messageCenter"].includes(route.name as string),
    },
  ])

  return {
    upperNavItems,
    lowerNavItems,
  }
}
