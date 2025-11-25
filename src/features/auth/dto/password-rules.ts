export const STRONG_PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{12,}$/;
export const STRONG_PASSWORD_MESSAGE =
  'Password must be at least 12 characters and include uppercase, lowercase, number, and symbol.';
