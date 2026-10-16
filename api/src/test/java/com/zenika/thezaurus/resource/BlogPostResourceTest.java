package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import com.zenika.thezaurus.model.BlogPost;
import com.zenika.thezaurus.model.BlogPostStatus;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.service.BlogPostService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@TestSecurity(
        user = "dev@zenika.com",
        roles = {Role.Names.CONSULTANT})
public class BlogPostResourceTest {

    @InjectMock
    BlogPostService service;

    @Test
    public void testList() throws Exception {
        BlogPost post = new BlogPost("1", "Titre", "https://blog.zenika.com/article");
        Mockito.when(service.findPaged(0, 10, null, false))
                .thenReturn(new com.zenika.thezaurus.model.PagedResult<>(Collections.singletonList(post), 1, 0, 10, 1));

        given().when()
                .get("/blog-posts")
                .then()
                .statusCode(200)
                .body("total", is(1))
                .body("page", is(0))
                .body("pageSize", is(10))
                .body("totalPages", is(1))
                .body("items.size()", is(1))
                .body("items[0].id", is("1"))
                .body("items[0].title", is("Titre"));
    }

    @Test
    public void testListWithParams() throws Exception {
        BlogPost post = new BlogPost("2", "Titre 2", "https://blog.zenika.com/article2");
        Mockito.when(service.findPaged(1, 25, BlogPostStatus.IDEA, true))
                .thenReturn(
                        new com.zenika.thezaurus.model.PagedResult<>(Collections.singletonList(post), 55, 1, 25, 3));

        given().when()
                .get("/blog-posts?page=1&pageSize=25&status=IDEA&personal=true")
                .then()
                .statusCode(200)
                .body("total", is(55))
                .body("page", is(1))
                .body("pageSize", is(25))
                .body("totalPages", is(3))
                .body("items.size()", is(1))
                .body("items[0].id", is("2"));
    }

    @Test
    public void testGetNotFound() throws Exception {
        Mockito.when(service.findById("999")).thenReturn(null);

        given().when().get("/blog-posts/999").then().statusCode(404);
    }

    @Test
    public void testCreate() throws Exception {
        BlogPost input = new BlogPost(null, "New Post", "https://blog.zenika.com/article");
        input.setWriters(Collections.singletonList(User.builder().name("Alice").build()));
        input.setStatus(BlogPostStatus.DRAFT);
        input.setTags(Collections.singletonList("java"));
        BlogPost created = new BlogPost("new-id", "New Post", "https://blog.zenika.com/article");

        Mockito.when(service.create(Mockito.any(BlogPost.class))).thenReturn(created);

        given().contentType(ContentType.JSON)
                .body(input)
                .when()
                .post("/blog-posts")
                .then()
                .statusCode(201)
                .body("id", is("new-id"))
                .body("title", is("New Post"));
    }
}
