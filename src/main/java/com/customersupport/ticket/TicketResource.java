package com.customersupport.ticket;

import com.customersupport.ticket.dto.TicketRequest;
import com.customersupport.ticket.dto.TicketResponse;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;

@Path("/ticket")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class TicketResource {

    private final TicketService ticketService;
    private final JsonWebToken jwt;

    @Inject
    public TicketResource(TicketService ticketService, JsonWebToken jwt) {
        this.ticketService = ticketService;
        this.jwt = jwt;
    }

    @GET
    @Path("/all")
    @RolesAllowed("OPERATOR")
    public Response getAllTickets(
            @QueryParam("status") TicketStatus status,
            @QueryParam("roomId") Long roomId,
            @QueryParam("operatorId") Long operatorId,
            @QueryParam("sort") @DefaultValue("updatedAt") String sort,
            @QueryParam("order") @DefaultValue("desc") String order) {
        List<TicketResponse> tickets = ticketService.getAllTickets(status, roomId, operatorId, sort, order);
        return Response.ok(tickets).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("OPERATOR")
    public Response getTicketById(@PathParam("id") Long id) {
        TicketResponse ticket = ticketService.getTicketById(id);
        return Response.ok(ticket).build();
    }

    @POST
    @Path("/new")
    public Response createTicket(@Valid TicketRequest request) {
        Long userId = Long.parseLong(jwt.getSubject());
        TicketResponse ticket = ticketService.createTicket(userId, request);
        return Response.status(Response.Status.CREATED).entity(ticket).build();
    }

    @POST
    @Path("/{id}/take")
    @RolesAllowed("OPERATOR")
    public Response takeTicket(@PathParam("id") Long id) {
        Long operatorId = Long.parseLong(jwt.getSubject());
        TicketResponse ticket = ticketService.takeTicket(id, operatorId);
        return Response.ok(ticket).build();
    }

    @POST
    @Path("/{id}/close")
    @RolesAllowed("OPERATOR")
    public Response closeTicket(@PathParam("id") Long id) {
        Long operatorId = Long.parseLong(jwt.getSubject());
        TicketResponse ticket = ticketService.closeTicket(id, operatorId);
        return Response.ok(ticket).build();
    }

    @POST
    @Path("/{id}/archive")
    @RolesAllowed("OPERATOR")
    public Response archiveTicket(@PathParam("id") Long id) {
        Long operatorId = Long.parseLong(jwt.getSubject());
        TicketResponse ticket = ticketService.archiveTicket(id, operatorId);
        return Response.ok(ticket).build();
    }
}