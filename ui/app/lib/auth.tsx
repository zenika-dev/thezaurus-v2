import React, { createContext, useContext, useEffect, useState, useCallback, useRef } from "react";
import { useNavigate } from "react-router";
import GoogleLoginButton from "../components/Auth/GoogleLoginButton";

export type Role = "membre" | "admin";

export interface User {
  email: string;
  roles: Role[];
}

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  error: string | null;
  setToken: (token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [token, setTokenState] = useState<string | null>(() => {
    if (typeof window !== "undefined") {
      return localStorage.getItem("google_token");
    }
    return null;
  });
  const isFirstVerification = useRef(true);

  const setToken = useCallback((newToken: string) => {
    localStorage.setItem("google_token", newToken);
    setError(null);
    setTokenState(newToken);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("google_token");
    setTokenState(null);
    setUser(null);
    setError(null);
  }, []);

  useEffect(() => {
    // Bypass conditionnel de l'authentification (activable via paramètre)
    if (import.meta.env.VITE_MOCK_AUTH === "true") {
      setUser({ email: "dev@zenika.com", roles: ["admin", "membre"] });
      setIsLoading(false);
      return;
    }

    if (!token) {
      setUser(null);
      setIsLoading(false);
      isFirstVerification.current = false;
      return;
    }

    setIsLoading(true);

    const headers: HeadersInit = {
      "Authorization": `Bearer ${token}`
    };

    fetch("/api/me", { headers })
      .then((res) => {
        if (!res.ok) {
          throw new Error("Not authenticated");
        }
        return res.json();
      })
      .then((data) => {
        setUser(data);
        setIsLoading(false);
      })
      .catch((err) => {
        console.error("Auth error:", err);
        localStorage.removeItem("google_token");
        setTokenState(null);
        setUser(null);
        setIsLoading(false);

        if (!isFirstVerification.current) {
          setError("Impossible de s'authentifier");
        }
      })
      .finally(() => {
        isFirstVerification.current = false;
      });
  }, [token]);

  return (
    <AuthContext.Provider value={{ user, isLoading, error, setToken, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}

export function ProtectedRoute({
  children,
  allowedRoles,
}: {
  children: React.ReactNode;
  allowedRoles: Role[];
}) {
  const { user, isLoading, setToken, error } = useAuth();
  const navigate = useNavigate();

  if (isLoading) {
    return <div className="p-4 text-center">Chargement de la session...</div>;
  }

  if (!user) {
    // En développement local (même si build de prod via Docker), on affiche le bouton Google Login
    const isLocalhost = typeof window !== 'undefined' && (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1");
    if (import.meta.env.DEV || isLocalhost) {
      return (
        <div className="flex items-center justify-center min-h-[50vh]">
          <div className="p-8 bg-white shadow-xl rounded-xl border border-gray-100 text-center space-y-6">
            <h2 className="text-2xl font-bold text-gray-800">Accès Protégé</h2>
            <p className="text-gray-600">
              Veuillez vous connecter pour accéder à theZaurus.
            </p>
            {error && <p className="text-red-500 text-sm">{error}</p>}
            <GoogleLoginButton
              onSuccess={(token) => setToken(token)}
              onError={() => console.error("Login failed")}
            />
          </div>
        </div>
      );
    } else {
      // En production avec IAP, on ne devrait jamais arriver ici si IAP fait bien son travail
      return <div className="p-4 text-center">Accès refusé. Veuillez passer par Google IAP.</div>;
    }
  }

  const hasRole = allowedRoles.some((role) => user.roles.includes(role));
  if (!hasRole) {
    return (
      <div className="p-4 text-center text-red-500">
        Vous n'avez pas les droits nécessaires pour accéder à cette page.
      </div>
    );
  }

  return <>{children}</>;
}
