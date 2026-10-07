export const HOST = import.meta.env.VITE_SERVER_URL;
export const WS_URL = `${HOST.replace(/^http/, "ws")}/ws`;

export const AUTH_ROUTES = "api/auth";
export const SIGNUP_ROUTE = `${AUTH_ROUTES}/signup`;
export const LOGIN_ROUTE = `${AUTH_ROUTES}/login`;
export const GET_USER_INFO = `${AUTH_ROUTES}/me`;
export const LOGOUT_ROUTE = `${AUTH_ROUTES}/logout`;

export const USERS_ROUTES = "api/users";
export const UPDATE_PROFILE_ROUTE = `${USERS_ROUTES}/me`;
export const PROFILE_IMAGE_ROUTE = `${USERS_ROUTES}/me/avatar`;
export const SEARCH_CONTACTS_ROUTES = `${USERS_ROUTES}/search`;
export const GET_ALL_CONTACTS_ROUTES = USERS_ROUTES;

export const CONVERSATIONS_ROUTES = "api/conversations";
export const OPEN_DIRECT_ROUTE = `${CONVERSATIONS_ROUTES}/direct`;
export const CREATE_CHANNEL_ROUTE = `${CONVERSATIONS_ROUTES}/group`;
export const conversationRoute = (id) => `${CONVERSATIONS_ROUTES}/${id}`;
export const conversationMessagesRoute = (id) => `${CONVERSATIONS_ROUTES}/${id}/messages`;

export const PRESIGN_ROUTE = "api/media/presign";

export const MESSAGE_PAGE_SIZE = 30;
