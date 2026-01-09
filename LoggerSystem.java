import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

enum LogLevel{
    DEBUG,
    INFO,
    ERROR,
    FATAL;
    boolean shouldLog(LogLevel currentLevel, LogLevel newLevel)
    {
        return newLevel.ordinal()>=currentLevel.ordinal();
    }
}
class LogMessage{
    String msg;
    LocalDateTime timeStamp;
    LogLevel logLevel;
    LogMessage(String msg, LogLevel logLevel)
    {
        this.msg=msg;
        this.logLevel=logLevel;
        this.timeStamp=LocalDateTime.now();
    }
}
interface LogFormatter{
    String format(LogMessage logMessage);
}
class SimpleFormatter implements LogFormatter{

    @Override
    public String format(LogMessage logMessage) {
      return logMessage.timeStamp +" [" + logMessage.logLevel + " ] "+logMessage.msg;
    }
}
interface Appender{
    void append(LogMessage logMessage);
}

enum BackPressurePolicy{
    BLOCK,
    DROP,
    FAIL
}

@Getter
class RetryPolicy{
    private final int maxRetries;
    private final long delayMilliSeconds;
    RetryPolicy(int maxRetries, long delayMilliSeconds)
    {
        this.maxRetries=maxRetries;
        this.delayMilliSeconds=delayMilliSeconds;
    }
}
interface AsyncTask{
    void execute() throws Exception;

}

@Getter
@Setter
class AsyncExecutor{
    private final BlockingQueue<AsyncTask> queue;
    private BackPressurePolicy backPressurePolicy;
    private RetryPolicy retryPolicy;
    private ExecutorService workers;
    private volatile boolean running=true;

    public AsyncExecutor(int queueSize, int workerThread, BackPressurePolicy backPressurePolicy, RetryPolicy retryPolicy)
    {
        this.backPressurePolicy=backPressurePolicy;
        this.retryPolicy=retryPolicy;
        this.queue=new LinkedBlockingQueue<>(queueSize);
        this.workers= Executors.newFixedThreadPool(workerThread);

        for(int i=0;i<workerThread;i++)
            this.workers.submit(this::workerLoop);
    }

    public void workerLoop()
    {
        while(running || !queue.isEmpty())
        {
            try{
                AsyncTask task=queue.poll(200, TimeUnit.MILLISECONDS);
                if(task == null) continue;

                executeWithRetry(task);
            } catch (InterruptedException ignored) {

            }
        }
    }

    public void executeWithRetry(AsyncTask task)
    {
        int attempt=0;
        while(true)
        {
            try{
                task.execute();
                return;
            } catch (Exception e) {
                System.out.println("Attempt Failed, Retrying");
                attempt++;
                if(attempt>this.retryPolicy.getMaxRetries())
                {
                    System.out.println("Maximum Retries happened");
                    return;
                }
                try{
                    Thread.sleep(retryPolicy.getDelayMilliSeconds());
                } catch (InterruptedException ignored) {

                }
            }
        }
    }

    public void submit(AsyncTask task)
    {
        switch (backPressurePolicy)
        {
            case BLOCK:
                try {
                    this.queue.put(task);
                } catch (InterruptedException ignored) {}
                break;
            case DROP:
                if(!queue.offer(task))
                    System.out.println("Request is dropped");
                break;
            case FAIL:
                if(!queue.offer(task))
                    throw new IllegalStateException("Queue Size exceeds");
                break;
        }
    }

    public void shutDown()
    {
        running=false;
        workers.shutdown();
    }
}
class LogTask implements AsyncTask{

    LogMessage logMessage;
    Appender appender;
    volatile static int i=0;
    LogTask(Appender appender, LogMessage logMessage)
    {
        this.appender=appender;
        this.logMessage=logMessage;
    }
    @Override
    public void execute() throws InterruptedException {
        i=i+1;
        if(i>2) {
            System.out.println("Starting Appender required");
            appender.append(logMessage);
        }else throw new InterruptedException();
    }
}
class AsyncAppender implements Appender{

    Appender appender;
    AsyncExecutor asyncExecutor;
    AsyncAppender(Appender appender, AsyncExecutor asyncExecutor)
    {
        this.asyncExecutor=asyncExecutor;
        this.appender=appender;
    }
    @Override
    public void append(LogMessage logMessage) {
        System.out.println("Calling Async Appender");
        asyncExecutor.submit(new LogTask(appender,logMessage));
        //asyncExecutor.submit(()->appender.append(logMessage));
    }
}
class ConsoleAppender implements Appender{

    LogFormatter logFormatter;
    ConsoleAppender(LogFormatter logFormatter)
    {
        this.logFormatter=logFormatter;
    }

    @Override
    public void append(LogMessage logMessage) {
        System.out.println(logFormatter.format(logMessage));
    }
}
class FileAppender implements Appender{

    String filePath;
    private final LogFormatter logFormatter;

    FileAppender(String filePath, LogFormatter logFormatter)
    {
        this.filePath=filePath;
        this.logFormatter=logFormatter;
    }

    @Override
    public void append(LogMessage logMessage) {
        try(FileWriter fw = new FileWriter(filePath, true);
            BufferedWriter bw = new BufferedWriter(fw)){
            bw.write(logFormatter.format(logMessage));
            bw.newLine();
            System.out.println("Appending to file "+ LocalDateTime.now());
            Thread.sleep(5000);
        }
        catch(IOException exception)
        {
            exception.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
class Logger {

    private Logger(){};
    List<Appender> appenders=new ArrayList<>();
    private static final class LoggerHolder {
        static final Logger logger = new Logger();
    }

    static Logger getInstance()
    {
        return LoggerHolder.logger;
    }
    void log(LogLevel logLevel, String msg)
    {
        LogMessage logMessage=new LogMessage(msg, logLevel);
        appenders.forEach(appender -> appender.append(logMessage));
    }

    void info(String msg) {log(LogLevel.INFO,msg);}
    void debug(String msg) {log(LogLevel.DEBUG,msg);}
    void error(String msg) {log(LogLevel.ERROR,msg);}
}
public class LoggerSystem {
    public static void main(String[] args){
        Logger logger=Logger.getInstance();

        AsyncExecutor asyncExecutor = new AsyncExecutor(1,2,BackPressurePolicy.DROP ,
                new RetryPolicy(2,200));



        logger.appenders.add(new AsyncAppender(new ConsoleAppender(new SimpleFormatter()), asyncExecutor));
        logger.appenders.add(new AsyncAppender(new FileAppender("system.log",new SimpleFormatter()), asyncExecutor));

        logger.info("Info message");
//        logger.debug("Debug Message");
//        logger.error("Error msg");
//        logger.info("Info message1");
//        logger.debug("Debug Message1");
//        logger.error("Error msg1");

        asyncExecutor.shutDown();
    }
}
