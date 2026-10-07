import { Avatar, AvatarImage } from "@/components/ui/avatar"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { apiClient } from "@/lib/api-client";
import { displayName, fileUrl, getColor, initial } from "@/lib/utils";
import { useAppStore } from "@/store"
import { LOGOUT_ROUTE } from "@/utils/constants";
import { FiEdit2 } from "react-icons/fi";
import { IoPowerSharp } from "react-icons/io5"
import { useNavigate } from "react-router-dom";

const ProfileInfo = () => {
    const { userInfo, setUserInfo, closeChat, setConversations } = useAppStore();
    const navigate = useNavigate();

    const logOut = async () => {
        try {
            await apiClient.post(LOGOUT_ROUTE);
            closeChat();
            setConversations([]);
            setUserInfo(undefined);
            navigate("/auth");

        } catch (error) {
            console.log(error);
        }
    }

    return (
        <div className="absolute bottom-0 h-16 flex items-center justify-between px-10 w-full bg-[#2a2b33]">
            <div className="flex gap-3 items-center justify-center">
                <div className="w-12 h-12 relative">
                    <Avatar className="h-12 w-12 rounded-full overflow-hidden">
                        {
                            userInfo.avatarUrl ? (
                                <AvatarImage
                                    src={fileUrl(userInfo.avatarUrl)}
                                    alt="profile"
                                    className="object-cover w-full h-full bg-black"
                                />
                            ) : (
                                <div className={`uppercase h-12 w-12 text-lg border-[1px] flex items-center justify-center rounded-full ${getColor(userInfo.color)}`} >
                                    {initial(userInfo)}
                                </div>
                            )}
                    </Avatar>
                </div>
                <div>
                    {displayName(userInfo)}
                </div>
            </div>
            <div className="flex gap-5">
                <TooltipProvider>
                    <Tooltip>
                        <TooltipTrigger>
                            <FiEdit2 className="text-purple-500 text-xl font-medium"
                                onClick={() => navigate("/profile")} />
                        </TooltipTrigger>
                        <TooltipContent className="bg-[#1c1b1e] border-none text-white">
                            Edit Profile
                        </TooltipContent>
                    </Tooltip>
                </TooltipProvider>
                <TooltipProvider>
                    <Tooltip>
                        <TooltipTrigger>
                            <IoPowerSharp className="text-red-500 text-xl font-medium"
                                onClick={logOut} />
                        </TooltipTrigger>
                        <TooltipContent className="bg-[#1c1b1e] border-none text-white">
                            Logout
                        </TooltipContent>
                    </Tooltip>
                </TooltipProvider>
            </div>
        </div>
    )
}

export default ProfileInfo