package ru.galtor85.household_store.validation;

public class PatternMobileNumber {
    private static final String RUSSIAN_REGEXP = "^\\+?[78]\\d{10}$";
    private static final String RUSSIAN_MESSAGE = "Некорректный формат российского номера телефона";

    private static final String INTERNATIONAL_REGEXP = "^\\+\\d{1,3}\\d{1,14}$";
    private static final String INTERNATIONAL_MESSAGE = "Invalid international phone number format";

    public static final String REGEXP = RUSSIAN_REGEXP + "|" + INTERNATIONAL_REGEXP;
    public static final String MESSAGE = RUSSIAN_MESSAGE + "|" + INTERNATIONAL_MESSAGE;





}
