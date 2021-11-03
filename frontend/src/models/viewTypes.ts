interface ViewType {
  value: string
  path: string
  title: string
}

const viewTypes: Array<ViewType> = [
  {value: 'cards', path: 'cards', title: 'cards view'},
  {value: 'article', path: 'article', title: 'article view'},
  {value: 'mindmap', path: 'mindmap', title: 'mindmap view'},
]

const viewType = (value:string): ViewType | undefined => viewTypes.find((vt=> vt.value === value))
export { viewTypes, viewType }