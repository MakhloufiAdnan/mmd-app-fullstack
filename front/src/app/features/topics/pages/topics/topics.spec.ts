import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { TopicsComponent } from './topics';
import { TopicsApiService } from '../../services/topics-api.service';

describe('TopicsComponent', () => {
  it('should create', async () => {
    await TestBed.configureTestingModule({
      imports: [TopicsComponent],
      providers: [
        {
          provide: TopicsApiService,
          useValue: {
            // Minimal stub for a "smoke test": Topics component expects an Observable.
            listTopics: () => of([{ id: 1, name: 'Java', subscribed: true }]),
            subscribeToTopic: () => of({ id: 1 }),
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(TopicsComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
  });
});
