"use client";
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { z } from "zod";

const JwtPayloadSchema = z.object({
  username: z.string(),
  iat: z.number().optional(),
  exp: z.number().optional(),
});
type JwtPayload = z.infer<typeof JwtPayloadSchema>;

type Auth = {
  token: string | null;
  tokenData: JwtPayload | null;
  setToken: (t: string | null) => void;
  logout: () => void;
};

const AuthCtx = createContext<Auth | null>(null);

// Small helper to decode a base64url string
function b64UrlDecode(input: string) {
  // replace url-safe chars
  const base64 = input.replace(/-/g, "+").replace(/_/g, "/");
  // pad
  const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
  return atob(padded);
}

function parseJwt(token: string): JwtPayload | null {
  try {
    const [, payloadB64] = token.split(".");
    if (!payloadB64) return null;
    const json = b64UrlDecode(payloadB64);
    const obj = JSON.parse(json);
    return JwtPayloadSchema.parse(obj);
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setTokenState] = useState<string | null>(null);

  const tokenData = useMemo(() => (token ? parseJwt(token) : null), [token]);

  useEffect(() => {
    const t = localStorage.getItem("token");
    if (t) setTokenState(t);
  }, []);

  const setToken = (t: string | null) => {
    if (t) {
      const parsed = parseJwt(t);
      if (!parsed) {
        localStorage.removeItem("token");
        setTokenState(null);
        return;
      }
      localStorage.setItem("token", t);
      setTokenState(t);
    } else {
      localStorage.removeItem("token");
      setTokenState(null);
    }
  };

  return (
    <AuthCtx.Provider
      value={{
        token,
        tokenData,
        setToken,
        logout: () => setToken(null),
      }}
    >
      {children}
    </AuthCtx.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
