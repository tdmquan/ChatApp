const sortByActivity = (conversations) =>
    [...conversations].sort((a, b) =>
        new Date(b.lastMessageAt ?? b.createdAt) - new Date(a.lastMessageAt ?? a.createdAt));

export const createChatSlice = (set, get) => ({
    // selectedChatType: "contact" (DIRECT) | "channel" (GROUP); selectedChatData: ConversationDto
    selectedChatType: undefined,
    selectedChatData: undefined,
    selectedChatMessages: [],
    hasMoreMessages: false,
    conversations: [],
    isUploading: false,
    fileUploadProgress: 0,
    setIsUploading: (isUploading) => set({ isUploading }),
    setFileUploadProgress: (fileUploadProgress) => set({ fileUploadProgress }),
    setConversations: (conversations) => set({ conversations: sortByActivity(conversations) }),
    setSelectedChatMessages: (selectedChatMessages) => set({ selectedChatMessages }),
    setHasMoreMessages: (hasMoreMessages) => set({ hasMoreMessages }),

    selectConversation: (conversation) => {
        if (get().selectedChatData?.id === conversation.id) return;
        set({
            selectedChatType: conversation.type === "GROUP" ? "channel" : "contact",
            selectedChatData: conversation,
            selectedChatMessages: [],
            hasMoreMessages: false,
        });
    },
    closeChat: () => set({
        selectedChatData: undefined,
        selectedChatType: undefined,
        selectedChatMessages: [],
        hasMoreMessages: false,
    }),

    /** Thêm hoặc cập nhật conversation trong sidebar. */
    upsertConversation: (conversation) => {
        const others = get().conversations.filter((c) => c.id !== conversation.id);
        set({ conversations: sortByActivity([conversation, ...others]) });
    },

    prependMessages: (older) => {
        set({ selectedChatMessages: [...older, ...get().selectedChatMessages] });
    },

    /**
     * Xử lý tin nhắn mới từ WebSocket. Trả về false nếu conversation chưa có trong sidebar
     * (người gọi cần tải conversation đó về).
     */
    receiveMessage: (message) => {
        const { selectedChatData, selectedChatMessages, conversations } = get();
        if (selectedChatData?.id === message.conversationId
            && !selectedChatMessages.some((m) => m.id === message.id)) {
            set({ selectedChatMessages: [...selectedChatMessages, message] });
        }
        const conversation = conversations.find((c) => c.id === message.conversationId)
            ?? (selectedChatData?.id === message.conversationId ? selectedChatData : undefined);
        if (!conversation) return false;
        get().upsertConversation({ ...conversation, lastMessageAt: message.createdAt });
        return true;
    },
});
