import axios from "axios";
import { HOST } from "@/utils/constants";

export const apiClient = axios.create({
    baseURL: HOST,
    withCredentials: true,
});

/** Lấy message lỗi từ response `{status, message}` của server. */
export const getErrorMessage = (error, fallback = "Something went wrong.") =>
    error?.response?.data?.message || fallback;
