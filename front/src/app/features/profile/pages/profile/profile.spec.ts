import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { Profile } from './profile';
import { UserMeApiService } from '../../services/user-me-api.service';
import { TopicsApiService } from '@features/topics/services/topics-api.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DEFAULT_COMPONENT_TEST_PROVIDERS } from '@core/testing/test.providers';

describe('Profile', () => {
  let fixture: ComponentFixture<Profile>;
  let component: Profile;

  const userMeApiMock = {
    me: jasmine
      .createSpy('me')
      .and.returnValue(of({ email: 'a@b.com', username: 'bob', subscriptions: [] })),
    updateMe: jasmine.createSpy('updateMe').and.returnValue(of({ updated: true })),
    unsubscribeFromTopic: jasmine.createSpy('unsubscribeFromTopic').and.returnValue(of(void 0)),
  };

  const topicsApiMock = {
    listTopics: jasmine.createSpy('listTopics').and.returnValue(
      of([
        { id: 1, name: 'Java', subscribed: true },
        { id: 2, name: 'Angular', subscribed: false },
      ])
    ),
    subscribeToTopic: jasmine.createSpy('subscribeToTopic').and.returnValue(of({ id: 123 })),
  };

  const snackBarMock = {
    open: jasmine.createSpy('open'),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Profile],
      providers: [
        ...DEFAULT_COMPONENT_TEST_PROVIDERS,
        { provide: UserMeApiService, useValue: userMeApiMock },
        { provide: TopicsApiService, useValue: topicsApiMock },
        { provide: MatSnackBar, useValue: snackBarMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Profile);
    component = fixture.componentInstance;

    // Act : ngOnInit() -> loadMe() + loadTopics()
    fixture.detectChanges();
  });

  it('should create', () => {
    // Arrange / Act / Assert
    expect(component).toBeTruthy();
  });

  it('submit() should send password=null when password is blank', () => {
  // Arrange
  component.form.patchValue({
    email: 'new@b.com',
    username: 'newName',
    password: '', // cas UX réel : champ laissé vide
  });

  component.form.updateValueAndValidity();
  expect(component.form.valid).toBeTrue();

  // Act
  component.submit();

  // Assert
  expect(userMeApiMock.updateMe).toHaveBeenCalledWith({
    email: 'new@b.com',
    username: 'newName',
    password: null,
  });
});
});
