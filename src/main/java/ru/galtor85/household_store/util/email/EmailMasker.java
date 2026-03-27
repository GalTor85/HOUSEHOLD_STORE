package ru.galtor85.household_store.util.email;

import org.springframework.stereotype.Component;

@Component
public class EmailMasker {

    public String maskEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        return localPart.charAt(0) + "***" +
                (localPart.length() > 2 ? localPart.charAt(localPart.length() - 1) : "") +
                domain;
    }

    public String maskEmailWithFullLocalPart(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 3) {
            return localPart.charAt(0) + "***" + domain;
        }

        return localPart.substring(0, 2) + "***" +
                localPart.charAt(localPart.length() - 1) + domain;
    }
}