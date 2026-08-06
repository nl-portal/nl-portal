import {
  AccountPage,
  CaseDetailsPage,
  CasesPage,
  EditContactInfoPage,
  MessageDetailsPage,
  MessagesPage,
  NoMatchPage,
  OverviewPage,
  TaskDetailsPage,
  TasksPage,
  ThemeListPage,
  ThemeMutatePage,
} from "@nl-portal/nl-portal-user-interface";
import { OidcCallbackPage } from "@nl-portal/nl-portal-authentication";
import { paths } from "./paths";
import { themes } from "./themes";
import { Navigate, useSearchParams } from "react-router";
import { FormattedNumber } from "react-intl";
import ParkerenOverview from "../pages/ParkerenOverview";
import ParkerenDetails from "../pages/ParkerenDetails";
import ParkerenHistory from "../pages/ParkerenHistory";
import { capitalizeFirstLetter, currencyFormat } from "../util/product-format";

export const routes = [
  {
    path: paths.overview,
    handle: { label: "breadcrumb.overview" },
    element: <OverviewPage />,
  },
  {
    path: paths.cases,
    handle: { label: "breadcrumb.cases" },
    children: [
      {
        index: true,
        handle: { label: "breadcrumb.cases" },
        element: <CasesPage />,
      },
      {
        path: paths.case(),
        handle: { label: "breadcrumb.cases.details" },
        element: <CaseDetailsPage />,
      },
    ],
  },
  {
    path: paths.tasks,
    handle: { label: "breadcrumb.tasks" },
    children: [
      {
        index: true,
        handle: { label: "breadcrumb.tasks" },
        element: <TasksPage />,
      },
      {
        path: paths.task(),
        handle: { label: "breadcrumb.tasks.details" },
        element: <TaskDetailsPage />,
      },
    ],
  },
  {
    path: paths.messages,
    handle: { label: "breadcrumb.messages" },
    children: [
      {
        index: true,
        handle: { label: "breadcrumb.messages" },
        element: <MessagesPage />,
      },
      {
        path: paths.message(),
        handle: { label: "breadcrumb.messages.details" },
        element: <MessageDetailsPage />,
      },
    ],
  },
  // Parkeren
  {
    path: paths.themeOverview(themes.parkeren.slug),
    handle: { label: `breadcrumb.${themes.parkeren.slug}` },
    children: [
      {
        index: true,
        handle: { label: `breadcrumb.${themes.parkeren.slug}` },
        element: <ParkerenOverview />,
      },
      {
        path: paths.themeList(
          themes.parkeren.slug,
          themes.parkeren.productTypes.vergunningen?.slug,
        ),
        handle: { label: `breadcrumb.${themes.parkeren.slug}` },
        element: (
          <ThemeListPage
            slug={themes.parkeren.slug}
            productSettings={{
              productTypeSlug: themes.parkeren.productTypes.vergunningen?.slug,
              productTypeCodes: [
                themes.parkeren.productTypes.vergunningen?.code,
              ],
              titleTranslationId: "Vergunningen",
              headerTranslationIds: [
                "Naam",
                "Startdatum",
                "Einddatum",
                "Status",
                "Prijs",
              ],
              dataMapping: [
                "naam",
                "startDatum",
                "eindDatum",
                (product) =>
                  capitalizeFirstLetter(product?.status.toLowerCase() ?? ""),
                (product) => {
                  return (
                    <FormattedNumber
                      value={product?.prijs ?? 0}
                      {...currencyFormat}
                    />
                  );
                },
              ],
            }}
          />
        ),
      },
      {
        path: paths.themeDetails(
          themes.parkeren.slug,
          themes.parkeren.productTypes.vergunningen?.slug,
        ),
        handle: { label: `breadcrumb.${themes.parkeren.slug}.details` },
        element: <ParkerenDetails />,
      },
      {
        path: paths.themeHistory(
          themes.parkeren.slug,
          themes.parkeren.productTypes.vergunningen?.slug,
        ),
        handle: { label: `breadcrumb.${themes.parkeren.slug}.details` },
        element: <ParkerenHistory />,
      },
      {
        path: paths.themeMutate(
          themes.parkeren.slug,
          themes.parkeren.productTypes.vergunningen?.slug,
        ),
        handle: { label: `breadcrumb.${themes.parkeren.slug}.details` },
        element: (
          <ThemeMutatePage slug={themes.parkeren.slug}>
            {() => {
              const [searchParams] = useSearchParams();
              return <div>Mutate parkeren: {searchParams.get("kenteken")}</div>;
            }}
          </ThemeMutatePage>
        ),
      },
      {
        path: paths.themeList(
          themes.parkeren.slug,
          themes.parkeren.productTypes.bezoekersvergunningen?.slug,
        ),
        handle: { label: `breadcrumb.${themes.parkeren.slug}` },
        element: (
          <ThemeListPage
            slug={themes.parkeren.slug}
            productSettings={{
              productTypeSlug:
                themes.parkeren.productTypes.bezoekersvergunningen?.slug,
              productTypeCodes: [
                themes.parkeren.productTypes.bezoekersvergunningen?.code,
              ],
              titleTranslationId: "Bezoekersvergunningen",
              headerTranslationIds: ["Naam", "Startdatum", "Status", "Prijs"],
              dataMapping: [
                "naam",
                "startDatum",
                (product) =>
                  capitalizeFirstLetter(product?.status.toLowerCase() ?? ""),
                (product) => {
                  return (
                    <FormattedNumber
                      value={product?.prijs ?? 0}
                      {...currencyFormat}
                    />
                  );
                },
              ],
            }}
          />
        ),
      },
      {
        path: paths.themeDetails(
          themes.parkeren.slug,
          themes.parkeren.productTypes.bezoekersvergunningen?.slug,
        ),
        handle: { label: `breadcrumb.${themes.parkeren.slug}.details` },
        element: <ParkerenDetails />,
      },
      {
        path: paths.themeHistory(
          themes.parkeren.slug,
          themes.parkeren.productTypes.bezoekersvergunningen?.slug,
        ),
        handle: { label: `breadcrumb.${themes.parkeren.slug}.details` },
        element: <ParkerenHistory />,
      },
      {
        path: paths.themeMutate(
          themes.parkeren.slug,
          themes.parkeren.productTypes.bezoekersvergunningen?.slug,
        ),
        handle: { label: `breadcrumb.${themes.parkeren.slug}.details` },
        element: (
          <ThemeMutatePage slug={themes.parkeren.slug}>
            {() => {
              const [searchParams] = useSearchParams();
              return <div>Mutate parkeren: {searchParams.get("kenteken")}</div>;
            }}
          </ThemeMutatePage>
        ),
      },
    ],
  },
  {
    path: paths.account,
    handle: { label: "breadcrumb.account" },
    children: [
      {
        index: true,
        handle: { label: "breadcrumb.account" },
        element: <AccountPage />,
      },
      {
        path: paths.changeContactInfo(),
        handle: { label: "breadcrumb.account.editContactInfo" },
        element: <EditContactInfoPage />,
      },
    ],
  },
  {
    path: new URL(window.OIDC_REDIRECT_URI).pathname,
    handle: { label: "breadcrumb.oidc" },
    element: <OidcCallbackPage />,
  },
  {
    path: paths.noMatch,
    handle: { label: "breadcrumb.noMatch" },
    element: <NoMatchPage contactLink={{ target: "_blank" }} />,
  },
  {
    path: "*",
    handle: { label: "breadcrumb.noMatch" },
    element: <Navigate to={paths.noMatch} />,
  },
];
