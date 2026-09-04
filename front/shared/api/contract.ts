import type { components } from "./schema";

/**
 * Alias lisibles sur les schémas générés (`components["schemas"]["X"]` est exact mais illisible).
 * Ne jamais redéclarer une interface `BackendXxx` à la main — elle dériverait silencieusement du
 * back. Régénération : `npm run generate:api`.
 */
type Schemas = components["schemas"];

export type BackendBlogPost = Schemas["BlogPost"];
export type BackendBlogPostStatus = Schemas["BlogPostStatus"];

export type BackendTalk = Schemas["Talk"];
export type BackendTalkStatus = Schemas["TalkStatus"];
export type BackendVisibility = Schemas["Visibility"];
export type BackendTalkReviewRequest = Schemas["TalkReviewRequest"];

export type BackendConference = Schemas["Conference"];
export type BackendConferenceType = Schemas["ConferenceType"];
export type BackendConferenceReach = Schemas["ConferenceReach"];
export type BackendConferencePeriod = Schemas["ConferencePeriod"];
export type BackendDatePrecision = Schemas["DatePrecision"];
export type BackendLocation = Schemas["Location"];

export type BackendUser = Schemas["User"];
export type BackendUserSummary = Schemas["UserSummary"];
export type BackendUserAdminView = Schemas["UserAdminView"];
export type BackendRole = Schemas["Role"];
export type BackendNotificationPreferences = Schemas["NotificationPreferences"];

export type BackendEventsDashboard = Schemas["EventsDashboard"];
