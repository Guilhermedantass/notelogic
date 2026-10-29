/** 754_000 ms → "12:34". Arredonda para cima: o timer só mostra 00:00 quando o tempo realmente acabou. */
export function formatarRelogio(ms: number): string {
  const total = Math.max(0, Math.ceil(ms / 1000));
  const min = Math.floor(total / 60);
  const seg = total % 60;
  return `${String(min).padStart(2, '0')}:${String(seg).padStart(2, '0')}`;
}

/** 5400 s → "1h 30min"; 1500 s → "25 min". */
export function formatarDuracao(segundos: number): string {
  const min = Math.floor(segundos / 60);
  if (min < 60) return `${min} min`;
  const h = Math.floor(min / 60);
  const resto = min % 60;
  return resto === 0 ? `${h}h` : `${h}h ${resto}min`;
}

/** Um "plim" curto gerado na hora pelo navegador (Web Audio), sem arquivo de som. */
export function tocarAlarme(): void {
  const AudioCtx = window.AudioContext;
  if (!AudioCtx) return;
  const ctx = new AudioCtx();
  [0, 0.25, 0.5].forEach((atraso, i) => {
    const osc = ctx.createOscillator();
    const ganho = ctx.createGain();
    osc.frequency.value = [880, 1046, 1318][i];
    ganho.gain.setValueAtTime(0.0001, ctx.currentTime + atraso);
    ganho.gain.exponentialRampToValueAtTime(0.25, ctx.currentTime + atraso + 0.02);
    ganho.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + atraso + 0.4);
    osc.connect(ganho).connect(ctx.destination);
    osc.start(ctx.currentTime + atraso);
    osc.stop(ctx.currentTime + atraso + 0.45);
  });
  setTimeout(() => ctx.close(), 1500);
}
