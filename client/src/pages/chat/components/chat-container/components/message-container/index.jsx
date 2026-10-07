import { apiClient } from "@/lib/api-client";
import { useAppStore } from "@/store";
import { conversationMessagesRoute, MESSAGE_PAGE_SIZE } from "@/utils/constants";
import moment from "moment";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { MdFolderZip } from "react-icons/md";
import { IoMdArrowRoundDown } from "react-icons/io"
import { IoCloseSharp } from "react-icons/io5";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { displayName, fileDownloadUrl, fileUrl, getColor, initial } from "@/lib/utils";

const MessageContainer = () => {
    const containerRef = useRef();
    const scrollRef = useRef();
    // Giữ vị trí cuộn khi chèn tin cũ lên đầu danh sách
    const prependAnchor = useRef(null);
    const prevCount = useRef(0);
    const { selectedChatType, selectedChatData, userInfo, selectedChatMessages, setSelectedChatMessages, prependMessages, hasMoreMessages, setHasMoreMessages } = useAppStore();
    const [showImage, setShowImage] = useState(false);
    const [imageURL, setImageURL] = useState(null);
    const [loadingOlder, setLoadingOlder] = useState(false);

    const conversationId = selectedChatData.id;

    useEffect(() => {
        let cancelled = false;
        const getMessages = async () => {
            try {
                const response = await apiClient.get(conversationMessagesRoute(conversationId), {
                    params: { limit: MESSAGE_PAGE_SIZE },
                });
                if (!cancelled) {
                    setSelectedChatMessages(response.data);
                    setHasMoreMessages(response.data.length === MESSAGE_PAGE_SIZE);
                }
            } catch (error) {
                console.log({ error })
            }
        };
        getMessages();
        return () => { cancelled = true; };
    }, [conversationId, setSelectedChatMessages, setHasMoreMessages])

    const loadOlder = async () => {
        if (loadingOlder || !hasMoreMessages || selectedChatMessages.length === 0) return;
        setLoadingOlder(true);
        try {
            const response = await apiClient.get(conversationMessagesRoute(conversationId), {
                params: { before: selectedChatMessages[0].id, limit: MESSAGE_PAGE_SIZE },
            });
            if (useAppStore.getState().selectedChatData?.id !== conversationId) return;
            const el = containerRef.current;
            prependAnchor.current = el.scrollHeight - el.scrollTop;
            prependMessages(response.data);
            setHasMoreMessages(response.data.length === MESSAGE_PAGE_SIZE);
        } catch (error) {
            console.log({ error });
        } finally {
            setLoadingOlder(false);
        }
    };

    useLayoutEffect(() => {
        if (prependAnchor.current !== null) {
            const el = containerRef.current;
            el.scrollTop = el.scrollHeight - prependAnchor.current;
            prependAnchor.current = null;
        } else if (scrollRef.current) {
            // Lần tải đầu nhảy thẳng xuống cuối, tránh smooth-scroll đi qua đỉnh làm kích hoạt loadOlder
            const firstLoad = prevCount.current === 0;
            scrollRef.current.scrollIntoView({ behavior: firstLoad ? "auto" : "smooth" })
        }
        prevCount.current = selectedChatMessages.length;
    }, [selectedChatMessages])

    const handleScroll = (e) => {
        if (e.currentTarget.scrollTop < 50) loadOlder();
    };

    const checkIfImage = (message) => message.contentType?.startsWith("image/");

    const isMine = (message) => message.sender.id === userInfo.id;

    const renderMessages = () => {
        let lastDate = null;
        return selectedChatMessages.map((message) => {
            const messageDate = moment(message.createdAt).format("YYYY-MM-DD");
            const showDate = messageDate !== lastDate;
            lastDate = messageDate;
            return (
                <div key={message.id}>
                    {showDate && (
                        <div className="text-center text-gray-500 my-2">
                            {moment(message.createdAt).format("LL")}
                        </div>
                    )}
                    {selectedChatType === "contact" && renderDMMessages(message)}
                    {selectedChatType === "channel" && renderChannelMessages(message)}
                </div>
            )
        })
    };

    const downloadFile = (url) => {
        // Server trả redirect tới MinIO với Content-Disposition: attachment → trình duyệt tự tải
        const link = document.createElement("a");
        link.href = fileDownloadUrl(url);
        document.body.appendChild(link);
        link.click();
        link.remove();
    }

    const bubbleClass = (message) => isMine(message)
        ? "bg-[#8417ff]/5 text-[#8417ff]/90 border-[#8417ff]/50"
        : "bg-[#2a2b33]/5 text-white/80 border-[#ffffff]/20";

    const renderFile = (message) => (
        checkIfImage(message) ? (
            <div
                className="cursor-pointer"
                onClick={() => {
                    setShowImage(true);
                    setImageURL(message.fileUrl)
                }}>
                <img src={fileUrl(message.fileUrl)} height={300} width={300} />
            </div>
        ) : (
            <div className="flex items-center justify-center gap-4">
                <span className="text-white/80 text-3xl bg-black/20 rounded-full p-3">
                    <MdFolderZip />
                </span>
                <span>{message.fileName}</span>
                <span
                    className="bg-black/20 p-3 text-2xl rounded-full hover:bg-black/50 cursor-pointer transition-all duration-300"
                    onClick={() => downloadFile(message.fileUrl)}
                >
                    <IoMdArrowRoundDown />
                </span>
            </div>
        )
    );

    const renderDMMessages = (message) =>
        <div className={`${isMine(message) ? "text-right" : "text-left"}`}>
            {message.type === "TEXT" && (
                <div className={`${bubbleClass(message)} border inline-block p-4 rounded my-1 max-w-[50%] break-words`}>
                    {message.content}
                </div>
            )}
            {
                message.type === "FILE" && (
                    <div className={`${bubbleClass(message)} border inline-block p-4 rounded my-1 max-w-[50%] break-words`}>
                        {renderFile(message)}
                    </div>
                )
            }
            <div className="text-xs text-gray-600">
                {moment(message.createdAt).format("LT")}
            </div>
        </div>

    const renderChannelMessages = (message) => {
        return (
            <div className={`mt-5 ${isMine(message) ? "text-right" : "text-left"}`}>
                {message.type === "TEXT" && (
                    <div className={`${bubbleClass(message)} border inline-block p-4 rounded my-1 max-w-[50%] break-words ml-9`}>
                        {message.content}
                    </div>
                )}
                {
                    message.type === "FILE" && (
                        <div className={`${bubbleClass(message)} border inline-block p-4 rounded my-1 max-w-[50%] break-words`}>
                            {renderFile(message)}
                        </div>
                    )
                }
                {!isMine(message) ? (
                    <div className="flex items-center justify-start gap-3">
                        <Avatar className="h-8 w-8 rounded-full overflow-hidden">
                            {
                                message.sender.avatarUrl && (
                                    <AvatarImage
                                        src={fileUrl(message.sender.avatarUrl)}
                                        alt="profile"
                                        className="object-cover w-full h-full bg-black"
                                    />
                                )}
                            <AvatarFallback className={`uppercase h-8 w-8 text-lg flex items-center justify-center rounded-full ${getColor(message.sender.color)}`} >
                                {initial(message.sender)}
                            </AvatarFallback>
                        </Avatar>
                        <span className="text-sm text-white/60">{displayName(message.sender)}</span>
                        <span className="text-xs text-white/60">
                            {moment(message.createdAt).format("LT")}
                        </span>
                    </div>
                ) : (
                    <div className="text-xs text-white/60 mt-1">
                        {moment(message.createdAt).format("LT")}
                    </div>
                )}
            </div>
        )
    }

    return (
        <div
            ref={containerRef}
            onScroll={handleScroll}
            className="flex-1 overflow-y-auto scrollbar-hidden p-4 px-8 md:w-[65vw] lg:w-[70vw] xl:w-[80vw] w-full"
        >
            {loadingOlder && <div className="text-center text-gray-500 text-sm my-2">Loading...</div>}
            {renderMessages()}
            <div ref={scrollRef} />
            {
                showImage && <div className="fixed z-[1000] top-0 left-0 h-[100vh] w-[100vw] flex items-center justify-center backdrop-blur-lg flex-col">
                    <div>
                        <img src={fileUrl(imageURL)} className="h-[80vh] w-full bg-cover" />
                    </div>
                    <div className="flex gap-5 fixed top-0 mt-5">
                        <button
                            className="bg-black/20 p-3 text-2xl rounded-full hover:bg-black/50 cursor-pointer transition-all duration-300"
                            onClick={() => { downloadFile(imageURL) }}
                        >
                            <IoMdArrowRoundDown />
                        </button>
                        <button
                            className="bg-black/20 p-3 text-2xl rounded-full hover:bg-black/50 cursor-pointer transition-all duration-300"
                            onClick={() => {
                                setShowImage(false);
                                setImageURL(null);
                            }}
                        >
                            <IoCloseSharp />
                        </button>
                    </div>
                </div>
            }
        </div>
    )
}

export default MessageContainer
