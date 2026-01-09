import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.min;
import static java.lang.Math.abs;

@Data
@AllArgsConstructor
class SplitWiseUser{
    String id;
    String name;
}

@Getter
enum SplitType {
    EQUAL(new EqualSplitStrategy()),
    PERCENTAGE(new PercentageSplitStrategy());

    private final SplitStrategy strategy;

    SplitType(SplitStrategy strategy) {
        this.strategy = strategy;
    }

}

@Data
@AllArgsConstructor
class Split{
    String userId;
    Double amount;
}

@Data
class Expense{
    String expenseId;
    String groupId;
    String description;
    LocalDateTime timestamp;
    private Map<String,Double> payer;
    private List<Split> splits;
    SplitType splitType;

    Expense(String expenseId, String groupId, String description, Map<String, Double> payer, SplitType splitType)
    {
        this.description=description;
        this.expenseId=expenseId;
        this.groupId=groupId;
        this.payer=payer;
        this.timestamp=LocalDateTime.now();
        this.splitType=splitType;
        this.splits=new ArrayList<>();
    }
}
class BalanceSheet{
    Map<String, Map<String, Double>> balances;
    BalanceSheet()
    {
        this.balances=new HashMap<>();
    }
    void updateBalance(String fromUser, String toUser, Double amount)
    {
        balances.computeIfAbsent(fromUser, k-> new HashMap<>()).merge(toUser,amount,Double::sum);
    }

}
class Group{
    String groupId;
    Set<String> users;
    String name;
    BalanceSheet balanceSheet;
    List<Expense> expenses;

    Group(String groupId, String name)
    {
        this.groupId=groupId;
        this.name=name;
        this.users=new HashSet<>();
        this.expenses=new ArrayList<>();
        this.balanceSheet=new BalanceSheet();
    }
    void addUser(String userId)
    {
        users.add(userId);
    }
    void addExpense(Expense expense)
    {
        expenses.add(expense);
    }
}
interface SplitStrategy{
    List<Split> createSplit(Expense expense, Set<String> borrower, Map<String, Double> amountSplit);
}
class EqualSplitStrategy implements SplitStrategy
{
    SplitType splitType=SplitType.EQUAL;
    @Override
    public List<Split> createSplit(Expense expense, Set<String> participant, Map<String,Double> amountSplit) {
        Map<String, Double> payer=expense.getPayer();
        Double totalAmount= payer.values().stream().filter(Objects::nonNull).reduce(0.0,Double::sum);
        int totalUser=participant.size();
        Double amountPerUser=totalAmount/totalUser;
        System.out.println(amountPerUser+" "+totalUser);
        List<Split> splits=new ArrayList<>();
        for(String userId:participant) {
            double paid=payer.getOrDefault(userId,0.0);
            splits.add(new Split(userId, paid-amountPerUser));
        }
        return splits;
    }
}
class PercentageSplitStrategy implements SplitStrategy{

    SplitType splitType=SplitType.PERCENTAGE;

    @Override
    public List<Split> createSplit(Expense expense, Set<String> participants, Map<String,Double>amountSplit) {
        Map<String, Double> payer=expense.getPayer();
        Double totalAmount= payer.values().stream().filter(Objects::nonNull).reduce(0.0,Double::sum);

        List<Split> splits=new ArrayList<>();
        for(String userId:participants)
        {
            double paid=payer.getOrDefault(userId,0.0);
            BigDecimal total = BigDecimal.valueOf(totalAmount);
            BigDecimal percentage = BigDecimal.valueOf(amountSplit.get(userId));
            BigDecimal share = total
                    .multiply(percentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            splits.add(new Split(userId,paid-share.doubleValue()));
        }

        return splits;
    }
}


@Data
@AllArgsConstructor
class Settlement{
    String fromUser;
    String toUser;
    Double amount;

}

class ExpenseService{

    Expense createExpense(String id, String desc, String groupId, SplitType splitType,
                       Double amount, Map<String,Double>payer){
        double sum = payer.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Double.compare(sum, amount) != 0)
            throw new IllegalArgumentException("Payer sum != expense amount");
        return new Expense(id,groupId,desc,payer,splitType);
    }
}
class BalanceService{

    BalanceSheet updateBalance(Expense expense, Group group)
    {
        List<Split> splits=expense.getSplits();
        List<Split> credit=splits.stream().filter(split -> split.amount>0.0).toList();
        List<Split> debit= new ArrayList<>(splits.stream().filter(split -> split.amount < 0.0).toList());
        int i=0,j=0;
        String fromUser, toUser;
        Double amount;
        BalanceSheet balanceSheet = group.balanceSheet;
        while(i<credit.size()&&j<debit.size())
        {
            fromUser= debit.get(j).getUserId();
            toUser = credit.get(i).getUserId();
            amount = min(abs(debit.get(j).getAmount()),abs(credit.get(i).getAmount()));
            balanceSheet.updateBalance(fromUser,toUser,-amount);
            balanceSheet.updateBalance(toUser,fromUser,amount);
            debit.get(j).setAmount(debit.get(j).getAmount()+amount);
            credit.get(i).setAmount(credit.get(i).getAmount()-amount);
            if(!(debit.get(j).getAmount()<0.0)) j++;
            if(!(credit.get(i).getAmount()>0.0)) i++;
        }
        return balanceSheet;
    }
}

