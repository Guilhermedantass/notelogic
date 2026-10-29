import { httpResource } from '@angular/common/http';
import { Component, computed, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Baralho, Cartao, Resposta } from '../core/api.models';
import { mensagemDeErro } from '../core/erros';
import { FlashcardService } from '../core/flashcard.service';
import { agendar, formatarIntervalo } from '../core/sm2';

interface Botao {
  resposta: Resposta;
  rotulo: string;
  tecla: string;
}

const BOTOES: Botao[] = [
  { resposta: 'ERREI', rotulo: 'Errei', tecla: '1' },
  { resposta: 'DIFICIL', rotulo: 'Difícil', tecla: '2' },
  { resposta: 'BOM', rotulo: 'Bom', tecla: '3' },
  { resposta: 'FACIL', rotulo: 'Fácil', tecla: '4' },
];

/**
 * A sessão de estudo. A fila é carregada uma vez e controlada aqui:
 * errou, o cartão volta para o fim da fila; acertou, sai. Acabou a fila, mostra o resumo.
 */
@Component({
  selector: 'app-estudo',
  imports: [RouterLink],
  templateUrl: './estudo.html',
  styleUrl: './estudo.scss',
  host: { '(document:keydown)': 'tecla($event)' },
})
export class Estudo implements OnInit {
  private readonly service = inject(FlashcardService);

  readonly id = input.required({ transform: numberAttribute });

  protected readonly baralho = httpResource<Baralho>(() => `/api/baralhos/${this.id()}`);
  protected readonly botoes = BOTOES;

  protected readonly fila = signal<Cartao[]>([]);
  protected readonly carregado = signal(false);
  protected readonly virado = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  /** Quantos cartões únicos entraram na sessão e quantos já saíram dela. */
  protected readonly total = signal(0);
  protected readonly concluidos = signal(0);
  protected readonly respostas = signal<Record<Resposta, number>>({ ERREI: 0, DIFICIL: 0, BOM: 0, FACIL: 0 });

  protected readonly atual = computed(() => this.fila()[0] ?? null);
  protected readonly terminou = computed(() => this.carregado() && this.fila().length === 0);
  protected readonly progresso = computed(() =>
    this.total() === 0 ? 0 : (this.concluidos() / this.total()) * 100,
  );

  /** O que cada botão faria com o cartão atual, calculado pela cópia do SM-2. */
  protected readonly previsoes = computed(() => {
    const c = this.atual();
    if (!c) return {} as Record<Resposta, string>;
    const estado = { repeticoes: c.repeticoes, intervaloDias: c.intervaloDias, facilidade: c.facilidade };
    return Object.fromEntries(
      BOTOES.map((b) => [b.resposta, formatarIntervalo(agendar(estado, b.resposta).intervaloDias)]),
    ) as Record<Resposta, string>;
  });

  ngOnInit(): void {
    this.service.filaDeEstudo(this.id()).subscribe({
      next: (cartoes) => {
        this.fila.set(cartoes);
        this.total.set(cartoes.length);
        this.carregado.set(true);
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  protected responder(resposta: Resposta): void {
    const cartao = this.atual();
    if (!cartao || !this.virado() || this.enviando()) return;

    this.enviando.set(true);
    this.service.revisar(cartao.id, resposta).subscribe({
      next: (atualizado) => {
        this.respostas.update((r) => ({ ...r, [resposta]: r[resposta] + 1 }));
        const voltaHoje = atualizado.intervaloDias === 0;
        this.fila.update((f) => (voltaHoje ? [...f.slice(1), atualizado] : f.slice(1)));
        if (!voltaHoje) this.concluidos.update((n) => n + 1);
        this.virado.set(false);
        this.enviando.set(false);
      },
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }

  /** Espaço ou Enter vira o cartão; 1 a 4 respondem. */
  protected tecla(evento: KeyboardEvent): void {
    if (!this.atual()) return;
    if (evento.key === ' ' || evento.key === 'Enter') {
      evento.preventDefault();
      if (!this.virado()) this.virado.set(true);
      return;
    }
    const botao = BOTOES.find((b) => b.tecla === evento.key);
    if (botao) this.responder(botao.resposta);
  }
}
