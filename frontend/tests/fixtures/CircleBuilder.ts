import type { Circle } from "@generated/doughnut-backend-api"
import Builder from "./Builder"
import generateId from "./generateId"

class CircleNoteBuilder extends Builder<Circle> {
  do(): Circle {
    const id = generateId()
    return {
      id,
      name: `circle ${id}`,
    }
  }
}

export default CircleNoteBuilder
