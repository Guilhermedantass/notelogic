import { Routes } from '@angular/router';

// loadComponent = carregamento preguiçoso: o código de cada tela só é baixado quando você entra nela.
export const routes: Routes = [
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
    data: { titulo: 'Painel', icone: 'insights' },
    loadComponent: () => import('./shared/em-breve').then((m) => m.EmBreve),
  },
  { path: '**', redirectTo: 'hoje' },
];