class SplitService{

    List<Split> createSplit(SplitType splitType, Expense expense, Set<String> participant, Map<String, Double> amountSplit)
    {
        SplitStrategy splitStrategy= splitType.getStrategy();
        if(splitStrategy==null)
            throw new IllegalStateException("Split Type does not exist");
        return splitStrategy.createSplit(expense,participant,amountSplit);
    }
}
class SettlementService{
    void settlement(Settlement settlement, BalanceSheet balanceSheet)
    {
        Map<String, Double> e1=balanceSheet.balances.get(settlement.fromUser);
        e1.replace(settlement.toUser,e1.get(settlement.toUser)+ settlement.amount);

        Map<String, Double> e2 =balanceSheet.balances.get(settlement.toUser);
        e2.replace(settlement.fromUser, e2.get(settlement.fromUser)-settlement.amount);

        balanceSheet.balances.replace(settlement.fromUser,e1);
        balanceSheet.balances.replace(settlement.toUser,e2);
    }
}
class SplitWiseHandler{
    ExpenseService expenseService;
    SettlementService settlementService;
    SplitService splitService;
    BalanceService balanceService;
    Map<String, Group> groupMap;
    Map<String,SplitWiseUser> userMap;
    SplitWiseHandler()
    {
        this.expenseService=new ExpenseService();
        this.settlementService=new SettlementService();
        this.splitService=new SplitService();
        this.balanceService=new BalanceService();
        groupMap=new HashMap<>();
        userMap=new HashMap<>();
    }
    void createExpense(String id, String desc, String groupId, SplitType splitType,
                       Double amount, Map<String,Double>payer, Set<String> borrower,
                       Map<String,Double>amountSplit)
    {
        Expense expense= this.expenseService.createExpense(id,desc,groupId,splitType,amount,payer);
        Group group=groupMap.get(groupId);
        group.addExpense(expense);

        List<Split> splits=this.splitService.createSplit(splitType, expense, group.users,amountSplit);
        expense.setSplits(splits);

        this.balanceService.updateBalance(expense,group);
    }

    void addUser(String userId, SplitWiseUser user)
    {
        this.userMap.put(userId,user);
    }

    void settlement(String from, String to, Double amount, Group group)
    {
        this.settlementService.settlement(new Settlement(from,to,amount),group.balanceSheet);
    }

    void showBalance(Group group)
    {
        BalanceSheet balanceSheet=group.balanceSheet;
        for(Map.Entry<String,Map<String,Double>> entry:balanceSheet.balances.entrySet())
        {
            Map<String, Double> entrysub=entry.getValue();
            for(Map.Entry<String,Double> entry1:entrysub.entrySet())
            {
                if(entry1.getValue().equals(0.0))
                    continue;
                if(entry1.getValue()>0.0)
                {
                    System.out.println(entry.getKey()+" Will Receive amount "+entry1.getValue() + " from "+entry1.getKey());
                }
                else
                {
                    System.out.println(entry.getKey()+" Will pay amount "+ abs(entry1.getValue()) + " to "+entry1.getKey());
                }
            }
        }
    }
}

public class SplitWise {
    public static void main(String[] a)
    {
        SplitWiseUser u1= new SplitWiseUser("1","Shubham");
        SplitWiseUser u2=new SplitWiseUser("2", "Jhalani");
        SplitWiseUser u3=new SplitWiseUser("3", "Kirty");
        SplitWiseUser u4=new SplitWiseUser("4","Astha");
        SplitWiseUser u5=new SplitWiseUser("5", "Vishnu");

        SplitWiseHandler splitWiseHandler=new SplitWiseHandler();

        splitWiseHandler.addUser("1",u1);
        splitWiseHandler.addUser("2",u2);
        splitWiseHandler.addUser("3",u3);
        splitWiseHandler.addUser("4",u4);
        splitWiseHandler.addUser("5",u5);

        Group group=new Group("1","DB");
        group.addUser("1");
        group.addUser("2");
        group.addUser("3");
        group.addUser("4");
        group.addUser("5");
        splitWiseHandler.groupMap.put("1",group);
        Map<String, Double> payer=new HashMap<>();
        payer.put("1",200.0);
        payer.put("2",100.0);
        splitWiseHandler.createExpense("1","Party","1",SplitType.EQUAL,300.0,payer,
                Set.of("1","2","3","4","5"),new HashMap<>());

        splitWiseHandler.showBalance(group);

//        splitWiseHandler.settlement("2","1",60.0,group);
//        splitWiseHandler.settlement("3","1",60.0,group);
//        splitWiseHandler.settlement("4","1",60.0,group);
//        splitWiseHandler.settlement("5","1",60.0,group);

        //splitWiseHandler.showBalance(group);



    }

}
