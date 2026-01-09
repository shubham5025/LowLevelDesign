import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
class BankAccount{
    String accountNumber;
    double balance;
    String pinHash;
}

@Data
class AccountService{
    private BankAccount account;
    int pinCount=0;

    void startSession(ATM atm, BankAccount account)
    {
        this.setAccount(account);
        this .pinCount=0;
    }
    boolean validatePin(String pin)
    {
        return account.getPinHash().equals(Integer.toString(pin.hashCode()));
    }

    void debit(double amount)
    {
        if(account.getBalance()<amount)
            throw new IllegalStateException("Amount exceeds with your balance in account "+account.getBalance());
        System.out.println("Amount debited from account "+amount);
        account.setBalance(account.getBalance()-amount);
    }

    void clearSession()
    {
        this.account=null;
        pinCount=0;
    }
}
interface ATMState{
    void insertCard(ATM atm);
    void enterPin(ATM atm, String pin);
    void withdraw(ATM atm, double amount);
    void ejectCard(ATM atm);
}

class IdleAtmState implements ATMState{

    @Override
    public void insertCard(ATM atm) {
        System.out.println("Card Inserted Successfully");
        atm.setAtmState(new InsertedCardState());
    }

    @Override
    public void enterPin(ATM atm, String pin) {
        // ignore pin can not be entered here
        throw  new IllegalStateException("Enter card First");
    }

    @Override
    public void withdraw(ATM atm, double amount) {
        throw  new IllegalStateException("Enter card First");
    }

    @Override
    public void ejectCard(ATM atm) {
        throw  new IllegalStateException("Enter card First");
    }
}

class InsertedCardState implements ATMState {

    @Override
    public void insertCard(ATM atm) {
        throw  new IllegalStateException("Card already Inserted");
    }

    @Override
    public void enterPin(ATM atm, String pin) {

        //Create Enum with Account State and return that from Account service and here change state accordingly
        if(atm.getAccountService().validatePin(pin))
            atm.setAtmState(new ProcessingState());
        else {
            atm.getAccountService().setPinCount(atm.getAccountService().getPinCount()+1);
            if(atm.getAccountService().getPinCount()==3) {

                atm.setAtmState(new EjectCardState());
                atm.getAtmState().ejectCard(atm);
                throw new IllegalStateException("Pin Incorrect 3 times");
            }
            else System.out.println("Re enter correct pin, Incorrect Attempt "+atm.getAccountService().getPinCount());
        }
    }

    @Override
    public void withdraw(ATM atm, double amount) {
        throw  new IllegalStateException("Please validate pin first");
    }

    @Override
    public void ejectCard(ATM atm) {
        throw new IllegalStateException("Card can not be ejected in this step");
    }
}

class ProcessingState implements ATMState{

    @Override
    public void insertCard(ATM atm) {

    }

    @Override
    public void enterPin(ATM atm, String pin) {

    }

    @Override
    public void withdraw(ATM atm, double amount) {
        try{
            atm.accountService.debit(amount);
        }catch (IllegalStateException exc)
        {
            System.out.println("Please enter Amount less than your balance");
        }

        atm.setAtmState(new EjectCardState());

    }

    @Override
    public void ejectCard(ATM atm) {

    }
}

class EjectCardState implements  ATMState{

    @Override
    public void insertCard(ATM atm) {

    }

    @Override
    public void enterPin(ATM atm, String pin) {

    }

    @Override
    public void withdraw(ATM atm, double amount) {

    }

    @Override
    public void ejectCard(ATM atm) {
        System.out.println("Card Ejected");
        atm.accountService.clearSession();
        atm.setAtmState(new IdleAtmState());
    }
}
@Data
@AllArgsConstructor
@NoArgsConstructor
class ATM{
    ATMState atmState;
    AccountService accountService;
    AccountRepository accountRepository;
    ATM(AccountRepository accountRepository)
    {
        this.accountService=new AccountService();
        this.accountRepository=accountRepository;
    }
    public void insertCard() {
        atmState.insertCard(this);
    }


    public void enterPin(String pin) {
        atmState.enterPin(this,pin);
    }


    public void withdraw(double amount) {
        atmState.withdraw(this,amount);
    }

    public void ejectCard() {
        atmState.ejectCard(this);
    }


    void readCard(String accountnumber)
    {
        //will fetch account details from DB
        BankAccount account= this.accountRepository.findByAccountNumber(accountnumber);
        this.accountService.startSession(this, account);
        this.atmState=new InsertedCardState();
    }
}
interface AccountRepository {
    BankAccount findByAccountNumber(String accountNumber);

}
class InMemoryAccountRepository implements AccountRepository{

    @Override
    public BankAccount findByAccountNumber(String accountNumber) {
        String pin = Integer.toString("1234".hashCode());
        return new BankAccount("123",1000, pin);
    }
}
public class ATMSystem {
    public static void main(String[] args)
    {
        ATM atm=new ATM(new InMemoryAccountRepository());
        atm.readCard("123");

        atm.enterPin("1234");
        atm.withdraw(500);
        System.out.println("Current Balance " +atm.accountService.getAccount().getBalance());
        atm.ejectCard();


    }
}
