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
import { useEffect, useState } from "react";

export type Status = "idle" | "running" | "paused" | "finished";

interface UseCountdownReturn {
  timeLeft: number;
  isActive: boolean;
  start: () => void;
  pause: () => void;
  resume: () => void;
  reset: (initialTime?: number) => void;
}

const useCountdown = (initialTime: number): UseCountdownReturn => {
  const [timeLeft, setTimeLeft] = useState(initialTime);
  const [isActive, setIsActive] = useState(false);

  useEffect(() => {
    if (!isActive) return;

    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          setIsActive(false);
          return 0;
        }
        return prev - 1000;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isActive]);

  const start = () => {
    if (timeLeft > 0) {
      setIsActive(true);
    }
  };

  const pause = () => {
    setIsActive(false);
  };

  const resume = () => {
    if (timeLeft > 0) {
      setIsActive(true);
    }
  };

  const reset = (newTime: number = initialTime) => {
    setIsActive(false);
    setTimeLeft(newTime);
  };

  return {
    timeLeft,
    isActive,
    start,
    pause,
    resume,
    reset,
  };
};

export default useCountdown;
