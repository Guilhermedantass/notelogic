import { httpResource } from '@angular/common/http';
import { Component, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { Baralho as BaralhoModel, Cartao, Materia } from '../core/api.models';
import { hojeIso, rotuloDoPrazo } from '../core/datas';
import { mensagemDeErro } from '../core/erros';
import { FlashcardService } from '../core/flashcard.service';

/** Um baralho: adicionar cartões, ver quando cada um volta, editar e apagar. */
@Component({
  selector: 'app-baralho',
  imports: [FormsModule, RouterLink],
  templateUrl: './baralho.html',
  styleUrl: './baralho.scss',
})
export class Baralho {
  private readonly service = inject(FlashcardService);
  private readonly router = inject(Router);

  /** O :id da rota chega aqui (withComponentInputBinding) e vira número. */
  readonly id = input.required({ transform: numberAttribute });

  protected readonly baralho = httpResource<BaralhoModel>(() => `/api/baralhos/${this.id()}`);
  protected readonly cartoes = httpResource<Cartao[]>(() => `/api/baralhos/${this.id()}/cartoes`, {
    defaultValue: [],
  });
  protected readonly materias = httpResource<Materia[]>(() => '/api/materias', { defaultValue: [] });

  protected readonly lista = computed(() => (this.cartoes.hasValue() ? this.cartoes.value() : []));
  protected readonly listaMaterias = computed(() =>
    this.materias.hasValue() ? this.materias.value() : [],
  );

  protected readonly hoje = hojeIso();
  protected readonly erro = signal<string | null>(null);

  protected frente = '';
  protected verso = '';

  protected readonly editandoBaralho = signal(false);
  protected rascunhoBaralho = { nome: '', materiaId: null as number | null };

  protected readonly editandoCartao = signal<number | null>(null);
  protected rascunhoCartao = { frente: '', verso: '' };

  protected quandoVolta(c: Cartao): string {
    return c.proximaRevisao <= this.hoje ? 'Para hoje' : rotuloDoPrazo(c.proximaRevisao, this.hoje);
  }

  protected adicionar(campoFrente: HTMLTextAreaElement): void {
    if (!this.frente.trim() || !this.verso.trim()) return;
    this.executar(
      this.service.criarCartao(this.id(), { frente: this.frente.trim(), verso: this.verso.trim() }),
      () => {
        this.frente = '';
        this.verso = '';
        campoFrente.focus(); // pronto para o próximo cartão
      },
    );
  }

  /** Ctrl+Enter (ou Cmd+Enter) salva sem tirar a mão do teclado. */
  protected atalho(evento: KeyboardEvent, campoFrente: HTMLTextAreaElement): void {
    if (evento.key === 'Enter' && (evento.ctrlKey || evento.metaKey)) {
      evento.preventDefault();
      this.adicionar(campoFrente);
    }
  }

  protected editarCartao(c: Cartao): void {
    this.rascunhoCartao = { frente: c.frente, verso: c.verso };
    this.editandoCartao.set(c.id);
  }

  protected salvarCartao(c: Cartao): void {
    this.executar(this.service.editarCartao(c.id, this.rascunhoCartao), () =>
      this.editandoCartao.set(null),
    );
  }

  protected removerCartao(c: Cartao): void {
    if (confirm('Apagar este cartão?')) {
      this.executar(this.service.removerCartao(c.id));
    }
  }

  protected editarBaralho(b: BaralhoModel): void {
    this.rascunhoBaralho = { nome: b.nome, materiaId: b.materia?.id ?? null };
    this.editandoBaralho.set(true);
  }

  protected salvarBaralho(): void {
    this.executar(this.service.editarBaralho(this.id(), this.rascunhoBaralho), () =>
      this.editandoBaralho.set(false),
    );
  }

  protected removerBaralho(b: BaralhoModel): void {
    if (confirm(`Apagar o baralho "${b.nome}" e todos os ${b.totalCartoes} cartões?`)) {
      this.service.removerBaralho(this.id()).subscribe({
        next: () => this.router.navigate(['/flashcards']),
        error: (e) => this.erro.set(mensagemDeErro(e)),
      });
    }
  }

  private executar(acao: Observable<unknown>, depois?: () => void): void {
    this.erro.set(null);
    acao.subscribe({
      next: () => {
        depois?.();
        this.cartoes.reload();
        this.baralho.reload(); // as contagens do cabeçalho mudam
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
