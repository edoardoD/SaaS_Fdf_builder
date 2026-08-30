import { S3Client } from "@aws-sdk/client-s3";

// We use minio for server-side S3 calls in docker, but we override it in presigned url to allow localhost fetching on host
const endpoint = process.env.S3_ENDPOINT || "http://localhost:9000";
const accessKeyId = process.env.S3_ACCESS_KEY_ID || "minioadmin";
const secretAccessKey = process.env.S3_SECRET_ACCESS_KEY || "minioadmin";
const region = process.env.S3_REGION || "us-east-1";

// Internal client used for PutObject inside docker network
export const s3Internal = new S3Client({
  endpoint: process.env.NODE_ENV === "production" ? "http://minio:9000" : endpoint,
  region,
  credentials: {
    accessKeyId,
    secretAccessKey,
  },
  forcePathStyle: true, // Required for MinIO
});

// External client used to generate presigned url pointing to host machine (localhost)
export const s3External = new S3Client({
  endpoint: endpoint,
  region,
  credentials: {
    accessKeyId,
    secretAccessKey,
  },
  forcePathStyle: true, // Required for MinIO
});

export const BUCKET_NAME = process.env.S3_BUCKET_NAME || "manutenzioni";
