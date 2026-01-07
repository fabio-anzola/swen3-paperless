import { api } from "@/config";

interface UserDto {
  id: number;
  username: string;
}

export type DocumentShareDto = {
  id: number;
  documentId: number;
  token: string;
  startsAt?: string | null;
  expiresAt?: string | null;
  active: boolean;
  passwordProtected: boolean;
  createdAt: string;
};

export type DocumentShareLogDto = {
  shareId: number;
  accessedAt: string;
  success: boolean;
  remoteAddress?: string | null;
  userAgent?: string | null;
  reason?: string | null;
};

export async function postLogin(
  username: string,
  password: string
): Promise<string> {
  const res = await fetch(api("/user/login"), {
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

export async function postRegister(
  username: string,
  password: string
): Promise<UserDto> {
  const res = await fetch(api("/user/register"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    const msg = await safeMessage(res);
    throw new Error(msg || `Registration failed (${res.status})`);
  }
  return res.json();
}

export async function uploadDocument(file: File, token: string, name?: string) {
  const formData = new FormData();
  formData.append("file", file);

  if (name) {
    formData.append("meta", JSON.stringify({ name }));
  }

  const res = await fetch(api("/document"), {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: formData,
  });

  if (!res.ok) {
    const msg = await safeMessage(res);
    throw new Error(msg || `Upload failed (${res.status})`);
  }

  return res.json();
}

export async function createDocumentShare(
  documentId: number,
  token: string,
  payload: {
    password?: string;
    startsAt?: string;
    expiresAt?: string;
  }
): Promise<DocumentShareDto> {
  const res = await fetch(api(`/document/${documentId}/shares`), {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const msg = await safeMessage(res);
    throw new Error(msg || `Failed to create share (${res.status})`);
  }

  return res.json();
}

export async function getDocumentShares(
  documentId: number,
  token: string
): Promise<DocumentShareDto[]> {
  const res = await fetch(api(`/document/${documentId}/shares`), {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!res.ok) {
    const msg = await safeMessage(res);
    throw new Error(msg || `Failed to load shares (${res.status})`);
  }

  return res.json();
}

export async function getDocumentShareLogs(
  documentId: number,
  token: string
): Promise<DocumentShareLogDto[]> {
  const res = await fetch(api(`/document/${documentId}/shares/logs`), {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!res.ok) {
    const msg = await safeMessage(res);
    throw new Error(msg || `Failed to load share logs (${res.status})`);
  }

  return res.json();
}

export async function deactivateDocumentShare(
  documentId: number,
  shareId: number,
  token: string
): Promise<DocumentShareDto> {
  const res = await fetch(api(`/document/${documentId}/shares/${shareId}`), {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!res.ok) {
    const msg = await safeMessage(res);
    throw new Error(msg || `Failed to deactivate share (${res.status})`);
  }

  return res.json();
}

async function safeMessage(res: Response) {
  try {
    const j = await res.json();
    return j?.message;
  } catch {
    return undefined;
  }
}
