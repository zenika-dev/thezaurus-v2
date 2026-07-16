package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.BlogPost;
import com.zenika.thezaurus.service.BlogPostService;
import com.zenika.thezaurus.model.Role;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestSecurity(user = "dev@zenika.com", roles = {Role.Names.CONSULTANT})
public class BlogPostResourceTest {

    @InjectMock
    BlogPostService service;

    @Test
    public void testList() throws Exception {
        Mockito.when(service.findAll()).thenReturn(Collections.singletonList(new BlogPost("1", "Titre", "Description")));

        given()
          .when().get("/blog-posts")
          .then()
             .statusCode(200)
             .body("size()", is(1))
             .body("[0].id", is("1"))
             .body("[0].title", is("Titre"));
    }

    @Test
    public void testGetNotFound() throws Exception {
        Mockito.when(service.findById("999")).thenReturn(null);

        given()
          .when().get("/blog-posts/999")
          .then()
             .statusCode(404);
    }

    @Test
    public void testCreate() throws Exception {
        BlogPost input = new BlogPost(null, "New Post", "Desc");
        BlogPost created = new BlogPost("new-id", "New Post", "Desc");
        
        Mockito.when(service.create(Mockito.any(BlogPost.class))).thenReturn(created);

        given()
          .contentType(ContentType.JSON)
          .body(input)
          .when().post("/blog-posts")
          .then()
             .statusCode(201)
             .body("id", is("new-id"))
             .body("title", is("New Post"));
    }
}
