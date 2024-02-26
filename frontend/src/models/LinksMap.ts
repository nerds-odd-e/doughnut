import { Thing } from "@/generated/backend";

type LinksMap = { [P in Thing.linkType]?: Generated.LinkViewed };

export default LinksMap;
