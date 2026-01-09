import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;

@Data
@AllArgsConstructor
class ChatUser{
    String userId;
    String name;
}
enum MessageState{
    SENT,
    DELIVERED,
    READ
}
@Data
class ChatMessage{
    private static int COUNTER=0;
    String msgId;
    String content;
    Instant timestamp;
    String senderId;
    String receiverId;
    MessageState messageState;
    ChatMessage(String content,String senderId, String receiverId)
    {
        COUNTER++;
        this.msgId= String.valueOf(COUNTER);
        this.content=content;
        this.senderId=senderId;
        this.receiverId=receiverId;
        this.timestamp=Instant.now();
        this.messageState=MessageState.SENT;
    }
}
class ChatUserService{
    Map<String, ChatUser> allUsers;

    ChatUserService()
    {
        allUsers=new HashMap<>();
    }

    void addUser(ChatUser user)
    {
        allUsers.putIfAbsent(user.getUserId(), user);
    }

    ChatUser getUser(String userId)
    {
       ChatUser user= allUsers.get(userId);
       if(user==null)
           throw new IllegalStateException("No User Exist with User id "+userId);
       return  user;
    }
}

class PresenceService{
    Map<String, Boolean> userPresence;
    MessageService messageService;
    PresenceService()
    {
        userPresence=new HashMap<>();
    }

    boolean isOnline(String userId)
    {
        return userPresence.getOrDefault(userId, false);
    }

    void setUserOnline(String userId)
    {
        userPresence.put(userId,true);
    }
    void setUserOffline(String userId)
    {
        userPresence.put(userId,false);
    }

    void notifyUserOnline(String userId)
    {
        setUserOnline(userId);
    }

}

class ChatNotificationService{

    void notifyUser(ChatMessage message)
    {
        System.out.println("Message Sent from "+message.getSenderId()+" to "+message.getReceiverId()+
                " with content "+message.getContent());
    }
}
class MessageService{
    Map<String, List<ChatMessage>> allConversation;
    Map<String, List<ChatMessage>> pendingMessages;
    Map<String, ChatMessage> allMsgs;
    PresenceService presenceService;
    ChatUserService chatUserService;
    ChatNotificationService chatNotificationService;
    MessageService(PresenceService presenceService, ChatUserService chatUserService, ChatNotificationService chatNotificationService)
    {
        this.presenceService=presenceService;
        this.chatUserService=chatUserService;
        this.chatNotificationService=chatNotificationService;
        this.allMsgs=new HashMap<>();
        this.allConversation=new HashMap<>();
        this.pendingMessages=new HashMap<>();
    }
    ChatMessage createMessage(String senderId, String receiverId, String content)
    {
        ChatMessage chatMessage= new ChatMessage(content, senderId, receiverId);
        allMsgs.put(chatMessage.getMsgId(), chatMessage);
        return chatMessage;
    }
    String getConvoId(String senderId, String receiverId)
    {
        String minStr=senderId.compareTo(receiverId)<=0?senderId:receiverId;
        String maxStr=senderId.compareTo(receiverId)>0?senderId:receiverId;
        return minStr+"_"+maxStr;
    }

    void sendMessage(ChatMessage message)
    {
        String senderId= message.getSenderId();;
        String receiverId= message.getReceiverId();

        String convoId=getConvoId(senderId,receiverId);

        if(presenceService.isOnline(receiverId))
        {
            List<ChatMessage> chatMessages=allConversation.getOrDefault(convoId, new ArrayList<>());
            chatMessages.add(message);
            allConversation.put(convoId,chatMessages);
            message.setMessageState(MessageState.DELIVERED);
            chatNotificationService.notifyUser(message);
        }
        else{
            List<ChatMessage> chatMessages=pendingMessages.getOrDefault(receiverId, new ArrayList<>());
            chatMessages.add(message);
            pendingMessages.put(receiverId,chatMessages);
            System.out.println("USer is Offline, Saving Message to send later");
        }
    }

