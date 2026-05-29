import { onUnmounted, type Ref } from 'vue';

export function useScrambler() {
  const activeIntervals = new Map<any, number>();

  const scramble = (
    targetRef: Ref<string>,
    finalText: string,
    options?: {
      duration?: number; // total animation time in ms, default 1000
      speed?: number; // interval duration in ms, default 30
      scrambleChars?: string;
    }
  ) => {
    // Calculate dynamic duration based on text length (30ms per character), clamped between 2000ms and 4000ms
    const defaultDuration = Math.min(4000, Math.max(2000, finalText.length * 30));
    const duration = options?.duration ?? defaultDuration;
    const speed = options?.speed ?? 30;
    const scrambleChars =
      options?.scrambleChars ??
      '!@#$%^&*()_+{}|:"<>?-=[]\\;\',./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';

    // Clear any existing interval for this ref
    if (activeIntervals.has(targetRef)) {
      clearInterval(activeIntervals.get(targetRef));
      activeIntervals.delete(targetRef);
    }

    if (!finalText) {
      targetRef.value = '';
      return;
    }

    const length = finalText.length;
    const totalSteps = Math.ceil(duration / speed);
    let step = 0;

    const intervalId = window.setInterval(() => {
      step++;
      // Calculate how many characters should be "solved" (correct) by now
      const progress = step / totalSteps;
      const solvedCount = Math.floor(length * progress);

      let currentText = '';
      for (let i = 0; i < length; i++) {
        const char = finalText[i];
        if (i < solvedCount) {
          currentText += char;
        } else {
          // Keep spaces, newlines, and carriage returns intact
          if (char === ' ' || char === '\n' || char === '\r') {
            currentText += char;
          } else {
            // Pick a random scramble character
            const randomIndex = Math.floor(Math.random() * scrambleChars.length);
            currentText += scrambleChars[randomIndex];
          }
        }
      }

      targetRef.value = currentText;

      if (step >= totalSteps) {
        clearInterval(intervalId);
        activeIntervals.delete(targetRef);
        targetRef.value = finalText; // Ensure the final value is exactly correct
      }
    }, speed);

    activeIntervals.set(targetRef, intervalId);
  };

  const cancelScramble = (targetRef: Ref<string>) => {
    if (activeIntervals.has(targetRef)) {
      clearInterval(activeIntervals.get(targetRef));
      activeIntervals.delete(targetRef);
    }
  };

  // Clean up all timers when component unmounts
  onUnmounted(() => {
    for (const intervalId of activeIntervals.values()) {
      clearInterval(intervalId);
    }
    activeIntervals.clear();
  });

  return {
    scramble,
    cancelScramble,
  };
}
