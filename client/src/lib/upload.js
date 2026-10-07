import axios from "axios";
import { apiClient } from "@/lib/api-client";
import { PRESIGN_ROUTE } from "@/utils/constants";

/**
 * Upload file thẳng lên MinIO bằng presigned URL, trả về objectKey để gửi cho API.
 * @param {{ purpose: "AVATAR" | "ATTACHMENT", conversationId?: number, file: File, onProgress?: (percent: number) => void }} options
 */
export const uploadFile = async ({ purpose, conversationId, file, onProgress }) => {
    const contentType = file.type || "application/octet-stream";
    const { data } = await apiClient.post(PRESIGN_ROUTE, {
        purpose,
        conversationId,
        fileName: file.name,
        contentType,
        size: file.size,
    });
    // Không dùng apiClient: request đi tới MinIO, không gửi cookie của API
    await axios.put(data.uploadUrl, file, {
        headers: { "Content-Type": contentType },
        onUploadProgress: (e) => onProgress?.(Math.round((100 * e.loaded) / (e.total || file.size))),
    });
    return data.objectKey;
};
