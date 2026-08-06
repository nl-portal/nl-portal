import { FormatNumberOptions } from "react-intl";

export const currencyFormat: FormatNumberOptions = {
  style: "currency",
  currency: "EUR",
};

export const capitalizeFirstLetter = (text: string): string =>
  text.charAt(0).toUpperCase() + text.slice(1);
