import { api } from "@/config";

interface UserDto {
  id: number;
  username: string;
}

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

async function safeMessage(res: Response) {
  try {
    const j = await res.json();
    return j?.message;
  } catch {
    return undefined;
  }
}
