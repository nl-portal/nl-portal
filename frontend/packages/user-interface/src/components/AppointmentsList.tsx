import { useContext } from "react";
import { useIntl } from "react-intl";
import { useOutletContext } from "react-router";
import { Paragraph } from "@gemeente-denhaag/typography";
import { ActionSingle } from "@gemeente-denhaag/action";
import { LocaleContext, useActionLabels } from "@nl-portal/nl-portal-localization";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import { listViewHeight } from "../constants/skeleton";
import SectionHeader from "./SectionHeader";
import Skeleton from "./Skeleton";
import PortalLink from "./PortalLink";
import styles from "./TasksList.module.scss";

interface AfspraakOverzicht {
  afspraakIdentificatie: string;
  onderwerp: string;
  geplandAanvangsmoment: string;
  geplandEindmoment: string;
}

interface Props {
  loading?: boolean;
  error?: boolean;
  errorTranslationId?: string;
  showEmpty?: boolean;
  emptyTranslationId?: string;
  titleTranslationId?: string | null;
  readMoreLink?: string;
  readMoreTranslationId?: string | null;
  appointments?: AfspraakOverzicht[];
}

const AppointmentsList = ({
  loading,
  error,
  errorTranslationId = "appointmentsList.fetchError",
  showEmpty = true,
  emptyTranslationId = "appointmentsList.empty",
  titleTranslationId = "appointmentsList.title",
  readMoreLink,
  readMoreTranslationId = "appointmentsList.viewAll",
  appointments,
}: Props) => {
  const intl = useIntl();
  const labels = useActionLabels();
  const { currentLocale } = useContext(LocaleContext);
  const { paths } = useOutletContext<RouterOutletContext>();
  const appointmentsPath = readMoreLink || paths.appointments;
  const title = titleTranslationId
    ? intl.formatMessage({ id: titleTranslationId })
    : undefined;
  const subTitle = readMoreTranslationId
    ? intl.formatMessage({ id: readMoreTranslationId })
    : undefined;
  const errorMessage = intl.formatMessage({ id: errorTranslationId });
  const emptyMessage = intl.formatMessage({ id: emptyTranslationId });

  if (!loading) {
    if (error)
      return (
        <section className={styles["tasks-list"]}>
          <SectionHeader title={title} />
          <Paragraph>{errorMessage}</Paragraph>
        </section>
      );

    if (!appointments || appointments.length === 0) {
      if (!showEmpty) return null;
      return (
        <section className={styles["tasks-list"]}>
          <SectionHeader title={title} />
          <Paragraph>{emptyMessage}</Paragraph>
        </section>
      );
    }
  }

  return (
    <section className={styles["tasks-list"]}>
      <SectionHeader title={title} subTitle={subTitle} href={appointmentsPath} />
      {loading ? (
        <>
          {[...Array(3)].map((_, index) => (
            <Skeleton key={index} height={listViewHeight} />
          ))}
        </>
      ) : (
        appointments?.map((afspraak) => (
          <ActionSingle
            key={afspraak.afspraakIdentificatie}
            relativeDate
            labels={labels}
            dateTime={afspraak.geplandAanvangsmoment}
            locale={currentLocale}
            link={paths.appointment(afspraak.afspraakIdentificatie)}
            Link={PortalLink}
          >
            {afspraak.onderwerp}
          </ActionSingle>
        ))
      )}
    </section>
  );
};

export default AppointmentsList;
