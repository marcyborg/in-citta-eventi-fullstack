import { Event } from './event.model';

describe('Event interface', () => {
  it('should allow object creation', () => {
    const ev: Event = {
      titolo: 'Test',
      descrizione: 'Un evento di test',
      data: '2025-10-19',
      luogo: 'Potenza',
      categoria: 'teatro'
    };
    expect(ev.titolo).toBe('Test');
  });
});