    void deliverPendingMessage(String userId)
    {
        if(!pendingMessages.containsKey(userId))
        {
            System.out.println("No Pending Message for User "+userId);
            return;
        }
        List<ChatMessage> chatMessages=pendingMessages.get(userId);
        pendingMessages.clear();
        if(chatMessages.isEmpty())
        {
            System.out.println("No Pending Message");
            return;
        }
        for(ChatMessage message:chatMessages)
        {
            sendMessage(message);
        }
    }

    void ack(String msgId)
    {
        ChatMessage message=allMsgs.get(msgId);
        if(message==null)
            throw new IllegalStateException("No Message Exist with messageID "+msgId);
        if(message.getMessageState()==MessageState.DELIVERED)
            message.setMessageState(MessageState.READ);
        else
            //Handle as per requirement
            throw new IllegalStateException("Message is not Delivered Yet");
    }

    void printAllMsg(String senderId, String receiverID)
    {
        String convoId=getConvoId(senderId,receiverID);
        System.out.println(allConversation.getOrDefault(convoId, new ArrayList<>()).toString());
    }
}
class ChatSystemService{
    ChatUserService chatUserService;
    ChatNotificationService chatNotificationService;
    MessageService messageService;
    PresenceService presenceService;

    private ChatSystemService()
    {
        this.chatUserService=new ChatUserService();
        this.chatNotificationService=new ChatNotificationService();
        this.presenceService=new PresenceService();
        this.messageService=new MessageService(this.presenceService,chatUserService,chatNotificationService);
    }

    private static class ChatSystemServiceHelper{
        public static ChatSystemService chatSystemService=new ChatSystemService();
    }

    public static ChatSystemService getInstance()
    {
        return ChatSystemServiceHelper.chatSystemService;
    }

    void sendMessage(String senderId, String receiverId, String content)
    {
        ChatMessage message=messageService.createMessage(senderId, receiverId, content);
        messageService.sendMessage(message);
    }
    void createUser(String id,String name)
    {
        chatUserService.addUser(new ChatUser(id, name));
    }

    void onOnline(String userID)
    {
        presenceService.notifyUserOnline(userID);
        messageService.deliverPendingMessage(userID);
    }

    void setUserOffline(String userId)
    {
        presenceService.setUserOffline(userId);
    }

    void setUserOnline(String userId)
    {
        presenceService.setUserOnline(userId);
    }

    void printConvo(String senderId, String receiverId)
    {
        messageService.printAllMsg(senderId, receiverId);
    }
    void ack(String msgId)
    {
        messageService.ack(msgId);
    }
}
public class ChatSystem {
    public static void main(String[] args)
    {
        ChatSystemService chatSystemService=ChatSystemService.getInstance();
        chatSystemService.createUser("1","Shubham");
        chatSystemService.createUser("2", "Astha");
        chatSystemService.createUser("3", "Kirty");
        chatSystemService.createUser("4", "Vishnu");

        chatSystemService.setUserOffline("4");
        chatSystemService.setUserOnline("2");
        chatSystemService.setUserOnline("3");

        chatSystemService.sendMessage("1","2", "Hi, How Are You");
        chatSystemService.sendMessage("1","3", "Hi, How Are You");
        chatSystemService.sendMessage("1","4", "Hi, How Are You");

        chatSystemService.sendMessage("2","1", "I am Fine, Wbu");
        chatSystemService.sendMessage("3","1", "Badiya, Tu bata");

        chatSystemService.onOnline("1");
        chatSystemService.printConvo("1","2");
        chatSystemService.ack("1");
        chatSystemService.printConvo("2","1");
        chatSystemService.printConvo("1","3");

        chatSystemService.printConvo("1","4");

        chatSystemService.onOnline("4");
        chatSystemService.printConvo("1","4");



    }
}
