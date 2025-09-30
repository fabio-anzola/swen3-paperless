interface DocumentItemProps {
  document: {
    id: number;
    name: string;
    s3Key?: string | null;
    ownerId: number;
  };
  isDownloading: boolean;
  onDownload: (id: number) => void;
}

export default function DocumentItem({
  document,
  isDownloading,
  onDownload,
}: DocumentItemProps) {
  return (
    <li className="flex items-center justify-between rounded-xl bg-white p-3 shadow">
      <span>{document.name}</span>
      <button
        className="rounded-lg border px-3 py-1 disabled:opacity-50"
        disabled={isDownloading}
        onClick={() => onDownload(document.id)}
      >
        {isDownloading ? "Downloading…" : "Download"}
      </button>
    </li>
  );
}
