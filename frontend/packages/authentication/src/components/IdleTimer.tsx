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
import { useEffect, useRef } from "react";

export interface IdleTimerProps {
  idleTimeoutMinutes: number;
  onTimeOut: () => void;
}

const BASIC_ACTIVITY_EVENTS: string[] = [
  "mousedown",
  "keydown",
  "pointerdown",
  "touchstart",
  "scroll",
  "wheel",
  "audiostart",
];

const calculateTimeoutMilliseconds = (minutes: number) => 1000 * 60 * minutes;

const IdleTimer = ({ idleTimeoutMinutes, onTimeOut }: IdleTimerProps) => {
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const resetTimer = () => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    timeoutRef.current = setTimeout(
      onTimeOut,
      calculateTimeoutMilliseconds(idleTimeoutMinutes),
    ); // Should be 15 minutes to comply with DigiD requirements
  };

  useEffect(() => {
    // initiate timeout
    resetTimer();

    // listen for activity events
    BASIC_ACTIVITY_EVENTS.forEach((eventType) => {
      window.addEventListener(eventType, resetTimer);
    });

    // cleanup
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
        BASIC_ACTIVITY_EVENTS.forEach((eventType) => {
          window.removeEventListener(eventType, resetTimer);
        });
      }
    };
  }, []);

  return null;
};

export default IdleTimer;
