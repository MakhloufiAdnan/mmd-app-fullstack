import { FormControl } from '@angular/forms';
import { passwordPolicyValidator } from './password-policy.validator';

describe('passwordPolicyValidator', () => {
  const validate = passwordPolicyValidator();

  it('should return null for empty string (required handled by Validators.required)', () => {
    // Arrange
    const control = new FormControl('');

    // Act
    const result = validate(control);

    // Assert
    expect(result).toBeNull();
  });

  it('should return { passwordPolicy: true } for weak password', () => {
    // Arrange
    const control = new FormControl('abc');

    // Act
    const result = validate(control);

    // Assert
    expect(result).toEqual({ passwordPolicy: true });
  });

  it('should return { passwordPolicy: true } for spaces-only (no trim in current implementation)', () => {
    // Arrange
    const control = new FormControl('   ');

    // Act
    const result = validate(control);

    // Assert
    expect(result).toEqual({ passwordPolicy: true });
  });

  it('should return null for strong password meeting all constraints', () => {
    // Arrange
    const control = new FormControl('Aa1!aaaa');

    // Act
    const result = validate(control);

    // Assert
    expect(result).toBeNull();
  });
});
