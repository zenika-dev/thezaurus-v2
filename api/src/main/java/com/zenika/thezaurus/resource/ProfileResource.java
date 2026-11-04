package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.ContributionStatistics;
import com.zenika.thezaurus.model.Office;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import com.zenika.thezaurus.service.ContributionStatisticsService;
import com.zenika.thezaurus.service.CurrentUserService;
import com.zenika.thezaurus.service.GooglePhotoIdentityService;
import com.zenika.thezaurus.service.ProfilePhotoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * Profil de la personne connectée : identité Google, photo et préférences personnelles.
 * Distinct de {@code /api/me}, endpoint d'identité appelé au login.
 */
@Path("/api/me/profile")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
public class ProfileResource {

    @Inject
    CurrentUserService currentUser;

    @Inject
    ContributionStatisticsService statistics;

    @GET
    @Path("/contributions")
    public ContributionStatistics contributions() throws ExecutionException, InterruptedException {
        return statistics.forUser(me().email());
    }

    @Inject
    UserRepository userRepository;

    @Inject
    ProfilePhotoService photos;

    @Inject
    GooglePhotoIdentityService googleIdentity;

    private User me() throws ExecutionException, InterruptedException {
        return currentUser.getUser().orElseThrow(NotFoundException::new);
    }

    @PUT
    @Path("/photo")
    @Consumes({"image/jpeg", "image/png", "image/webp", MediaType.APPLICATION_OCTET_STREAM})
    public void uploadPhoto(InputStream input) throws Exception {
        photos.upload(me(), input);
    }

    @DELETE
    @Path("/photo")
    public void resetPhoto() throws ExecutionException, InterruptedException {
        photos.reset(me());
    }

    public record AiPhotoPreference(Boolean allowed) {}

    @PUT
    @Path("/ai-photo")
    @Consumes(MediaType.APPLICATION_JSON)
    public void consent(AiPhotoPreference preference) throws ExecutionException, InterruptedException {
        if (preference == null || preference.allowed() == null) throw new BadRequestException();
        userRepository.updateAiPhotoAllowed(me().email(), preference.allowed());
    }

    public record GooglePhotoSync(String idToken) {}

    /** La source est vérifiée indépendamment du JWT IAP qui ne contient pas de photo. */
    @POST
    @Path("/photo/google")
    @Consumes(MediaType.APPLICATION_JSON)
    public void syncGooglePhoto(GooglePhotoSync request) throws ExecutionException, InterruptedException {
        if (request == null) throw new BadRequestException();
        User user = me();
        photos.syncGoogle(user, googleIdentity.verifiedPhoto(request.idToken(), user.email()));
    }

    @Inject
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "thezaurus.slack.bot-token")
    Optional<String> botToken;

    @PUT
    @Path("/office")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
    public RestResponse<OfficePreference> updateOffice(OfficePreference preference)
            throws ExecutionException, InterruptedException {
        if (preference == null) {
            return RestResponse.status(RestResponse.Status.BAD_REQUEST);
        }
        Optional<User> user = currentUser.getUser();
        if (user.isEmpty()) return RestResponse.notFound();
        userRepository.updateOffice(
                user.get().email(),
                preference.office() == null ? "" : preference.office().value());
        return RestResponse.ok(preference);
    }

    public record OfficePreference(Office office) {}

    @GET
    @RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
    public RestResponse<ProfileView> getProfile() throws ExecutionException, InterruptedException {
        return currentUser
                .getUser()
                .map(user -> RestResponse.ok(toProfile(user)))
                .orElseGet(RestResponse::notFound);
    }

    /** Remplace les deux canaux d'un coup : le front renvoie l'objet complet à chaque bascule. */
    @PUT
    @Path("/notification-preferences")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
    public RestResponse<NotificationPreferences> updateNotificationPreferences(NotificationPreferences preferences)
            throws ExecutionException, InterruptedException {
        if (preferences == null) {
            return RestResponse.status(RestResponse.Status.BAD_REQUEST);
        }
        Optional<User> user = currentUser.getUser();
        if (user.isEmpty()) return RestResponse.notFound();
        userRepository.updateNotificationPreferences(user.get().email(), preferences.email(), preferences.slack());
        return RestResponse.ok(preferences);
    }

    private ProfileView toProfile(User user) {
        boolean slackAvailable =
                botToken != null && botToken.filter(token -> !token.isBlank()).isPresent();
        return new ProfileView(
                user.name(),
                user.email(),
                new NotificationPreferences(user.notifiesByEmail(), user.notifiesOnSlack()),
                slackAvailable,
                Office.fromValue(user.office()),
                user.allowsAiPhoto(),
                user.hasCustomPhoto());
    }

    /**
     * {@code slackAvailable} indique si l'intégration Slack est activée sur l'environnement
     * (permettant la résolution à la volée du compte Slack si l'utilisateur active les notifications).
     */
    public record ProfileView(
            @NotBlank String name,
            @NotBlank String email,
            @NotNull NotificationPreferences notificationPreferences,
            boolean slackAvailable,
            Office office,
            boolean aiPhotoAllowed,
            boolean customPhoto) {}

    public record NotificationPreferences(boolean email, boolean slack) {}
}
