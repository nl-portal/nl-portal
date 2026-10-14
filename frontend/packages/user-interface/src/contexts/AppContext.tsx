/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
  ApiContext,
  GetOpenProductHoofdThemasByProductenDocument,
  GetOpenProductHoofdThemasByProductenQuery,
  GetUnopenedBerichtenCountDocument,
} from "@nl-portal/nl-portal-api";
import { useQuery } from "@apollo/client/react";
import {
  createContext,
  ReactNode,
  useContext,
  useEffect,
  useMemo,
  useState,
  useTransition,
} from "react";
import { useLocation, useNavigationType } from "react-router";
import { stringToSlug } from "../utils/string-to-slug";
import RouterContext from "./RouterContext";
import UserContext from "./UserContext";
import FullscreenSkeleton from "../components/FullscreenSkeleton";
import {
  Features,
  loadFrontendConfig,
  applyFrontendTheme,
} from "../utils/frontend-config";

type Themes =
  GetOpenProductHoofdThemasByProductenQuery["getOpenProductHoofdThemasByProducten"];

interface AppContextType {
  history: string[];
  features: Features | undefined;
  themes: Themes;
  messagesCount: number;
  refetchThemes: () => void;
  refetchMessages: () => void;
}

const AppContext = createContext<AppContextType>({} as AppContextType);

interface MessagesProviderProps {
  children: ReactNode;
}

export const AppProvider = ({ children }: MessagesProviderProps) => {
  const location = useLocation();
  const navType = useNavigationType();
  const [firstLoad, setFirstLoad] = useState(true);
  const { restUri } = useContext(ApiContext);
  const [features, setFeatures] = useState<Features | undefined>(undefined);
  const [loadingConfig, startTransition] = useTransition();
  const { initNavigationItems, updateNavigationItems } =
    useContext(RouterContext);
  const { isLoading: loadingUser } = useContext(UserContext);

  const history = useMemo<string[]>(() => {
    const stored = JSON.parse(
      localStorage.getItem("history") || "[]",
    ) as string[];

    if (navType === "POP" || location.key === "default") return stored;

    const newHistory = stored
      .filter((item) => item !== location.pathname)
      .slice(0, 1);
    newHistory.unshift(location.pathname);

    return newHistory;
  }, [location.pathname, location.key, navType]);

  useEffect(() => {
    if (features) return;
    startTransition(async () => {
      try {
        const json = await loadFrontendConfig(restUri);

        applyFrontendTheme(json);
        setFeatures(json);
      } catch (err) {
        console.error("Failed to load features:", err);
      }
    });
  }, [features, restUri, startTransition]);

  const {
    loading: loadingThemes,
    refetch: refetchThemes,
    data: themesData,
  } = useQuery(GetOpenProductHoofdThemasByProductenDocument, {
    skip: !features?.toggles.openProductEnabled,
  });

  const themes: Themes = useMemo(
    () => themesData?.getOpenProductHoofdThemasByProducten ?? [],
    [themesData],
  );

  useEffect(() => {
    if (!themesData) return;

    const activeThemes = themes.map((theme) => stringToSlug(theme.naam)) || [];

    const newNavigationItems = initNavigationItems.map((group) =>
      group.filter(
        (item) => !item.themeSlug || activeThemes.includes(item.themeSlug),
      ),
    );

    updateNavigationItems(newNavigationItems);
  }, [themesData, initNavigationItems, themes]);

  const { refetch: refetchMessages, data: messagesData } = useQuery(
    GetUnopenedBerichtenCountDocument,
    {
      pollInterval: features?.properties?.messageCountPollingInterval || 30000,
      fetchPolicy: "cache-and-network",
      skip: !features?.toggles.messageCountEnabled,
      skipPollAttempt: () => {
        return !document.hasFocus();
      },
    },
  );

  const messagesCount = useMemo(
    () => messagesData?.getUnopenedBerichtenCount || 0,
    [messagesData],
  );

  const loading = loadingConfig || loadingThemes || loadingUser;

  useEffect(() => {
    localStorage.setItem("history", JSON.stringify(history));
  }, [history]);

  useEffect(() => {
    if (loading || !firstLoad) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setFirstLoad(false);
  }, [loading]);

  if (loading || firstLoad) return <FullscreenSkeleton />;

  return (
    <AppContext.Provider
      value={{
        history,
        features,
        themes,
        messagesCount,
        refetchThemes,
        refetchMessages,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export default AppContext;
