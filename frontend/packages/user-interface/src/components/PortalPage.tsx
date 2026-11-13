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
  useCallback,
  useContext,
  useEffect,
  useEffectEvent,
  useMemo,
  useRef,
  useState,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Navigate, useOutletContext, useParams } from "react-router";
import { ErrorBoundary } from "react-error-boundary";
import { Alert } from "@gemeente-denhaag/alert";
import { Paragraph } from "@gemeente-denhaag/typography";
import { ApiContext } from "@nl-portal/nl-portal-api";
import { OidcContext } from "@nl-portal/nl-portal-authentication";
import PageGrid from "./PageGrid";
import Notification from "./Notification";
import Skeleton from "./Skeleton";
import LiquidParser from "./LiquidParser";
import NotificationContext from "../contexts/NotificationContext";
import PortalPageBindingContext, {
  type PortalPageBindingContextValue,
} from "../contexts/PortalPageBindingContext";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import { listViewHeight } from "../constants/skeleton";
import type { MenuEntryRendererProps } from "../utils/menu-entry-renderers";
import {
  loadPortalPage,
  PortalPageNotFoundError,
  refetchPortalPageBinding,
  type PortalPageBindingResponse,
  type PortalPageResponse,
} from "../utils/portal-pages";

type LoadState =
  | { status: "loading"; key: string }
  | { status: "ready"; key: string; page: PortalPageResponse }
  | { status: "not-found"; key: string }
  | { status: "error"; key: string };

const REFETCH_NOTIFICATION_ID = "portalPageRefetch";

const asRecord = (data: unknown): Record<string, unknown> =>
  data && typeof data === "object" && !Array.isArray(data)
    ? (data as Record<string, unknown>)
    : {};

const ErrorText = () => (
  <Paragraph>
    <FormattedMessage id="portalPage.error" />
  </Paragraph>
);

/**
 * Renders one template; a failing render or component shows the generic error text instead,
 * while the rest of the page keeps working.
 */
const TemplateBlock = ({
  template,
  data,
  messages,
}: {
  template: string;
  data: Record<string, unknown>;
  messages?: Record<string, string>;
}) => {
  const [failedFor, setFailedFor] = useState<Record<string, unknown>>();
  const onError = useCallback(() => setFailedFor(data), [data]);

  if (failedFor === data) return <ErrorText />;
  return (
    <ErrorBoundary fallback={<ErrorText />} resetKeys={[template, data]}>
      <LiquidParser
        template={template}
        data={data}
        messages={messages}
        onError={onError}
      />
    </ErrorBoundary>
  );
};

/**
 * A template supplied by a binding, rendered against that binding's data only (#64).
 */
const RootTemplate = ({
  template,
  binding,
}: {
  template: string | null;
  binding?: PortalPageBindingResponse;
}) => {
  const intl = useIntl();
  if (!binding || binding.error) {
    return (
      <Alert
        variant="error"
        title={intl.formatMessage({ id: "portalPage.error" })}
        text=""
      />
    );
  }
  if (!template) {
    return (
      <Paragraph>
        <FormattedMessage id="portalPage.noDetails" />
      </Paragraph>
    );
  }
  return <TemplateBlock template={template} data={asRecord(binding.data)} />;
};

/**
 * Renderer for `portal-page` menu entries: loads the portal page `entry.route.page` from the backend and
 * renders it with LiquidJS and the page's messages. A route with one path param loads the page for that id.
 * When the template comes from a binding (`root`), it renders with that binding's data only and without interaction.
 * Pages supply their own heading in their template; this component only adds notifications.
 */
