import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { TopicsComponent } from './topics';
import { TopicsApiService } from '../../services/topics-api.service';

describe('TopicsComponent', () => {
  let api: jasmine.SpyObj<TopicsApiService>;

  beforeEach(async () => {
    // Arrange
    api = jasmine.createSpyObj<TopicsApiService>('TopicsApiService', [
      'listTopics',
      'subscribeToTopic',
    ]);

    await TestBed.configureTestingModule({
      imports: [TopicsComponent],
      providers: [{ provide: TopicsApiService, useValue: api }],
    }).compileComponents();
  });

  it('should create and load topics on init', () => {
    // Arrange
    api.listTopics.and.returnValue(
      of([{ id: 1, name: 'Java', description: 'Java', subscribed: true }])
    );

    // Act
    const fixture = TestBed.createComponent(TopicsComponent);
    fixture.detectChanges();

    // Assert
    expect(fixture.componentInstance).toBeTruthy();
    expect(api.listTopics).toHaveBeenCalledTimes(1);
  });

  it('onSubscribe() should call API then mark the topic as subscribed', fakeAsync(() => {
    // Arrange
    api.listTopics.and.returnValue(
      of([{ id: 1, name: 'Java', description: 'Java', subscribed: false }])
    );
    api.subscribeToTopic.and.returnValue(of({ id: 1 }));

    const fixture = TestBed.createComponent(TopicsComponent);
    fixture.detectChanges();

    let vm: any;
    const sub = fixture.componentInstance.vm$.subscribe((v) => (vm = v));

    tick(); // laisse le loadTopics() terminer

    expect(vm.topics[0].subscribed).toBeFalse();

    // Act
    fixture.componentInstance.onSubscribe(1);
    tick(); // laisse l'exhaustMap + subscribeToTopic se terminer

    // Assert
    expect(api.subscribeToTopic).toHaveBeenCalledWith(1);
    expect(vm.topics[0].subscribed).toBeTrue();
    expect(vm.pendingIds.size).toBe(0);

    sub.unsubscribe();
  }));

  it('onSubscribe() should "repair UI" on 409 conflict (already subscribed)', fakeAsync(() => {
    // Arrange
    api.listTopics.and.returnValue(
      of([{ id: 1, name: 'Java', description: 'Java', subscribed: false }])
    );
    api.subscribeToTopic.and.returnValue(throwError(() => ({ status: 409 })));

    const fixture = TestBed.createComponent(TopicsComponent);
    fixture.detectChanges();

    let vm: any;
    const sub = fixture.componentInstance.vm$.subscribe((v) => (vm = v));

    tick();

    // Act
    fixture.componentInstance.onSubscribe(1);
    tick();

    // Assert
    expect(api.subscribeToTopic).toHaveBeenCalledWith(1);
    expect(vm.topics[0].subscribed).toBeTrue();
    expect(vm.error).toBeNull();

    sub.unsubscribe();
  }));

  it('onSubscribe() should NOT call API when topic is already subscribed', fakeAsync(() => {
    // Arrange
    api.listTopics.and.returnValue(
      of([{ id: 1, name: 'Java', description: 'Java', subscribed: true }])
    );
    api.subscribeToTopic.and.returnValue(of({ id: 1 }));

    const fixture = TestBed.createComponent(TopicsComponent);
    fixture.detectChanges();

    tick();

    // Act
    fixture.componentInstance.onSubscribe(1);
    tick();

    // Assert
    expect(api.subscribeToTopic).not.toHaveBeenCalled();
  }));
});
