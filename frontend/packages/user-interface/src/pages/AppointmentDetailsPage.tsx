import { useQuery } from "@apollo/client/react";
import { QUERY_GET_AFSPRAAK } from "@nl-portal/nl-portal-api";
import { FormattedMessage, useIntl } from "react-intl";
import { useOutletContext, useParams } from "react-router";
import { Paragraph } from "@gemeente-denhaag/typography";
import BackLink from "../components/BackLink";
import PageGrid from "../components/PageGrid";
import PageHeader from "../components/PageHeader";
import { RouterOutletContext } from "../interfaces/router-outlet-context";

interface Adres {
    adresregel1: string;
    adresregel2: string;
}

interface AfspraakLocatie {
    naam: string;
    bezoekadres?: Adres | null;
}

interface Activiteit {
    naamActiviteitsoort: string;
    aantal: number;
    aanduidingWatMeeTeNemen?: string | null;
}

interface ContactInformatie {
    naam: string;
    emailadres: string;
    telefoonnummer: string;
    adres: Adres;
}

interface AfspraakDetail {
    afspraakIdentificatie: string;
    afspraakSoort: string;
    onderwerp: string;
    geplandAanvangsmoment: string;
    geplandEindmoment: string;
    aanmeldkenmerkAlfanumeriek?: string | null;
    onlineAdres?: string | null;
    afspraakLocatie?: AfspraakLocatie | null;
    activiteiten: Activiteit[];
    contactInformatie: ContactInformatie;
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

const AppointmentDetailsPage = () => {
    const intl = useIntl();
    const { id } = useParams<{ id: string }>();
    const { paths } = useOutletContext<RouterOutletContext>();

    interface GetAfspraakData {
        getAfspraak: AfspraakDetail | null;
    }

    const { data, loading, error } = useQuery<GetAfspraakData>(QUERY_GET_AFSPRAAK, {
        variables: { id },
        skip: !id,
    });

    const afspraak = data?.getAfspraak;

    if (loading) {
        return (
            <PageGrid variant="medium">
                <BackLink href={paths.appointments as string} />
                <PageHeader loading title="" />
            </PageGrid>
        );
    }

    if (error || !afspraak) {
        return (
            <PageGrid variant="medium">
                <BackLink href={paths.appointments as string} />
                <PageHeader title={intl.formatMessage({ id: "appointments.notFound" })} />
            </PageGrid>
        );
    }

    const isOnline = afspraak.afspraakSoort === "online";

    return (
        <PageGrid variant="medium">
            <div>
                <BackLink href={paths.appointments as string} />
                <PageHeader title={afspraak.onderwerp}>
                    <Paragraph>
                        {formatDatumTijd(afspraak.geplandAanvangsmoment)}
                        {" — "}
                        {formatDatumTijd(afspraak.geplandEindmoment)}
                    </Paragraph>
                </PageHeader>
            </div>

            {isOnline ? (
                <section>
                    <Paragraph>
                        <strong>
                            <FormattedMessage id="appointments.onlineLink" />
                        </strong>{" "}
                        <a href={afspraak.onlineAdres ?? "#"}>{afspraak.onlineAdres}</a>
                    </Paragraph>
                </section>
            ) : (
                <section>
                    {afspraak.aanmeldkenmerkAlfanumeriek && (
                        <Paragraph>
                            <strong>
                                <FormattedMessage id="appointments.aanmeldkenmerk" />
                            </strong>{" "}
                            {afspraak.aanmeldkenmerkAlfanumeriek}
                        </Paragraph>
                    )}
                    {afspraak.afspraakLocatie && (
                        <>
                            <Paragraph>
                                <strong>
                                    <FormattedMessage id="appointments.location" />
                                </strong>{" "}
                                {afspraak.afspraakLocatie.naam}
                            </Paragraph>
                            {afspraak.afspraakLocatie.bezoekadres && (
                                <Paragraph>
                                    {afspraak.afspraakLocatie.bezoekadres.adresregel1}
                                    <br />
                                    {afspraak.afspraakLocatie.bezoekadres.adresregel2}
                                </Paragraph>
                            )}
                        </>
                    )}
                </section>
            )}

            {afspraak.activiteiten.length > 0 && (
                <section>
                    <Paragraph>
                        <strong>
                            <FormattedMessage id="appointments.activities" />
                        </strong>
                    </Paragraph>
                    <ul style={{ paddingLeft: "1.25rem", margin: 0 }}>
                        {afspraak.activiteiten.map((activiteit, index) => (
                            <li key={index}>
                                {activiteit.aantal}× {activiteit.naamActiviteitsoort}
                                {activiteit.aanduidingWatMeeTeNemen && (
                                    <div style={{ margin: "0.25rem 0 0", fontStyle: "italic" }}>
                                        <Paragraph>
                                            <FormattedMessage id="appointments.bringAlong" />:{" "}
                                            {activiteit.aanduidingWatMeeTeNemen}
                                        </Paragraph>
                                    </div>
                                )}
                            </li>
                        ))}
                    </ul>
                </section>
            )}

            <section>
                <Paragraph>
                    <strong>
                        <FormattedMessage id="appointments.contact" />
                    </strong>
                </Paragraph>
                <Paragraph>
                    {afspraak.contactInformatie.naam}
                    <br />
                    <a href={`mailto:${afspraak.contactInformatie.emailadres}`}>
                        {afspraak.contactInformatie.emailadres}
                    </a>
                    <br />
                    {afspraak.contactInformatie.telefoonnummer}
                </Paragraph>
            </section>
        </PageGrid>
    );
};

export default AppointmentDetailsPage;
