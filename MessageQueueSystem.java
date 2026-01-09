import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Data
class Msg{
    String msgId;
    String payload;
    int deliveryAttempt;
    Msg(String id, String payload)
    {
        this.msgId=id;
        this.payload=payload;
        this.deliveryAttempt=0;
    }
}

@Data
class InFlightMessage{
    Msg message;
    Instant visibilityDeadline;
    InFlightMessage(Msg message, Instant visibilityDeadline)
    {
        this.message= message;
        this.visibilityDeadline =visibilityDeadline;
    }
}
class DeadLetterQueue{
    List<Msg> messages;
    DeadLetterQueue()
    {
        messages=new ArrayList<>();

    }

    synchronized void addMessage(Msg message)
    {
        this.messages.add(message);
        System.out.println("Moved to DLQ Message with "+message.getMsgId());
    }

}
class MessageQueue{
    Deque<Msg> messageQueue;
    Map<String, InFlightMessage> inFlightMessages;
    // Can use priority queue or Delay Queue
    // Can use Hierarchical Timing Wheel to reduce the time complexity of removing message from MAP.
    int maxRetries;
    long visibilityTimeoutInMs;
    DeadLetterQueue dlq;

    MessageQueue(int maxRetries, long visibilityTimeoutInMs)
    {
        this.messageQueue=new ArrayDeque<>();
        this.inFlightMessages=new HashMap<>();
        this.maxRetries=maxRetries;
        this.visibilityTimeoutInMs =visibilityTimeoutInMs;
        this.dlq=new DeadLetterQueue();
    }

    synchronized void enqueue(Msg message)
    {
        messageQueue.addLast(message);
    }

    synchronized List<Msg> poll(int batchSize)
    {
        List<Msg> results=new ArrayList<>();
        while(!messageQueue.isEmpty()&& batchSize>0)
        {
            Msg message=messageQueue.removeFirst();
            message.setDeliveryAttempt(message.getDeliveryAttempt()+1);
            if(message.getDeliveryAttempt()>maxRetries)
            {
                System.out.println("Maximum retry completed for the Message "+message.getMsgId());
                dlq.addMessage(message);
                continue;
            }
            Instant deadline=Instant.now().plusMillis(visibilityTimeoutInMs);
            inFlightMessages.put(message.getMsgId(),new InFlightMessage(message,deadline));
            results.add(message);
            batchSize--;
        }
        System.out.println(results);
        return results;
    }

    synchronized void ack(String msgId)
    {
        if(!inFlightMessages.containsKey(msgId))
            throw new IllegalStateException("Invalid Ack for message id "+msgId);
        inFlightMessages.remove(msgId);
    }

    synchronized void requeueExpiredFlightMessage(){

        Iterator<Map.Entry<String, InFlightMessage>> iterator=inFlightMessages.entrySet().iterator();
        while(iterator.hasNext())
        {
            InFlightMessage inFlightMessage=iterator.next().getValue();
            if(inFlightMessage.getVisibilityDeadline().isBefore(Instant.now()))
            {
                messageQueue.addLast(inFlightMessage.getMessage());
                iterator.remove();
            }
        }
    }

}

class MessageQueueService{
    MessageQueue messageQueue;
    ScheduledExecutorService scheduledExecutorService;
    MessageQueueService(int maxRetries, long visibilityTimeoutMillis)
    {
        this.messageQueue=new MessageQueue(maxRetries,visibilityTimeoutMillis);
        this.scheduledExecutorService= Executors.newSingleThreadScheduledExecutor();
        this.scheduledExecutorService.scheduleAtFixedRate(messageQueue::requeueExpiredFlightMessage,
                1,1, TimeUnit.SECONDS);
    }

    void addMessage(Msg message)
    {
        messageQueue.enqueue(message);
    }

    List<Msg> poll(int batchSize)
    {
       return messageQueue.poll(batchSize);
    }

    void ack(String msgId)
    {
        messageQueue.ack(msgId);
    }
    void shutDown()
    {
        scheduledExecutorService.shutdown();
    }

}
class Producer{
    String id;

}
public class MessageQueueSystem {
    public static void main(String[] args) throws InterruptedException {
        MessageQueueService messageQueueService=new MessageQueueService(5,1000);
        messageQueueService.addMessage(new Msg("1","First Message"));
        messageQueueService.addMessage(new Msg("2", "Second Message"));

        messageQueueService.poll(2);

        messageQueueService.ack("1");
        Thread.sleep(2000);

        messageQueueService.poll(2);
        messageQueueService.shutDown();
    }

}
