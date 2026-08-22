package com.outzdir.in.outzdir.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.outzdir.in.outzdir.DTO.RefreshTokenRequestDTO;
import com.outzdir.in.outzdir.DTO.TokenRefreshResponseDTO;
import com.outzdir.in.outzdir.DTO.UsersLoginRequestDTO;
import com.outzdir.in.outzdir.DTO.UsersLoginResponseDTO;
import com.outzdir.in.outzdir.DTO.UsersSignUpDTO;
import com.outzdir.in.outzdir.DTO.UsersSignUpResponseDTO;
import com.outzdir.in.outzdir.Entity.RefreshToken;
import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Repository.RefreshTokenRepository;
import com.outzdir.in.outzdir.Repository.UsersRepository;
import com.outzdir.in.outzdir.Security.AuthUtil;
import com.outzdir.in.outzdir.Security.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthUtil authUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsersService usersService;

    private Users testUser;
    private UsersSignUpDTO signUpDTO;
    private UsersLoginRequestDTO loginRequestDTO;

    @BeforeEach
    void setUp() {
        testUser = new Users();
        testUser.setId(1L);
        testUser.setEmail("test@gmail.com");
        testUser.setName("Test User");
        testUser.setPassword("Prakash");
        testUser.setPhoneNumber("768183858");

        signUpDTO = new UsersSignUpDTO();
        signUpDTO.setName("Test");
        signUpDTO.setEmail("test@gmail.com");
        signUpDTO.setPassword("test123");
        signUpDTO.setPhoneNumber("7681838458");

        loginRequestDTO = new UsersLoginRequestDTO("test@gmail.com", "test123");
    }

    @Test
    void testSignup_Success() {
        when(usersRepository.findByEmail(signUpDTO.getEmail())).thenReturn(null);
        when(modelMapper.map(signUpDTO, Users.class)).thenReturn(testUser);
        when(passwordEncoder.encode(signUpDTO.getPassword())).thenReturn("encodedPassword");

        ResponseEntity<?> response = usersService.signup(signUpDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof UsersSignUpResponseDTO);

        UsersSignUpResponseDTO responseDTO = (UsersSignUpResponseDTO) response.getBody();
        assertEquals(testUser.getId(), responseDTO.getId());
        assertEquals(testUser.getName(), responseDTO.getName());
        assertEquals(testUser.getEmail(), responseDTO.getEmail());

        verify(usersRepository).save(testUser);
    }

    @Test
    void testSignup_DuplicateEmail() {
        when(usersRepository.findByEmail(signUpDTO.getEmail())).thenReturn(testUser);

        ResponseEntity<?> response = usersService.signup(signUpDTO);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User already exist with email: " + signUpDTO.getEmail(), response.getBody());
        verify(usersRepository, never()).save(any());
    }

    @Test
    void testLogin_Success() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        when(authUtil.generateAccessToken(testUser)).thenReturn("access-token-123");
        when(authUtil.generateRefreshToken(testUser)).thenReturn("refresh-token-123");

        UsersLoginResponseDTO response = usersService.login(loginRequestDTO);

        assertNotNull(response);
        assertEquals("access-token-123", response.getAccessToken());
        assertEquals("refresh-token-123", response.getRefreshToken());
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getEmail(), response.getName());
        assertEquals(testUser.getName(), response.getEmail());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testLogin_InvalidPassword() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> usersService.login(loginRequestDTO));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void testRefresh_Success() {
        String tokenStr = "valid-refresh-token";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(tokenStr);

        RefreshToken dbToken = new RefreshToken();
        dbToken.setId(10L);
        dbToken.setToken(tokenStr);
        dbToken.setRevoked(false);
        dbToken.setExpiryDate(Instant.now().plusSeconds(3600));
        dbToken.setUser(testUser);

        when(authUtil.validateToken(tokenStr)).thenReturn(true);
        when(authUtil.getTokenType(tokenStr)).thenReturn("refresh");
        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(dbToken));
        when(authUtil.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(authUtil.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

        TokenRefreshResponseDTO response = usersService.refresh(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        assertTrue(dbToken.isRevoked());
        verify(refreshTokenRepository).save(dbToken); // Verify old token is marked revoked and saved
        verify(refreshTokenRepository).save(argThat(newToken -> 
            newToken.getUser().equals(testUser) && 
            "new-refresh-token".equals(newToken.getToken()) && 
            !newToken.isRevoked()
        )); // Verify new token is saved
    }

    @Test
    void testRefresh_InvalidToken() {
        String tokenStr = "invalid-token";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(tokenStr);

        when(authUtil.validateToken(tokenStr)).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> usersService.refresh(request));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void testRefresh_WrongTokenType() {
        String tokenStr = "access-token-used-as-refresh";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(tokenStr);

        when(authUtil.validateToken(tokenStr)).thenReturn(true);
        when(authUtil.getTokenType(tokenStr)).thenReturn("access");

        assertThrows(BadCredentialsException.class, () -> usersService.refresh(request));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void testRefresh_TokenNotFoundInDb() {
        String tokenStr = "valid-token-but-not-in-db";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(tokenStr);

        when(authUtil.validateToken(tokenStr)).thenReturn(true);
        when(authUtil.getTokenType(tokenStr)).thenReturn("refresh");
        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> usersService.refresh(request));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void testRefresh_RevokedToken() {
        String tokenStr = "revoked-refresh-token";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(tokenStr);

        RefreshToken dbToken = new RefreshToken();
        dbToken.setToken(tokenStr);
        dbToken.setRevoked(true);
        dbToken.setUser(testUser);

        when(authUtil.validateToken(tokenStr)).thenReturn(true);
        when(authUtil.getTokenType(tokenStr)).thenReturn("refresh");
        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(dbToken));

        assertThrows(BadCredentialsException.class, () -> usersService.refresh(request));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void testLogout_Success() {
        String tokenStr = "token-to-revoke";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(tokenStr);

        RefreshToken dbToken = new RefreshToken();
        dbToken.setToken(tokenStr);
        dbToken.setRevoked(false);
        dbToken.setUser(testUser);

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(dbToken));

        usersService.logout(request);

        assertTrue(dbToken.isRevoked());
        verify(refreshTokenRepository).save(dbToken);
    }

    @Test
    void testLogout_TokenNotFound() {
        String tokenStr = "non-existent-token";
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(tokenStr);

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.empty());

        usersService.logout(request);

        verify(refreshTokenRepository, never()).save(any());
    }
}
