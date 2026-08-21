package com.outzdir.in.outzdir.Service;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.outzdir.in.outzdir.DTO.UsersSignUpDTO;
import com.outzdir.in.outzdir.DTO.UsersSignUpResponseDTO;
import com.outzdir.in.outzdir.DTO.UsersLoginRequestDTO;
import com.outzdir.in.outzdir.DTO.UsersLoginResponseDTO;
import com.outzdir.in.outzdir.DTO.RefreshTokenRequestDTO;
import com.outzdir.in.outzdir.DTO.TokenRefreshResponseDTO;
import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Entity.RefreshToken;
import com.outzdir.in.outzdir.Repository.UsersRepository;
import com.outzdir.in.outzdir.Repository.RefreshTokenRepository;
import com.outzdir.in.outzdir.Security.AuthUtil;
import com.outzdir.in.outzdir.Security.CustomUserDetails;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UsersService {

    private final ModelMapper modelMapper;
    private final UsersRepository usersRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final PasswordEncoder passwordEncoder;

    public UsersLoginResponseDTO login(UsersLoginRequestDTO usersLoginRequestDTO) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usersLoginRequestDTO.getEmail(),
                        usersLoginRequestDTO.getPassword()));

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        Users users = customUserDetails.getUser();

        String accessToken = authUtil.generateAccessToken(users);
        String refreshToken = authUtil.generateRefreshToken(users);

        saveRefreshToken(users, refreshToken);

        return new UsersLoginResponseDTO(accessToken, refreshToken, users.getId(), users.getEmail(), users.getName());

    }

    private void saveRefreshToken(Users user, String tokenStr) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(tokenStr);
        refreshToken.setRevoked(false);
        refreshToken.setExpiryDate(Instant.now().plus(15, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);
    }

    public TokenRefreshResponseDTO refresh(RefreshTokenRequestDTO request) {
        String tokenStr = request.getRefreshToken();

        if (!authUtil.validateToken(tokenStr) || !"refresh".equals(authUtil.getTokenType(tokenStr))) {
            throw new BadCredentialsException("email or password invalid");
        }

        RefreshToken dbToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new BadCredentialsException("email or password invalid"));

        if (dbToken.isRevoked()) {
            throw new BadCredentialsException("email or password invalid");
        }

        dbToken.setRevoked(true);
        refreshTokenRepository.save(dbToken);

        Users user = dbToken.getUser();
        String newAccessToken = authUtil.generateAccessToken(user);
        String newRefreshToken = authUtil.generateRefreshToken(user);

        saveRefreshToken(user, newRefreshToken);

        return new TokenRefreshResponseDTO(newAccessToken, newRefreshToken);
    }

    public void logout(RefreshTokenRequestDTO request) {
        String tokenStr = request.getRefreshToken();
        RefreshToken dbToken = refreshTokenRepository.findByToken(tokenStr).orElse(null);
        if (dbToken != null) {
            dbToken.setRevoked(true);
            refreshTokenRepository.save(dbToken);
        }
    }

    public ResponseEntity<?> signup(UsersSignUpDTO usersSignUpDTO) {

        Users user = usersRepository.findByEmail(usersSignUpDTO.getEmail());
        if (user != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("User already exist with email: " + usersSignUpDTO.getEmail());
        }

        Users users = modelMapper.map(usersSignUpDTO, Users.class);
        users.setPassword(passwordEncoder.encode(users.getPassword()));
        usersRepository.save(users);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsersSignUpResponseDTO(users.getId(), users.getName(), users.getEmail()));
    }

}
