export type CloudSnapshot = {
  schemaVersion: number;
  githubUserId: number;
  githubLogin: string;
  updatedAtUtc: string;
  uploadedAtUtc: string;
  appVersion: string;
  jsonHash: string;
  payload: unknown;
};

export class SyncStore {
  constructor(
    private readonly url: string,
    private readonly token: string,
  ) {}

  async getSnapshot(githubUserId: number): Promise<CloudSnapshot | null> {
    const value = await this.command<string | null>(["GET", snapshotKey(githubUserId)]);
    return value ? (JSON.parse(value) as CloudSnapshot) : null;
  }

  async putSnapshot(snapshot: CloudSnapshot): Promise<void> {
    await this.command(["SET", snapshotKey(snapshot.githubUserId), JSON.stringify(snapshot)]);
  }

  private async command<T>(command: Array<string | number>): Promise<T> {
    const response = await fetch(this.url, {
      method: "POST",
      headers: {
        authorization: `Bearer ${this.token}`,
        "content-type": "application/json",
      },
      body: JSON.stringify(command),
    });
    if (!response.ok) throw new Error(`redis-${response.status}`);
    const body = (await response.json()) as { result?: T; error?: string };
    if (body.error) throw new Error(body.error);
    return body.result as T;
  }
}

function snapshotKey(githubUserId: number): string {
  return `focuswell:cloud-ledger:github:${githubUserId}`;
}
