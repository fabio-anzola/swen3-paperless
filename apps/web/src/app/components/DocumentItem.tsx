interface DocumentItemProps {
  document: {
    id: number;
    name: string;
    s3Key?: string | null;
    ownerId: number;
    summary?: string | null;
    score?: number | null;
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
    <li className="flex items-center justify-between rounded-xl bg-white p-4 shadow gap-4">
      <div className="flex-1">
        <div className="text-gray-900 font-medium">{document.name}</div>
        {document.summary && (
          <p className="mt-1 text-sm text-gray-600 overflow-hidden text-ellipsis">
            {document.summary}
          </p>
        )}
      </div>
      <button
        className="rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        disabled={isDownloading}
        onClick={() => onDownload(document.id)}
      >
        {isDownloading ? "Downloading..." : "Download"}
      </button>
    </li>
  );
}
