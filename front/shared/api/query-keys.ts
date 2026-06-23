export const queryKeys = {
  talks: {
    all: ["talks"] as const,
    lists: () => ["talks", "list"] as const,
  },
  posts: {
    all: ["posts"] as const,
    lists: () => ["posts", "list"] as const,
  },
  conferences: {
    all: ["conferences"] as const,
    lists: () => ["conferences", "list"] as const,
  },
} as const;
