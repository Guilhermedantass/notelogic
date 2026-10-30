import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Shell } from './layout/shell';

describe('Shell', () => {
  beforeEach(async () => {
    localStorage.setItem('notelogic.usuario', JSON.stringify({ id: 1, nome: 'Ana', email: 'ana@teste.dev' }));
    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [provideRouter([]), provideHttpClient()],
    }).compileComponents();
  });

  afterEach(() => localStorage.clear());

  it('mostra a marca, a navegação e quem está logado', async () => {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;

    expect(html.querySelector('.marca')?.textContent).toContain('notelogic');
    expect(html.querySelectorAll('nav a').length).toBe(6);
    expect(html.querySelector('.conta')?.textContent).toContain('Ana');
  });
});
