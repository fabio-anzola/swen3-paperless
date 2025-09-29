export const API_BASE = process.env.NEXT_PUBLIC_API_BASE!;

export async function postLogin(username: string, password: string): Promise<string> {
  const res = await fetch(`${API_BASE}/user/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    const msg = await safeMessage(res);
    throw new Error(msg || `Login failed (${res.status})`);
  }
  // backend returns token as plain text
  return res.text();
}

async function safeMessage(res: Response) {
  try { const j = await res.json(); return j?.message; } catch { return undefined; }
}