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
  events: {
    all: ["events"] as const,
    dashboard: (year: number) => ["events", "dashboard", year] as const,
  },
  profile: {
    all: ["profile"] as const,
    me: () => ["profile", "me"] as const,
  },
} as const;
