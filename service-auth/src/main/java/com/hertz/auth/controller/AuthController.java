package com.hertz.auth.controller;

import com.hertz.auth.dto.LoginRequest;
import com.hertz.auth.dto.LoginResponse;
import com.hertz.auth.dto.PerfilResponse;
import com.hertz.auth.dto.ValidarTokenRequest;
import com.hertz.auth.dto.ValidarTokenResponse;
import com.hertz.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/validar")
    public ValidarTokenResponse validar(@Valid @RequestBody ValidarTokenRequest request) {
        return authService.validarToken(request.token());
    }

    @GetMapping("/perfil")
    public PerfilResponse perfil(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.perfil(authorization);
    }
}