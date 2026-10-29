import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Baralho, BaralhoRequest, Cartao, CartaoRequest, Resposta } from './api.models';

@Injectable({ providedIn: 'root' })
export class FlashcardService {
  private readonly http = inject(HttpClient);

  criarBaralho(dados: BaralhoRequest): Observable<Baralho> {
    return this.http.post<Baralho>('/api/baralhos', dados);
  }

  editarBaralho(id: number, dados: BaralhoRequest): Observable<Baralho> {
    return this.http.put<Baralho>(`/api/baralhos/${id}`, dados);
  }

  removerBaralho(id: number): Observable<void> {
    return this.http.delete<void>(`/api/baralhos/${id}`);
  }

  filaDeEstudo(baralhoId: number): Observable<Cartao[]> {
    return this.http.get<Cartao[]>(`/api/baralhos/${baralhoId}/estudo`);
  }

  criarCartao(baralhoId: number, dados: CartaoRequest): Observable<Cartao> {
    return this.http.post<Cartao>(`/api/baralhos/${baralhoId}/cartoes`, dados);
  }

  editarCartao(id: number, dados: CartaoRequest): Observable<Cartao> {
    return this.http.put<Cartao>(`/api/cartoes/${id}`, dados);
  }

  removerCartao(id: number): Observable<void> {
    return this.http.delete<void>(`/api/cartoes/${id}`);
  }

  revisar(id: number, resposta: Resposta): Observable<Cartao> {
    return this.http.post<Cartao>(`/api/cartoes/${id}/revisao`, { resposta });
  }
}
