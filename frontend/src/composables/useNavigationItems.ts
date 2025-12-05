import { computed } from "vue"
import { useRoute } from "vue-router"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useRecallData } from "@/composables/useRecallData"
import SvgNote from "@/components/svgs/SvgNote.vue"
import SvgAssimilate from "@/components/svgs/SvgAssimilate.vue"
import SvgCalendarCheck from "@/components/svgs/SvgCalendarCheck.vue"
import SvgResume from "@/components/svgs/SvgResume.vue"
import SvgShop from "@/components/svgs/SvgShop.vue"
import SvgPeople from "@/components/svgs/SvgPeople.vue"
import SvgChat from "@/components/svgs/SvgChat.vue"
import { messageCenterConversations } from "@/store/messageStore"

export function useNavigationItems() {
  const route = useRoute()
  const { dueCount } = useAssimilationCount()
  const { toRepeatCount, isRecallPaused, currentIndex } = useRecallData()

  const upperNavItems = computed(() => {
    const baseItems = [
      {
        name: "notebooks",
        label: "Note",
        icon: SvgNote,
        isActive: ["notebooks", "noteShow", "notebookEdit"].includes(
          route.name as string
        ),
      },
      {
        name: "assimilate",
        label: "Assimilate",
        icon: SvgAssimilate,
        badge: dueCount.value,
        badgeClass: "due-count",
        isActive: ["assimilate"].includes(route.name as string),
      },
      {
        name: "recall",
        label: "Recall",
        icon: SvgCalendarCheck,
        badge: toRepeatCount.value,
        badgeClass: "recall-count",
        isActive: ["recall"].includes(route.name as string),
      },
    ]

    // Recall is paused if:
    // 1. previousAnsweredQuestionCursor is set (viewing answered question), OR
    // 2. currentIndex > 0 (not at first memory tracker) AND not on recall page
    const isPausedByCursor = isRecallPaused.value
    const isPausedByIndex = currentIndex.value > 0 && route.name !== "recall"
    const shouldShowResume = isPausedByCursor || isPausedByIndex

    if (shouldShowResume) {
      return [
        {
          name: "resumeRecall",
          label: "Resume",
          icon: SvgResume,
          badge: toRepeatCount.value,
          badgeClass: "recall-count",
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
      icon: SvgPeople,
      isActive: ["circles", "circleShow", "circleJoin"].includes(
        route.name as string
      ),
    },
    {
      name: "bazaar",
      label: "Bazaar",
      icon: SvgShop,
      isActive: ["bazaar"].includes(route.name as string),
    },
    {
      name: "messageCenter",
      label: "Messages",
      icon: SvgChat,
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
