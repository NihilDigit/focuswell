import { createHash } from "node:crypto";
import type { CloudSnapshot } from "./sync-store";
import { SyncStore } from "./sync-store";

export type GitHubUser = {
  id: number;
  login: string;
};

export type SnapshotMetadata = Omit<CloudSnapshot, "payload">;

export class SyncService {
  constructor(
    private readonly store: SyncStore,
    private readonly clientId: string,
    private readonly clientSecret: string,
  ) {}

  config(): { clientId: string; redirectUri: string } {
    return { clientId: this.clientId, redirectUri: redirectUri() };
  }

  async exchangeCode(code: string): Promise<{ accessToken: string; user: GitHubUser }> {
    const tokenResponse = await fetch("https://github.com/login/oauth/access_token", {
      method: "POST",
      headers: {
        accept: "application/json",
        "content-type": "application/json",
      },
      body: JSON.stringify({
        client_id: this.clientId,
        client_secret: this.clientSecret,
        code,
        redirect_uri: redirectUri(),
      }),
    });
    if (!tokenResponse.ok) throw new Error(`github-token-${tokenResponse.status}`);
    const tokenBody = (await tokenResponse.json()) as { access_token?: string; error?: string };
    if (tokenBody.error) throw new Error(`github-${tokenBody.error}`);
    if (!tokenBody.access_token) throw new Error("github-token-missing");
    return {
      accessToken: tokenBody.access_token,
      user: await this.githubUser(tokenBody.access_token),
    };
  }

  async getSnapshot(accessToken: string): Promise<{ user: GitHubUser; snapshot: CloudSnapshot | null }> {
    const user = await this.githubUser(accessToken);
    return { user, snapshot: await this.store.getSnapshot(user.id) };
  }

  async putSnapshot(
    accessToken: string,
    args: {
      updatedAtUtc: string;
      appVersion: string;
      payload: unknown;
    },
  ): Promise<{ user: GitHubUser; snapshot: CloudSnapshot }> {
    const user = await this.githubUser(accessToken);
    const snapshot: CloudSnapshot = {
      schemaVersion: 1,
      githubUserId: user.id,
      githubLogin: user.login,
      updatedAtUtc: args.updatedAtUtc,
      uploadedAtUtc: new Date().toISOString(),
      appVersion: args.appVersion,
      jsonHash: sha256Json(args.payload),
      payload: args.payload,
    };
    await this.store.putSnapshot(snapshot);
    return { user, snapshot };
  }

  private async githubUser(accessToken: string): Promise<GitHubUser> {
    const response = await fetch("https://api.github.com/user", {
      headers: {
        accept: "application/vnd.github+json",
        authorization: `Bearer ${accessToken}`,
        "user-agent": "FocusWell",
      },
    });
    if (!response.ok) throw new Error(response.status === 401 ? "unauthorized" : `github-user-${response.status}`);
    const body = (await response.json()) as { id?: number; login?: string };
    if (typeof body.id !== "number" || typeof body.login !== "string") throw new Error("github-user-invalid");
    return { id: body.id, login: body.login };
  }
}

export function createSyncService(): SyncService | null {
  const redisUrl = process.env.KV_REST_API_URL;
  const redisToken = process.env.KV_REST_API_TOKEN;
  const clientId = process.env.GITHUB_OAUTH_CLIENT_ID;
  const clientSecret = process.env.GITHUB_OAUTH_CLIENT_SECRET;
  if (!redisUrl || !redisToken || !clientId || !clientSecret) return null;
  return new SyncService(new SyncStore(redisUrl, redisToken), clientId, clientSecret);
}

export function metadata(snapshot: CloudSnapshot): SnapshotMetadata {
  const { payload: _, ...rest } = snapshot;
  return rest;
}

function sha256Json(value: unknown): string {
  return createHash("sha256").update(JSON.stringify(value)).digest("hex");
}

function redirectUri(): string {
  return "focuswell://sync/oauth";
}
