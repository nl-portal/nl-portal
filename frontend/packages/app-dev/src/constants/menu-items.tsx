import { NavigationItem } from "@nl-portal/nl-portal-user-interface";
import {
  ArchiveIcon,
  CarIcon,
  CheckCircleIcon,
  GridIcon,
  InboxIcon,
  UserIcon,
} from "@gemeente-denhaag/icons";
import { paths } from "./paths";
import { themes } from "./themes";

export const menuItems: NavigationItem[][] = [
  [
    {
      titleTranslationKey: "overview",
      path: paths.overview,
      icon: <GridIcon />,
    },
  ],
  [
    {
      titleTranslationKey: "tasks",
      path: paths.tasks,
      icon: <CheckCircleIcon />,
    },
    {
      titleTranslationKey: "messages",
      path: paths.messages,
      icon: <InboxIcon />,
      hasMessagesCount: true,
    },
    {
      titleTranslationKey: "cases",
      path: paths.cases,
      icon: <ArchiveIcon />,
    },
  ],
  [
    {
      titleTranslationKey: themes.parkeren.slug,
      path: paths.themeOverview(themes.parkeren.slug),
      icon: <CarIcon />,
      themeSlug: themes.parkeren.slug,
    },
  ],
  [
    {
      titleTranslationKey: "account",
      path: paths.account,
      icon: <UserIcon />,
    },
  ],
];
