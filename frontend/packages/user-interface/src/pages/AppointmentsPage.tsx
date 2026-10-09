import { useQuery } from "@apollo/client/react";
import { QUERY_GET_AFSPRAKEN } from "@nl-portal/nl-portal-api";
import { FormattedMessage, useIntl } from "react-intl";
import { useOutletContext } from "react-router";
import { Link } from "@gemeente-denhaag/link";
import { Paragraph } from "@gemeente-denhaag/typography";
import PageGrid from "../components/PageGrid";
import PageHeader from "../components/PageHeader";
import PortalLink from "../components/PortalLink";
import { RouterOutletContext } from "../interfaces/router-outlet-context";

const AFSPRAKEN_EMAIL = "m.kooyman@example.com";

interface AfspraakOverzicht {
    afspraakIdentificatie: string;
    onderwerp: string;
    geplandAanvangsmoment: string;
    geplandEindmoment: string;
}

interface GetAfsprakenData {
    getAfspraken: {
        afspraken: AfspraakOverzicht[];
        totalCount: number;
    };
}

const formatDatumTijd = (iso: string): string => {
    try {
        return new Date(iso).toLocaleString("nl-NL", {
            weekday: "long",
            day: "numeric",
            month: "long",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    } catch {
        return iso;
    }
};

const AppointmentsPage = () => {
    const intl = useIntl();
    const { paths } = useOutletContext<RouterOutletContext>();

    const { data, loading, error } = useQuery<GetAfsprakenData>(QUERY_GET_AFSPRAKEN, {
        variables: {
            identificaties: [{ type: "email", waarde: AFSPRAKEN_EMAIL }],
        },
        fetchPolicy: "cache-and-network",
    });

    const afspraken = data?.getAfspraken?.afspraken ?? [];

    return (
        <PageGrid variant="medium">
            <PageHeader title={intl.formatMessage({ id: "pageTitles.appointments" })} />

            {loading && (
                <Paragraph>
                    <FormattedMessage id="appointments.loading" />
                </Paragraph>
            )}

            {!loading && error && (
                <Paragraph>
                    <FormattedMessage id="appointments.error" />
                </Paragraph>
            )}

            {!loading && !error && afspraken.length === 0 && (
                <Paragraph>
                    <FormattedMessage id="appointments.empty" />
                </Paragraph>
            )}

            {!loading && !error && afspraken.length > 0 && (
                <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                    {afspraken.map((afspraak) => (
                        <li
                            key={afspraak.afspraakIdentificatie}
                            style={{ padding: "0.75rem 0", borderBottom: "1px solid var(--color-neutral-3, #e0e0e0)" }}
                        >
                            <Link
                                Link={PortalLink}
                                href={paths.appointment(afspraak.afspraakIdentificatie)}
                            >
                                {afspraak.onderwerp}
                            </Link>
                            <div style={{ marginTop: "0.25rem" }}>
                                <Paragraph>{formatDatumTijd(afspraak.geplandAanvangsmoment)}</Paragraph>
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </PageGrid>
    );
};

export default AppointmentsPage;
