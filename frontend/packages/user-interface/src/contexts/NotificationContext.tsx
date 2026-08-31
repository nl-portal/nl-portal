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
import { createContext, ReactNode, useLayoutEffect, useReducer } from "react";
import { NotificationProps } from "../components/Notification";
import { useLocation } from "react-router";
import { isEqual } from "lodash-es";
import { FormattedMessage } from "react-intl";

type State = Record<string, NotificationProps>;
export type Action =
  | { type: "CREATE"; id: string; notification: NotificationProps }
  | { type: "REMOVE"; id: string }
  | { type: "CLEAR_ROUTE" };

const reducer = (state: State, action: Action) => {
  switch (action.type) {
    case "CREATE": {
      const prevNotification = state[action.id];
      if (prevNotification && isEqual(action.notification, prevNotification)) {
        return state;
      }
      return { ...state, [action.id]: action.notification };
    }
    case "REMOVE": {
      if (!state[action.id]) return state;
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { [action.id]: _, ...rest } = state;
      return rest;
    }
    case "CLEAR_ROUTE":
      return {};
    default:
      return state;
  }
};

interface NotificationContextType {
  state: State;
  dispatch: React.Dispatch<Action>;
}

const NotificationContext = createContext<NotificationContextType>(
  {} as NotificationContextType,
);

interface NotificationProviderProps {
  children: ReactNode;
}

export const NotificationProvider = ({
  children,
}: NotificationProviderProps) => {
  const [state, dispatch] = useReducer(reducer, {});
  const location = useLocation();

  useLayoutEffect(() => {
    dispatch({ type: "CLEAR_ROUTE" });
    // Check if there is an notification passed via state
    const notificationState = location.state?.notification;
    if (notificationState) {
      const { id, titleMessage, textMessage, variant } = notificationState;
      dispatch({
        id,
        type: "CREATE",
        notification: {
          variant,
          title: titleMessage && <FormattedMessage {...titleMessage} />,
          text: textMessage && <FormattedMessage {...textMessage} />,
        },
      });
    }
  }, [location.pathname]);

  return (
    <NotificationContext.Provider value={{ state, dispatch }}>
      {children}
    </NotificationContext.Provider>
  );
};

export default NotificationContext;
