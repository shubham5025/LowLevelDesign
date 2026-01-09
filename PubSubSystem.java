import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.nio.channels.IllegalBlockingModeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Data
class Message {
    String msgID;
    String payload;
    Instant timeStamp;
    Message(String msgID, String payload) {
        this.msgID = msgID;
        this.payload = payload;
        this.timeStamp = Instant.now();
    }
}

class Publisher {
    String id;
    PubSubService pubSubService;

    void publish(String topicName, Message message) {
        pubSubService.publish(topicName, message);
    }
}

interface Subscriber {
    String getId();

    void onMessage(Message message);
}

@Getter
@Setter
@AllArgsConstructor
class PrintSubscriber implements Subscriber {
    String id;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void onMessage(Message message) {
        System.out.println("Message received " + message.getPayload());
    }
}

class SubscriberWorker implements Runnable {
    Subscriber subscriber;
    BlockingQueue<Message> messagesQueue;
    int retry = 0;
    volatile boolean running = true;
    static final Message POISON = new Message("0", "__POISON__");
    SubscriberWorker(Subscriber subscriber) {
        this.subscriber = subscriber;
        this.messagesQueue = new LinkedBlockingQueue<>(50);
    }

    void enqueue(Message message) {
        if (!this.messagesQueue.offer(message)) {
            throw new RejectedExecutionException();
        }
    }


    @Override
    public void run() {
        while (running) {
            try {
                Message message = messagesQueue.take();
                if(message==POISON)
                    break;
                subscriber.onMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
                //Retry Policy and DLQ

            }
        }
    }

    void shutdown() {
        running = false;
    }
}

@Data
class Topic {
    String name;
    Map<String, SubscriberWorker> pushSubscriber;
    Map<String, Integer> pullSubscriber;
    List<Message> messages;

    Topic(String name) {
        this.name = name;
        this.pullSubscriber = new ConcurrentHashMap<>();
        this.pushSubscriber = new ConcurrentHashMap<>();
        this.messages = new CopyOnWriteArrayList<>();
    }

    void addPushSubscriber(SubscriberWorker subscriberWorker) {
        this.pushSubscriber.putIfAbsent(subscriberWorker.subscriber.getId(), subscriberWorker);
    }

    void addPullSubscriber(Subscriber subscriber) {
        this.pullSubscriber.putIfAbsent(subscriber.getId(), 0);
    }
}

enum DeliveryMode {
    PUSH,
    PULL
}

class PubSubService {

    Map<String, Topic> topics;

    ExecutorService executors;
    volatile static boolean isPublishingEnabled=true;


    PubSubService() {
        this.topics = new ConcurrentHashMap<>();
        this.executors = Executors.newCachedThreadPool();
    }

    void createTopic(String name) {
        if (topics.get(name) != null)
            System.out.println("Topic Already Exist with same Name");
        Topic topic = new Topic(name);
        topics.putIfAbsent(topic.name, topic);
    }

    void addSubscriber(Subscriber subscriber, String topicName, DeliveryMode deliveryMode) {
        Topic topic = topics.get(topicName);
        if (DeliveryMode.PUSH == deliveryMode) {
            SubscriberWorker subscriberWorker = new SubscriberWorker(subscriber);
            topic.addPushSubscriber(subscriberWorker);
            executors.submit(subscriberWorker);
        } else {
            topic.addPullSubscriber(subscriber);
        }
    }

    void publish(String topicName, Message message) {
        if(!isPublishingEnabled)
            throw new IllegalBlockingModeException();
        Topic topic = topics.get(topicName);
        synchronized (topic) {
            topic.messages.add(message);
            for (SubscriberWorker worker : topic.getPushSubscriber().values()) {
                try {
                    worker.enqueue(message);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    List<Message> poll(String topicName, Subscriber subscriber) {
        Topic topic = topics.get(topicName);
        if(topic == null)
            throw new IllegalArgumentException("Topic Not Found");
        Integer offset = topic.getPullSubscriber().get(subscriber.getId());

        List<Message> result = new ArrayList<>();
        synchronized (topic) {
            while (offset < topic.messages.size()) {
                result.add(topic.messages.get(offset));
                offset++;
            }
            topic.getPullSubscriber().put(subscriber.getId(), offset);
        }
        return result;
    }

    void shutDown() throws InterruptedException {

        isPublishingEnabled=false;
        for(Topic topic: topics.values())
        {
            for(SubscriberWorker subscriberWorker:topic.getPushSubscriber().values())
                subscriberWorker.enqueue(SubscriberWorker.POISON);
        }
        executors.shutdown();
        executors.awaitTermination(30, TimeUnit.SECONDS);
    }

}

public class PubSubSystem {
    public static void main(String[] args) throws InterruptedException {
        PubSubService service = new PubSubService();
        service.createTopic("orders");

        Subscriber pushSub = new PrintSubscriber("PUSH-A");
        Subscriber pullSub = new PrintSubscriber("PULL-B");

        service.addSubscriber(pushSub, "orders", DeliveryMode.PUSH);
        service.addSubscriber(pullSub, "orders", DeliveryMode.PULL);

        service.publish("orders", new Message("1", "order-1"));
        service.publish("orders", new Message("2", "order-2"));

        // PULL subscriber explicitly polls
        for (Message m : service.poll("orders", pullSub)) {
            pullSub.onMessage(m);
        }
        service.shutDown();
    }
}
