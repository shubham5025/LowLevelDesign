import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

enum ChannelType{
    SMS,
    EMAIL,
    POPUP
}
interface Notification{
    String getNotificationMsg();
}
class SimpleNotification implements Notification{

    String msg;
    int id;

    SimpleNotification(String msg, int id)
    {
        this.msg=msg;
        this.id=id;
    }

    @Override
    public String getNotificationMsg() {
        return this.msg;
    }
}
interface Channel{
    void sendNotification(Notification notification);
}
@Data
class SMSChannel implements Channel{

    ChannelType channelType;

    @Override
    public void sendNotification(Notification notification) {
        System.out.println("Sending Sms Notification "+notification.getNotificationMsg());
    }
}

@Data
class EmailNotification implements Channel{

    ChannelType channelType;
    @Override
    public void sendNotification(Notification notification) {
        System.out.println("Sending Email Notification "+notification.getNotificationMsg());
        try{
            Thread.sleep(5000);
            System.out.println("Email Sent ");
        } catch (InterruptedException ignored) {
        }
    }
}

class NotificationTask  {

}

@Data
@AllArgsConstructor
class AsyncNotification implements Channel{
    public Channel channel;
    AsyncExecutor asyncExecutor;

    @Override
    public void sendNotification(Notification notification) {
        System.out.println("Calling Notification Async");
        asyncExecutor.submit(()->channel.sendNotification(notification));
    }
}

@Getter
@Setter
class NotificationUser{
    String id;
    String name;
    Set<ChannelType> channels;

    NotificationUser(String id, String name)
    {
        this.id=id;
        this.name=name;
        this.channels=new HashSet<>();
    }

    void addChannel(ChannelType channelType)
    {
        this.channels.add(channelType);
    }

}

class NotificationUserManager{
    Map<String, Set<ChannelType>> allUsersChannels=new HashMap<>();
    Set<ChannelType> getUserAllChannels(String user)
    {
        return allUsersChannels.get(user);
    }
}

//Make it singleton
class NotificationService{
    NotificationUserManager notificationUserManager=new NotificationUserManager();
    Map<ChannelType, Channel> channels=new HashMap<>();

    void send(Notification notification, List<String> users)
    {
        for(String id:users)
        {
            Set<ChannelType> channelTypes= notificationUserManager.getUserAllChannels(id);
            for(ChannelType channelType:channelTypes)
            {
                channels.get(channelType).sendNotification(notification);
            }
            System.out.println("All Notification for User "+ id +" Completed");
        }
    }
}

public class NotificationSystem {
    public static void main(String[] args){
        NotificationService notificationService=new NotificationService();

        AsyncExecutor asyncExecutor=new AsyncExecutor(2,1,BackPressurePolicy.DROP,
                new RetryPolicy(2,500));

        notificationService.channels.put(ChannelType.EMAIL, new AsyncNotification(new EmailNotification(),asyncExecutor));
        notificationService.channels.put(ChannelType.SMS, new AsyncNotification(new SMSChannel(),asyncExecutor));
        NotificationUser notificationUser=new NotificationUser("1","Shubham");
        notificationUser.addChannel(ChannelType.EMAIL);

        NotificationUser notificationUser1=new NotificationUser("2","Jhalani");
        notificationUser1.addChannel(ChannelType.EMAIL);
        notificationUser1.addChannel(ChannelType.SMS);

        notificationService.notificationUserManager.allUsersChannels.put("1", notificationUser.getChannels());
        notificationService.notificationUserManager.allUsersChannels.put("2", notificationUser1.getChannels());



        notificationService.send(new SimpleNotification("Testing First Notification",1),List.of(notificationUser.id));
        notificationService.send(new SimpleNotification("Testing Multiple Notification",2), List.of(notificationUser.id,notificationUser1.id));
        notificationService.send(new SimpleNotification("Testing Multiple Notification",2), List.of(notificationUser.id,notificationUser1.id));
        notificationService.send(new SimpleNotification("Testing Multiple Notification",2), List.of(notificationUser.id,notificationUser1.id));

        asyncExecutor.shutDown();
    }

}
