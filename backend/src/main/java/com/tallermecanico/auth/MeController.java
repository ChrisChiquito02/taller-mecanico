package com.tallermecanico.auth;

import com.tallermecanico.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api")
public class MeController {
    private final UserRepository users;
    /** Inicializa el controlador de la cuenta autenticada. */
    public MeController(UserRepository users){this.users=users;}
    /** Devuelve los datos del usuario identificado por el JWT validado. */
    @GetMapping("/me") public AuthDtos.UserView me(Authentication authentication){
        var user=users.findById(authentication.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new AuthDtos.UserView(user.getId(),user.getName(),user.getLastName(),user.getPhone(),user.getEmail(),user.getFecha());
    }
}
