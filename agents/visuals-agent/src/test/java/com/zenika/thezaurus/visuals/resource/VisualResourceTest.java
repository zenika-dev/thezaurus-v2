package com.zenika.thezaurus.visuals.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(VisualResourceTest.RenderingOnly.class)
class VisualResourceTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "talks,speakers,speakerName,speakerPhotoBase64",
        "blogs,writers,authorName,authorPhotoBase64"
    })
    void acceptsHistoricalPersonAliases(String category, String peopleField, String nameField, String photoField)
            throws IOException {
        var body = new java.util.HashMap<String, Object>();
        body.put("title", "Compatibilité");
        body.put(
                peopleField,
                List.of(Map.of(
                        nameField, "Alice", photoField, Base64.getEncoder().encodeToString(photo()))));
        body.put("autoCrop", false);
        if (category.equals("talks")) body.put("conference", "Devoxx");
        given().contentType("application/json")
                .body(body)
                .post("/visuals/" + category)
                .then()
                .statusCode(200)
                .body("crops[0].status", equalTo("disabled"));
    }

    @Test
    void documentsTheDeterministicCatalogInsteadOfLegacyGenerationPrompts() {
        given().get("/visuals/templates?category=BLOG")
                .then()
                .statusCode(200)
                .body("[0].fields", org.hamcrest.Matchers.contains("title", "writers", "autoCrop"))
                .body("[0].prompt", org.hamcrest.Matchers.nullValue());
    }
    /** No credential or live AI request is needed for HTTP rendering tests. */
    public static class RenderingOnly implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("visuals.crop.enabled", "false", "visuals.api-key", "");
        }
    }

    @Test
    void exposesTheEightPrTemplatesAndCategoryFilters() {
        given().get("/visuals/templates").then().statusCode(200).body("", hasSize(8));
        given().queryParam("category", "TALK")
                .get("/visuals/templates")
                .then()
                .statusCode(200)
                .body("", hasSize(4));
        given().queryParam("category", "BLOG")
                .get("/visuals/templates")
                .then()
                .statusCode(200)
                .body("", hasSize(2));
        given().queryParam("category", "NIGHT_CLAZZ")
                .get("/visuals/templates")
                .then()
                .statusCode(200)
                .body("", hasSize(2));
    }

    @Test
    void rendersAllEightPrTemplatesAsNativePngsWithoutAi() throws IOException {
        List<Case> cases = List.of(
                new Case("talks", talk(List.of("Alice Martin")), "generate-image-speaker-event", 1080),
                new Case(
                        "talks",
                        talk(List.of("Alice Martin", "Thomas Bernard")),
                        "generate-image-2-speaker-event",
                        1080),
                new Case(
                        "talks",
                        talk(List.of("Alice Martin", "Thomas Bernard", "Camille Dupont")),
                        "generate-image-3-speaker-event",
                        1080),
                new Case(
                        "talks",
                        Map.of(
                                "conference",
                                "Devoxx France",
                                "talks",
                                List.of(
                                        Map.of(
                                                "title",
                                                "Les agents IA avec Quarkus",
                                                "speakers",
                                                List.of("Alice Martin")),
                                        Map.of("title", "Java en production", "speakers", List.of("Thomas Bernard")))),
                        "generate-image-2-talks-event",
                        1080),
                new Case("blogs", blog(List.of("Alice Martin")), "generate-image-blog-post", 1350),
                new Case("blogs", blog(List.of("Alice Martin", "Thomas Bernard")), "generate-image-2-blog-post", 1350),
                new Case(
                        "nightclazz",
                        night(List.of("Alice Martin", "Thomas Bernard")),
                        "generate-image-nightclazz",
                        1080),
                new Case(
                        "nightclazz",
                        Map.of(
                                "title",
                                "Agents IA en production",
                                "date",
                                "19/11",
                                "hour",
                                "18h30–21h",
                                "location",
                                "Zenika Nantes",
                                "speakers",
                                List.of("Alice Martin"),
                                "partner",
                                "Google Cloud"),
                        "generate-image-nightclazz-partner",
                        1080));
        for (Case entry : cases) {
            Response response = given().contentType("application/json")
                    .accept("image/png")
                    .body(entry.body())
                    .post("/visuals/" + entry.category() + "/image")
                    .then()
                    .statusCode(200)
                    .contentType("image/png")
                    .extract()
                    .response();
            assertEquals(entry.template(), response.header("X-Visual-Template"));
            BufferedImage image = readPng(response.asByteArray());
            assertEquals(1080, image.getWidth(), entry.template());
            assertEquals(entry.height(), image.getHeight(), entry.template());
        }
    }

    @Test
    void keepsThePrJsonResponseAndReportsZeroAiUsageWhenDisabled() throws IOException {
        String photo = "data:image/png;base64," + Base64.getEncoder().encodeToString(photo());
        Response response = given().contentType("application/json")
                .body(Map.of(
                        "title",
                        "Découvrir Java",
                        "writers",
                        List.of(Map.of("name", "Élodie Martin", "photo", Map.of("base64", photo))),
                        "autoCrop",
                        true))
                .post("/visuals/blogs")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(
                        "category",
                        equalTo("BLOG"),
                        "templateName",
                        equalTo("generate-image-blog-post"),
                        "mimeType",
                        equalTo("image/png"),
                        "width",
                        equalTo(1080),
                        "height",
                        equalTo(1350),
                        "usage.calls",
                        equalTo(0),
                        "usage.inputTokens",
                        equalTo(0),
                        "usage.outputTokens",
                        equalTo(0),
                        "usage.totalTokens",
                        equalTo(0))
                .extract()
                .response();
        byte[] png = Base64.getDecoder().decode(response.jsonPath().getString("imageBase64"));
        assertEquals(1350, readPng(png).getHeight());
        assertNotNull(response.jsonPath().getString("generatedAt"));
    }

    @Test
    void rendersTheSoloNightClazzVariant() throws IOException {
        byte[] png = given().contentType("application/json")
                .body(night(List.of("Alice Martin")))
                .post("/visuals/nightclazz/image")
                .then()
                .statusCode(200)
                .contentType("image/png")
                .extract()
                .asByteArray();
        assertEquals(1080, readPng(png).getHeight());
    }

    @Test
    void rejectsMissingFieldsAndUnsupportedSpeakerCounts() {
        given().contentType("application/json")
                .body(Map.of("title", "Talk", "speakers", List.of("Alice")))
                .post("/visuals/talks/image")
                .then()
                .statusCode(400);
        given().contentType("application/json")
                .body(talk(List.of()))
                .post("/visuals/talks/image")
                .then()
                .statusCode(400);
        given().contentType("application/json")
                .body(talk(List.of("A", "B", "C", "D")))
                .post("/visuals/talks/image")
                .then()
                .statusCode(400);
        given().contentType("application/json")
                .body(Map.of("title", " ", "writers", List.of("Alice")))
                .post("/visuals/blogs/image")
                .then()
                .statusCode(400);
        given().contentType("application/json")
                .body(blog(List.of("A", "B", "C")))
                .post("/visuals/blogs/image")
                .then()
                .statusCode(400);
        given().contentType("application/json")
                .body(Map.of("title", "NightClazz", "speakers", List.of("A", "B"), "partner", "Partner"))
                .post("/visuals/nightclazz/image")
                .then()
                .statusCode(400);
        given().contentType("application/json")
                .body("{")
                .post("/visuals/blogs/image")
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsManualCropParametersAtEveryInputLevel() throws IOException {
        Map<String, Object> crop = Map.of("zoom", 2, "x", .3, "y", 0);
        given().contentType("application/json")
                .body(Map.of("title", "Talk", "conference", "Devoxx", "speakers", List.of("Alice"), "crop", crop))
                .post("/visuals/talks/image")
                .then()
                .statusCode(400);
        given().contentType("application/json")
                .body(Map.of(
                        "title",
                        "Talk",
                        "conference",
                        "Devoxx",
                        "speakers",
                        List.of(Map.of("name", "Alice", "crop", crop))))
                .post("/visuals/talks/image")
                .then()
                .statusCode(400);
        given().contentType("application/json")
                .body(Map.of(
                        "title",
                        "Blog",
                        "writers",
                        List.of(Map.of(
                                "name",
                                "Alice",
                                "photo",
                                Map.of("base64", Base64.getEncoder().encodeToString(photo()), "crop", crop)))))
                .post("/visuals/blogs/image")
                .then()
                .statusCode(400);
    }

    private static Map<String, Object> talk(List<String> people) {
        return Map.of("title", "Les agents IA avec Quarkus", "conference", "Devoxx France", "speakers", people);
    }

    private static Map<String, Object> blog(List<String> people) {
        return Map.of("title", "Les agents IA avec Quarkus", "writers", people);
    }

    private static Map<String, Object> night(List<String> people) {
        return Map.of(
                "title",
                "Les agents IA avec Quarkus",
                "date",
                "19/11",
                "hour",
                "18h30–21h",
                "location",
                "Zenika Nantes",
                "speakers",
                people);
    }

    private static BufferedImage readPng(byte[] png) throws IOException {
        assertTrue(png.length > 8);
        BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(image, "The response must be a decodable PNG");
        return image;
    }

    private static byte[] photo() throws IOException {
        BufferedImage image = new BufferedImage(40, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(20, 90, 124));
            g.fillRect(0, 0, 40, 60);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", encoded);
        return encoded.toByteArray();
    }

    private record Case(String category, Map<String, Object> body, String template, int height) {}
}
