import { httpResource } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';

import { DiaEstatistica, Estatisticas } from '../core/api.models';
import { formatarDia } from '../core/datas';
import { formatarDuracao } from '../core/tempo';
import { atividade, nivel, rotulosDeMes, semanas, topoDoEixo } from './graficos';

interface Dica {
  x: number;
  y: number;
  titulo: string;
  linhas: string[];
}

const PERIODOS = [7, 30, 90];
/** 52 semanas: o heatmap mostra sempre o último ano, independente do período escolhido. */
const DIAS_HEATMAP = 364;

@Component({
  selector: 'app-painel',
  templateUrl: './painel.html',
  styleUrl: './painel.scss',
})
export class Painel {
  protected readonly periodos = PERIODOS;
  protected readonly periodo = signal(30);
  protected readonly dica = signal<Dica | null>(null);
  protected readonly mostrarTabela = signal(false);
  protected readonly formatarDuracao = formatarDuracao;
  protected readonly nivel = nivel;
  protected readonly atividade = atividade;

  protected readonly estatisticas = httpResource<Estatisticas>(() => ({
    url: '/api/estatisticas',
    params: { dias: this.periodo() },
  }));

  protected readonly historico = httpResource<Estatisticas>(() => ({
    url: '/api/estatisticas',
    params: { dias: DIAS_HEATMAP },
  }));

  protected readonly dados = computed(() =>
    this.estatisticas.hasValue() ? this.estatisticas.value() : null,
  );

  // ---- barras de foco por dia ----
  protected readonly topo = computed(() =>
    topoDoEixo(Math.max(0, ...(this.dados()?.dias ?? []).map((d) => d.minutosFocados))),
  );
  protected readonly ticks = computed(() => [0, 1, 2, 3].map((i) => (this.topo() / 3) * i));
  protected readonly totalFoco = computed(() =>
    (this.dados()?.dias ?? []).reduce((s, d) => s + d.minutosFocados, 0),
  );
  protected readonly rotulosX = computed(() => {
    const dias = this.dados()?.dias ?? [];
    if (dias.length === 0) return [];
    const indices = dias.length <= 7 ? dias.map((_, i) => i) : [0, Math.floor(dias.length / 2), dias.length - 1];
    return indices.map((i) => ({
      posicao: ((i + 0.5) / dias.length) * 100,
      rotulo:
        dias.length <= 7
          ? formatarDia(dias[i].data, { weekday: 'short' }).replace('.', '')
          : formatarDia(dias[i].data, { day: 'numeric', month: 'short' }).replace('.', ''),
    }));
  });

  // ---- foco por matéria ----
  protected readonly maiorMateria = computed(() =>
    Math.max(1, ...(this.dados()?.focoPorMateria ?? []).map((m) => m.minutos)),
  );

  // ---- acertos ----
  protected readonly revisoesNoPeriodo = computed(() =>
    (this.dados()?.dias ?? []).reduce((s, d) => s + d.cartoesRevisados, 0),
  );
  protected readonly taxa = computed(() => {
    const t = this.dados()?.taxaDeAcerto;
    return t === null || t === undefined ? null : Math.round(t * 100);
  });

  // ---- heatmap ----
  protected readonly colunas = computed(() =>
    semanas(this.historico.hasValue() ? this.historico.value().dias : []),
  );
  protected readonly meses = computed(() => rotulosDeMes(this.colunas()));
  protected readonly diasEstudados = computed(
    () => (this.historico.hasValue() ? this.historico.value().dias : []).filter((d) => atividade(d) > 0).length,
  );

  protected mostrarDica(evento: MouseEvent, dia: DiaEstatistica): void {
    // na coluna, a dica fica em cima da barra (e não no topo da faixa inteira)
    const elemento = evento.target as HTMLElement;
    const alvo = (elemento.querySelector('.barra') ?? elemento).getBoundingClientRect();
    this.dica.set({
      x: alvo.left + alvo.width / 2,
      y: alvo.top,
      titulo: formatarDia(dia.data, { weekday: 'long', day: 'numeric', month: 'long' }),
      linhas: [
        `${formatarDuracao(dia.minutosFocados * 60)} de foco`,
        `${dia.cartoesRevisados} ${dia.cartoesRevisados === 1 ? 'cartão revisado' : 'cartões revisados'}`,
        `${dia.tarefasConcluidas} ${dia.tarefasConcluidas === 1 ? 'tarefa concluída' : 'tarefas concluídas'}`,
      ],
    });
  }

  protected rotuloCurto(data: string): string {
    return formatarDia(data, { day: '2-digit', month: '2-digit' });
  }
}
