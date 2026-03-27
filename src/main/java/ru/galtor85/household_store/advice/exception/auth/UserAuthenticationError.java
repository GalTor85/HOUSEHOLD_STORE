package ru.galtor85.household_store.advice.exception.auth;

public class UserAuthenticationError extends RuntimeException {
  public UserAuthenticationError(String message) {
    super(message);
  }
}
