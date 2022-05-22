type ViewTypeName = "cards" | "article" | "mindmap";

const viewTypeNamesRaw = ["cards", "article", "mindmap"];
const viewTypeNames = viewTypeNamesRaw as ViewTypeName[];

const sanitizeViewTypeName = (
  viewTypeString: string | undefined
): ViewTypeName => {
  if (viewTypeString) {
    if (viewTypeNamesRaw.includes(viewTypeString))
      return viewTypeString as ViewTypeName;
  }
  return "cards";
};

const routeNameForViewType = (viewType: string) => {
  if (viewType === "mindmap") {
    return "noteShowMindmap";
  }
  if (viewType === "article") {
    return "noteShowArticle";
  }
  return "noteShow";
};

const viewTypeFromRouteName = (routeName: string | undefined): ViewTypeName => {
  if (routeName === "noteShowMindmap") {
    return "mindmap";
  }
  if (routeName === "noteShowArticle") {
    return "article";
  }
  return "cards";
};

export {
  viewTypeNames,
  sanitizeViewTypeName,
  routeNameForViewType,
  viewTypeFromRouteName,
};
export type { ViewTypeName };
