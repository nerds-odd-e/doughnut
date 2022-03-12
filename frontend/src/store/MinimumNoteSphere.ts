import Links from "./Links";

interface MinimumNoteSphere {
  id: number,
  parentId: number | undefined
  links: Links | undefined
  childrenIds: number[]
}

export default MinimumNoteSphere