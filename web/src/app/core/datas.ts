// Datas "de calendário" (prazo, dia do heatmap) são strings "AAAA-MM-DD", como o LocalDate da API.
// Nunca passam por new Date("2026-11-05") direto: isso vira meia-noite UTC e, no Brasil, mostra o dia anterior.

/** Hoje no fuso do navegador, no formato AAAA-MM-DD. */
export function hojeIso(agora: Date = new Date()): string {
  const ano = agora.getFullYear();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

export function somarDias(iso: string, dias: number): string {
  const [a, m, d] = iso.split('-').map(Number);
  return hojeIso(new Date(a, m - 1, d + dias));
}

/** "qui., 5 de nov." — lido como data local, sem conversão de fuso. */
export function formatarDia(iso: string, opcoes: Intl.DateTimeFormatOptions): string {
  const [a, m, d] = iso.split('-').map(Number);
  return new Intl.DateTimeFormat('pt-BR', opcoes).format(new Date(a, m - 1, d));
}

export type GrupoPrazo = 'atrasadas' | 'hoje' | 'proximas' | 'sem-prazo';

/** Comparar strings AAAA-MM-DD funciona porque a ordem alfabética coincide com a cronológica. */
export function grupoDoPrazo(prazo: string | null, hoje: string): GrupoPrazo {
  if (prazo === null) return 'sem-prazo';
  if (prazo < hoje) return 'atrasadas';
  if (prazo === hoje) return 'hoje';
  return 'proximas';
}

export function rotuloDoPrazo(prazo: string, hoje: string): string {
  if (prazo === hoje) return 'Hoje';
  if (prazo === somarDias(hoje, 1)) return 'Amanhã';
  if (prazo === somarDias(hoje, -1)) return 'Ontem';
  return formatarDia(prazo, { weekday: 'short', day: 'numeric', month: 'short' });
}
