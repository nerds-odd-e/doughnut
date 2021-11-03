interface ViewType {
  value: string
  path: string
  routeName: string
  title: string
}

const viewTypes: Array<ViewType> = [
  {value: 'cards', path: 'cards', routeName: 'noteCards', title: 'cards view'},
  {value: 'article', path: 'article', routeName: 'noteArticle', title: 'article view'},
  {value: 'mindmap', path: 'mindmap', routeName: 'noteMindmap', title: 'mindmap view'},
]

export default viewTypes