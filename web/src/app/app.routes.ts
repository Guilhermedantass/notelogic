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
    data: { titulo: 'Flashcards', icone: 'style' },
    loadComponent: () => import('./shared/em-breve').then((m) => m.EmBreve),
  },
  {
    path: 'pomodoro',
    title: 'Pomodoro · NoteLogic',
    data: { titulo: 'Pomodoro', icone: 'timer' },
    loadComponent: () => import('./shared/em-breve').then((m) => m.EmBreve),
  },
  {
    path: 'painel',
    title: 'Painel · NoteLogic',
    data: { titulo: 'Painel', icone: 'insights' },
    loadComponent: () => import('./shared/em-breve').then((m) => m.EmBreve),
  },
  { path: '**', redirectTo: 'hoje' },
];
