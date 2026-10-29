import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Baralho, Materia } from '../core/api.models';
import { mensagemDeErro } from '../core/erros';
import { FlashcardService } from '../core/flashcard.service';

/** Grade de baralhos: cada card mostra a matéria, o total de cartões e quantos vencem hoje. */
@Component({
  selector: 'app-baralhos',
  imports: [FormsModule, RouterLink],
  templateUrl: './baralhos.html',
  styleUrl: './baralhos.scss',
})
export class Baralhos {
  private readonly service = inject(FlashcardService);

  protected readonly baralhos = httpResource<Baralho[]>(() => '/api/baralhos', { defaultValue: [] });
  protected readonly materias = httpResource<Materia[]>(() => '/api/materias', { defaultValue: [] });

  protected readonly lista = computed(() => (this.baralhos.hasValue() ? this.baralhos.value() : []));
  protected readonly listaMaterias = computed(() =>
    this.materias.hasValue() ? this.materias.value() : [],
  );
  protected readonly totalParaHoje = computed(() =>
    this.lista().reduce((soma, b) => soma + b.paraRevisarHoje, 0),
  );

  protected readonly criando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected nome = '';
  protected materiaId: number | null = null;

  protected criar(): void {
    if (!this.nome.trim()) return;
    this.service.criarBaralho({ nome: this.nome.trim(), materiaId: this.materiaId }).subscribe({
      next: () => {
        this.nome = '';
        this.materiaId = null;
        this.criando.set(false);
        this.baralhos.reload();
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
