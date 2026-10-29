import { Component, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { AuthService } from '../core/auth.service';
import { mensagemDeErro } from '../core/erros';
import { Logo } from '../layout/logo';

/**
 * Entrar e Criar conta numa tela só (o "data.modo" da rota escolhe qual).
 * Volta à ideia da v1, formulário de um lado e ilustração do outro, no visual novo.
 */
@Component({
  selector: 'app-acesso',
  imports: [FormsModule, RouterLink, Logo],
  templateUrl: './acesso.html',
  styleUrl: './acesso.scss',
})
export class Acesso {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly modo = input<'entrar' | 'cadastro'>('entrar');
  /** ?voltar=/flashcards: depois de entrar, volta para onde a pessoa tentou ir. */
  readonly voltar = input<string>();

  protected readonly cadastro = computed(() => this.modo() === 'cadastro');
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mostrarSenha = signal(false);

  protected nome = '';
  protected email = '';
  protected senha = '';

  protected enviar(): void {
    if (this.enviando()) return;
    this.erro.set(null);
    this.enviando.set(true);

    const acao: Observable<unknown> = this.cadastro()
      ? this.auth.cadastrar(this.nome, this.email, this.senha)
      : this.auth.entrar(this.email, this.senha);

    acao.subscribe({
      next: () => this.router.navigateByUrl(this.voltar() || '/hoje'),
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }
}
