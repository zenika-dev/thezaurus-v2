package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.ExecutionException;

/**
 * Profil de la personne connectée : identité en lecture seule, préférences de notification
 * modifiables. Distinct de {@code /api/me}, endpoint d'identité appelé au login.
 */
@Path("/api/me/profile")
@Produces(MediaType.APPLICATION_JSON)
public class ProfileResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    UserRepository userRepository;

    @GET
    @RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
    public Response getProfile() throws ExecutionException, InterruptedException {
        String email = identity.getPrincipal().getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toProfile(user)).build();
    }

    /** Remplace les deux canaux d'un coup : le front renvoie l'objet complet à chaque bascule. */
    @PUT
    @Path("/notification-preferences")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
    public Response updateNotificationPreferences(NotificationPreferences preferences)
            throws ExecutionException, InterruptedException {
        if (preferences == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        String email = identity.getPrincipal().getName();
        // Garde consultative : rend un 404 lisible plutôt que le 500 de l'update Firestore.
        if (userRepository.findByEmail(email) == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        userRepository.updateNotificationPreferences(email, preferences.email(), preferences.slack());
        return Response.ok(preferences).build();
    }

    private static ProfileView toProfile(User user) {
        return new ProfileView(
                user.name(),
                user.email(),
                new NotificationPreferences(user.notifiesByEmail(), user.notifiesOnSlack()),
                user.slackUserId() != null && !user.slackUserId().isBlank());
    }

    /** {@code slackLinked} et non le {@code slackUserId} : la page n'a besoin que de la joignabilité. */
    public record ProfileView(
            String name, String email, NotificationPreferences notificationPreferences, boolean slackLinked) {}

    public record NotificationPreferences(boolean email, boolean slack) {}
}
