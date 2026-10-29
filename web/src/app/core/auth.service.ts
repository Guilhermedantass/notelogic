import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
}

interface SessaoResponse {
  token: string;
  usuario: Usuario;
}

const CHAVE_TOKEN = 'notelogic.token';
const CHAVE_USUARIO = 'notelogic.usuario';

/**
 * Guarda quem está logado. O token fica no localStorage para sobreviver ao F5;
 * o interceptor (auth.interceptor.ts) o coloca em toda requisição.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _token = signal<string | null>(localStorage.getItem(CHAVE_TOKEN));
  readonly usuario = signal<Usuario | null>(lerUsuario());

  readonly token = this._token.asReadonly();
  readonly logado = computed(() => this._token() !== null && !expirou(this._token()!));

  entrar(email: string, senha: string): Observable<SessaoResponse> {
    return this.http
      .post<SessaoResponse>('/api/auth/login', { email, senha })
      .pipe(tap((s) => this.guardar(s)));
  }

  cadastrar(nome: string, email: string, senha: string): Observable<SessaoResponse> {
    return this.http
      .post<SessaoResponse>('/api/auth/cadastro', { nome, email, senha })
      .pipe(tap((s) => this.guardar(s)));
  }

  sair(): void {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_USUARIO);
    this._token.set(null);
    this.usuario.set(null);
    this.router.navigate(['/entrar']);
  }

  private guardar(sessao: SessaoResponse): void {
    localStorage.setItem(CHAVE_TOKEN, sessao.token);
    localStorage.setItem(CHAVE_USUARIO, JSON.stringify(sessao.usuario));
    this._token.set(sessao.token);
    this.usuario.set(sessao.usuario);
  }
}

function lerUsuario(): Usuario | null {
  const salvo = localStorage.getItem(CHAVE_USUARIO);
  return salvo ? (JSON.parse(salvo) as Usuario) : null;
}

/**
 * O meio do JWT é JSON em base64: dá para LER a data de expiração sem o segredo.
 * (Só o servidor consegue conferir a assinatura; aqui é só para não tentar usar um token já vencido.)
 */
export function expirou(token: string, agora = Date.now()): boolean {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const { exp } = JSON.parse(atob(base64)) as { exp?: number };
    return exp !== undefined && exp * 1000 <= agora;
  } catch {
    return true;
  }
}
