import { clsx } from "clsx";
import { twMerge } from "tailwind-merge"
import animationData from "@/assets/lottie-json"
import { HOST } from "@/utils/constants"

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

export const colors = [
  "bg-[#712c4a57] text-[#ff006e] border-[1px] border-[#ff006faa]",
  "bg-[#ffd60a2a] text-[#ffd60a] border-[1px] border-[#ffd60abb]",
  "bg-[#06d6a02a] text-[#06d6a0] border-[1px] border-[#06d6a0bb]",
  "bg-[#4cc9f02a] text-[#4cc9f0] border-[1px] border-[#4cc9f0bb]",
];

export const getColor = (color) => {
  if (color >= 0 && color < colors.length) {
    return colors[color];
  }
  return colors[0];
};

export const animationDefaultOptions = {
  loop: true,
  autoplay: true,
  animationData,
}
/** Đường dẫn file từ server (vd. /api/files/...) → URL tuyệt đối. */
export const fileUrl = (path) => (path ? `${HOST}${path}` : undefined);

export const fileDownloadUrl = (path) => `${fileUrl(path)}?download=true`;

export const displayName = (user) =>
  user?.firstName ? `${user.firstName} ${user.lastName ?? ""}`.trim() : user?.email ?? "";

export const initial = (user) => (user?.firstName || user?.email || "?").charAt(0);

/** Với chat 1-1, trả về người còn lại trong conversation. */
export const getPeer = (conversation, myId) =>
  conversation?.members?.find((m) => m.id !== myId) ?? conversation?.members?.[0];
