import { useAppStore } from "@/store";
import { apiClient } from "@/lib/api-client";
import { CONVERSATIONS_ROUTES, conversationMessagesRoute, conversationRoute, MESSAGE_PAGE_SIZE, WS_URL } from "@/utils/constants";
import { Client } from "@stomp/stompjs";
import { createContext, useContext, useEffect, useMemo, useRef } from "react"
import { toast } from "sonner";

const SocketContext = createContext(null);

export const useSocket = () => {
    return useContext(SocketContext);
};

/** Tải conversation chưa có trong sidebar (vd. người khác vừa nhắn lần đầu). */
const fetchConversation = async (id) => {
    try {
        const response = await apiClient.get(conversationRoute(id));
        useAppStore.getState().upsertConversation(response.data);
    } catch (error) {
        console.log({ error });
    }
};

/** Sau khi mất kết nối: tải lại sidebar và tin nhắn của chat đang mở để không bỏ lỡ tin. */
const resync = async () => {
    const { selectedChatData, setConversations, setSelectedChatMessages, setHasMoreMessages } = useAppStore.getState();
    try {
        const conversations = await apiClient.get(CONVERSATIONS_ROUTES);
        setConversations(conversations.data);
        if (selectedChatData) {
            const messages = await apiClient.get(conversationMessagesRoute(selectedChatData.id), {
                params: { limit: MESSAGE_PAGE_SIZE },
            });
            if (useAppStore.getState().selectedChatData?.id === selectedChatData.id) {
                setSelectedChatMessages(messages.data);
                setHasMoreMessages(messages.data.length === MESSAGE_PAGE_SIZE);
            }
        }
    } catch (error) {
        console.log({ error });
    }
};

export const SocketProvider = ({ children }) => {
    const client = useRef();
    const { userInfo } = useAppStore();
    const userId = userInfo?.id;

    useEffect(() => {
        if (!userId) return;

        let connectedBefore = false;
        // Cookie JWT được trình duyệt gửi kèm handshake, server lấy user từ đó
        const stomp = new Client({
            brokerURL: WS_URL,
            reconnectDelay: 3000,
            onConnect: () => {
                console.log("Connected to socket server");
                stomp.subscribe("/user/queue/messages", (frame) => {
                    const message = JSON.parse(frame.body);
                    if (!useAppStore.getState().receiveMessage(message)) {
                        fetchConversation(message.conversationId);
                    }
                });
                stomp.subscribe("/user/queue/conversations", (frame) => {
                    useAppStore.getState().upsertConversation(JSON.parse(frame.body));
                });
                stomp.subscribe("/user/queue/errors", (frame) => {
                    toast.error(JSON.parse(frame.body).message);
                });
                if (connectedBefore) resync();
                connectedBefore = true;
            },
            onStompError: (frame) => console.log("STOMP error", frame.headers.message),
        });
        stomp.activate();
        client.current = stomp;

        return () => {
            stomp.deactivate();
            client.current = undefined;
        };
    }, [userId]);

    const api = useMemo(() => ({
        /** payload: { type: "TEXT", content } | { type: "FILE", objectKey } */
        sendMessage: (conversationId, payload) => {
            const stomp = client.current;
            if (!stomp?.connected) {
                toast.error("Not connected. Please wait and try again.");
                return false;
            }
            stomp.publish({
                destination: `/app/conversations/${conversationId}/send`,
                body: JSON.stringify({ clientMsgId: crypto.randomUUID(), ...payload }),
            });
            return true;
        },
    }), []);

    return (
        <SocketContext.Provider value={api}>
            {children}
        </SocketContext.Provider>
    )
}
