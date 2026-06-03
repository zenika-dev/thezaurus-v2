import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  index("routes/events.tsx"),
  route("talks", "routes/talks.tsx"),
  route("conferences", "routes/conferences.tsx"),
  route("blog-posts", "routes/blog-posts.tsx"),
  route("api/*", "routes/api.$.tsx"),
] satisfies RouteConfig;
