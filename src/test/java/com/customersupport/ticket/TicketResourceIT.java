package com.customersupport.ticket;

import com.customersupport.ticket.dto.TicketRequest;
import com.customersupport.ticket.dto.TicketResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class TicketResourceIT {

        @Inject
        TicketService ticketService;

        @Inject
        TicketRepository ticketRepository;

        @Inject
        UserTransaction tx;

        @BeforeEach
        void closeAllOpenTickets() throws Exception {
                tx.begin();
                ticketRepository.update("status = ?1 where status = ?2 or status = ?3",
                                TicketStatus.CLOSED, TicketStatus.WAITING, TicketStatus.ACTIVE);
                tx.commit();
        }

        // GET /ticket/all

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return200WithList_when_operatorRequestsAllTickets() {
                given()
                                .when().get("/ticket/all")
                                .then()
                                .statusCode(200)
                                .body("$", instanceOf(java.util.List.class));
        }

        @Test
        @TestSecurity(user = "1", roles = "USER")
        void should_return403_when_userRequestsAllTickets() {
                given()
                                .when().get("/ticket/all")
                                .then()
                                .statusCode(403);
        }

        @Test
        void should_return401_when_unauthenticatedRequestsAllTickets() {
                given()
                                .when().get("/ticket/all")
                                .then()
                                .statusCode(401);
        }

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return200WithFilteredList_when_statusFilterApplied() {
                given()
                                .queryParam("status", "WAITING")
                                .when().get("/ticket/all")
                                .then()
                                .statusCode(200)
                                .body("$", instanceOf(java.util.List.class))
                                .body("status", everyItem(equalTo("WAITING")));
        }

        // GET /ticket/{id}

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return404_when_ticketDoesNotExist() {
                given()
                                .when().get("/ticket/999999")
                                .then()
                                .statusCode(404);
        }

        @Test
        @TestSecurity(user = "1", roles = "USER")
        void should_return403_when_userGetsTicketById() {
                given()
                                .when().get("/ticket/1")
                                .then()
                                .statusCode(403);
        }

        // POST /ticket/new

        @Test
        @TestSecurity(user = "1", roles = "USER")
        void should_return201_when_userCreatesTicket() {
                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                { "roomId": 1, "initialMessage": "I need help" }
                                                """)
                                .when().post("/ticket/new")
                                .then()
                                .statusCode(201)
                                .body("status", equalTo("WAITING"));
        }

        @Test
        @TestSecurity(user = "1", roles = "USER")
        void should_return400_when_roomIdIsMissing() {
                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                { "initialMessage": "I need help" }
                                                """)
                                .when().post("/ticket/new")
                                .then()
                                .statusCode(400);
        }

        @Test
        @TestSecurity(user = "1", roles = "USER")
        void should_return400_when_initialMessageIsBlank() {
                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                { "roomId": 1, "initialMessage": "" }
                                                """)
                                .when().post("/ticket/new")
                                .then()
                                .statusCode(400);
        }

        @Test
        @TestSecurity(user = "1", roles = "USER")
        void should_return404_when_roomIdIsInvalid() {
                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                { "roomId": 999999, "initialMessage": "I need help" }
                                                """)
                                .when().post("/ticket/new")
                                .then()
                                .statusCode(404);
        }

        // POST /ticket/{id}/take

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return200_when_operatorTakesWaitingTicket() {
                Integer ticketId = given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                { "roomId": 1, "initialMessage": "Help needed" }
                                                """)
                                .when().post("/ticket/new")
                                .then().statusCode(201)
                                .extract().path("id");

                given()
                                .when().post("/ticket/" + ticketId + "/take")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("ACTIVE"))
                                .body("operatorId", notNullValue());
        }

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return400_when_ticketIsAlreadyActive() {
                Integer ticketId = given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                { "roomId": 1, "initialMessage": "Help needed" }
                                                """)
                                .when().post("/ticket/new")
                                .then().statusCode(201)
                                .extract().path("id");

                given().when().post("/ticket/" + ticketId + "/take").then().statusCode(200);

                given()
                                .when().post("/ticket/" + ticketId + "/take")
                                .then()
                                .statusCode(400);
        }

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return404_when_ticketToTakeNotFound() {
                given()
                                .when().post("/ticket/999999/take")
                                .then()
                                .statusCode(404);
        }

        @Test
        @TestSecurity(user = "1", roles = "USER")
        void should_return403_when_userAttemptsToTakeTicket() {
                given()
                                .when().post("/ticket/1/take")
                                .then()
                                .statusCode(403);
        }

        // POST /ticket/{id}/close

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return200_when_assignedOperatorClosesTicket() {
                TicketResponse created = ticketService.createTicket(1L, new TicketRequest(1L, "Help needed"));
                ticketService.takeTicket(created.id(), 3L);

                given()
                                .when().post("/ticket/" + created.id() + "/close")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("CLOSED"));
        }

        @Test
        @TestSecurity(user = "4", roles = "OPERATOR")
        void should_return403_when_differentOperatorClosesTicket() {
                TicketResponse created = ticketService.createTicket(1L, new TicketRequest(1L, "Help needed"));
                ticketService.takeTicket(created.id(), 3L); // operator 3 takes it

                // operator 4 (jwt subject = "4") tries to close — should be rejected
                given()
                                .when().post("/ticket/" + created.id() + "/close")
                                .then()
                                .statusCode(403);
        }

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return400_when_ticketIsNotActive() {
                TicketResponse created = ticketService.createTicket(1L, new TicketRequest(1L, "Help needed"));
                // ticket remains WAITING — not ACTIVE

                given()
                                .when().post("/ticket/" + created.id() + "/close")
                                .then()
                                .statusCode(400);
        }

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return404_when_ticketToCloseNotFound() {
                given()
                                .when().post("/ticket/999999/close")
                                .then()
                                .statusCode(404);
        }

        @Test
        @TestSecurity(user = "1", roles = "USER")
        void should_return403_when_userAttemptsToCloseTicket() {
                given()
                                .when().post("/ticket/1/close")
                                .then()
                                .statusCode(403);
        }

        // POST /ticket/{id}/archive

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return200_when_assignedOperatorArchivesClosedTicket() {
                TicketResponse created = ticketService.createTicket(1L, new TicketRequest(1L, "Help needed"));
                ticketService.takeTicket(created.id(), 3L);
                ticketService.closeTicket(created.id(), 3L);

                given()
                                .when().post("/ticket/" + created.id() + "/archive")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("ARCHIVED"));
        }

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return400_when_ticketIsNotClosed() {
                TicketResponse created = ticketService.createTicket(1L, new TicketRequest(1L, "Help needed"));
                ticketService.takeTicket(created.id(), 3L);
                // ticket is ACTIVE, not yet CLOSED

                given()
                                .when().post("/ticket/" + created.id() + "/archive")
                                .then()
                                .statusCode(400);
        }

        @Test
        @TestSecurity(user = "4", roles = "OPERATOR")
        void should_return403_when_differentOperatorArchivesTicket() {
                TicketResponse created = ticketService.createTicket(1L, new TicketRequest(1L, "Help needed"));
                ticketService.takeTicket(created.id(), 3L);
                ticketService.closeTicket(created.id(), 3L);

                // operator 4 (jwt subject = "4") tries to archive — only operator 3 may
                given()
                                .when().post("/ticket/" + created.id() + "/archive")
                                .then()
                                .statusCode(403);
        }

        @Test
        @TestSecurity(user = "3", roles = "OPERATOR")
        void should_return404_when_ticketToArchiveNotFound() {
                given()
                                .when().post("/ticket/999999/archive")
                                .then()
                                .statusCode(404);
        }

        @Test
        @TestSecurity(user = "1", roles = "USER")
        void should_return403_when_userAttemptsToArchiveTicket() {
                given()
                                .when().post("/ticket/1/archive")
                                .then()
                                .statusCode(403);
        }
}
