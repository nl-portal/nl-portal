import React from "react";

export type NavigationItem = {
  titleTranslationKey: string;
  path: string;
  icon: React.ElementType;
  hasMessagesCount?: boolean;
  themeSlug?: string;
  showInQuickLinks?: boolean;
};
