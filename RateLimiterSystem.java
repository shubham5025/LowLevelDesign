import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

enum PolicyType{
    FREE,
    PREMIUM
}
@Data
@AllArgsConstructor
class Policy{
    int capacity;
    double refillRatePerMs;
}
class PolicyProvider{
    static Map<PolicyType, Policy> policyMap=new HashMap<>();
    static Policy getPolicy(PolicyType policyType)
    {
        return policyMap.get(policyType);
    }

    //added for if in future need to add any policy
    static void setPolicy(PolicyType policyType, int capacity, double refillIntervalMs)
    {
        policyMap.put(policyType, new Policy(capacity,refillIntervalMs));
    }
}

interface RateLimiter{
    boolean consume();
}

@AllArgsConstructor
@Data
class TokenBasedRateLimiter implements RateLimiter{

    Policy policy;
    long lastRefillTimeMs;
    long currentToken;
    TokenBasedRateLimiter(Policy policy)
    {
        this.policy=policy;
        this.lastRefillTimeMs=System.currentTimeMillis();
        this.currentToken= policy.getCapacity();
        System.out.println("Assigned "+this.currentToken);
    }


    @Override
    public synchronized boolean consume() {

        refillToken();
        long token = this.getCurrentToken();
        if(token>0)
        {
            this.setCurrentToken(token-1);
            return true;
        }
        else
            return false;
    }

    public void refillToken(){
        long currentTimeStamp=System.currentTimeMillis();
        long timeElapse=currentTimeStamp-this.lastRefillTimeMs;
        this.lastRefillTimeMs=currentTimeStamp;
        System.out.printf("Time" + currentTimeStamp);
        if(timeElapse<0)
            return;

        long tokenAdd =(long) (timeElapse * this.policy.getRefillRatePerMs());

        if(tokenAdd>0)
        {
            this.setCurrentToken(Long.min(tokenAdd+getCurrentToken(),this.policy.getCapacity()));
            this.lastRefillTimeMs=currentTimeStamp;
        }
    }
}

@AllArgsConstructor
class LeakyBasedRateLimiter implements RateLimiter
{
    Policy policy;
    @Override
    public boolean consume() {
        return false;
    }
}


enum RateLimiterType{

}

interface RateLimiterFactory{
    RateLimiter createRateLimiterInstance(Policy policy);
}
class TokenBasedFactory implements RateLimiterFactory{

    @Override
    public RateLimiter createRateLimiterInstance(Policy policy) {
        return new TokenBasedRateLimiter(policy);
    }
}

class LeakyBasedFactory implements RateLimiterFactory {

    @Override
    public RateLimiter createRateLimiterInstance(Policy policy) {
        return new LeakyBasedRateLimiter(policy);
    }
}
class RateLimiterStore{
    Map<String, RateLimiter> rateLimiterMap= new ConcurrentHashMap<>();

    RateLimiter getBucket(String key, RateLimiterFactory rateLimiterFactory, Policy policy)
    {
        return rateLimiterMap.computeIfAbsent(key, k-> rateLimiterFactory.createRateLimiterInstance(policy));
    }

}
class RateLimiterManager{
    RateLimiterStore rateLimiterStore=new RateLimiterStore();
    RateLimiterFactory rateLimiterFactory=new TokenBasedFactory();

    boolean allow(String key, PolicyType policyType) throws InterruptedException {
        Policy policy=PolicyProvider.getPolicy(policyType);
        RateLimiter rateLimiter = rateLimiterStore.getBucket(key, this.rateLimiterFactory,policy);
        System.out.println(rateLimiter.toString());
        Thread.sleep(1);
        return rateLimiter.consume();
    }

}
public class RateLimiterSystem {

    public  static void main(String[] args) throws InterruptedException {
        RateLimiterManager rateLimiterManager=new RateLimiterManager();
        PolicyProvider.setPolicy(PolicyType.FREE,2,1);
        PolicyProvider.setPolicy(PolicyType.PREMIUM,3,0.1);

        for(int i=0;i<10;i++) {
            boolean allow = rateLimiterManager.allow("shubham", PolicyType.FREE);
            if (allow)
                System.out.println("Request Allowed");
            else System.out.println("Request Rejected");
        }

    for(int i=0;i<4;i++) {
        boolean allow = rateLimiterManager.allow("Jhalani", PolicyType.PREMIUM);
        if (allow)
            System.out.println("Request Allowed");
        else System.out.println("Request Rejected");
    } }

}
