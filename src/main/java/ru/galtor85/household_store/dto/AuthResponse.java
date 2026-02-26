package ru.galtor85.household_store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.security.JwtTokenProvider;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserResponse user;

    public static AuthResponse buildAuthResponse(User user, JwtTokenProvider jwtTokenProvider) {
        String identify = user.getEmail() != null ? user.getEmail() : user.getMobileNumber();
        String accessToken = jwtTokenProvider.createToken(identify, user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(identify);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getValidity())
                .user(UserResponse.fromEntity(user))
                .build();
    }
}