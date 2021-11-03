interface ViewType {
  value: string
  path: string
  title: string
  redirectAfterDelete?: Boolean
}

const viewTypes: Array<ViewType> = [
  {value: 'cards', path: 'cards', title: 'cards view', redirectAfterDelete: true},
  {value: 'article', path: 'article', title: 'article view'},
  {value: 'mindmap', path: 'mindmap', title: 'mindmap view'},
]

const viewType = (value:string): ViewType | undefined => {
  const result = viewTypes.find((vt=> vt.value === value))
  if (result) return result
  return viewTypes[0]
}
export { viewTypes, viewType }