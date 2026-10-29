import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Materia, MateriaRequest } from './api.models';

@Injectable({ providedIn: 'root' })
export class MateriaService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/materias';

  criar(dados: MateriaRequest): Observable<Materia> {
    return this.http.post<Materia>(this.url, dados);
  }

  editar(id: number, dados: MateriaRequest): Observable<Materia> {
    return this.http.put<Materia>(`${this.url}/${id}`, dados);
  }

  remover(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
