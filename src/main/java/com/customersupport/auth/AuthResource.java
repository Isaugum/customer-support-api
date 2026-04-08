package com.customersupport.auth;

import com.customersupport.auth.dto.LoginRequest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;
    private final int cookieMaxAge;

    @Inject
    public AuthResource(AuthService authService,
            @ConfigProperty(name = "app.cookie.max-age-seconds") int cookieMaxAge) {
        this.authService = authService;
        this.cookieMaxAge = cookieMaxAge;
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        return authService.authenticate(request.email(), request.password())
                .map(token -> {
                    NewCookie cookie = new NewCookie.Builder("jwt")
                            .value(token)
                            .path("/")
                            .httpOnly(true)
                            .sameSite(NewCookie.SameSite.LAX)
                            .maxAge(cookieMaxAge)
                            .build();
                    return Response.ok().cookie(cookie).build();
                })
                .orElse(Response.status(Response.Status.UNAUTHORIZED).build());
    }

    @POST
    @Path("/logout")
    public Response logout() {
        NewCookie expired = new NewCookie.Builder("jwt")
                .value("")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build();
        return Response.ok().cookie(expired).build();
    }
}
