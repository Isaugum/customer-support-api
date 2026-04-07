package com.customersupport.auth;

import com.customersupport.auth.dto.LoginRequest;
import com.customersupport.auth.dto.LoginResponse;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;

    @Inject
    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        return authService.authenticate(request.email(), request.password())
                .map(token -> Response.ok(new LoginResponse(token)).build())
                .orElse(Response.status(Response.Status.UNAUTHORIZED).build());
    }
}
