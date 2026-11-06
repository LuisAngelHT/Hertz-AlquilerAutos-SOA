package com.hertz.auth.controller;

import com.hertz.auth.dto.;
import com.hertz.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.;

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