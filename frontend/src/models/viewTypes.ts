type ViewTypeName = "cards" | "article" | "mindmap";

const viewTypeNamesRaw = ["cards", "article", "mindmap"];
const viewTypeNames = viewTypeNamesRaw as ViewTypeName[];

const sanitizeViewTypeName = (
  viewTypeString: string | undefined,
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

export { viewTypeNames, sanitizeViewTypeName, routeNameForViewType };
export type { ViewTypeName };
