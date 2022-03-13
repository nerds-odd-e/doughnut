import Doughnut from "../@types/Doughnut";
import Links from "./Links";

interface MinimumNoteSphere {
  id: Doughnut.ID,
  parentId: Doughnut.ID | undefined
  links: Links | undefined
  childrenIds: Doughnut.ID[]
}

export default MinimumNoteSphere