import { Routes } from '@angular/router';

import { exigeLogin, soDeslogado } from './core/auth.guard';
import { Shell } from './layout/shell';

// loadComponent = carregamento preguiçoso: o código de cada tela só é baixado quando você entra nela.
export const routes: Routes = [
  {
    path: 'entrar',
    title: 'Entrar · NoteLogic',
    canActivate: [soDeslogado],
    data: { modo: 'entrar' },
    loadComponent: () => import('./auth/acesso').then((m) => m.Acesso),
  },
  {
    path: 'cadastro',
    title: 'Criar conta · NoteLogic',
    canActivate: [soDeslogado],
    data: { modo: 'cadastro' },
    loadComponent: () => import('./auth/acesso').then((m) => m.Acesso),
  },
  {
    // Rota "pai" sem caminho próprio: o guard protege todas as filhas de uma vez
    path: '',
    component: Shell,
    canActivate: [exigeLogin],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'hoje' },
      {
        path: 'hoje',
        title: 'Hoje · NoteLogic',
        loadComponent: () => import('./tarefas/hoje').then((m) => m.Hoje),
      },
      {
        path: 'materias',
        title: 'Matérias · NoteLogic',
        loadComponent: () => import('./materias/materias').then((m) => m.Materias),
      },
      {
        path: 'flashcards',
        title: 'Flashcards · NoteLogic',
        loadComponent: () => import('./flashcards/baralhos').then((m) => m.Baralhos),
      },
      {
        path: 'flashcards/:id',
        title: 'Baralho · NoteLogic',
        loadComponent: () => import('./flashcards/baralho').then((m) => m.Baralho),
      },
      {
        path: 'flashcards/:id/estudo',
        title: 'Estudando · NoteLogic',
        loadComponent: () => import('./flashcards/estudo').then((m) => m.Estudo),
      },
      {
        path: 'pomodoro',
        title: 'Pomodoro · NoteLogic',
        loadComponent: () => import('./pomodoro/pomodoro').then((m) => m.Pomodoro),
      },
      {
        path: 'painel',
        title: 'Painel · NoteLogic',
        loadComponent: () => import('./painel/painel').then((m) => m.Painel),
      },
    ],
  },
  { path: '**', redirectTo: 'hoje' },
];
