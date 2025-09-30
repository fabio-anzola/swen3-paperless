import { api } from "@/config";

export async function downloadDocument(id: number, token: string) {
  const res = await fetch(api(`/document/${id}/content`), {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!res.ok) {
    throw new Error((await safeText(res)) || `${res.status} ${res.statusText}`);
  }

  const cd = res.headers.get("content-disposition") || "";
  const match = /filename\*?=([^;]+)/i.exec(cd);
  const raw = match
    ? match[1]
        .trim()
        .replace(/^UTF-8''/, "")
        .replace(/"/g, "")
    : `document-${id}`;
  const filename = decodeURIComponent(raw);

  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

async function safeText(res: Response) {
  try {
    return await res.text();
  } catch {
    return null;
  }
}
