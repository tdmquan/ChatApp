import { useAppStore } from "@/store"
import { Avatar, AvatarImage } from "./ui/avatar";
import { displayName, fileUrl, getColor, getPeer, initial } from "@/lib/utils";

/** contacts: danh sách ConversationDto (DIRECT hoặc GROUP). */
const ContactList = ({ contacts, isChannel = false }) => {
    const { selectedChatData, selectConversation, userInfo } = useAppStore();

    return (
        < div className="mt-5">
            {contacts.map((conversation) => {
                const isSelected = selectedChatData && selectedChatData.id === conversation.id;
                const peer = isChannel ? undefined : getPeer(conversation, userInfo.id);
                return (
                    <div
                        key={conversation.id}
                        className={`pl-10 py-2 transition-all duration-300 cursor-pointer ${isSelected ? "bg-[#8417ff] hover:bg-[#8417ff]" : "hover:bg-[#f1f1f111]"}`}
                        onClick={() => selectConversation(conversation)}
                    >
                        <div className="flex gap-5 items-center justify-start text-neutral-300">
                            {
                                !isChannel && (
                                    <Avatar className="h-10 w-10 rounded-full overflow-hidden">
                                        {
                                            peer?.avatarUrl ? (
                                                <AvatarImage
                                                    src={fileUrl(peer.avatarUrl)}
                                                    alt="profile"
                                                    className="object-cover w-full h-full bg-black"
                                                />
                                            ) : (
                                                <div className={`
                                                ${isSelected ? "bg-[#ffffff22] border border-white/70" : getColor(peer?.color)
                                                    }
                                            uppercase h-10 w-10 text-lg border-[1px] flex items-center justify-center rounded-full`} >
                                                    {initial(peer)}
                                                </div>
                                            )}
                                    </Avatar>
                                )}
                            {isChannel && <div className="bg-[#ffffff22] h-10 w-10 flex items-center justify-center rounded-full">#</div>}
                            {
                                isChannel ? (<span>{conversation.name}</span>) : (<span>{displayName(peer)}</span>)
                            }
                        </div>
                    </div>
                )
            })}
        </div>
    )
}

export default ContactList
