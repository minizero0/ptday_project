import { useEffect, useRef } from 'react';

interface Tone {
  frequency: number; // Hz
  durationMs: number;
}

// 출석 완료: 올라가는 두 음. 만료 임박: 같은 음 뒤에 두 번 더 울려 화면을 보게 한다. 실패: 낮은 한 음.
const SUCCESS_TONES: Tone[] = [
  { frequency: 880, durationMs: 130 },
  { frequency: 1175, durationMs: 220 },
];
const NOTICE_TONES: Tone[] = [
  ...SUCCESS_TONES,
  { frequency: 880, durationMs: 130 },
  { frequency: 880, durationMs: 220 },
];
const ERROR_TONES: Tone[] = [{ frequency: 220, durationMs: 380 }];

const VOLUME = 0.25;
const SILENCE = 0.001; // 지수 감쇠는 0 까지 갈 수 없어 아주 작은 값으로 끝낸다
const MILLIS_PER_SECOND = 1000;

function playTones(context: AudioContext, tones: Tone[]): void {
  let startAt = context.currentTime;
  tones.forEach((tone) => {
    const endAt = startAt + tone.durationMs / MILLIS_PER_SECOND;
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    oscillator.type = 'sine';
    oscillator.frequency.value = tone.frequency;
    gain.gain.setValueAtTime(VOLUME, startAt);
    gain.gain.exponentialRampToValueAtTime(SILENCE, endAt);
    oscillator.connect(gain).connect(context.destination);
    oscillator.start(startAt);
    oscillator.stop(endAt);
    startAt = endAt;
  });
}

/**
 * 출석 키오스크의 안내음. 음원 파일 없이 브라우저가 직접 소리를 만든다.
 * 브라우저는 사용자가 화면을 누르기 전에는 소리를 막는데, 출석은 항상 번호를 누른 뒤라 문제가 없다.
 * 소리는 보조 수단이라 재생에 실패해도 출석 흐름을 막지 않는다.
 */
export function useKioskSound() {
  const contextRef = useRef<AudioContext | null>(null);

  useEffect(
    () => () => {
      void contextRef.current?.close();
    },
    [],
  );

  const play = (tones: Tone[]) => {
    try {
      const context = contextRef.current ?? new AudioContext();
      contextRef.current = context;
      if (context.state === 'suspended') {
        void context.resume();
      }
      playTones(context, tones);
    } catch {
      // 소리를 낼 수 없는 기기·브라우저. 화면 안내만으로도 출석은 끝난다.
    }
  };

  return {
    playSuccess: () => play(SUCCESS_TONES),
    playNotice: () => play(NOTICE_TONES),
    playError: () => play(ERROR_TONES),
  };
}
