export interface GoogleRefreshedTokens {
  idToken?: string;
  refreshToken?: string;
  expiresIn?: number;
}

export async function requestGoogleTokenRefresh(
  refreshToken: string,
): Promise<GoogleRefreshedTokens | null> {
  const clientId = process.env.GOOGLE_CLIENT_ID;
  const clientSecret = process.env.GOOGLE_CLIENT_SECRET;
  if (!clientId || !clientSecret || !refreshToken) {
    return null;
  }

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({
      client_id: clientId,
      client_secret: clientSecret,
      grant_type: "refresh_token",
      refresh_token: refreshToken,
    }),
  });

  if (!response.ok) {
    throw await response.json().catch(() => ({}));
  }

  const data = await response.json();
  return {
    idToken: data.id_token,
    refreshToken: data.refresh_token,
    expiresIn: typeof data.expires_in === "number" ? data.expires_in : 3600,
  };
}
