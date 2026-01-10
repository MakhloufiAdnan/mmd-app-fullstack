import { TestBed } from '@angular/core/testing';
import { AuthStore } from './auth.store';

describe('AuthStore', () => {
  let store: AuthStore;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    store = TestBed.inject(AuthStore);
  });

  it('should start unauthenticated with initialized=false', () => {
    // Arrange / Act
    const token = store.accessToken();
    const isAuth = store.isAuthenticated();
    const init = store.initialized();

    // Assert
    expect(token).toBeNull();
    expect(isAuth).toBeFalse();
    expect(init).toBeFalse();
  });

  it('setAccessToken should update isAuthenticated computed', () => {
    // Arrange
    expect(store.isAuthenticated()).toBeFalse();

    // Act
    store.setAccessToken('jwt');

    // Assert
    expect(store.accessToken()).toBe('jwt');
    expect(store.isAuthenticated()).toBeTrue();
  });

  it('clear should remove token', () => {
    // Arrange
    store.setAccessToken('jwt');

    // Act
    store.clear();

    // Assert
    expect(store.accessToken()).toBeNull();
    expect(store.isAuthenticated()).toBeFalse();
  });

  it('markInitialized should set initialized=true', () => {
    // Arrange
    expect(store.initialized()).toBeFalse();

    // Act
    store.markInitialized();

    // Assert
    expect(store.initialized()).toBeTrue();
  });
});