const PortalPage = ({ entry }: MenuEntryRendererProps) => {
  const intl = useIntl();
  const { restUri } = useContext(ApiContext);
  const { oidcToken } = useContext(OidcContext);
  const { state: notifications, dispatch } = useContext(NotificationContext);
  const { paths } = useOutletContext<RouterOutletContext>();
  const routeParams = useParams();

  const ids = Object.entries(routeParams)
    .filter(([name]) => name !== "*")
    .map(([, value]) => value ?? "");
  const id = ids.length === 1 ? ids[0] : undefined;
  const page = entry.route.page;
  const locale = intl.locale;
  const key = `${page}|${id ?? ""}`;
  const requestLocale = useEffectEvent(() => locale);

  const [state, setState] = useState<LoadState>({ status: "loading", key });
  const [refetched, setRefetched] = useState<{
    key: string;
    bindings: Record<string, PortalPageBindingResponse>;
  }>({ key, bindings: {} });
  const requests = useRef<Record<string, number>>({});

  useEffect(() => {
    if (ids.length > 1 || !oidcToken) return;
    let active = true;
    loadPortalPage(restUri, oidcToken, requestLocale(), page, id)
      .then((response) => {
        if (active) setState({ status: "ready", key, page: response });
      })
      .catch((error: unknown) => {
        if (!active) return;
        setState({
          status:
            error instanceof PortalPageNotFoundError ? "not-found" : "error",
          key,
        });
      });
    return () => {
      active = false;
    };
  }, [restUri, oidcToken, page, id, key, ids.length]);

  const current = state.key === key ? state : { status: "loading" as const };
  const loaded = current.status === "ready" ? current.page : undefined;

  const bindings = useMemo(() => {
    const overrides = refetched.key === key ? refetched.bindings : {};
    return (loaded?.bindings ?? []).map(
      (binding) => overrides[binding.alias] ?? binding,
    );
  }, [loaded, refetched, key]);

  const templateData = useMemo(
    () => ({
      ...Object.fromEntries(bindings.map((b) => [b.alias, b.data])),
      errors: Object.fromEntries(bindings.map((b) => [b.alias, b.error])),
      meta: Object.fromEntries(bindings.map((b) => [b.alias, b.meta])),
    }),
    [bindings],
  );

  const bindingContext = useMemo<PortalPageBindingContextValue>(
    () => ({
      meta: (alias) => bindings.find((b) => b.alias === alias)?.meta,
      refetch: (alias, changes) => {
        if (!oidcToken) return;
        const params = {
          ...bindings.find((b) => b.alias === alias)?.meta.params,
          ...changes,
        };
        const request = (requests.current[alias] ?? 0) + 1;
        requests.current[alias] = request;
        refetchPortalPageBinding(
          restUri,
          oidcToken,
          locale,
          page,
          alias,
          params,
        )
          .then((response) => {
            if (requests.current[alias] !== request) return;
            setRefetched((previous) => ({
              key,
              bindings: {
                ...(previous.key === key ? previous.bindings : {}),
                [alias]: response,
              },
            }));
          })
          .catch(() => {
            if (requests.current[alias] !== request) return;
            dispatch({
              type: "CREATE",
              id: REFETCH_NOTIFICATION_ID,
              notification: {
                variant: "error",
                title: <FormattedMessage id="portalPage.refetchError" />,
                text: "",
              },
            });
          });
      },
    }),
    [bindings, oidcToken, restUri, locale, page, key, dispatch],
  );

  if (ids.length > 1) return <ErrorText />;
  if (current.status === "not-found") return <Navigate to={paths.noMatch} />;

  return (
    <PageGrid variant="medium">
      {Object.entries(notifications).map(([id, props]) => (
        <Notification key={id} {...props} />
      ))}
      {current.status === "loading" && <Skeleton height={listViewHeight} />}
      {current.status === "error" && <ErrorText />}
      {loaded && loaded.root !== null && (
        <RootTemplate
          template={loaded.template}
          binding={bindings.find((b) => b.alias === loaded.root)}
        />
      )}
      {loaded && loaded.root === null && loaded.template && (
        <PortalPageBindingContext.Provider value={bindingContext}>
          <TemplateBlock
            template={loaded.template}
            data={templateData}
            messages={loaded.messages[locale]}
          />
        </PortalPageBindingContext.Provider>
      )}
    </PageGrid>
  );
};

export default PortalPage;
