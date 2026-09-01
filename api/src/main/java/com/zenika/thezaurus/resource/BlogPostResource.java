package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.BlogPost;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.service.BlogPostService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Path("/blog-posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
public class BlogPostResource {

    @Inject
    BlogPostService service;

    @GET
    public List<BlogPost> list() throws ExecutionException, InterruptedException {
        return service.findAll();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        BlogPost blogPost = service.findById(id);
        if (blogPost == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(blogPost).build();
    }

    @POST
    public Response create(BlogPost blogPost) throws ExecutionException, InterruptedException {
        BlogPost created = service.create(blogPost);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, BlogPost blogPost)
            throws ExecutionException, InterruptedException {
        BlogPost updated = service.update(id, blogPost);
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(Role.Names.ADMIN)
    public Response delete(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        boolean deleted = service.delete(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
