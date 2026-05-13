package com.zenika.thezaurus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/status")
public class AppStatusResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> status() {
        return Map.of("status", "UP", "message", "TheZaurus API is running");
    }
}
