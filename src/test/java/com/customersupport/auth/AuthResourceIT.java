package com.customersupport.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AuthResourceIT {

        private static final String LOGIN_URL = "/auth/login";

        // POST /auth/login

        @Test
        void should_return200WithToken_when_credentialsAreValid() {
                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                { "email": "tone@example.com", "password": "password" }
                                                """)
                                .when().post(LOGIN_URL)
                                .then()
                                .statusCode(200)
                                .body("token", notNullValue());
        }

        @Test
        void should_return401_when_passwordIsWrong() {
                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                { "email": "tone@example.com", "password": "wrongpassword" }
                                                """)
                                .when().post(LOGIN_URL)
                                .then()
                                .statusCode(401);
        }

        @Test
        void should_return401_when_emailIsUnknown() {
                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                { "email": "nobody@example.com", "password": "password" }
                                                """)
                                .when().post(LOGIN_URL)
                                .then()
                                .statusCode(401);
        }

        @Test
        void should_return400_when_emailIsBlank() {
                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                { "email": "", "password": "password" }
                                                """)
                                .when().post(LOGIN_URL)
                                .then()
                                .statusCode(400);
        }

        @Test
        void should_return400_when_bodyIsMissing() {
                given()
                                .contentType(ContentType.JSON)
                                .when().post(LOGIN_URL)
                                .then()
                                .statusCode(400);
        }
}
