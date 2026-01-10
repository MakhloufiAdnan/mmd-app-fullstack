import { HttpContext } from '@angular/common/http';
import { AUTH_REFRESH_ATTEMPTED } from './auth.http-context';

describe('AUTH_REFRESH_ATTEMPTED HttpContextToken', () => {
  it('should default to false when not set', () => {
    // Arrange
    const ctx = new HttpContext();

    // Act
    const value = ctx.get(AUTH_REFRESH_ATTEMPTED);

    // Assert
    expect(value).toBeFalse();
  });

  it('should be true when explicitly set', () => {
    // Arrange
    const ctx = new HttpContext().set(AUTH_REFRESH_ATTEMPTED, true);

    // Act
    const value = ctx.get(AUTH_REFRESH_ATTEMPTED);

    // Assert
    expect(value).toBeTrue();
  });
});
