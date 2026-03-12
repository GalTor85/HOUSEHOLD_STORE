package ru.galtor85.household_store.advice.exception;

public class TokenValidationError extends RuntimeException {
  public TokenValidationError(String message) {
    super(message);
  }
}
