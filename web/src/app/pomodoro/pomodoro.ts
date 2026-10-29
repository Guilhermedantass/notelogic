import { httpResource } from '@angular/common/http';
import { Component, computed, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { Materia, Sessao, Tarefa, TipoSessao } from '../core/api.models';
import { mensagemDeErro } from '../core/erros';
import { PomodoroService } from '../core/pomodoro.service';
import { formatarDuracao, formatarRelogio, tocarAlarme } from '../core/tempo';

const TIPOS: { valor: TipoSessao; rotulo: string; minutos: number }[] = [
  { valor: 'FOCO', rotulo: 'Foco', minutos: 25 },
  { valor: 'PAUSA_CURTA', rotulo: 'Pausa curta', minutos: 5 },
  { valor: 'PAUSA_LONGA', rotulo: 'Pausa longa', minutos: 15 },
];

const RAIO = 130;
const CIRCUNFERENCIA = 2 * Math.PI * RAIO;

/**
 * O timer nunca "conta" sozinho: ele sempre calcula fimPrevisto (do servidor) menos a hora atual.
 * Assim um F5, uma aba em segundo plano ou o computador dormindo não fazem o tempo atrasar.
 */
@Component({
  selector: 'app-pomodoro',
  imports: [FormsModule],
  templateUrl: './pomodoro.html',
  styleUrl: './pomodoro.scss',
})
export class Pomodoro {
  private readonly service = inject(PomodoroService);

  protected readonly tipos = TIPOS;
  protected readonly raio = RAIO;
  protected readonly circunferencia = CIRCUNFERENCIA;
  protected readonly formatarDuracao = formatarDuracao;
  protected readonly Math = Math;

  // ---- estado ----
  protected readonly sessao = signal<Sessao | null>(null);
  protected readonly carregado = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly agora = signal(Date.now());
  private finalizando = false;

  // ---- configuração da próxima sessão ----
  protected readonly tipo = signal<TipoSessao>('FOCO');
  protected readonly duracao = signal(25);
  protected tarefaId: number | null = null;
  protected materiaId: number | null = null;

  // ---- dados de apoio ----
  protected readonly tarefas = httpResource<Tarefa[]>(
    () => ({ url: '/api/tarefas', params: { concluida: false } }),
    { defaultValue: [] },
  );
  protected readonly materias = httpResource<Materia[]>(() => '/api/materias', { defaultValue: [] });
  protected readonly historico = httpResource<Sessao[]>(() => '/api/pomodoro/sessoes', {
    defaultValue: [],
  });

  protected readonly listaTarefas = computed(() => (this.tarefas.hasValue() ? this.tarefas.value() : []));
  protected readonly listaMaterias = computed(() =>
    this.materias.hasValue() ? this.materias.value() : [],
  );
  protected readonly sessoesDeHoje = computed(() =>
    (this.historico.hasValue() ? this.historico.value() : []).filter((s) => s.status !== 'EM_ANDAMENTO'),
  );
  protected readonly focosConcluidos = computed(
    () => this.sessoesDeHoje().filter((s) => s.tipo === 'FOCO' && s.status === 'CONCLUIDA').length,
  );
  protected readonly segundosDeFocoHoje = computed(() =>
    this.sessoesDeHoje()
      .filter((s) => s.tipo === 'FOCO')
      .reduce((soma, s) => soma + (s.segundosFocados ?? 0), 0),
  );

  // ---- o relógio ----
  protected readonly restanteMs = computed(() => {
    const s = this.sessao();
    return s ? Math.max(0, Date.parse(s.fimPrevisto) - this.agora()) : this.duracao() * 60_000;
  });

  /** 0 no começo, 1 no fim: vira o stroke-dashoffset do círculo. */
  protected readonly progresso = computed(() => {
    const s = this.sessao();
    if (!s) return 0;
    return 1 - this.restanteMs() / (s.duracaoPlanejadaMin * 60_000);
  });

  protected readonly relogio = computed(() => formatarRelogio(this.restanteMs()));
  protected readonly tipoAtual = computed(() => this.sessao()?.tipo ?? this.tipo());
  protected readonly rotuloTipo = computed(() => TIPOS.find((t) => t.valor === this.tipoAtual())!.rotulo);

  constructor() {
    const intervalo = setInterval(() => this.agora.set(Date.now()), 250);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(intervalo);
      document.title = 'Pomodoro · NoteLogic';
    });

    // Retoma a sessão que estava rodando (depois de um F5, por exemplo)
    this.service.atual().subscribe({
      next: (s) => {
        this.sessao.set(s);
        this.carregado.set(true);
      },
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        this.carregado.set(true);
      },
    });

    // O tempo no título da aba: dá para acompanhar de outra aba
    effect(() => {
      document.title = this.sessao() ? `${this.relogio()} · ${this.rotuloTipo()}` : 'Pomodoro · NoteLogic';
    });

    // Chegou a zero: conclui sozinho, toca o alarme e avisa
    effect(() => {
      const s = this.sessao();
      if (s && this.restanteMs() === 0) {
        untracked(() => this.terminarNaHora(s));
      }
    });
  }

  protected escolherTipo(tipo: TipoSessao): void {
    this.tipo.set(tipo);
    this.duracao.set(TIPOS.find((t) => t.valor === tipo)!.minutos);
  }

  protected iniciar(): void {
    // A permissão de notificação precisa ser pedida num clique do usuário
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
    const foco = this.tipo() === 'FOCO';
    this.executar(
      this.service.iniciar({
        tipo: this.tipo(),
        duracaoMin: this.duracao(),
        tarefaId: foco ? this.tarefaId : null,
        materiaId: foco && this.tarefaId === null ? this.materiaId : null,
      }),
      (s) => this.sessao.set(s),
    );
  }

  protected concluir(): void {
    const s = this.sessao();
    if (s) this.executar(this.service.concluir(s.id), () => this.depoisDeFinalizar(s.tipo));
  }

  protected interromper(): void {
    const s = this.sessao();
    if (s && confirm('Interromper esta sessão? O tempo até agora fica registrado.')) {
      this.executar(this.service.interromper(s.id), () => this.depoisDeFinalizar(s.tipo, false));
    }
  }

  protected horario(iso: string): string {
    return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  }

  protected rotuloDe(tipo: TipoSessao): string {
    return TIPOS.find((t) => t.valor === tipo)!.rotulo;
  }

  private terminarNaHora(s: Sessao): void {
    if (this.finalizando) return;
    this.finalizando = true;
    tocarAlarme();
    const mensagem = s.tipo === 'FOCO' ? 'Foco concluído! Hora de uma pausa.' : 'Pausa encerrada. Bora focar?';
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification('NoteLogic', { body: mensagem, icon: 'favicon.svg' });
    }
    this.service.concluir(s.id).subscribe({
      next: () => this.depoisDeFinalizar(s.tipo),
      error: (e) => {
        // 409: outra aba já concluiu esta sessão. Só sincroniza.
        if (e.status !== 409) this.erro.set(mensagemDeErro(e));
        this.depoisDeFinalizar(s.tipo);
      },
    });
  }

  /** Depois de um foco, sugere pausa (longa a cada 4 focos); depois de uma pausa, sugere foco. */
  private depoisDeFinalizar(tipo: TipoSessao, contou = true): void {
    this.sessao.set(null);
    this.finalizando = false;
    this.historico.reload();
    if (tipo !== 'FOCO') {
      this.escolherTipo('FOCO');
    } else if (contou) {
      const focos = this.focosConcluidos() + 1;
      this.escolherTipo(focos % 4 === 0 ? 'PAUSA_LONGA' : 'PAUSA_CURTA');
    }
  }

  private executar<T>(acao: Observable<T>, depois: (resultado: T) => void): void {
    this.erro.set(null);
    acao.subscribe({ next: depois, error: (e) => this.erro.set(mensagemDeErro(e)) });
  }
}
