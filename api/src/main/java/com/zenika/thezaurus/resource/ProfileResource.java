package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Office;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import com.zenika.thezaurus.service.CurrentUserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * Profil de la personne connectée : identité en lecture seule, préférences de notification
 * modifiables. Distinct de {@code /api/me}, endpoint d'identité appelé au login.
 */
@Path("/api/me/profile")
@Produces(MediaType.APPLICATION_JSON)
public class ProfileResource {

    @Inject
    CurrentUserService currentUser;

    @Inject
    UserRepository userRepository;

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

    private static ProfileView toProfile(User user) {
        return new ProfileView(
                user.name(),
                user.email(),
                new NotificationPreferences(user.notifiesByEmail(), user.notifiesOnSlack()),
                user.slackUserId() != null && !user.slackUserId().isBlank(),
                Office.fromValue(user.office()));
    }

    /** {@code slackLinked} et non le {@code slackUserId} : la page n'a besoin que de la joignabilité. */
    public record ProfileView(
            @NotBlank String name,
            @NotBlank String email,
            @NotNull NotificationPreferences notificationPreferences,
            boolean slackLinked,
            Office office) {}

    public record NotificationPreferences(boolean email, boolean slack) {}
}
