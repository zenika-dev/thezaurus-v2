package com.zenika.thezaurus.visuals.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.visuals.client.ImageInputClient;
import com.zenika.thezaurus.visuals.crop.AutoCropService;
import com.zenika.thezaurus.visuals.crop.CropGeometry;
import com.zenika.thezaurus.visuals.model.BlogVisualRequest;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.PhotoInput;
import com.zenika.thezaurus.visuals.render.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VisualGenerationServiceTest {
    final VisualGenerationService service = new VisualGenerationService();
    final byte[] denied = {1};
    final byte[] allowed = {2};

    @BeforeEach
    void setup() {
        service.catalog = new TemplateCatalog();
        service.templates = new TemplateService();
        service.images = mock(ImageInputClient.class);
        service.cropper = mock(AutoCropService.class);
        service.renderer = mock(ImageRenderer.class);
        when(service.images.load("denied")).thenReturn(new ImageInputClient.Image(denied, 400, 800));
        when(service.images.load("allowed")).thenReturn(new ImageInputClient.Image(allowed, 800, 400));
        when(service.renderer.render(any())).thenReturn(new RenderResult(new byte[] {3}, 1080, 1350));
    }

    @Test
    void perPersonOptOutNeverReachesAiWhileOtherPhotosCanBeAnalyzed() {
        when(service.cropper.analyze(eq(allowed), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new AutoCropService.CropResult(
                        new CropGeometry.Crop(1, 0, 0),
                        "accepted",
                        null,
                        List.of(),
                        new AutoCropService.Usage(10, 2, 12),
                        1,
                        false));
        var response = service.generateBlogVisual(new BlogVisualRequest(
                "Article",
                List.of(
                        new PersonInput("Sans IA", PhotoInput.ofBase64("denied"), false),
                        new PersonInput("Avec IA", PhotoInput.ofBase64("allowed"), true)),
                true));
        verify(service.cropper).analyze(eq(allowed), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(service.cropper, never()).analyze(eq(denied), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        assertEquals(1, response.usage().calls());
        assertEquals("disabled", response.crops().getFirst().status());
        assertEquals(new CropGeometry.Crop(1, 0, 1), response.crops().getFirst().crop());
        var rendered = ArgumentCaptor.forClass(RenderData.class);
        verify(service.renderer).render(rendered.capture());
        assertArrayEquals(denied, rendered.getValue().people().getFirst().photo());
        assertEquals(
                RenderData.Crop.topCentered(),
                rendered.getValue().people().getFirst().crop());
    }

    @Test
    void globalOptOutNeverInvokesAiEvenForAnOptedInPerson() {
        var response = service.generateBlogVisual(new BlogVisualRequest(
                "Article", List.of(new PersonInput("Sans IA", PhotoInput.ofBase64("denied"), true)), false));
        verifyNoInteractions(service.cropper);
        assertEquals(0, response.usage().calls());
        assertEquals(new CropGeometry.Crop(1, 0, 1), response.crops().getFirst().crop());
    }
}
