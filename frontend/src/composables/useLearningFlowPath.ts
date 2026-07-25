import { onMounted, onUnmounted, ref, type Ref } from "vue"

type Point = { x: number; y: number }

function updateLearningFlowPath() {
  const svg = document.querySelector(".flow-background") as SVGElement | null
  const iconWrappers = document.querySelectorAll(
    ".nav-item-wrapper"
  ) as NodeListOf<HTMLElement>
  if (!svg || iconWrappers.length !== 3) return

  const svgRect = svg.getBoundingClientRect()
  const positions = Array.from(iconWrappers).map((wrapper) => {
    const rect = wrapper.getBoundingClientRect()
    return {
      x: rect.left + rect.width / 2 - svgRect.left,
      y: rect.top + rect.height / 2 - svgRect.top,
    }
  })

  const path = document.querySelector(".flow-path") as SVGPathElement | null
  if (!path) return

  const spacing = 30
  const curveRadius = 20
  const arrowSize = 8
  const wrapPadding = 80
  const edgeMargin = 60
  const sideMargin = 60
  const leftOffset = 60
  const connectionGap = 40
  const iconWidth = 70

  const [first, second, third] = positions
  if (!first || !second || !third) return

  path.setAttribute(
    "d",
    `
      M ${first.x + connectionGap + leftOffset - iconWidth},${first.y}
      H ${second.x - connectionGap + leftOffset - iconWidth}
      M ${second.x + connectionGap + leftOffset - iconWidth},${second.y}
      H ${third.x - connectionGap + leftOffset - iconWidth}
      M ${third.x + leftOffset},${third.y}
      h ${wrapPadding}
      q ${curveRadius} 0 ${curveRadius} ${curveRadius}
      v ${svgRect.height - third.y - edgeMargin}
      q 0 ${curveRadius} -${curveRadius} ${curveRadius}
      h -${svgRect.width - sideMargin * 2}
      q -${curveRadius} 0 -${curveRadius} -${curveRadius}
      v -${svgRect.height - first.y - edgeMargin}
      q 0 -${curveRadius} ${curveRadius} -${curveRadius}
      H ${first.x - spacing + leftOffset - iconWidth}
    `
  )

  const arrowPositions: Point[] = [
    {
      x: second.x - connectionGap + leftOffset - iconWidth,
      y: second.y,
    },
    {
      x: third.x - connectionGap + leftOffset - iconWidth,
      y: third.y,
    },
    {
      x: first.x - spacing + leftOffset - iconWidth,
      y: first.y,
    },
  ]

  const arrowHeads = document.querySelectorAll(
    ".arrow-marker"
  ) as NodeListOf<SVGPathElement>
  arrowHeads.forEach((arrow, index) => {
    const pos = arrowPositions[index]
    if (!pos) return
    arrow.setAttribute(
      "d",
      `
          M ${pos.x - arrowSize},${pos.y - arrowSize / 2}
          L ${pos.x},${pos.y}
          L ${pos.x - arrowSize},${pos.y + arrowSize / 2}
        `
    )
  })
}

export function useLearningFlowPath(upperHalf: Ref<HTMLElement | undefined>) {
  const isScrolling = ref(false)

  const handleScroll = () => {
    if (!upperHalf.value) return

    const upperRect = upperHalf.value.getBoundingClientRect()
    const threshold = upperRect.height * 0.7
    isScrolling.value = window.scrollY > threshold
  }

  onMounted(() => {
    window.addEventListener("scroll", handleScroll)
    window.addEventListener("resize", updateLearningFlowPath)
    setTimeout(updateLearningFlowPath, 100)
  })

  onUnmounted(() => {
    window.removeEventListener("scroll", handleScroll)
    window.removeEventListener("resize", updateLearningFlowPath)
  })

  return { isScrolling }
}
