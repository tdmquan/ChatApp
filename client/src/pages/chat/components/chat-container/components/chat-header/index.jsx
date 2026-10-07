import { Avatar, AvatarImage } from "@/components/ui/avatar";
import { displayName, fileUrl, getColor, getPeer, initial } from "@/lib/utils";
import { useAppStore } from "@/store"
import { RiCloseFill } from "react-icons/ri"
const ChatHeader = () => {
    const { closeChat, selectedChatData, selectedChatType, userInfo } = useAppStore();
    const peer = selectedChatType === "contact" ? getPeer(selectedChatData, userInfo.id) : undefined;
    return (
        <div className="h-[10vh] border-b-2 border-[#2f303b] flex items-center justify-between px-20">
            <div className="flex gap-5 items-center w-full justify-between">
                <div className="flex gap-3 items-center justify-center">
                    <div className="w-12 h-12 relative">
                        {
                            selectedChatType === "contact" ? <Avatar className="h-12 w-12 rounded-full overflow-hidden">
                                {
                                    peer?.avatarUrl ? (
                                        <AvatarImage
                                            src={fileUrl(peer.avatarUrl)}
                                            alt="profile"
                                            className="object-cover w-full h-full bg-black"
                                        />
                                    ) : (
                                        <div className={`uppercase h-12 w-12 text-lg border-[1px] flex items-center justify-center rounded-full ${getColor(peer?.color)}`} >
                                            {initial(peer)}
                                        </div>
                                    )}
                            </Avatar> : (
                                <div className="bg-[#ffffff22] h-10 w-10 flex items-center justify-center rounded-full">#</div>
                            )
                        }

                    </div>
                    <div>
                        {selectedChatType === "channel" ? selectedChatData.name : displayName(peer)}
                    </div>
                </div>
                <div className="flex gap-5 items-center justify-center">
                    <button
                        className="text-neutral-500 focus:border-none focus:outline-none focus:text-white duration-300 transition-all"
                        onClick={closeChat}
                    >
                        <RiCloseFill className="text-3xl" />
                    </button>
                </div>

            </div>
        </div>
    )
}

export default ChatHeader